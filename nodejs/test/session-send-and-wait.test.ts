/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { CopilotSession } from "../src/session.js";
import type { SessionEvent } from "../src/generated/session-events.js";

function sessionEvent(type: "session.idle", data: Record<string, never> = {}): SessionEvent {
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

function shutdownEvent(errorReason?: string): SessionEvent {
    return {
        type: "session.shutdown",
        id: "00000000-0000-4000-8000-000000000002",
        parentId: null,
        timestamp: new Date().toISOString(),
        data: {
            codeChanges: { filesModified: [], linesAdded: 0, linesRemoved: 0 },
            errorReason,
            modelMetrics: {},
            sessionStartTime: Date.now(),
            shutdownType: errorReason ? "error" : "routine",
            totalApiDurationMs: 0,
        },
    } as SessionEvent;
}

function controlledSession(): {
    session: CopilotSession;
    sendStarted: Promise<void>;
    resolveSend: () => void;
    rejectSend: (error: Error) => void;
} {
    let resolveSendRequest: ((value: unknown) => void) | undefined;
    let rejectSendRequest: ((error: Error) => void) | undefined;
    let markSendStarted: () => void;
    const sendStarted = new Promise<void>((resolve) => {
        markSendStarted = resolve;
    });
    const connection = {
        sendRequest: () =>
            new Promise((resolve, reject) => {
                resolveSendRequest = resolve;
                rejectSendRequest = reject;
                markSendStarted();
            }),
    } as unknown as MessageConnection;

    return {
        session: new CopilotSession("session-1", connection),
        sendStarted,
        resolveSend: () => resolveSendRequest?.({ messageId: "msg-1" }),
        rejectSend: (error) => rejectSendRequest?.(error),
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
        session._dispatchEvent(shutdownEvent("later shutdown"));

        const stateBeforeSend = await Promise.race([
            pending.then(() => "settled"),
            new Promise<"pending">((resolve) => setTimeout(() => resolve("pending"), 0)),
        ]);
        expect(stateBeforeSend).toBe("pending");

        resolveSend();
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

    it("rejects when the session shuts down before becoming idle", async () => {
        const { session, sendStarted } = controlledSession();
        const pending = session.sendAndWait({ prompt: "hi" });
        await sendStarted;

        session._dispatchEvent(shutdownEvent("runtime session was lost"));

        await expect(pending).rejects.toThrow(
            "Session session-1 shut down before becoming idle: runtime session was lost"
        );
    });
});
