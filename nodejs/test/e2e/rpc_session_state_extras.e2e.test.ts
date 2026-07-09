/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import type { CopilotSession } from "../../src/index.js";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

describe("Session-scoped state extras RPC", async () => {
    const { copilotClient: client, env, openAiEndpoint, workDir } = await createSdkTestContext();

    function createClientWithEnv(
        extraEnv: Record<string, string | undefined>,
        token = DEFAULT_GITHUB_TOKEN
    ): CopilotClient {
        return new CopilotClient({
            workingDirectory: workDir,
            env: {
                ...env,
                ...extraEnv,
            },
            logLevel: "error",
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken: token,
        });
    }

    function createAuthenticatedClient(token: string): CopilotClient {
        return createClientWithEnv(
            {
                COPILOT_DEBUG_GITHUB_API_URL: env.COPILOT_API_URL,
            },
            token
        );
    }

    async function configureAuthenticatedUser(token: string): Promise<void> {
        await openAiEndpoint.setCopilotUserByToken(token, {
            login: "rpc-session-extras-user",
            copilot_plan: "individual_pro",
            endpoints: {
                api: env.COPILOT_API_URL,
                telemetry: "https://localhost:1/telemetry",
            },
            analytics_tracking_id: "rpc-session-extras-tracking-id",
        });
    }

    async function createSession(): Promise<CopilotSession> {
        return client.createSession({ onPermissionRequest: approveAll });
    }

    async function disconnect(session: CopilotSession | undefined): Promise<void> {
        if (!session) {
            return;
        }
        try {
            await session.disconnect();
        } catch {
            // Best-effort cleanup.
        }
    }

    it("should list models for session", { timeout: 120_000 }, async () => {
        const token = "rpc-session-model-list-token";
        await configureAuthenticatedUser(token);
        const authClient = createAuthenticatedClient(token);
        let session: CopilotSession | undefined;
        try {
            await authClient.start();
            session = await authClient.createSession({
                model: "claude-sonnet-4.5",
                onPermissionRequest: approveAll,
            });

            const result = await session.rpc.model.list();

            expect(Array.isArray(result.list)).toBe(true);
            expect(result.list.length).toBeGreaterThan(0);
            expect(
                result.list.some((model) => JSON.stringify(model).includes("claude-sonnet-4.5"))
            ).toBe(true);
        } finally {
            await disconnect(session);
            try {
                await authClient.stop();
            } catch {
                // Best-effort cleanup.
            }
        }
    });

    it("should report session activity when idle", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const activity = await session.rpc.metadata.activity();

            expect(activity.hasActiveWork).toBe(false);
            expect(activity.abortable).toBe(false);
        } finally {
            await session.disconnect();
        }
    });

    it("should add byok provider and model at runtime", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const providerName = `sdk-runtime-provider-${Date.now()}-${Math.random().toString(36).slice(2)}`;
            const modelId = "sdk-runtime-model";
            const selectionId = `${providerName}/${modelId}`;

            const added = await session.rpc.provider.add({
                providers: [
                    {
                        name: providerName,
                        type: "openai",
                        wireApi: "completions",
                        baseUrl: "https://api.example.test/v1",
                        apiKey: "runtime-provider-secret",
                        headers: { "X-SDK-Provider": "runtime" },
                    },
                ],
                models: [
                    {
                        provider: providerName,
                        id: modelId,
                        name: "SDK Runtime Model",
                        modelId: "claude-sonnet-4.5",
                        wireModel: "wire-sdk-runtime-model",
                        maxContextWindowTokens: 4096,
                        maxPromptTokens: 3072,
                        maxOutputTokens: 1024,
                        capabilities: {
                            limits: {
                                maxContextWindowTokens: 4096,
                                maxPromptTokens: 3072,
                                maxOutputTokens: 1024,
                            },
                            supports: {
                                reasoningEffort: false,
                                vision: false,
                            },
                        },
                    },
                ],
            });

            expect(added.models).toHaveLength(1);
            expect(JSON.stringify(added.models[0])).toContain(selectionId);
            expect(JSON.stringify(added.models[0])).toContain("SDK Runtime Model");

            const listed = await session.rpc.model.list();
            expect(listed.list.some((model) => JSON.stringify(model).includes(selectionId))).toBe(
                true
            );

            const switched = await session.rpc.model.switchTo({ modelId: selectionId });
            expect(switched.modelId).toBe(selectionId);
            expect((await session.rpc.model.getCurrent()).modelId).toBe(selectionId);
        } finally {
            await session.disconnect();
        }
    });

    it(
        "should return empty completions when host does not provide them",
        { timeout: 120_000 },
        async () => {
            const session = await createSession();
            try {
                const triggers = await session.rpc.completions.getTriggerCharacters();
                expect(triggers.triggerCharacters).toEqual([]);

                const completions = await session.rpc.completions.request({
                    text: "Use @",
                    offset: 5,
                });
                expect(completions.items).toEqual([]);
            } finally {
                await session.disconnect();
            }
        }
    );

    it("should report visibility as unsynced for local session", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const initial = await session.rpc.visibility.get();
            expect(initial.synced).toBe(false);
            expect(initial.status).toBeUndefined();
            expect(initial.shareUrl).toBeUndefined();

            const set = await session.rpc.visibility.set({ status: "repo" });
            expect(set.synced).toBe(false);
            expect(set.status).toBeUndefined();
            expect(set.shareUrl).toBeUndefined();
        } finally {
            await session.disconnect();
        }
    });

    it("should get and set allowall permissions", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const initial = await session.rpc.permissions.getAllowAll();
            expect(initial.enabled).toBe(false);

            const enable = await session.rpc.permissions.setAllowAll({ enabled: true });
            expect(enable.success).toBe(true);
            expect(enable.enabled).toBe(true);
            expect((await session.rpc.permissions.getAllowAll()).enabled).toBe(true);

            const disable = await session.rpc.permissions.setAllowAll({ enabled: false });
            expect(disable.success).toBe(true);
            expect(disable.enabled).toBe(false);
            expect((await session.rpc.permissions.getAllowAll()).enabled).toBe(false);
        } finally {
            try {
                await session.rpc.permissions.setAllowAll({ enabled: false });
            } catch {
                // Best-effort reset.
            }
            await session.disconnect();
        }
    });

    it(
        "should get context attribution and heaviest messages after turn",
        { timeout: 120_000 },
        async () => {
            const session = await createSession();
            try {
                const answer = await session.sendAndWait({
                    prompt: "Say CONTEXT_METADATA_OK exactly.",
                });
                expect(answer?.data.content ?? "").toContain("CONTEXT_METADATA_OK");

                const attribution = await session.rpc.metadata.getContextAttribution();
                expect(attribution.contextAttribution).not.toBeNull();
                const contextAttribution = attribution.contextAttribution!;
                expect(contextAttribution.totalTokens).toBeGreaterThan(0);
                expect(contextAttribution.entries.length).toBeGreaterThan(0);
                for (const entry of contextAttribution.entries) {
                    expect(entry.id.trim()).toBeTruthy();
                    expect(entry.kind.trim()).toBeTruthy();
                    expect(entry.label.trim()).toBeTruthy();
                    expect(entry.tokens).toBeGreaterThanOrEqual(0);
                    for (const attribute of entry.attributes ?? []) {
                        expect(attribute.key.trim()).toBeTruthy();
                    }
                }

                const heaviest = await session.rpc.metadata.getContextHeaviestMessages({
                    limit: 2,
                });
                expect(heaviest.totalTokens).toBeGreaterThan(0);
                expect(heaviest.messages.length).toBeLessThanOrEqual(2);
                for (const message of heaviest.messages) {
                    expect(message.id.trim()).toBeTruthy();
                    expect(message.tokens).toBeGreaterThanOrEqual(0);
                }
            } finally {
                await session.disconnect();
            }
        }
    );

    it("should update and clear live subagent settings", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            await expect(
                session.rpc.tools.updateSubagentSettings({
                    subagents: {
                        "general-purpose": {
                            model: "claude-haiku-4.5",
                            effortLevel: "low",
                            contextTier: "default",
                        },
                    },
                })
            ).resolves.toBeDefined();

            await expect(
                session.rpc.tools.updateSubagentSettings({
                    subagents: null,
                })
            ).resolves.toBeDefined();
        } finally {
            await session.disconnect();
        }
    });

    it("should read empty sql todos for fresh session", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const result = await session.rpc.plan.readSqlTodos();

            expect(result.rows).toBeDefined();
            expect(result.rows).toEqual([]);
        } finally {
            await session.disconnect();
        }
    });

    it("should get telemetry engagement id", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const result = await session.rpc.telemetry.getEngagementId();

            expect(result).toBeDefined();
        } finally {
            await session.disconnect();
        }
    });

    it("should get current tool metadata after initialization", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            const answer = await session.sendAndWait({ prompt: "What is 2+2?" });
            expect(answer).toBeDefined();

            const result = await session.rpc.tools.getCurrentMetadata();

            expect(result.tools).not.toBeNull();
            expect(result.tools!.length).toBeGreaterThan(0);
            for (const tool of result.tools!) {
                expect(tool.name).toBeTruthy();
                expect(tool.description).toBeDefined();
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should reload session plugins", { timeout: 120_000 }, async () => {
        const session = await createSession();
        try {
            await session.rpc.plugins.reload();

            const plugins = await session.rpc.plugins.list();
            expect(plugins.plugins).toBeDefined();
            for (const plugin of plugins.plugins) {
                expect(plugin.name).toBeTruthy();
            }
        } finally {
            await session.disconnect();
        }
    });
});
