/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import type { SessionEvent, ToolResultObject } from "../../src/index.js";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";
import { getNextEventOfType } from "./harness/sdkTestHelper";

describe("Tool Results", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

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

    it("should handle structured ToolResultObject from custom tool", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("get_weather", {
                    description: "Gets weather for a city",
                    parameters: z.object({
                        city: z.string(),
                    }),
                    handler: ({ city }): ToolResultObject => ({
                        textResultForLlm: `The weather in ${city} is sunny and 72°F`,
                        resultType: "success",
                    }),
                }),
            ],
        });

        const assistantMessage = await session.sendAndWait({
            prompt: "What's the weather in Paris?",
        });

        const content = assistantMessage?.data.content ?? "";
        expect(content).toMatch(/sunny|72/i);

        await session.disconnect();
    });

    it("should handle tool result with failure resultType", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("check_status", {
                    description: "Checks the status of a service",
                    handler: (): ToolResultObject => ({
                        textResultForLlm: "Service unavailable",
                        resultType: "failure",
                        error: "API timeout",
                    }),
                }),
            ],
        });

        const assistantMessage = await session.sendAndWait({
            prompt: "Check the status of the service using check_status. If it fails, say 'service is down'.",
        });

        const failureContent = assistantMessage?.data.content ?? "";
        expect(failureContent).toMatch(/service is down/i);

        await session.disconnect();
    });

    it("should pass validated Zod parameters to tool handler", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("calculate", {
                    description: "Calculates a math expression",
                    parameters: z.object({
                        operation: z.enum(["add", "subtract", "multiply"]),
                        a: z.number(),
                        b: z.number(),
                    }),
                    handler: ({ operation, a, b }) => {
                        expect(typeof a).toBe("number");
                        expect(typeof b).toBe("number");
                        switch (operation) {
                            case "add":
                                return String(a + b);
                            case "subtract":
                                return String(a - b);
                            case "multiply":
                                return String(a * b);
                        }
                    },
                }),
            ],
        });

        const assistantMessage = await session.sendAndWait({
            prompt: "Use calculate to add 17 and 25",
        });

        expect(assistantMessage?.data.content).toContain("42");

        await session.disconnect();
    });

    it("should preserve toolTelemetry and not stringify structured results for LLM", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("analyze_code", {
                    description: "Analyzes code for issues",
                    parameters: z.object({
                        file: z.string(),
                    }),
                    handler: ({ file }): ToolResultObject => ({
                        textResultForLlm: `Analysis of ${file}: no issues found`,
                        resultType: "success",
                        toolTelemetry: {
                            metrics: { analysisTimeMs: 150 },
                            properties: { analyzer: "eslint" },
                        },
                    }),
                }),
            ],
        });

        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));

        const assistantMessage = await session.sendAndWait({
            prompt: "Analyze the file main.ts for issues.",
        });

        expect(assistantMessage?.data.content).toMatch(/no issues/i);

        // Verify the LLM received just textResultForLlm, not stringified JSON
        const traffic = await openAiEndpoint.getExchanges();
        const lastConversation = traffic[traffic.length - 1]!;
        const toolResults = lastConversation.request.messages.filter(
            (m: { role: string }) => m.role === "tool"
        );
        expect(toolResults.length).toBe(1);
        expect(toolResults[0]!.content).not.toContain("toolTelemetry");
        expect(toolResults[0]!.content).not.toContain("resultType");

        // Verify tool.execution_complete event fires for this tool call
        const toolCompletes = events.filter((e) => e.type === "tool.execution_complete");
        expect(toolCompletes.length).toBeGreaterThanOrEqual(1);
        const completeEvent = toolCompletes[0]!;
        expect(completeEvent.data.success).toBe(true);
        // When the server preserves the structured result, toolTelemetry should
        // be present and non-empty (not the {} that results from stringification).
        if (completeEvent.data.toolTelemetry) {
            expect(Object.keys(completeEvent.data.toolTelemetry).length).toBeGreaterThan(0);
        }

        await session.disconnect();
    });

    it("should handle tool result with rejected resulttype", async () => {
        let toolHandlerCalled = false;
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("deploy_service", {
                    description: "Deploys a service",
                    parameters: z.object({}),
                    handler: (): ToolResultObject => {
                        toolHandlerCalled = true;
                        return {
                            textResultForLlm:
                                "Deployment rejected: policy violation - production deployments require approval",
                            resultType: "rejected",
                        };
                    },
                }),
            ],
        });

        const toolCompletePromise = getNextEventOfType(session, "tool.execution_complete");
        const idlePromise = getNextEventOfType(session, "session.idle");

        await session.send({
            prompt: "Deploy the service using deploy_service. If it's rejected, tell me it was 'rejected by policy'.",
        });

        // Verify the rejected tool result is surfaced via tool.execution_complete.
        const toolComplete = await withTimeout(
            toolCompletePromise,
            60_000,
            "rejected tool.execution_complete"
        );
        expect(toolHandlerCalled).toBe(true);
        if (toolComplete?.type === "tool.execution_complete") {
            expect(toolComplete.data.success).toBe(false);
            expect(toolComplete.data.error?.code).toBe("rejected");
            expect(toolComplete.data.error?.message).toContain("Deployment rejected");
        }

        await withTimeout(idlePromise, 60_000, "session.idle after rejected tool result");

        await session.disconnect();
    });

    it("should handle tool result with denied resulttype", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [
                defineTool("access_secret", {
                    description: "A tool that returns a denied result",
                    parameters: z.object({}),
                    handler: (): ToolResultObject => ({
                        resultType: "denied",
                        textResultForLlm: "Access denied: insufficient permissions to read secrets",
                    }),
                }),
            ],
        });

        const toolCompletePromise = getNextEventOfType(session, "tool.execution_complete");

        const answer = await session.sendAndWait({
            prompt: "Use access_secret to get the API key. If access is denied, tell me it was 'access denied'.",
        });

        const toolComplete = await withTimeout(
            toolCompletePromise,
            60_000,
            "denied tool.execution_complete"
        );
        if (toolComplete?.type === "tool.execution_complete") {
            expect(toolComplete.data.success).toBe(false);
            expect(toolComplete.data.error?.code).toBe("denied");
            expect(toolComplete.data.error?.message).toContain("Access denied");
        }
        expect(answer?.data.content?.toLowerCase()).toContain("access denied");

        await session.disconnect();
    });
});
