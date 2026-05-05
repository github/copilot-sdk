/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import { SessionEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Multi-turn Tool Usage", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    function snapshotAndClearEvents(events: SessionEvent[]): SessionEvent[] {
        const snapshot = [...events];
        events.length = 0;
        return snapshot;
    }

    function assertToolTurnOrdering(turnEvents: SessionEvent[], turnDescription: string): void {
        const types = turnEvents.map((e) => e.type);
        const observedTypes = types.join(", ");

        const userMsgIdx = types.indexOf("user.message");
        expect(
            userMsgIdx,
            `Expected user.message in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeGreaterThanOrEqual(0);

        const toolStarts = turnEvents
            .map((e, i) => ({ e, i }))
            .filter(({ e }) => e.type === "tool.execution_start");
        const toolCompletes = turnEvents
            .map((e, i) => ({ e, i }))
            .filter(({ e }) => e.type === "tool.execution_complete");

        expect(
            toolStarts.length,
            `Expected tool starts in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeGreaterThan(0);
        expect(
            toolCompletes.length,
            `Expected tool completes in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeGreaterThan(0);

        const firstToolStartIdx = Math.min(...toolStarts.map(({ i }) => i));
        expect(
            userMsgIdx,
            `Expected user.message before first tool start in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeLessThan(firstToolStartIdx);

        for (const { e: complete, i: completeIdx } of toolCompletes) {
            const matchingStart = toolStarts.find(
                ({ e: start, i: startIdx }) =>
                    start.data.toolCallId === complete.data.toolCallId && startIdx < completeIdx
            );
            expect(
                matchingStart,
                `Expected matching tool start for tool complete with id ${complete.data.toolCallId}`
            ).toBeDefined();
        }

        const lastToolCompleteIdx = Math.max(...toolCompletes.map(({ i }) => i));
        let assistantAfterToolsIdx = -1;
        for (let i = lastToolCompleteIdx + 1; i < turnEvents.length; i++) {
            if (turnEvents[i]!.type === "assistant.message") {
                assistantAfterToolsIdx = i;
                break;
            }
        }

        let sessionIdleIdx = -1;
        const searchFrom = assistantAfterToolsIdx >= 0 ? assistantAfterToolsIdx + 1 : 0;
        for (let i = searchFrom; i < turnEvents.length; i++) {
            if (turnEvents[i]!.type === "session.idle") {
                sessionIdleIdx = i;
                break;
            }
        }

        expect(
            assistantAfterToolsIdx,
            `Expected assistant.message after tool completion in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeGreaterThanOrEqual(0);
        expect(
            sessionIdleIdx,
            `Expected session.idle after assistant.message in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeGreaterThanOrEqual(0);
        expect(
            lastToolCompleteIdx,
            `Expected final tool completion before final assistant message in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeLessThan(assistantAfterToolsIdx);
        expect(
            assistantAfterToolsIdx,
            `Expected final assistant message before idle in ${turnDescription}. Observed: ${observedTypes}`
        ).toBeLessThan(sessionIdleIdx);
    }

    it("should use tool results from previous turns", async () => {
        // Write a file, then ask the model to read it and reason about its content
        await writeFile(join(workDir, "secret.txt"), "The magic number is 42.");
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        const msg1 = await session.sendAndWait({
            prompt: "Read the file 'secret.txt' and tell me what the magic number is.",
        });
        expect(msg1?.data.content).toContain("42");
        assertToolTurnOrdering(snapshotAndClearEvents(events), "file read turn");

        // Follow-up that requires context from the previous turn
        const msg2 = await session.sendAndWait({
            prompt: "What is that magic number multiplied by 2?",
        });
        expect(msg2?.data.content).toContain("84");
    });

    it("should handle file creation then reading across turns", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        // First turn: create a file
        await session.sendAndWait({
            prompt: "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'.",
        });

        // Verify file was created with correct content before checking ordering
        const { readFile } = await import("fs/promises");
        const createdContent = await readFile(join(workDir, "greeting.txt"), "utf-8");
        expect(createdContent).toBe("Hello from multi-turn test");
        assertToolTurnOrdering(snapshotAndClearEvents(events), "file creation turn");

        // Second turn: read the file
        const msg = await session.sendAndWait({
            prompt: "Read the file 'greeting.txt' and tell me its exact contents.",
        });
        expect(msg?.data.content).toContain("Hello from multi-turn test");
        assertToolTurnOrdering(snapshotAndClearEvents(events), "file read turn");
    });
});
