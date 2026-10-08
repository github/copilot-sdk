/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdirSync, writeFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import { CopilotClient, RuntimeConnection, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext, isInProcessTransport } from "./harness/sdkTestContext.js";

const MARKETPLACE_NAME = "managed-progress-market";
const PLUGIN_NAME = "managed-progress-plugin";
const PLUGIN_SPEC = `${PLUGIN_NAME}@${MARKETPLACE_NAME}`;

type ManagedPluginProgressEvent = Extract<
    SessionEvent,
    { type: "session.managed_plugin_progress" }
>;

describe("Managed plugin progress", async () => {
    const { env, workDir } = await createSdkTestContext();

    it.skipIf(isInProcessTransport)(
        "emits presentation-neutral completion after installing required plugins",
        { timeout: 120_000 },
        async () => {
            const marketplaceDir = join(workDir, "managed-progress-marketplace");
            const pluginDir = join(marketplaceDir, "plugins", PLUGIN_NAME);
            const marketplaceManifest = join(
                marketplaceDir,
                ".github",
                "plugin",
                "marketplace.json"
            );
            mkdirSync(join(marketplaceDir, ".github", "plugin"), { recursive: true });
            mkdirSync(pluginDir, { recursive: true });
            writeFileSync(
                marketplaceManifest,
                JSON.stringify({
                    name: MARKETPLACE_NAME,
                    owner: { name: "SDK E2E" },
                    plugins: [{ name: PLUGIN_NAME, source: `./plugins/${PLUGIN_NAME}` }],
                })
            );
            writeFileSync(
                join(pluginDir, "plugin.json"),
                JSON.stringify({ name: PLUGIN_NAME, version: "1.0.0" })
            );
            execFileSync("git", ["init", "--initial-branch=main"], { cwd: marketplaceDir });
            execFileSync("git", ["config", "user.name", "SDK E2E"], { cwd: marketplaceDir });
            execFileSync("git", ["config", "user.email", "sdk-e2e@example.invalid"], {
                cwd: marketplaceDir,
            });
            execFileSync("git", ["add", "."], { cwd: marketplaceDir });
            execFileSync("git", ["commit", "-m", "Publish managed plugin"], {
                cwd: marketplaceDir,
            });

            const policyPath = join(workDir, "managed-progress-policy.json");
            writeFileSync(
                policyPath,
                JSON.stringify({
                    extraKnownMarketplaces: {
                        [MARKETPLACE_NAME]: {
                            source: {
                                source: "git",
                                url: pathToFileURL(marketplaceDir).toString(),
                                ref: "main",
                            },
                        },
                    },
                    enabledPlugins: { [PLUGIN_SPEC]: true },
                })
            );

            const home = join(workDir, "managed-progress-home");
            mkdirSync(home, { recursive: true });
            const client = new CopilotClient({
                workingDirectory: workDir,
                connection: RuntimeConnection.forStdio({
                    path: process.env.COPILOT_LEGACY_CLI_PATH,
                }),
                useLoggedInUser: false,
                env: {
                    ...env,
                    COPILOT_HOME: home,
                    GH_CONFIG_DIR: home,
                    XDG_CONFIG_HOME: home,
                    XDG_STATE_HOME: home,
                    GH_TOKEN: "",
                    GITHUB_TOKEN: "",
                    COPILOT_GITHUB_TOKEN: "",
                    GITHUB_COPILOT_API_TOKEN: "",
                    COPILOT_HMAC_KEY: "",
                    CAPI_HMAC_KEY: "",
                    COPILOT_E2E_TEST_HOOKS: "1",
                    COPILOT_TEST_MANAGED_SETTINGS_FILE_PATH: policyPath,
                },
            });
            onTestFinished(() => client.stop());
            await client.start();

            const observedEventTypes: string[] = [];
            const progress: ManagedPluginProgressEvent[] = [];
            const session = await client.createSession({
                enableManagedSettings: true,
                onEvent: (event) => {
                    observedEventTypes.push(event.type);
                    if (event.type === "session.managed_plugin_progress") {
                        progress.push(event);
                    }
                },
            });
            try {
                const managedSettings = await session.rpc.managedSettings.get();
                expect(managedSettings.settings).toMatchObject({
                    enabledPlugins: { [PLUGIN_SPEC]: true },
                });
                await vi.waitFor(
                    () =>
                        expect(
                            progress.at(-1)?.data,
                            `Observed event types: ${observedEventTypes.join(", ")}`
                        ).toEqual({ phase: "complete", pluginSpecs: [] }),
                    { timeout: 45_000 }
                );

                const installed = (await client.rpc.plugins.list()).plugins.find(
                    (plugin) =>
                        plugin.name === PLUGIN_NAME && plugin.marketplace === MARKETPLACE_NAME
                );
                expect(installed).toMatchObject({ enabled: false });
            } finally {
                await session.disconnect();
            }
        }
    );
});
