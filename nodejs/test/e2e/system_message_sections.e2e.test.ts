/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer } from "node:http";
import { text } from "node:stream/consumers";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    approveAll,
    type CopilotSession,
    type NamedProviderConfig,
    type ProviderModelConfig,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("System message sections", async () => {
    const { copilotClient: client, createClient, workDir } = await createSdkTestContext();

    it("should_use_replaced_identity_section_in_response", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    identity: {
                        action: "replace",
                        content:
                            "You are a helpful gardening assistant called Botanica. You only answer questions about plants and gardening.",
                    },
                },
            },
        });

        const response = await session.sendAndWait({ prompt: "Who are you?" });

        expect(response).not.toBeNull();
        const content = response!.data.content.toLowerCase();
        expect(
            content.includes("botanica") || content.includes("garden") || content.includes("plant"),
            `Expected response to reflect the replaced identity section, but got: ${response!.data.content}`
        ).toBe(true);

        await session.disconnect();
    });

    it("should_use_replaced_preamble_section_in_response", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            systemMessage: {
                mode: "customize",
                sections: {
                    preamble: {
                        action: "replace",
                        content:
                            "You are a helpful gardening assistant called Botanica. You only answer questions about plants and gardening.",
                    },
                },
            },
        });

        const response = await session.sendAndWait({ prompt: "Who are you?" });

        expect(response).not.toBeNull();
        const content = response!.data.content.toLowerCase();
        expect(
            content.includes("botanica") || content.includes("garden") || content.includes("plant"),
            `Expected response to reflect the replaced preamble section, but got: ${response!.data.content}`
        ).toBe(true);

        await session.disconnect();
    });

    it(
        "should_move_autopilot_instructions_out_of_a_non_preamble_customized_system_prompt",
        { timeout: 90_000 },
        async () => {
            let modelRequest:
                | {
                      messages?: Array<{ role?: string; content?: string }>;
                  }
                | undefined;
            let modelFailure: Error | undefined;
            const modelServer = createServer((request, response) => {
                void (async () => {
                    modelRequest ??= JSON.parse(await text(request));
                    response.writeHead(200, { "content-type": "application/json" });
                    response.end(
                        JSON.stringify({
                            id: "mode-customization-completion",
                            object: "chat.completion",
                            created: 0,
                            model: "test-model",
                            choices: [
                                {
                                    index: 0,
                                    message: {
                                        role: "assistant",
                                        content: null,
                                        tool_calls: [
                                            {
                                                id: "mode-customization-task-complete",
                                                type: "function",
                                                function: {
                                                    name: "task_complete",
                                                    arguments:
                                                        '{"summary":"MODE_CUSTOMIZATION_REPRO_OK"}',
                                                },
                                            },
                                        ],
                                    },
                                    finish_reason: "tool_calls",
                                },
                            ],
                            usage: { prompt_tokens: 10, completion_tokens: 10, total_tokens: 20 },
                        })
                    );
                })().catch((error: unknown) => {
                    modelFailure = error instanceof Error ? error : new Error(String(error));
                    response.writeHead(500).end();
                });
            });
            const localClient = createClient();
            let session: CopilotSession | undefined;
            let serverStart: Promise<void> | undefined;
            onTestFinished(async () => {
                const errors: unknown[] = [];
                if (session) {
                    try {
                        await session.disconnect();
                    } catch (error) {
                        errors.push(error);
                    }
                }
                try {
                    errors.push(...(await localClient.stop()));
                } catch (error) {
                    errors.push(error);
                }
                if (serverStart) {
                    const [startup] = await Promise.allSettled([serverStart]);
                    if (startup.status === "rejected") {
                        errors.push(startup.reason);
                    }
                }
                modelServer.closeAllConnections();
                if (modelServer.listening) {
                    try {
                        await new Promise<void>((resolve, reject) => {
                            modelServer.close((error) => (error ? reject(error) : resolve()));
                        });
                    } catch (error) {
                        errors.push(error);
                    }
                }
                if (errors.length) {
                    throw new AggregateError(errors, "Mode customization fixture cleanup failed");
                }
            });
            serverStart = new Promise<void>((resolve, reject) => {
                modelServer.once("error", reject);
                modelServer.listen(0, "127.0.0.1", resolve);
            });
            await serverStart;
            const address = modelServer.address();
            if (!address || typeof address === "string") {
                throw new Error("Missing local model server address");
            }

            const providers: NamedProviderConfig[] = [
                {
                    name: "local",
                    type: "openai",
                    baseUrl: `http://127.0.0.1:${address.port}`,
                    apiKey: "test",
                    wireApi: "completions",
                },
            ];
            const models: ProviderModelConfig[] = [
                { id: "model", provider: "local", modelId: "test-model", wireModel: "test-model" },
            ];
            session = await localClient.createSession({
                onPermissionRequest: approveAll,
                workingDirectory: workDir,
                providers,
                models,
                model: "local/model",
                systemMessage: {
                    mode: "customize",
                    sections: {
                        identity: {
                            action: "replace",
                            content: "You are a customized coding assistant.",
                        },
                    },
                },
            });

            await session.rpc.mode.set({ mode: "autopilot" });
            await session.send({
                prompt: "Call task_complete with summary MODE_CUSTOMIZATION_REPRO_OK.",
            });
            await waitForCondition(() => modelRequest !== undefined, {
                timeoutMs: 30_000,
                timeoutMessage: "The local model did not receive the autopilot request",
            });
            await session.abort();

            if (modelFailure) {
                throw modelFailure;
            }
            expect(modelRequest).toBeDefined();
            const systemMessage = getMessage(modelRequest!, "system");
            const userMessage = getMessage(modelRequest!, "user");
            expect(systemMessage).toContain("You are a customized coding assistant.");
            expect(systemMessage).not.toContain("<autopilot_mode>");
            expect(userMessage).toContain("<mode_changed_notice>");
            expect(userMessage).toContain("<autopilot_mode>");
        }
    );
});

function getMessage(
    request: { messages?: Array<{ role?: string; content?: string }> },
    role: "system" | "user"
): string | undefined {
    const message = request.messages?.find((candidate) => candidate.role === role);
    return message?.content;
}
