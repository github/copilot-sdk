/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { SessionEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Event Fidelity", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should emit events in correct order for tool-using conversation", async () => {
        await writeFile(join(workDir, "hello.txt"), "Hello World");

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        await session.sendAndWait({
            prompt: "Read the file 'hello.txt' and tell me its contents.",
        });

        const types = events.map((e) => e.type);

        // Must have user message, tool execution, assistant message, and idle
        expect(types).toContain("user.message");
        expect(types).toContain("assistant.message");

        // user.message should come before assistant.message
        const userIdx = types.indexOf("user.message");
        const assistantIdx = types.lastIndexOf("assistant.message");
        expect(userIdx).toBeLessThan(assistantIdx);

        // session.idle should be last
        const idleIdx = types.lastIndexOf("session.idle");
        expect(idleIdx).toBe(types.length - 1);

        await session.destroy();
    });

    it("should include valid fields on all events", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        await session.sendAndWait({
            prompt: "What is 5+5? Reply with just the number.",
        });

        // All events must have id and timestamp
        for (const event of events) {
            expect(event.id).toBeDefined();
            expect(typeof event.id).toBe("string");
            expect(event.id.length).toBeGreaterThan(0);

            expect(event.timestamp).toBeDefined();
            expect(typeof event.timestamp).toBe("string");
        }

        // user.message should have content
        const userEvent = events.find((e) => e.type === "user.message");
        expect(userEvent).toBeDefined();
        expect(userEvent?.data.content).toBeDefined();

        // assistant.message should have messageId and content
        const assistantEvent = events.find((e) => e.type === "assistant.message");
        expect(assistantEvent).toBeDefined();
        expect(assistantEvent?.data.messageId).toBeDefined();
        expect(assistantEvent?.data.content).toBeDefined();

        await session.destroy();
    });

    it("should emit tool execution events with correct fields", async () => {
        await writeFile(join(workDir, "data.txt"), "test data");

        const session = await client.createSession({ onPermissionRequest: approveAll });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        await session.sendAndWait({
            prompt: "Read the file 'data.txt'.",
        });

        // Should have tool.execution_start and tool.execution_complete
        const toolStarts = events.filter((e) => e.type === "tool.execution_start");
        const toolCompletes = events.filter((e) => e.type === "tool.execution_complete");

        expect(toolStarts.length).toBeGreaterThanOrEqual(1);
        expect(toolCompletes.length).toBeGreaterThanOrEqual(1);

        // Tool start should have toolCallId and toolName
        const firstStart = toolStarts[0]!;
        expect(firstStart.data.toolCallId).toBeDefined();
        expect(firstStart.data.toolName).toBeDefined();

        // Tool complete should have toolCallId
        const firstComplete = toolCompletes[0]!;
        expect(firstComplete.data.toolCallId).toBeDefined();

        await session.destroy();
    });

    it("should emit assistant.message with messageId", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        await session.sendAndWait({
            prompt: "Say 'pong'.",
        });

        const assistantEvents = events.filter((e) => e.type === "assistant.message");
        expect(assistantEvents.length).toBeGreaterThanOrEqual(1);

        // messageId should be present
        const msg = assistantEvents[0]!;
        expect(msg.data.messageId).toBeDefined();
        expect(typeof msg.data.messageId).toBe("string");
        expect(msg.data.content).toContain("pong");

        await session.destroy();
    });
});
