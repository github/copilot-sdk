/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { SessionEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

/**
 * Polls until predicate returns true or deadline expires. Used in lieu of arbitrary
 * `setTimeout` waits for "session flushed to disk" so fast machines exit immediately
 * and slow CI machines still get up to `timeoutMs` before the test fails.
 */
async function waitFor(
    predicate: () => Promise<boolean> | boolean,
    timeoutMs = 10_000
): Promise<void> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (await predicate()) return;
        await new Promise((r) => setTimeout(r, 50));
    }
    throw new Error(`waitFor: condition not met within ${timeoutMs}ms`);
}

describe("Session Lifecycle", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should list created sessions after sending a message", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const session2 = await client.createSession({ onPermissionRequest: approveAll });

        // Sessions must have activity to be persisted to disk
        await session1.sendAndWait({ prompt: "Say hello" });
        await session2.sendAndWait({ prompt: "Say world" });

        // Poll until both sessions are visible on disk instead of a hard 500ms wait.
        await waitFor(async () => {
            const ids = (await client.listSessions()).map((s) => s.sessionId);
            return ids.includes(session1.sessionId) && ids.includes(session2.sessionId);
        });

        const sessions = await client.listSessions();
        const sessionIds = sessions.map((s) => s.sessionId);

        expect(sessionIds).toContain(session1.sessionId);
        expect(sessionIds).toContain(session2.sessionId);

        await session1.disconnect();
        await session2.disconnect();
    });

    it("should delete session permanently", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session.sessionId;

        // Send a message so the session is persisted
        await session.sendAndWait({ prompt: "Say hi" });

        // Poll until the session is visible on disk instead of a hard 500ms wait.
        await waitFor(async () => {
            const ids = (await client.listSessions()).map((s) => s.sessionId);
            return ids.includes(sessionId);
        });

        // Verify it appears in the list
        const before = await client.listSessions();
        expect(before.map((s) => s.sessionId)).toContain(sessionId);

        await session.disconnect();
        await client.deleteSession(sessionId);

        // After delete, the session should not be in the list
        const after = await client.listSessions();
        expect(after.map((s) => s.sessionId)).not.toContain(sessionId);
    });

    it("should return events via getMessages after conversation", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({
            prompt: "What is 2+2? Reply with just the number.",
        });

        const messages = await session.getMessages();
        expect(messages.length).toBeGreaterThan(0);

        // Should have at least session.start, user.message, assistant.message, session.idle
        const types = messages.map((m: SessionEvent) => m.type);
        expect(types).toContain("session.start");
        expect(types).toContain("user.message");
        expect(types).toContain("assistant.message");

        await session.disconnect();
    });

    it("should support multiple concurrent sessions", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const session2 = await client.createSession({ onPermissionRequest: approveAll });

        // Send to both sessions
        const [msg1, msg2] = await Promise.all([
            session1.sendAndWait({ prompt: "What is 1+1? Reply with just the number." }),
            session2.sendAndWait({ prompt: "What is 3+3? Reply with just the number." }),
        ]);

        expect(msg1?.data.content).toContain("2");
        expect(msg2?.data.content).toContain("6");

        await session1.disconnect();
        await session2.disconnect();
    });
});
