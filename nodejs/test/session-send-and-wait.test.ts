/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { CopilotSession } from "../src/session.js";
import type { AssistantMessageEvent, SessionEvent } from "../src/generated/session-events.js";
import { withFinalAssistantMessage } from "./e2e/harness/sdkTestHelper.js";

function sessionEvent(
    type: "session.idle",
    data: { mode?: "interactive" | "plan" | "autopilot" } = {}
): SessionEvent {
    return {
        type,
        id: "00000000-0000-4000-8000-000000000001",
        parentId: null,
        timestamp: new Date().toISOString(),
        ephemeral: true,
        data,
    } as SessionEvent;
}

/** Builds a `session.error` event, the shape `session.log(…, { level: "error" })` produces. */
function errorEvent(message: string): SessionEvent {
    return {
        type: "session.error",
        id: "00000000-0000-4000-8000-000000000001",
        parentId: null,
        timestamp: new Date().toISOString(),
        data: { errorType: "notification", message },
    } as SessionEvent;
}

function assistantMessage(content: string): AssistantMessageEvent {
    return {
        type: "assistant.message",
        id: "assistant-1",
        parentId: null,
        timestamp: new Date().toISOString(),
        data: { messageId: "message-1", content },
    };
}

function controlledSession(
    history: SessionEvent[] = [],
    onSend?: (session: CopilotSession) => void
) {
    let resolveSendRequest: ((value: unknown) => void) | undefined;
    let rejectSendRequest: ((error: Error) => void) | undefined;
    let markSendStarted: () => void;
    const sendStarted = new Promise<void>((resolve) => {
        markSendStarted = resolve;
    });
    const sendRequest = vi.fn((method: string) => {
        if (method === "session.getMessages") {
            return Promise.resolve({ events: history });
        }
        if (method !== "session.send") {
            throw new Error(`Unexpected RPC: ${method}`);
        }
        return new Promise((resolve, reject) => {
            resolveSendRequest = resolve;
            rejectSendRequest = reject;
            markSendStarted();
            onSend?.(session);
        });
    });
    const connection = { sendRequest } as unknown as MessageConnection;
    const session = new CopilotSession("session-1", connection);

    return {
        session,
        sendRequest,
        sendStarted,
        resolveSend: () => resolveSendRequest?.({ messageId: "msg-1" }),
        rejectSend: (error: Error) => rejectSendRequest?.(error),
    };
}

describe("sendAndWait", () => {
    it("does not emit an unhandled rejection when session.error arrives before the idle race is armed", async () => {
        const { session, sendStarted, resolveSend } = controlledSession();

        const unhandled: unknown[] = [];
        const onUnhandled = (reason: unknown): void => {
            unhandled.push(reason);
        };
        process.on("unhandledRejection", onUnhandled);
        onTestFinished(() => {
            process.off("unhandledRejection", onUnhandled);
        });

        const pending = session.sendAndWait({ prompt: "hi" });
        await sendStarted;

        // A session.error lands while send()'s RPC is still in flight. This is
        // ordinary traffic: a joined client calling session.log(…, { level: "error" })
        // or an MCP server failing to start both produce one.
        session._dispatchEvent(errorEvent("MCP server failed to start"));

        // Yield past a macrotask boundary so Node has run the checkpoint at which
        // it classifies a rejection as unhandled.
        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(unhandled).toEqual([]);

        resolveSend();
        await expect(pending).rejects.toThrow("MCP server failed to start");
    });

    it("preserves an early idle event until send completes", async () => {
        const { session, sendStarted, resolveSend } = controlledSession();
        const pending = session.sendAndWait({ prompt: "hi" });
        await sendStarted;

        session._dispatchEvent(sessionEvent("session.idle"));

        const stateBeforeSend = await Promise.race([
            pending.then(() => "settled"),
            new Promise<"pending">((resolve) => setTimeout(() => resolve("pending"), 0)),
        ]);
        expect(stateBeforeSend).toBe("pending");

        resolveSend();
        await expect(pending).resolves.toBeUndefined();
    });

    it("ignores autopilot continuation idle events", async () => {
        const { session, sendStarted, resolveSend } = controlledSession();
        const pending = session.sendAndWait({ prompt: "hi" });
        await sendStarted;

        session._dispatchEvent(sessionEvent("session.idle", { mode: "autopilot" }));
        resolveSend();

        const stateAfterContinuation = await Promise.race([
            pending.then(() => "settled"),
            new Promise<"pending">((resolve) => setTimeout(() => resolve("pending"), 0)),
        ]);
        expect(stateAfterContinuation).toBe("pending");

        session._dispatchEvent(sessionEvent("session.idle", { mode: "interactive" }));
        await expect(pending).resolves.toBeUndefined();
    });

    it("ignores child messages, errors, and idle while waiting for the root", async () => {
        const { session, sendStarted, resolveSend } = controlledSession();
        const received: SessionEvent[] = [];
        session.on((event) => received.push(event));
        const pending = session.sendAndWait({ prompt: "delegate" });
        await sendStarted;
        resolveSend();

        const childMessage = { ...assistantMessage("child reply"), agentId: "child-1" };
        const childError = { ...errorEvent("child failed"), agentId: "child-1" };
        const childIdle = { ...sessionEvent("session.idle"), agentId: "child-1" };
        session._dispatchEvent(childMessage);
        session._dispatchEvent(childError);
        session._dispatchEvent(childIdle);
        expect(received).toEqual([childMessage, childError, childIdle]);

        session._dispatchEvent(sessionEvent("session.idle"));
        await expect(pending).resolves.toBeUndefined();
    });

    it("preserves the send rejection when a session error arrives first", async () => {
        const { session, sendStarted, rejectSend } = controlledSession();
        const pending = session.sendAndWait({ prompt: "hi" });
        await sendStarted;

        session._dispatchEvent(errorEvent("session error"));
        rejectSend(new Error("send failed"));

        await expect(pending).rejects.toThrow("send failed");
    });

    it("uses the first session outcome observed while send is in flight", async () => {
        const idleFirst = controlledSession();
        const idleFirstPending = idleFirst.session.sendAndWait({ prompt: "hi" });
        await idleFirst.sendStarted;
        idleFirst.session._dispatchEvent(sessionEvent("session.idle"));
        idleFirst.session._dispatchEvent(errorEvent("later error"));
        idleFirst.resolveSend();
        await expect(idleFirstPending).resolves.toBeUndefined();

        const errorFirst = controlledSession();
        const errorFirstPending = errorFirst.session.sendAndWait({ prompt: "hi" });
        await errorFirst.sendStarted;
        errorFirst.session._dispatchEvent(errorEvent("first error"));
        errorFirst.session._dispatchEvent(sessionEvent("session.idle"));
        errorFirst.resolveSend();
        await expect(errorFirstPending).rejects.toThrow("first error");
    });
});

