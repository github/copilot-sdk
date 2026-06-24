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
                await authClient.forceStop();
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
