/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { SessionEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Streaming Fidelity", async () => {
    const { copilotClient: client } = await createSdkTestContext();

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
});
