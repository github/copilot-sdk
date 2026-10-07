/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { mkdirSync, rmSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { formatError, waitForCondition } from "./harness/sdkTestHelper.js";

describe("Miscellaneous server-scoped RPC", async () => {
    const { copilotClient: client, env, openAiEndpoint, workDir } = await createSdkTestContext();

    function createUniqueDirectory(prefix: string): string {
        const directory = join(workDir, `${prefix}-${randomUUID()}`);
        mkdirSync(directory, { recursive: true });
        return directory;
    }

    function createClient(
        extraEnv: Record<string, string | undefined>,
        gitHubToken: string | undefined
    ): CopilotClient {
        return new CopilotClient({
            workingDirectory: workDir,
            env: {
                ...env,
                ...extraEnv,
            },
            logLevel: "error",
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken,
            useLoggedInUser: gitHubToken === undefined ? false : undefined,
        });
    }

    async function createIsolatedStartedClient(
        gitHubToken: string | null = DEFAULT_GITHUB_TOKEN
    ): Promise<{
        client: CopilotClient;
        home: string;
    }> {
        const home = createUniqueDirectory("copilot-e2e-misc-home");
        const effectiveGitHubToken = gitHubToken === null ? undefined : gitHubToken;
        const isolatedClient = createClient(
            {
                COPILOT_HOME: home,
                GH_CONFIG_DIR: home,
                XDG_CONFIG_HOME: home,
                XDG_STATE_HOME: home,
                COPILOT_DEBUG_GITHUB_API_URL: env.COPILOT_API_URL,
            },
            effectiveGitHubToken
        );
        try {
            await isolatedClient.start();
            return { client: isolatedClient, home };
        } catch (error) {
            await disposeIsolated(isolatedClient, home);
            throw error;
        }
    }

    async function disposeIsolated(isolatedClient: CopilotClient, home: string): Promise<void> {
        try {
            await isolatedClient.stop();
        } catch {
            // Best-effort cleanup.
        }
        tryRemoveDirectory(home);
    }

    async function forceStop(target: CopilotClient): Promise<void> {
        try {
            await target.stop();
        } catch {
            // Runtime may already be gone.
        }
    }

    function tryRemoveDirectory(directory: string): void {
        try {
            rmSync(directory, { recursive: true, force: true });
        } catch {
            // Temp directories are reclaimed by the harness/OS.
        }
    }

    it("should reload user settings", { timeout: 120_000 }, async () => {
        await client.start();

        await client.rpc.user.settings.reload();
    });

    it("should get set and clear user settings", { timeout: 120_000 }, async () => {
        const { client: isolatedClient, home } = await createIsolatedStartedClient();
        try {
            const before = await isolatedClient.rpc.user.settings.get();
            expect(Object.keys(before.settings).length).toBeGreaterThan(0);
            for (const [key, setting] of Object.entries(before.settings)) {
                expect(key.trim()).toBeTruthy();
                expect(setting.value !== undefined || setting.default !== undefined).toBe(true);
            }

            const entry = Object.entries(before.settings).find(
                ([, setting]) => typeof setting.value === "boolean"
            );
            expect(entry).toBeDefined();
            const [settingKey, setting] = entry!;
            const toggledValue = setting.value !== true;

            const set = await isolatedClient.rpc.user.settings.set({
                settings: { [settingKey]: toggledValue },
            });
            expect(set.shadowedKeys).not.toContain(settingKey);

            await isolatedClient.rpc.user.settings.reload();
            const afterSet = await isolatedClient.rpc.user.settings.get();
            expect(afterSet.settings[settingKey].isDefault).toBe(false);
            expect(afterSet.settings[settingKey].value).toBe(toggledValue);

            await isolatedClient.rpc.user.settings.set({
                settings: { [settingKey]: null },
            });
            await isolatedClient.rpc.user.settings.reload();
            const afterClear = await isolatedClient.rpc.user.settings.get();
            expect(afterClear.settings[settingKey].isDefault).toBe(true);
        } finally {
            await disposeIsolated(isolatedClient, home);
        }
    });

    it("should login list getCurrentAuth and logout account", { timeout: 120_000 }, async () => {
        const login = `rpc-account-${randomUUID().replaceAll("-", "")}`;
        const token = `rpc-account-token-${randomUUID().replaceAll("-", "")}`;
        await openAiEndpoint.setCopilotUserByToken(token, {
            login,
            copilot_plan: "individual_pro",
            endpoints: {
                api: env.COPILOT_API_URL,
                telemetry: "https://localhost:1/telemetry",
            },
            analytics_tracking_id: "rpc-account-tracking-id",
        });

        const { client: isolatedClient, home } = await createIsolatedStartedClient(null);
        try {
            const initial = await isolatedClient.rpc.account.getCurrentAuth();
            expect(initial.authInfo).toBeUndefined();

            const loginResult = await isolatedClient.rpc.account.login({
                host: "https://github.com",
                login,
                token,
            });
            expect(typeof loginResult.storedInVault).toBe("boolean");

            const current = await isolatedClient.rpc.account.getCurrentAuth();
            expect(current.authErrors).toBeUndefined();
            expect(current.authInfo).toMatchObject({
                type: "user",
                host: "https://github.com",
                login,
            });

            const users = await isolatedClient.rpc.account.getAllUsers();
            expect(Array.isArray(users)).toBe(true);
            for (const user of users) {
                expect(user.authInfo.type.trim()).toBeTruthy();
            }
            const account = users.find(
                (user) => user.authInfo.type === "user" && user.authInfo.login === login
            );
            if (account) {
                expect(account?.token).toBe(token);
            }

            const logout = await isolatedClient.rpc.account.logout({
                authInfo: current.authInfo!,
            });
            expect(logout.hasMoreUsers).toBe(false);

            const afterLogout = await isolatedClient.rpc.account.getCurrentAuth();
            expect(afterLogout.authInfo).toBeUndefined();
        } finally {
            await disposeIsolated(isolatedClient, home);
        }
    });

    it("should report agent registry spawn gate closed", { timeout: 120_000 }, async () => {
        const { client: isolatedClient, home } = await createIsolatedStartedClient();
        try {
            await expect(
                isolatedClient.rpc.agentRegistry.spawn({ cwd: workDir })
            ).rejects.toSatisfy((error: unknown) => {
                const message = formatError(error);
                expect(message.toLowerCase()).not.toContain("unhandled method");
                expect(message.toLowerCase()).toContain("agentregistry.spawn");
                expect(
                    message.toLowerCase().includes("not enabled") ||
                        message.toLowerCase().includes("no delegate")
                ).toBe(true);
                return true;
            });
        } finally {
            await disposeIsolated(isolatedClient, home);
        }
    });

    it("should shut down owned runtime", { timeout: 120_000 }, async () => {
        const dedicatedClient = createClient({}, DEFAULT_GITHUB_TOKEN);
        try {
            await dedicatedClient.start();
            await dedicatedClient.rpc.user.settings.reload();

            await dedicatedClient.rpc.runtime.shutdown();

            await waitForCondition(
                async () => {
                    try {
                        await dedicatedClient.rpc.user.settings.reload();
                        return false;
                    } catch {
                        return true;
                    }
                },
                {
                    timeoutMs: 15_000,
                    intervalMs: 100,
                    timeoutMessage: "Runtime kept serving RPCs after a graceful shutdown.",
                }
            );
        } finally {
            await forceStop(dedicatedClient);
        }
    });

    it(
        "should report not found when opening session without context",
        { timeout: 120_000 },
        async () => {
            const { client: isolatedClient, home } = await createIsolatedStartedClient();
            try {
                const result = await isolatedClient.rpc.sessions.open({ kind: "resumeLast" });

                expect(result.status).toBe("not_found");
                expect(result.sessionId ?? null).toBeNull();
            } finally {
                await disposeIsolated(isolatedClient, home);
            }
        }
    );

    it(
        "should reject send attachments from non extension connection",
        { timeout: 120_000 },
        async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                await expect(
                    session.rpc.extensions.sendAttachmentsToMessage({ attachments: [] })
                ).rejects.toSatisfy((error: unknown) => {
                    const message = formatError(error);
                    expect(message.toLowerCase()).not.toContain("unhandled method");
                    expect(message.toLowerCase()).toContain("extension");
                    return true;
                });
            } finally {
                await session.disconnect();
            }
        }
    );
});
