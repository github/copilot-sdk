/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Abort", async () => {
    const { copilotClient: client } = await createSdkTestContext();
    const TEST_TIMEOUT_MS = 120_000;

    async function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
        let timer: ReturnType<typeof setTimeout> | undefined;
        try {
            return await Promise.race([
                promise,
                new Promise<T>((_, reject) => {
                    timer = setTimeout(() => reject(new Error(`Timeout: ${label}`)), ms);
                }),
            ]);
        } finally {
            if (timer) clearTimeout(timer);
        }
    }

    it("should abort during active streaming", { timeout: TEST_TIMEOUT_MS }, async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            streaming: true,
        });

        let firstDeltaResolve!: (value: void) => void;
        const firstDeltaReceived = new Promise<void>((resolve) => {
            firstDeltaResolve = resolve;
        });

        const events: { type: string }[] = [];
        session.on((event) => {
            events.push({ type: event.type });
            if (event.type === "assistant.message_delta") {
                firstDeltaResolve();
            }
        });

        // Fire-and-forget — we'll abort before it finishes
        void session.send({
            prompt: "Write a very long essay about the history of computing, covering every decade from the 1940s to the 2020s in great detail.",
        });

        // Wait for at least one delta to arrive (proves streaming started)
        await withTimeout(firstDeltaReceived, 60_000, "first assistant.message_delta");

        const deltaEvents = events.filter((e) => e.type === "assistant.message_delta");
        expect(deltaEvents.length).toBeGreaterThanOrEqual(1);

        // Abort mid-stream
        await session.abort();

        // Session should be usable after abort — send a follow-up and get a response
        const followUp = await session.sendAndWait({
            prompt: "Say 'abort_recovery_ok'.",
        });
        expect(followUp?.data.content?.toLowerCase()).toContain("abort_recovery_ok");

        await session.disconnect();
    });

    it("should abort during active tool execution", { timeout: TEST_TIMEOUT_MS }, async () => {
        let toolStartedResolve!: (value: string) => void;
        const toolStarted = new Promise<string>((resolve) => {
            toolStartedResolve = resolve;
        });

        let releaseToolResolve!: (value: string) => void;
        const releaseTool = new Promise<string>((resolve) => {
            releaseToolResolve = resolve;
        });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("slow_analysis", {
                    description: "A slow analysis tool that blocks until released",
                    parameters: z.object({
                        value: z.string().describe("Value to analyze"),
                    }),
                    handler: async ({ value }) => {
                        toolStartedResolve(value);
                        return await releaseTool;
                    },
                }),
            ],
        });

        // Fire-and-forget
        void session.send({
            prompt: "Use slow_analysis with value 'test_abort'. Wait for the result.",
        });

        // Wait for the tool to start executing
        const toolValue = await withTimeout(toolStarted, 60_000, "slow_analysis start");
        expect(toolValue).toBe("test_abort");

        // Abort while the tool is running
        await session.abort();

        // Release the tool so its task doesn't leak
        releaseToolResolve("RELEASED_AFTER_ABORT");

        // Session should be usable after abort — verify with a follow-up
        let recoveryResolve!: (value: void) => void;
        const recoveryReceived = new Promise<void>((resolve) => {
            recoveryResolve = resolve;
        });

        session.on((event) => {
            if (
                event.type === "assistant.message" &&
                event.data.content?.includes("tool_abort_recovery_ok")
            ) {
                recoveryResolve();
            }
        });

        void session.send({
            prompt: "Say 'tool_abort_recovery_ok'.",
        });

        await withTimeout(recoveryReceived, 60_000, "tool abort recovery message");

        await session.disconnect();
    });
});
