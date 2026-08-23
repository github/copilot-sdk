/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { CopilotSession } from "../src/session.js";
import type { SessionEvent } from "../src/types.js";

function event(type: SessionEvent["type"], data: Record<string, unknown>): SessionEvent {
    return {
        type,
        id: crypto.randomUUID(),
        parentId: null,
        timestamp: new Date().toISOString(),
        data,
    } as SessionEvent;
}

function createSession(events: SessionEvent[]): {
    session: CopilotSession;
    sendRequest: ReturnType<typeof vi.fn>;
} {
    const sendRequest = vi.fn().mockResolvedValue({ events });
    const connection = { sendRequest } as unknown as MessageConnection;
    return { session: new CopilotSession("session-1", connection), sendRequest };
}

describe("searchMessages", () => {
    const events = [
        event("user.message", { content: "Configure Authentication" }),
        event("session.error", { message: "authentication failed" }),
        event("assistant.message", { content: "Authentication is configured" }),
        event("assistant.message", { content: "Deployment complete" }),
    ];

    it("searches only conversational content case-insensitively and preserves order", async () => {
        const { session, sendRequest } = createSession(events);

        const results = await session.searchMessages("authentication");

        expect(results).toEqual([events[0], events[2]]);
        expect(sendRequest).toHaveBeenCalledWith("session.getMessages", {
            sessionId: "session-1",
        });
    });

    it("supports case-sensitive matching and message type filtering", async () => {
        const { session } = createSession(events);

        await expect(
            session.searchMessages("Authentication", {
                eventType: "assistant.message",
                caseSensitive: true,
            })
        ).resolves.toEqual([events[2]]);
    });

    it("uses regular expression flags without mutating stateful expressions", async () => {
        const { session } = createSession(events);
        const query = /authentication/gi;
        query.lastIndex = 4;

        await expect(session.searchMessages(query)).resolves.toEqual([events[0], events[2]]);
        expect(query.lastIndex).toBe(4);
    });
});
