/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { CopilotSession } from "../src/session.js";
import type { SessionEvent } from "../src/generated/session-events.js";

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

describe("sendAndWait", () => {
    it("does not emit an unhandled rejection when session.error arrives before the idle race is armed", async () => {
        // Hold the `session.send` RPC open so the test can dispatch an event in the
        // window between the event listener being registered and Promise.race
        // attaching the first consumer to the internal idle promise.
        let resolveSend: ((value: unknown) => void) | undefined;
        const connection = {
            sendRequest: () =>
                new Promise((resolve) => {
                    resolveSend = resolve;
                }),
        } as unknown as MessageConnection;

        const session = new CopilotSession("session-1", connection);

        const unhandled: unknown[] = [];
        const onUnhandled = (reason: unknown): void => {
            unhandled.push(reason);
        };
        process.on("unhandledRejection", onUnhandled);
        onTestFinished(() => {
            process.off("unhandledRejection", onUnhandled);
        });

        const pending = session.sendAndWait({ prompt: "hi" });

        // A session.error lands while send()'s RPC is still in flight. This is
        // ordinary traffic: a joined client calling session.log(…, { level: "error" })
        // or an MCP server failing to start both produce one.
        session._dispatchEvent(errorEvent("MCP server failed to start"));

        // Yield past a macrotask boundary so Node has run the checkpoint at which
        // it classifies a rejection as unhandled.
        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(unhandled).toEqual([]);

        resolveSend?.({ messageId: "msg-1" });
        await expect(pending).rejects.toThrow("MCP server failed to start");
    });
});
