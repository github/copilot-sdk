/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import type { ToolResultObject } from "../../src/index.js";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Tool Results", async () => {
    const { copilotClient: client } = await createSdkTestContext();

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

        await session.destroy();
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

        await session.destroy();
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

        await session.destroy();
    });
});
