/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterEach, describe, expect, expectTypeOf, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { z } from "zod";
import { CopilotSession } from "../src/session.js";
import type { SessionEvent } from "../src/generated/session-events.js";
import type { MessageOptions } from "../src/types.js";

const answer = z.object({ answer: z.number().int() });

function event(type: SessionEvent["type"], data: unknown, agentId?: string): SessionEvent {
    return {
        type,
        data,
        agentId,
        id: crypto.randomUUID(),
        timestamp: new Date().toISOString(),
        parentId: null,
    } as SessionEvent;
}

function user(messageId: string): SessionEvent {
    return event("user.message", { messageId, content: "question", turnId: "0" });
}

function assistant(originatingMessageId: string, content: string, agentId?: string): SessionEvent {
    return event(
        "assistant.message",
        { messageId: crypto.randomUUID(), originatingMessageId, content, turnId: "1" },
        agentId
    );
}

function controlledSession() {
    const sends: Array<{
        params: Record<string, unknown>;
        resolve: (value: { messageId: string }) => void;
        reject: (error: Error) => void;
    }> = [];
    const sendRequest = vi.fn((_method: string, params: Record<string, unknown>) => {
        return new Promise<{ messageId: string }>((resolve, reject) => {
            sends.push({ params, resolve, reject });
        });
    });
    const session = new CopilotSession("session", { sendRequest } as unknown as MessageConnection);
    return { session, sends, sendRequest };
}

async function sent(sends: unknown[], count = 1) {
    await vi.waitFor(() => expect(sends).toHaveLength(count));
}

