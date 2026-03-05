/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient, SessionEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext, isCI } from "./harness/sdkTestContext";

describe("Streaming Fidelity", async () => {
    const { copilotClient: client, env } = await createSdkTestContext();

    it("should produce delta events when streaming is enabled", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            streaming: true,
        });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        await session.sendAndWait({
            prompt: "Count from 1 to 5, separated by commas.",
        });

        const types = events.map((e) => e.type);

        // Should have streaming deltas before the final message
        const deltaEvents = events.filter((e) => e.type === "assistant.message_delta");
        expect(deltaEvents.length).toBeGreaterThanOrEqual(1);

        // Deltas should have content
        for (const delta of deltaEvents) {
            expect(delta.data.deltaContent).toBeDefined();
            expect(typeof delta.data.deltaContent).toBe("string");
        }

        // Should still have a final assistant.message
        expect(types).toContain("assistant.message");

        // Deltas should come before the final message
        const firstDeltaIdx = types.indexOf("assistant.message_delta");
        const lastAssistantIdx = types.lastIndexOf("assistant.message");
        expect(firstDeltaIdx).toBeLessThan(lastAssistantIdx);

        await session.destroy();
    });

    it("should not produce deltas when streaming is disabled", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            streaming: false,
        });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        await session.sendAndWait({
            prompt: "Say 'hello world'.",
        });

        const deltaEvents = events.filter((e) => e.type === "assistant.message_delta");

        // No deltas when streaming is off
        expect(deltaEvents.length).toBe(0);

        // But should still have a final assistant.message
        const assistantEvents = events.filter((e) => e.type === "assistant.message");
        expect(assistantEvents.length).toBeGreaterThanOrEqual(1);

        await session.destroy();
    });

    it("should produce deltas after session resume", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            streaming: false,
        });
        await session.sendAndWait({ prompt: "What is 3 + 6?" });
        await session.destroy();

        // Resume using a new client
        const newClient = new CopilotClient({
            env,
            githubToken: isCI ? "fake-token-for-e2e-tests" : undefined,
        });
        onTestFinished(() => newClient.forceStop());
        const session2 = await newClient.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            streaming: true,
        });
        const events: SessionEvent[] = [];
        session2.on((event) => events.push(event));

        const secondAssistantMessage = await session2.sendAndWait({
            prompt: "Now if you double that, what do you get?",
        });
        expect(secondAssistantMessage?.data.content).toContain("18");

        // Should have streaming deltas before the final message
        const deltaEvents = events.filter((e) => e.type === "assistant.message_delta");
        expect(deltaEvents.length).toBeGreaterThanOrEqual(1);

        // Deltas should have content
        for (const delta of deltaEvents) {
            expect(delta.data.deltaContent).toBeDefined();
            expect(typeof delta.data.deltaContent).toBe("string");
        }

        await session2.destroy();
    });
});
