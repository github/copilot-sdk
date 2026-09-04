/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("External tool cancellation", async () => {
    const { copilotClient: client } = await createSdkTestContext();

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

    it("should cancel tool handler when session disconnects", { timeout: 120_000 }, async () => {
        let toolStartedResolve!: () => void;
        const toolStarted = new Promise<void>((resolve) => {
            toolStartedResolve = resolve;
        });
        let toolCancelledResolve!: () => void;
        const toolCancelled = new Promise<void>((resolve) => {
            toolCancelledResolve = resolve;
        });
        let releaseToolResolve!: () => void;
        const releaseTool = new Promise<void>((resolve) => {
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
                    handler: async (_args, invocation) => {
                        toolStartedResolve();
                        await Promise.race([
                            releaseTool,
                            new Promise<never>((_, reject) =>
                                setImmediate(() => {
                                    const onAbort = () => {
                                        toolCancelledResolve();
                                        reject(new Error("aborted"));
                                    };
                                    if (invocation.signal?.aborted) {
                                        onAbort();
                                        return;
                                    }
                                    invocation.signal?.addEventListener("abort", onAbort, {
                                        once: true,
                                    });
                                })
                            ),
                        ]);
                        return "RELEASED";
                    },
                }),
            ],
        });

        try {
            void session.send({
                prompt: "Use slow_analysis with value 'test_abort'. Wait for the result.",
            });

            await withTimeout(toolStarted, 60_000, "slow_analysis start");
            await session.disconnect();
            await withTimeout(toolCancelled, 60_000, "slow_analysis cancellation");
        } finally {
            releaseToolResolve();
        }
    });
});
