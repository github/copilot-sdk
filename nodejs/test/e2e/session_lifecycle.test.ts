/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Session Lifecycle", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should list created sessions after sending a message", async () => {
        const session1 = await client.createSession();
        const session2 = await client.createSession();

        // Sessions must have activity to be persisted to disk
        await session1.sendAndWait({ prompt: "Say hello" });
        await session2.sendAndWait({ prompt: "Say world" });

        // Wait for session data to flush to disk
        await new Promise((r) => setTimeout(r, 500));

        const sessions = await client.listSessions();
        const sessionIds = sessions.map((s) => s.sessionId);

        expect(sessionIds).toContain(session1.sessionId);
        expect(sessionIds).toContain(session2.sessionId);

        await session1.destroy();
        await session2.destroy();
    });

    it("should delete session permanently", async () => {
        const session = await client.createSession();
        const sessionId = session.sessionId;

        // Send a message so the session is persisted
        await session.sendAndWait({ prompt: "Say hi" });

        // Wait for session data to flush to disk
        await new Promise((r) => setTimeout(r, 500));

        // Verify it appears in the list
        const before = await client.listSessions();
        expect(before.map((s) => s.sessionId)).toContain(sessionId);

        await session.destroy();
        await client.deleteSession(sessionId);

        // After delete, the session should not be in the list
        const after = await client.listSessions();
        expect(after.map((s) => s.sessionId)).not.toContain(sessionId);
    });

    it("should return events via getMessages after conversation", async () => {
        const session = await client.createSession();

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

        await session.destroy();
    });

    it("should support multiple concurrent sessions", async () => {
        const session1 = await client.createSession();
        const session2 = await client.createSession();

        // Send to both sessions
        const [msg1, msg2] = await Promise.all([
            session1.sendAndWait({ prompt: "What is 1+1? Reply with just the number." }),
            session2.sendAndWait({ prompt: "What is 3+3? Reply with just the number." }),
        ]);

        expect(msg1?.data.content).toContain("2");
        expect(msg2?.data.content).toContain("6");

        await session1.destroy();
        await session2.destroy();
    });
});
