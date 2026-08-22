/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { CopilotSession } from "../src/session.js";
import type { SessionEvent } from "../src/types.js";

/**
 * Builds a session event with an arbitrary `type` and `data` payload. The shape
 * is cast to {@link SessionEvent} so tests can exercise `searchMessages` against
 * realistic fixtures without reconstructing every field of each event variant.
 */
function event(type: string, data: unknown, id: string): SessionEvent {
    return {
        type,
        id,
        parentId: null,
        timestamp: "2026-08-23T00:00:00.000Z",
        data,
    } as unknown as SessionEvent;
}

/** History fixture covering assistant/user/error events, nested data, and a string-free event. */
function historyFixture(): SessionEvent[] {
    return [
        event(
            "user.message",
            { content: "Please add user Authentication to the login flow" },
            "u1"
        ),
        event(
            "assistant.message",
            { content: "I'll implement authentication using JWT tokens." },
            "a1"
        ),
        event("user.message", { content: "Now DEPLOY it to staging" }, "u2"),
        event("assistant.message", { content: "Deploying to staging now." }, "a2"),
        event(
            "session.error",
            { errorType: "notification", message: "Connection refused: timed out after 30s" },
            "e1"
        ),
        // Nested string values (an attachment's displayName) must be searchable.
        event(
            "user.message",
            {
                content: "See attached",
                attachments: [{ type: "file", path: "/tmp/x", displayName: "deploy-config.yaml" }],
            },
            "t1"
        ),
        // No string values anywhere in `data`.
        event("assistant.usage", { inputTokens: 10, outputTokens: 20, ok: true }, "n1"),
    ];
}

/**
 * Creates a session backed by a mock connection that answers `session.getMessages`
 * with the given events. Returns the session and a spy on the connection's
 * `sendRequest` so tests can assert how history is fetched.
 */
function sessionWithHistory(events: SessionEvent[]): {
    session: CopilotSession;
    sendRequest: ReturnType<typeof vi.fn>;
} {
    const sendRequest = vi.fn((method: string) => {
        if (method === "session.getMessages") {
            return Promise.resolve({ events });
        }
        throw new Error(`unexpected request: ${method}`);
    });
    const connection = { sendRequest } as unknown as MessageConnection;
    return { session: new CopilotSession("session-search", connection), sendRequest };
}

/** Convenience: run a search and return only the matching event ids, in order. */
async function searchIds(
    session: CopilotSession,
    query: string | RegExp,
    options?: Parameters<CopilotSession["searchMessages"]>[1]
): Promise<string[]> {
    const results = await session.searchMessages(query, options);
    return results.map((e) => e.id);
}

describe("searchMessages", () => {
    it("matches string queries case-insensitively by default", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, "authentication")).toEqual(["u1", "a1"]);
    });

    it("honors caseSensitive: true for string queries", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // Capitalized in u1 ("Authentication"), lowercase in a1 ("authentication").
        expect(await searchIds(session, "Authentication", { caseSensitive: true })).toEqual(["u1"]);
        expect(await searchIds(session, "authentication", { caseSensitive: true })).toEqual(["a1"]);
    });

    it("treats caseSensitive: false the same as the default", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, "AUTHENTICATION", { caseSensitive: false })).toEqual([
            "u1",
            "a1",
        ]);
    });

    it("filters by eventType", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // "deploy" appears in u2, a2, and t1 (nested), but only the assistant one is kept.
        expect(await searchIds(session, "deploy", { eventType: "assistant.message" })).toEqual([
            "a2",
        ]);
    });

    it("combines eventType with the text query", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // Both user.message events mention deploy: u2 directly, t1 via a nested attachment name.
        expect(await searchIds(session, "deploy", { eventType: "user.message" })).toEqual([
            "u2",
            "t1",
        ]);
    });

    it("returns an empty array when the eventType matches nothing", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(
            await searchIds(session, "deploy", { eventType: "tool.execution_complete" })
        ).toEqual([]);
    });

    it("matches RegExp queries", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, /jwt|refused/i)).toEqual(["a1", "e1"]);
    });

    it("uses a RegExp's own flags for case sensitivity", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // "JWT" appears verbatim in a1; a case-sensitive pattern for "jwt" matches nothing.
        expect(await searchIds(session, /JWT/)).toEqual(["a1"]);
        expect(await searchIds(session, /jwt/)).toEqual([]);
    });

    it("ignores caseSensitive when the query is a RegExp", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // The i flag wins; the option is inert for RegExp queries.
        expect(await searchIds(session, /AUTHENTICATION/i, { caseSensitive: true })).toEqual([
            "u1",
            "a1",
        ]);
    });

    it("matches every applicable event for a global-flagged RegExp (no lastIndex leakage)", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // A stateful `g` regex reused across events would skip alternating matches;
        // all three "deploy" events must be returned regardless of the flag.
        expect(await searchIds(session, /deploy/gi)).toEqual(["u2", "a2", "t1"]);
    });

    it("searches string values nested inside data", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, "deploy-config")).toEqual(["t1"]);
    });

    it("does not match object keys or structural fields", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // "content" is a data key; the event type strings and top-level fields are not searched.
        expect(await searchIds(session, "content")).toEqual([]);
        expect(await searchIds(session, "assistant.message")).toEqual([]);
        expect(await searchIds(session, "timestamp")).toEqual([]);
    });

    it("does not stringify non-string leaves such as numbers or booleans", async () => {
        const { session } = sessionWithHistory(historyFixture());
        // n1's data is { inputTokens: 10, outputTokens: 20, ok: true } — no string values.
        expect(await searchIds(session, "true")).toEqual([]);
        expect(await searchIds(session, "10")).toEqual([]);
    });

    it("returns an empty array when nothing matches", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, "no-such-token-xyz")).toEqual([]);
    });

    it("treats an empty string query as matching every event", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, "")).toEqual(["u1", "a1", "u2", "a2", "e1", "t1", "n1"]);
        // Still subject to the eventType filter.
        expect(await searchIds(session, "", { eventType: "assistant.message" })).toEqual([
            "a1",
            "a2",
        ]);
    });

    it("preserves history order in the results", async () => {
        const { session } = sessionWithHistory(historyFixture());
        expect(await searchIds(session, "staging")).toEqual(["u2", "a2"]);
    });

    it("returns the original SessionEvent objects unchanged", async () => {
        const events = historyFixture();
        const { session } = sessionWithHistory(events);
        const results = await session.searchMessages("JWT tokens");
        expect(results).toHaveLength(1);
        // The returned event is the very object from history, not a copy.
        expect(results[0]).toBe(events[1]);
    });

    it("does not mutate a RegExp passed by the caller", async () => {
        const { session } = sessionWithHistory(historyFixture());
        const query = /deploy/g;
        await session.searchMessages(query);
        // A shared global regex would have advanced lastIndex; ours must not touch it.
        expect(query.lastIndex).toBe(0);
    });

    it("fetches history via a single session.getMessages request per call", async () => {
        const { session, sendRequest } = sessionWithHistory(historyFixture());
        await session.searchMessages("authentication");
        expect(sendRequest).toHaveBeenCalledTimes(1);
        expect(sendRequest).toHaveBeenCalledWith("session.getMessages", {
            sessionId: "session-search",
        });
    });

    it("returns an empty array when the session has no history", async () => {
        const { session } = sessionWithHistory([]);
        expect(await session.searchMessages("anything")).toEqual([]);
    });
});