describe("structured output", () => {
    afterEach(() => vi.useRealTimers());

    it("infers TResult from a Zod schema and forwards its JSON Schema", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("What is 19 + 23?", answer);
        expectTypeOf(pending).toEqualTypeOf<Promise<{ answer: number }>>();
        await sent(sends);
        expect(sends[0].params.responseFormat).toEqual({
            type: "json_schema",
            jsonSchema: { name: "response", strict: true, schema: answer.toJSONSchema() },
        });
        session._dispatchEvent(user("one"));
        session._dispatchEvent(assistant("one", '{"answer":42}'));
        session._dispatchEvent(event("session.idle", {}));
        sends[0].resolve({ messageId: "one" });
        await expect(pending).resolves.toEqual({ answer: 42 });
    });

    it("keeps raw schema sends and options-based Zod sends event-shaped", async () => {
        for (const schema of [answer.toJSONSchema(), answer]) {
            const { session, sends } = controlledSession();
            const options: MessageOptions = { prompt: "question", responseSchema: schema };
            const pending = session.sendAndWait(options);
            await sent(sends);
            const final = assistant("one", '{"answer":42}');
            sends[0].resolve({ messageId: "one" });
            session._dispatchEvent(user("one"));
            session._dispatchEvent(final);
            session._dispatchEvent(event("session.idle", {}));
            await expect(pending).resolves.toEqual(final);
        }
    });

    it("isolates queued concurrent sends and excludes subagent messages", async () => {
        const { session, sends } = controlledSession();
        const first = session.sendAndWait("first", answer);
        const second = session.sendAndWait("second", answer);
        await sent(sends, 2);
        session._dispatchEvent(event("session.idle", {}));
        session._dispatchEvent(user("one"));
        session._dispatchEvent(assistant("one", "intermediate tool-call text"));
        session._dispatchEvent(assistant("one", '{"answer":42}'));
        session._dispatchEvent(user("two"));
        session._dispatchEvent(assistant("two", '{"answer":37}'));
        session._dispatchEvent(assistant("one", '{"answer":999}', "subagent"));
        session._dispatchEvent(assistant("unrelated", '{"answer":123}'));
        session._dispatchEvent(event("session.idle", {}));
        sends[1].resolve({ messageId: "two" });
        sends[0].resolve({ messageId: "one" });
        await expect(first).resolves.toEqual({ answer: 42 });
        await expect(second).resolves.toEqual({ answer: 37 });
    });

    it("freezes the final message at idle even when more events precede send acknowledgement", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("question", answer);
        await sent(sends);
        session._dispatchEvent(user("one"));
        session._dispatchEvent(assistant("one", '{"answer":42}'));
        session._dispatchEvent(event("session.idle", {}));
        session._dispatchEvent(assistant("one", '{"answer":999}'));
        sends[0].resolve({ messageId: "one" });
        await expect(pending).resolves.toEqual({ answer: 42 });
    });

    it("ignores autopilot idle boundaries until a final idle", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("question", answer);
        await sent(sends);
        session._dispatchEvent(user("one"));
        session._dispatchEvent(event("session.idle", { mode: "autopilot" }));
        session._dispatchEvent(assistant("one", '{"answer":42}'));
        session._dispatchEvent(event("session.idle", {}));
        sends[0].resolve({ messageId: "one" });
        await expect(pending).resolves.toEqual({ answer: 42 });
    });

    it.each(["refusal", '{"answer":"not a number"}', "null"])(
        "rejects invalid final output: %s",
        async (content) => {
            const { session, sends } = controlledSession();
            const pending = session.sendAndWait("question", answer);
            const assertion = expect(pending).rejects.toThrow();
            await sent(sends);
            session._dispatchEvent(user("one"));
            session._dispatchEvent(assistant("one", content));
            session._dispatchEvent(event("session.idle", {}));
            sends[0].resolve({ messageId: "one" });
            await assertion;
        }
    );

    it("rejects missing or uncorrelated output rather than borrowing another message", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("question", answer);
        const assertion = expect(pending).rejects.toThrow(
            "without a structured assistant response"
        );
        await sent(sends);
        session._dispatchEvent(user("one"));
        session._dispatchEvent(assistant("two", '{"answer":42}'));
        session._dispatchEvent(event("session.idle", {}));
        sends[0].resolve({ messageId: "one" });
        await assertion;
    });

    it("rejects conflicting explicit and inferred schemas before sending", async () => {
        const { session, sendRequest } = controlledSession();
        await expect(
            session.sendAndWait({ prompt: "question", responseSchema: answer }, answer)
        ).rejects.toThrow("Do not specify responseSchema");
        expect(sendRequest).not.toHaveBeenCalled();
    });

    it("does not return a partial result after abort", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("question", answer);
        const assertion = expect(pending).rejects.toThrow("aborted");
        await sent(sends);
        session._dispatchEvent(user("one"));
        session._dispatchEvent(assistant("one", '{"answer":42}'));
        session._dispatchEvent(event("session.idle", { aborted: true }));
        sends[0].resolve({ messageId: "one" });
        await assertion;
    });

    it("does not parse a tool-call message as the final result", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("question", answer);
        const assertion = expect(pending).rejects.toThrow(
            "without a structured assistant response"
        );
        await sent(sends);
        session._dispatchEvent(user("one"));
        session._dispatchEvent(
            event("assistant.message", {
                messageId: "assistant-one",
                originatingMessageId: "one",
                content: '{"answer":42}',
                toolRequests: [{ toolCallId: "tool-one", name: "lookup" }],
            })
        );
        session._dispatchEvent(event("session.idle", {}));
        sends[0].resolve({ messageId: "one" });
        await assertion;
    });

    it("propagates send and model failures", async () => {
        const first = controlledSession();
        const sendFailure = first.session.sendAndWait("question", answer);
        const sendAssertion = expect(sendFailure).rejects.toThrow("admission failed");
        await sent(first.sends);
        first.sends[0].reject(new Error("admission failed"));
        await sendAssertion;

        const second = controlledSession();
        const modelFailure = second.session.sendAndWait("question", answer);
        const modelAssertion = expect(modelFailure).rejects.toThrow("provider rejected");
        await sent(second.sends);
        second.session._dispatchEvent(user("one"));
        second.session._dispatchEvent(
            event("session.error", { message: "provider rejected", errorType: "query" })
        );
        second.sends[0].resolve({ messageId: "one" });
        await modelAssertion;
    });

    it("times out even while send acknowledgement is pending", async () => {
        vi.useFakeTimers();
        const { session } = controlledSession();
        const pending = session.sendAndWait("question", answer, 100);
        const assertion = expect(pending).rejects.toThrow("Timeout after 100ms");
        await vi.advanceTimersByTimeAsync(100);
        await assertion;
    });

    it("rejects promptly when the session disconnects", async () => {
        const { session, sends } = controlledSession();
        const pending = session.sendAndWait("question", answer);
        const assertion = expect(pending).rejects.toThrow("Session disconnected");
        await sent(sends);
        session._markDisconnected();
        await assertion;
    });
});
