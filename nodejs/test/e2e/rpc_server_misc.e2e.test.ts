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
    const { copilotClient: client, env, workDir } = await createSdkTestContext();

    function createUniqueDirectory(prefix: string): string {
        const directory = join(workDir, `${prefix}-${randomUUID()}`);
        mkdirSync(directory, { recursive: true });
        return directory;
    }

    function createClient(extraEnv: Record<string, string | undefined> = {}): CopilotClient {
        return new CopilotClient({
            workingDirectory: workDir,
            env: {
                ...env,
                ...extraEnv,
            },
            logLevel: "error",
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken: DEFAULT_GITHUB_TOKEN,
        });
    }

    async function createIsolatedStartedClient(): Promise<{
        client: CopilotClient;
        home: string;
    }> {
        const home = createUniqueDirectory("copilot-e2e-misc-home");
        const isolatedClient = createClient({
            COPILOT_HOME: home,
            GH_CONFIG_DIR: home,
            XDG_CONFIG_HOME: home,
            XDG_STATE_HOME: home,
        });
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
            await isolatedClient.forceStop();
        } catch {
            // Best-effort cleanup.
        }
        tryRemoveDirectory(home);
    }

    async function forceStop(target: CopilotClient): Promise<void> {
        try {
            await target.forceStop();
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
        const dedicatedClient = createClient();
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
