/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    approveAll,
    CopilotClient,
    RuntimeConnection,
    type CopilotSession,
} from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const MARKETPLACE_NAME = "csharp-e2e-marketplace";
const PLUGIN_NAME = "csharp-e2e-plugin";
const DIRECT_PLUGIN_NAME = "csharp-e2e-direct";
const TEST_MCP_SERVER = fileURLToPath(
    new URL("../../../test/harness/test-mcp-server.mjs", import.meta.url)
);

describe("Server-scoped plugin RPC", async () => {
    const { env, workDir } = await createSdkTestContext();

    function createUniqueDirectory(prefix: string): string {
        const directory = join(workDir, `${prefix}-${randomUUID()}`);
        mkdirSync(directory, { recursive: true });
        return directory;
    }

    function createClient(home: string): CopilotClient {
        return new CopilotClient({
            workingDirectory: workDir,
            env: {
                ...env,
                COPILOT_HOME: home,
                GH_CONFIG_DIR: home,
                XDG_CONFIG_HOME: home,
                XDG_STATE_HOME: home,
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
        const home = createUniqueDirectory("copilot-e2e-home");
        const client = createClient(home);
        try {
            await client.start();
            return { client, home };
        } catch (error) {
            await disposeIsolated(client, home);
            throw error;
        }
    }

    async function disposeIsolated(
        client: CopilotClient,
        home: string,
        fixtureDir?: string
    ): Promise<void> {
        try {
            await client.stop();
        } catch {
            // Best-effort cleanup.
        }
        tryRemoveDirectory(home);
        if (fixtureDir) {
            tryRemoveDirectory(fixtureDir);
        }
    }

    function tryRemoveDirectory(directory: string): void {
        try {
            rmSync(directory, { recursive: true, force: true });
        } catch {
            // Temp directories are reclaimed by the harness/OS.
        }
    }

    function createLocalMarketplaceFixture(): string {
        const directory = createUniqueDirectory("copilot-e2e-mp");
        const manifest = `{
  "name": "${MARKETPLACE_NAME}",
  "owner": { "name": "Copilot SDK E2E" },
  "metadata": { "description": "Local marketplace fixture for SDK E2E tests." },
  "plugins": [
    {
      "name": "${PLUGIN_NAME}",
      "source": "./${PLUGIN_NAME}",
      "description": "E2E demo plugin advertised by the local marketplace.",
      "version": "1.0.0"
    }
  ]
}
`;
        writeFileSync(join(directory, "marketplace.json"), manifest);

        const pluginDir = join(directory, PLUGIN_NAME);
        mkdirSync(pluginDir, { recursive: true });
        writeSkillFile(pluginDir);

        return directory;
    }

    function createDirectPluginFixture(): string {
        const directory = createUniqueDirectory("copilot-e2e-plugin");
        const manifest = `{
  "name": "${DIRECT_PLUGIN_NAME}",
  "description": "E2E demo plugin installed directly from a local path.",
  "version": "1.0.0"
}
`;
        writeFileSync(join(directory, "plugin.json"), manifest);
        writeSkillFile(directory);
        return directory;
    }

    function writeSkillFile(pluginDir: string): void {
        const skill = `---
name: csharp-e2e-skill
description: A demo skill contributed by the E2E test plugin.
---
# Demo Skill

This skill exists so the plugin reports at least one installed skill.
`;
        writeFileSync(join(pluginDir, "SKILL.md"), skill);
    }

    it(
        "keeps unrelated MCP servers running when Computer Use is toggled",
        { timeout: 120_000 },
        async ({ expect }) => {
            const pluginDir = createUniqueDirectory("computer-use");
            const { client, home } = await createIsolatedStartedClient();
            let session: CopilotSession | undefined;
            onTestFinished(async () => {
                try {
                    await session?.disconnect();
                } finally {
                    await disposeIsolated(client, home, pluginDir);
                }
            });
            writeFileSync(
                join(pluginDir, "plugin.json"),
                JSON.stringify({
                    name: "computer-use",
                    version: "1.0.0",
                    mcpServers: "./.mcp.json",
                })
            );
            writeFileSync(
                join(pluginDir, ".mcp.json"),
                JSON.stringify({
                    mcpServers: {
                        "computer-use": {
                            command: process.execPath,
                            args: [TEST_MCP_SERVER, "--server-name", "computer-use"],
                            cwd: dirname(TEST_MCP_SERVER),
                            tools: ["*"],
                        },
                    },
                })
            );
            await client.rpc.plugins.builtin.set({ paths: [pluginDir] });
            const serverName = "unrelated-to-computer-use";
            const startupMarker = join(home, "unrelated-mcp-starts.log");
            const mcpSession = await client.createSession({
                onPermissionRequest: approveAll,
                mcpServers: {
                    [serverName]: {
                        type: "local",
                        command: process.execPath,
                        args: [
                            TEST_MCP_SERVER,
                            "--startup-marker",
                            startupMarker,
                            "--server-name",
                            serverName,
                        ],
                        workingDirectory: dirname(TEST_MCP_SERVER),
                        tools: ["*"],
                    },
                },
            });
            session = mcpSession;
            const waitForConnected = (name: string) =>
                waitForCondition(
                    async () =>
                        (await mcpSession.rpc.mcp.list()).servers.some(
                            (server) => server.name === name && server.status === "connected"
                        ),
                    {
                        timeoutMs: 60_000,
                        intervalMs: 200,
                        timeoutMessage: `${name} did not connect`,
                    }
                );
            await waitForConnected(serverName);
            expect(readFileSync(startupMarker, "utf8")).toBe(`${serverName}\n`);
            const toolsBefore = await mcpSession.rpc.mcp.listTools({ serverName });
            expect(toolsBefore.tools.map((tool) => tool.name)).toContain("get_env");
            expect(
                (await mcpSession.rpc.mcp.list()).servers.map((server) => server.name)
            ).not.toContain("computer-use");

            for (const enabled of [true, false, true, false]) {
                if (enabled) {
                    await client.rpc.plugins.enable({ names: ["computer-use"] });
                    await waitForConnected("computer-use");
                } else {
                    await client.rpc.plugins.disable({ names: ["computer-use"] });
                    await waitForCondition(
                        async () =>
                            !(await mcpSession.rpc.mcp.list()).servers.some(
                                (server) => server.name === "computer-use"
                            ),
                        {
                            timeoutMs: 60_000,
                            intervalMs: 200,
                            timeoutMessage: "Computer Use MCP server was not removed",
                        }
                    );
                }
                expect((await mcpSession.rpc.mcp.isServerRunning({ serverName })).running).toBe(
                    true
                );
                expect(await mcpSession.rpc.mcp.listTools({ serverName })).toEqual(toolsBefore);
                expect(readFileSync(startupMarker, "utf8")).toBe(`${serverName}\n`);
            }
        }
    );

    it("should install and list plugin from local marketplace", { timeout: 120_000 }, async () => {
        const marketplaceDir = createLocalMarketplaceFixture();
        const { client, home } = await createIsolatedStartedClient();
        try {
            await client.rpc.plugins.marketplaces.add({ source: marketplaceDir });

            const spec = `${PLUGIN_NAME}@${MARKETPLACE_NAME}`;
            const install = await client.rpc.plugins.install({ source: spec });

            expect(install.plugin.name).toBe(PLUGIN_NAME);
            expect(install.plugin.marketplace).toBe(MARKETPLACE_NAME);
            expect(install.plugin.enabled).toBe(true);
            expect(install.skillsInstalled).toBeGreaterThanOrEqual(1);
            expect(install.deprecationWarning ?? null).toBeNull();

            const afterInstall = await client.rpc.plugins.list();
            const listed = afterInstall.plugins.filter(
                (plugin) => plugin.name === PLUGIN_NAME && plugin.marketplace === MARKETPLACE_NAME
            );
            expect(listed).toHaveLength(1);
            expect(listed[0].enabled).toBe(true);
        } finally {
            await disposeIsolated(client, home, marketplaceDir);
        }
    });

    it("should enable and disable marketplace plugin", { timeout: 120_000 }, async () => {
        const marketplaceDir = createLocalMarketplaceFixture();
        const { client, home } = await createIsolatedStartedClient();
        try {
            const spec = `${PLUGIN_NAME}@${MARKETPLACE_NAME}`;
            await client.rpc.plugins.marketplaces.add({ source: marketplaceDir });
            await client.rpc.plugins.install({ source: spec });

            await client.rpc.plugins.disable({ names: [spec] });
            expect(getPlugin(await client.rpc.plugins.list()).enabled).toBe(false);

            await client.rpc.plugins.enable({ names: [spec] });
            expect(getPlugin(await client.rpc.plugins.list()).enabled).toBe(true);
        } finally {
            await disposeIsolated(client, home, marketplaceDir);
        }
    });

    it("should update single marketplace plugin", { timeout: 120_000 }, async () => {
        const marketplaceDir = createLocalMarketplaceFixture();
        const { client, home } = await createIsolatedStartedClient();
        try {
            const spec = `${PLUGIN_NAME}@${MARKETPLACE_NAME}`;
            await client.rpc.plugins.marketplaces.add({ source: marketplaceDir });
            await client.rpc.plugins.install({ source: spec });

            const update = await client.rpc.plugins.update({ name: spec });

            expect(update.skillsInstalled).toBeGreaterThanOrEqual(1);
            expect(update.previousVersion).toBe("1.0.0");
            expect(update.newVersion).toBe("1.0.0");
        } finally {
            await disposeIsolated(client, home, marketplaceDir);
        }
    });

    it("should update all installed plugins", { timeout: 120_000 }, async () => {
        const marketplaceDir = createLocalMarketplaceFixture();
        const { client, home } = await createIsolatedStartedClient();
        try {
            const spec = `${PLUGIN_NAME}@${MARKETPLACE_NAME}`;
            await client.rpc.plugins.marketplaces.add({ source: marketplaceDir });
            await client.rpc.plugins.install({ source: spec });

            const result = await client.rpc.plugins.updateAll();

            const entries = result.results.filter(
                (entry) => entry.name === PLUGIN_NAME && entry.marketplace === MARKETPLACE_NAME
            );
            expect(entries).toHaveLength(1);
            expect(entries[0].success).toBe(true);
            expect(entries[0].skillsInstalled).toBeGreaterThanOrEqual(1);
        } finally {
            await disposeIsolated(client, home, marketplaceDir);
        }
    });

    it(
        "should install direct local plugin with deprecation warning",
        { timeout: 120_000 },
        async () => {
            const pluginDir = createDirectPluginFixture();
            const { client, home } = await createIsolatedStartedClient();
            try {
                const install = await client.rpc.plugins.install({ source: pluginDir });

                expect(install.plugin.name).toBe(DIRECT_PLUGIN_NAME);
                expect(install.plugin.marketplace).toBe("");
                expect(install.deprecationWarning).toBeTruthy();
                expect(install.deprecationWarning?.toLowerCase()).toContain("deprecated");
                expect(install.skillsInstalled).toBeGreaterThanOrEqual(1);

                const afterInstall = await client.rpc.plugins.list();
                expect(
                    afterInstall.plugins.filter((plugin) => plugin.name === DIRECT_PLUGIN_NAME)
                ).toHaveLength(1);
                expect(install.plugin.directSourceId).toBeTruthy();

                await client.rpc.plugins.uninstall({
                    name: DIRECT_PLUGIN_NAME,
                    directSourceId: install.plugin.directSourceId,
                });

                const afterUninstall = await client.rpc.plugins.list();
                expect(
                    afterUninstall.plugins.some((plugin) => plugin.name === DIRECT_PLUGIN_NAME)
                ).toBe(false);
            } finally {
                await disposeIsolated(client, home, pluginDir);
            }
        }
    );

    it(
        "should list browse refresh and remove local marketplace",
        { timeout: 120_000 },
        async () => {
            const marketplaceDir = createLocalMarketplaceFixture();
            const { client, home } = await createIsolatedStartedClient();
            try {
                const add = await client.rpc.plugins.marketplaces.add({ source: marketplaceDir });
                expect(add.name).toBe(MARKETPLACE_NAME);

                const list = await client.rpc.plugins.marketplaces.list();
                const mine = list.marketplaces.filter(
                    (marketplace) => marketplace.name === MARKETPLACE_NAME
                );
                expect(mine).toHaveLength(1);
                expect(mine[0].isDefault).not.toBe(true);
                expect(
                    list.marketplaces.some((marketplace) => marketplace.isDefault === true)
                ).toBe(true);

                const browse = await client.rpc.plugins.marketplaces.browse({
                    name: MARKETPLACE_NAME,
                });
                const advertised = browse.plugins.filter((plugin) => plugin.name === PLUGIN_NAME);
                expect(advertised).toHaveLength(1);
                expect(advertised[0].description).toBeTruthy();

                const refresh = await client.rpc.plugins.marketplaces.refresh({
                    name: MARKETPLACE_NAME,
                });
                const refreshed = refresh.results.filter(
                    (result) => result.name === MARKETPLACE_NAME
                );
                expect(refreshed).toHaveLength(1);
                expect(refreshed[0].success).toBe(true);

                const remove = await client.rpc.plugins.marketplaces.remove({
                    name: MARKETPLACE_NAME,
                });
                expect(remove.removed).toBe(true);

                const afterRemove = await client.rpc.plugins.marketplaces.list();
                expect(
                    afterRemove.marketplaces.some(
                        (marketplace) => marketplace.name === MARKETPLACE_NAME
                    )
                ).toBe(false);
            } finally {
                await disposeIsolated(client, home, marketplaceDir);
            }
        }
    );

    it("should reload mcp config cache", { timeout: 120_000 }, async () => {
        const { client, home } = await createIsolatedStartedClient();
        try {
            await client.rpc.mcp.config.reload();
        } finally {
            await disposeIsolated(client, home);
        }
    });

    function getPlugin(list: {
        plugins: Array<{ name: string; marketplace: string; enabled: boolean }>;
    }) {
        const plugins = list.plugins.filter(
            (plugin) => plugin.name === PLUGIN_NAME && plugin.marketplace === MARKETPLACE_NAME
        );
        expect(plugins).toHaveLength(1);
        return plugins[0];
    }
});
