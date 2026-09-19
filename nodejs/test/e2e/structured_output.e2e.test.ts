/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, expectTypeOf, it } from "vitest";
import { z } from "zod";
import {
    approveAll,
    defineTool,
    type AssistantMessageEvent,
    type CopilotSession,
    type ProviderConfig,
    type SessionEvent,
} from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN, isCI } from "./harness/sdkTestContext";
import { waitForCondition } from "./harness/sdkTestHelper";

describe("Structured output", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext({
        copilotClientOptions: {
            env: { COPILOT_CLI_ENABLED_FEATURE_FLAGS: "HYDRAFUSION,HYDRAFUSION_ROLLOUT" },
        },
    });
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

    it("infers_typed_result_after_custom_tool", async () => {
        let calls = 0;
        const schema = z.object({ count: z.number().int(), color: z.string() });
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            streaming: true,
            tools: [
                defineTool("get_inventory", {
                    description: "Get the current widget inventory.",
                    parameters: z.object({}),
                    handler: () => {
                        calls++;
                        return "The inventory contains 42 red widgets.";
                    },
                }),
            ],
        });
        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));
        const result = await session.sendAndWait(
            "Call get_inventory, then report the widget count and color.",
            schema
        );
        expectTypeOf(result).toEqualTypeOf<{ count: number; color: string }>();
        expect(result).toEqual({ count: 42, color: "red" });
        expect(calls).toBeGreaterThan(0);
        expect(events.some((event) => event.type === "assistant.message_delta")).toBe(true);
        const ordinary = await session.sendAndWait(
            "Now reply with exactly the plain text HELLO, not JSON."
        );
        expect(ordinary?.data.content.trim()).toBe("HELLO");
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThanOrEqual(3);
        for (const exchange of exchanges.slice(0, -1)) {
            expect(exchange.request).toHaveProperty(
                "response_format.json_schema.schema",
                schema.toJSONSchema()
            );
        }
        expect(exchanges.at(-1)!.request).not.toHaveProperty("response_format");
    });

    it("typed_result_after_terminal_tool_and_steering", async () => {
        const events: SessionEvent[] = [];
        let calls = 0;
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
                        calls++;
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
        expect(calls).toBe(1);
        expect(events.some((event) => event.type === "tool.execution_complete")).toBe(true);
        expect(events.some((event) => event.type === "assistant.message_delta")).toBe(true);
        const replies = events.filter(
            (event) => event.type === "assistant.message" && !event.agentId
        );
        expect(replies.some((event) => event.data.toolRequests?.length)).toBe(true);
        expect(replies.at(-1)?.data.toolRequests ?? []).toEqual([]);
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThanOrEqual(2);
        for (const exchange of exchanges.slice(1)) {
            expect(exchange.request).toHaveProperty("tool_choice", "none");
        }
        for (const exchange of exchanges) {
            expect(exchange.request).toHaveProperty(
                "response_format.json_schema.schema",
                schema.toJSONSchema()
            );
        }
    });

    it("typed_wait_returns_stop_hook_correction_after_terminal_tool", async () => {
        let calls = 0;
        let stops = 0;
        const replies: AssistantMessageEvent[] = [];
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            tools: [
                defineTool("lookup_number", {
                    description: "Return the number needed for the calculation.",
                    parameters: z.object({}),
                    skipPermission: true,
                    isTerminal: true,
                    handler: () => {
                        calls++;
                        return 58;
                    },
                }),
            ],
            onEvent: (event) => {
                if (event.type === "assistant.message" && !event.agentId) replies.push(event);
            },
            hooks: {
                onAgentStop: () =>
                    ++stops === 1
                        ? {
                              decision: "block",
                              reason: "Correct the answer to 99, not 63. Do not use tools.",
                          }
                        : undefined,
            },
        });
        const schema = z.object({ answer: z.number().int() });
        const result = await session.sendAndWait(
            "Call lookup_number exactly once, then add 5 to the returned number. Do not guess its result.",
            schema
        );
        expect(result).toEqual({ answer: 99 });
        expect(calls).toBe(1);
        expect(stops).toBe(2);
        const answers = replies.filter((reply) => !reply.data.toolRequests?.length);
        expect(answers.map((reply): unknown => JSON.parse(reply.data.content))).toEqual([
            { answer: 63 },
            { answer: 99 },
        ]);
        expect(answers[0].data.originatingMessageId).toBeTruthy();
        expect(answers[1].data.originatingMessageId).toBe(answers[0].data.originatingMessageId);
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges).toHaveLength(3);
        expect(exchanges[1].request).toHaveProperty("tool_choice", "none");
        for (const exchange of exchanges) {
            expect(exchange.request).toHaveProperty(
                "response_format.json_schema.schema",
                schema.toJSONSchema()
            );
        }
    });

    it("rejects_unsupported_or_oversized_schemas_before_admission", async () => {
        for (const model of ["gpt-4.1", "hydrafusion"]) {
            const session = await client.createSession({
                model,
                provider,
                onPermissionRequest: approveAll,
                availableTools: [],
            });
            await expect(
                session.sendAndWait(
                    { prompt: "Must not be admitted", mode: "immediate" },
                    z.object({ answer: z.number().int() })
                )
            ).rejects.toThrow(/immediate/);
            const schema = {
                type: "object",
                description: model === "gpt-4.1" ? "x".repeat(32 * 1024 * 1024) : "Small schema",
            };
            const message = model === "gpt-4.1" ? /32 MiB/ : /HydraFusion/;
            await expect(
                session.sendAndWait({
                    prompt: "Must not be admitted",
                    responseSchema: schema,
                })
            ).rejects.toThrow(message);
            await expect(
                session.rpc.sendMessages({
                    messages: [],
                    responseFormat: {
                        type: "json_schema",
                        jsonSchema: { name: "response", schema },
                    },
                })
            ).rejects.toThrow(message);
            expect((await session.rpc.queue.pendingItems()).items).toEqual([]);
            expect(
                (await session.getEvents()).filter(
                    (event) => event.type === "user.message" || event.type === "session.error"
                )
            ).toEqual([]);
        }
        expect(await openAiEndpoint.getExchanges()).toEqual([]);
    });

    it("send_selects_correlated_response_after_idle", async () => {
        let releaseHook!: () => void;
        let hookEntered = false;
        const hookReleased = new Promise<void>((resolve) => {
            releaseHook = resolve;
        });
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            tools: [
                defineTool("read_inventory", {
                    description: "Read the current widget count and color.",
                    parameters: z.object({}),
                    skipPermission: true,
                    handler: () => "The inventory contains 42 red widgets.",
                }),
            ],
            hooks: {
                onAgentStop: async () => {
                    hookEntered = true;
                    await hookReleased;
                },
            },
        });
        const replies: AssistantMessageEvent[] = [];
        const errors: string[] = [];
        let idle = false;
        const unsubscribe = session.on((event) => {
            if (event.agentId) return;
            if (event.type === "assistant.message") {
                replies.push(event);
            } else if (event.type === "session.error") {
                errors.push(event.data.message);
            } else if (event.type === "session.idle") {
                idle = true;
            }
        });
        const schema = z.object({ count: z.number().int(), color: z.string() });
        try {
            const messageId = await session.send({
                prompt: "Call read_inventory once, then report the current widget count and color.",
                responseSchema: schema,
            });
            await waitForCondition(() => hookEntered || errors.length > 0, {
                timeoutMessage: "Stop hook did not start",
            });
            expect(errors).toEqual([]);
            expect(idle).toBe(false);
            releaseHook();
            await waitForCondition(() => idle || errors.length > 0, {
                timeoutMessage: "Session did not become idle",
            });
            expect(errors).toEqual([]);
            const reply = replies.findLast(
                (event) => event.data.originatingMessageId === messageId
            );
            expect(reply).toBeDefined();
            if (!reply) throw new Error("No correlated assistant response");
            expect(reply.data.originatingMessageId).toBe(messageId);
            expect(schema.parse(JSON.parse(reply.data.content))).toEqual({
                count: 42,
                color: "red",
            });
            expect(replies.some((event) => event.data.toolRequests?.length)).toBe(true);
            expect(reply.data.toolRequests ?? []).toEqual([]);
            expect(replies.at(-1)).toBe(reply);
        } finally {
            releaseHook();
            unsubscribe();
        }
    }, 60_000);

    it("typed_wait_returns_stop_hook_correction", async () => {
        let stops = 0;
        const replies: AssistantMessageEvent[] = [];
        const schema = z.object({ answer: z.number().int() });
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            onEvent: (event) => {
                if (event.type === "assistant.message" && !event.agentId) replies.push(event);
            },
            hooks: {
                onAgentStop: () =>
                    ++stops === 1
                        ? {
                              decision: "block",
                              reason: "Correct the answer to 99, not 42. Do not use tools.",
                          }
                        : undefined,
            },
        });
        const result = await session.sendAndWait("What is 19 + 23? Do not use tools.", schema);
        expect(result).toEqual({ answer: 99 });
        expect(stops).toBe(2);
        expect(replies.map((reply): unknown => JSON.parse(reply.data.content))).toEqual([
            { answer: 42 },
            { answer: 99 },
        ]);
        expect(replies[0].data.originatingMessageId).toBeTruthy();
        expect(replies[1].data.originatingMessageId).toBe(replies[0].data.originatingMessageId);
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges).toHaveLength(2);
        for (const exchange of exchanges) {
            expect(exchange.request).toHaveProperty(
                "response_format.json_schema.schema",
                schema.toJSONSchema()
            );
        }
    });

    it("typed_wait_returns_late_steering_response", async () => {
        let stops = 0;
        let steeringId: string | undefined;
        let session: CopilotSession;
        const replies: AssistantMessageEvent[] = [];
        const schema = z.object({ answer: z.number().int() });
        session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
            onEvent: (event) => {
                if (event.type === "assistant.message" && !event.agentId) replies.push(event);
            },
            hooks: {
                onAgentStop: async () => {
                    if (++stops === 1) {
                        // The final model request has finished, but this run still admits steering.
                        steeringId = await session.send({
                            prompt: "Change the answer to 99. Do not use tools.",
                            mode: "immediate",
                        });
                    }
                },
            },
        });
        const result = await session.sendAndWait("What is 19 + 23? Do not use tools.", schema);
        expect(result).toEqual({ answer: 99 });
        expect(stops).toBe(2);
        expect(replies.map((reply): unknown => JSON.parse(reply.data.content))).toEqual([
            { answer: 42 },
            { answer: 99 },
        ]);
        expect(steeringId).toBeTruthy();
        expect(replies[0].data.originatingMessageId).toBeTruthy();
        expect(replies[0].data.originatingMessageId).not.toBe(steeringId);
        expect(replies[1].data.originatingMessageId).toBe(replies[0].data.originatingMessageId);
        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges).toHaveLength(2);
        for (const exchange of exchanges) {
            expect(exchange.request).toHaveProperty(
                "response_format.json_schema.schema",
                schema.toJSONSchema()
            );
        }
    });

    it("concurrent_typed_sends_return_their_own_results", async () => {
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
            z.object({ first: z.number().int() })
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
                z.object({ second: z.number().int() })
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
            expect(firstResult).toEqual({ first: 42 });
            expect(secondResult).toEqual({ second: 37 });
        } finally {
            releaseTool();
        }
    });

    it("sends_explicit_schema_for_message_and_batch", async () => {
        const session = await client.createSession({
            model: "gpt-4.1",
            provider,
            onPermissionRequest: approveAll,
            availableTools: [],
        });
        const schema = z.object({ count: z.number().int(), color: z.string() });
        const events: SessionEvent[] = [];
        session.on((event) => events.push(event));
        const response = await session.rpc.sendMessages({
            messages: [
                { prompt: "There are 42 red widgets in stock." },
                { prompt: "Report the widget count and color." },
            ],
            responseFormat: {
                type: "json_schema",
                jsonSchema: { name: "inventory", strict: true, schema: schema.toJSONSchema() },
            },
            wait: true,
        });
        const final = events.findLast((event) => event.type === "assistant.message");
        expect(final?.type).toBe("assistant.message");
        if (final?.type !== "assistant.message") throw new Error("No assistant response");
        expect(schema.parse(JSON.parse(final.data.content))).toEqual({ count: 42, color: "red" });
        expect(final.data.originatingMessageId).toBe(response.messageIds.at(-1));
        const raw = await session.sendAndWait({
            prompt: "The inventory now has 21 blue widgets. Report the new count and color.",
            responseSchema: schema.toJSONSchema(),
        });
        expect(schema.parse(JSON.parse(raw!.data.content))).toEqual({ count: 21, color: "blue" });
    });
});