describe("completion subscriptions", () => {
    it.each(["sendAndWait", "withFinalAssistantMessage"] as const)(
        "%s captures final output and ephemeral idle before the send reply",
        async (waiter) => {
            const finalMessage = assistantMessage("done");
            const history: SessionEvent[] = [];
            const { session, sendRequest, sendStarted, resolveSend } = controlledSession(
                history,
                (session) => {
                    const events = [
                        assistantMessage("working"),
                        finalMessage,
                        sessionEvent("session.idle"),
                    ];
                    for (const event of events) {
                        if (!event.ephemeral) {
                            history.push(event);
                        }
                        session._dispatchEvent(event);
                    }
                }
            );

            let completed = false;
            const pending =
                waiter === "sendAndWait"
                    ? session.sendAndWait({ prompt: "hi" })
                    : withFinalAssistantMessage(session, () => session.send({ prompt: "hi" }));
            const observed = pending.then((message) => {
                completed = true;
                return message;
            });
            await sendStarted;
            expect(completed).toBe(false);
            expect(sendRequest).toHaveBeenCalledTimes(1);

            // Only durable messages can be retrieved, even though idle was delivered.
            const stored = await session.getEvents();
            expect(stored).toEqual(history);
            expect(stored.map((event) => event.type)).toEqual([
                "assistant.message",
                "assistant.message",
            ]);

            resolveSend();
            await expect(observed).resolves.toBe(finalMessage);
        }
    );
});

describe("withFinalAssistantMessage", () => {
    it("does not accept a prior turn's message when the new turn has no assistant output", async () => {
        const { session, sendStarted, resolveSend } = controlledSession(
            [assistantMessage("old turn")],
            (session) => session._dispatchEvent(sessionEvent("session.idle"))
        );
        const pending = withFinalAssistantMessage(session, () => session.send({ prompt: "hi" }));
        const outcome = expect(pending).rejects.toThrow(
            "Received session.idle without a preceding assistant.message"
        );

        await sendStarted;
        resolveSend();
        await outcome;
    });

    it("does not complete on an assistant message or an autopilot continuation", async () => {
        const { session, sendStarted, resolveSend } = controlledSession();
        const pending = withFinalAssistantMessage(session, () => session.send({ prompt: "hi" }));
        await sendStarted;
        resolveSend();

        session._dispatchEvent(assistantMessage("continuing"));
        session._dispatchEvent(sessionEvent("session.idle", { mode: "autopilot" }));

        const finalMessage = assistantMessage("done");
        session._dispatchEvent(finalMessage);
        session._dispatchEvent(sessionEvent("session.idle", { mode: "interactive" }));
        await expect(pending).resolves.toBe(finalMessage);
    });

    it.each(["idle", "session.error", "send rejection", "trigger throw"] as const)(
        "removes the completion subscription after %s",
        async (outcome) => {
            const { session, sendStarted, resolveSend, rejectSend } = controlledSession();
            const originalOn = session.on.bind(session);
            const unsubscribe = vi.fn<() => void>();
            vi.spyOn(session, "on").mockImplementation((handler) => {
                unsubscribe.mockImplementation(originalOn(handler));
                return unsubscribe;
            });

            const pending = withFinalAssistantMessage(session, () => {
                if (outcome === "trigger throw") {
                    throw new Error("trigger failed");
                }
                return session.send({ prompt: "hi" });
            });
            const finalMessage = assistantMessage("done");
            const errorMessage =
                outcome === "session.error"
                    ? "session failed"
                    : outcome === "send rejection"
                      ? "send failed"
                      : "trigger failed";
            const expected =
                outcome === "idle"
                    ? expect(pending).resolves.toBe(finalMessage)
                    : expect(pending).rejects.toThrow(errorMessage);

            if (outcome !== "trigger throw") {
                await sendStarted;
            }

            if (outcome === "idle") {
                session._dispatchEvent(finalMessage);
                session._dispatchEvent(sessionEvent("session.idle"));
                // Later events must not replace an already observed terminal outcome.
                session._dispatchEvent(assistantMessage("next turn"));
                session._dispatchEvent(errorEvent("later error"));
                resolveSend();
            } else if (outcome === "session.error") {
                session._dispatchEvent(errorEvent("session failed"));
                session._dispatchEvent(sessionEvent("session.idle"));
                resolveSend();
            } else if (outcome === "send rejection") {
                session._dispatchEvent(errorEvent("session failed"));
                rejectSend(new Error("send failed"));
            }

            await expected;
            expect(unsubscribe).toHaveBeenCalled();
        }
    );
});
