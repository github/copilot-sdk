/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, expectTypeOf, it } from "vitest";
import { z } from "zod";
import {
    approveAll,
    defineTool,
    type CopilotSession,
    type ProviderConfig,
    type SessionEvent,
} from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN, isCI } from "./harness/sdkTestContext";
import { waitForCondition } from "./harness/sdkTestHelper";

describe("Structured output", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();
    const provider: ProviderConfig = {
        type: "openai",
        wireApi: "completions",
        baseUrl: openAiEndpoint.url,
        modelId: "gpt-4.1",
        wireModel: "gpt-4.1",
        apiKey: isCI ? DEFAULT_GITHUB_TOKEN : (process.env.GITHUB_TOKEN ?? DEFAULT_GITHUB_TOKEN),
        headers: {
            "Copilot-Integration-Id": "copilot-developer-cli",
            "Copilot-Harness-Id": "copilot-sdk",
            "X-GitHub-Api-Version": "2026-08-01",
        },
    };

    it("node_raw_schema_and_unformatted_followup", async () => {
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
        });
        const schema = {
            type: "object",
            properties: {
                answer: { type: "integer" },
                contract: { type: "string", enum: ["raw_schema"] },
            },
            required: ["answer", "contract"],
            additionalProperties: false,
        };
        const result = await session.sendAndWait({
            prompt: "What is 19 + 23? Do not use tools.",
            responseSchema: schema,
        });
        expect(
            result,
            JSON.stringify(
                (await openAiEndpoint.getExchanges()).map((exchange) => exchange.response)
            )
        ).toBeDefined();
        expect(JSON.parse(result!.data.content)).toEqual({ answer: 42, contract: "raw_schema" });
        expect(result!.data.originatingMessageId).toBeTruthy();

        const ordinary = await session.sendAndWait(
            "Reply exactly SCHEMA_CLEARED without JSON or quotes."
        );
        expect(ordinary?.data.content).toBe("SCHEMA_CLEARED");
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges).toHaveLength(2);
        expect(exchanges[0].request).toMatchObject({
            response_format: {
                type: "json_schema",
                json_schema: { name: "response", strict: true, schema },
            },
        });
        expect(exchanges[1].request).not.toHaveProperty("response_format");
    });

    it("node_zod_typed_result_after_terminal_tool_and_steering", async () => {
        const events: SessionEvent[] = [];
        let session: CopilotSession;
        session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            streaming: true,
            onEvent: (event) => events.push(event),
            tools: [
                defineTool("lookup_number", {
                    description: "Return the number needed for the calculation.",
                    parameters: z.object({}),
                    skipPermission: true,
                    isTerminal: true,
                    handler: async () => {
                        await session.send({
                            prompt: "Continue with the original calculation. Do not call any more tools.",
                            mode: "immediate",
                        });
                        return 58;
                    },
                }),
            ],
        });
        const schema = z.object({ answer: z.number().int(), contract: z.literal("typed_tool") });
        const result = await session.sendAndWait(
            "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.",
            schema
        );
        expectTypeOf(result).toEqualTypeOf<{ answer: number; contract: "typed_tool" }>();
        expect(result).toEqual({ answer: 63, contract: "typed_tool" });
        expect(events.some((event) => event.type === "tool.execution_complete")).toBe(true);
        expect(events.some((event) => event.type === "assistant.message_delta")).toBe(true);
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThanOrEqual(2);
        for (const exchange of exchanges) {
            expect(exchange.request).toHaveProperty(
                "response_format.json_schema.schema",
                schema.toJSONSchema()
            );
        }
    });

    it("node_concurrent_typed_sends_return_their_own_results", async () => {
        let markToolEntered!: () => void;
        let releaseTool!: () => void;
        const toolEntered = new Promise<void>((resolve) => {
            markToolEntered = resolve;
        });
        const toolReleased = new Promise<void>((resolve) => {
            releaseTool = resolve;
        });
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            tools: [
                defineTool("first_number", {
                    description: "Get the number for the first question.",
                    parameters: z.object({}),
                    skipPermission: true,
                    handler: async () => {
                        markToolEntered();
                        await toolReleased;
                        return 42;
                    },
                }),
            ],
        });
        const first = session.sendAndWait(
            "Call first_number exactly once and report its returned number.",
            z.object({ first: z.number().int(), contract: z.literal("first") })
        );
        try {
            await Promise.race([
                toolEntered,
                first.then(() => {
                    throw new Error("First run completed without calling first_number");
                }),
            ]);
            const secondPrompt = "What is 30 + 7? Do not use tools.";
            const second = session.sendAndWait(
                secondPrompt,
                z.object({ second: z.number().int(), contract: z.literal("second") })
            );
            const results = Promise.all([first, second]);
            await Promise.race([
                waitForCondition(
                    async () =>
                        (await session.rpc.queue.pendingItems()).items.some((item) =>
                            item.displayText.includes(secondPrompt)
                        ),
                    { timeoutMessage: "Second structured send was not queued behind the tool call" }
                ),
                results.then(() => {
                    throw new Error("Runs completed before the tool was released");
                }),
            ]);
            releaseTool();
            const [firstResult, secondResult] = await results;
            expect(firstResult).toEqual({ first: 42, contract: "first" });
            expect(secondResult).toEqual({ second: 37, contract: "second" });
        } finally {
            releaseTool();
        }
    });

    it("node_generated_rpc_accepts_a_batch_response_format", async () => {
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
        });
        const schema = z.object({ total: z.number().int() });
        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));
        const response = await session.rpc.sendMessages({
            messages: [{ prompt: "What is 16 + 26? Do not use tools." }],
            responseFormat: {
                type: "json_schema",
                jsonSchema: { name: "batch", strict: true, schema: schema.toJSONSchema() },
            },
            wait: true,
        });
        const final = events.findLast((event) => event.type === "assistant.message");
        expect(final?.type).toBe("assistant.message");
        if (final?.type !== "assistant.message") throw new Error("No assistant response");
        expect(schema.parse(JSON.parse(final.data.content))).toEqual({ total: 42 });
        expect(final.data.originatingMessageId).toBe(response.messageIds[0]);
    });
});
