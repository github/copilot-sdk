/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { approveAll, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const BROWSER_NAME = "github-copilot-browser";
const SHADOW_SERVER = "installed-browser-shadow";
const TEST_MCP_SERVER = resolve(
    dirname(fileURLToPath(import.meta.url)),
    "../../../test/harness/test-mcp-server.mjs"
);

describe("Browser shadow reload", async () => {
    const { createClient, workDir, env } = await createSdkTestContext({ useStdio: true });

    it(
        "observes another process's install, disable and removal on a warm session",
        { timeout: 90_000 },
        async () => {
            const bundle = join(workDir, "bundle");
            const shadow = join(workDir, "shadow");
            for (const [directory, server] of [
                [bundle, BROWSER_NAME],
                [shadow, SHADOW_SERVER],
            ]) {
                mkdirSync(join(directory, ".plugin"), { recursive: true });
                writeFileSync(
                    join(directory, ".plugin", "plugin.json"),
                    JSON.stringify({
                        name: BROWSER_NAME,
                        version: "0.0.3",
                        mcpServers: "./.mcp.json",
                    })
                );
                writeFileSync(
                    join(directory, ".mcp.json"),
                    JSON.stringify({
                        mcpServers: {
                            [server]: {
                                command: process.execPath,
                                args: [TEST_MCP_SERVER],
                                tools: ["*"],
                            },
                        },
                    })
                );
            }
            writeFileSync(
                join(env.COPILOT_HOME, "settings.json"),
                JSON.stringify({ enabledPlugins: { [BROWSER_NAME]: true }, disableAllHooks: true })
            );
            await using owner = createClient({
                connection: RuntimeConnection.forStdio(),
                builtinPluginDirectories: [bundle],
            });
            await using writer = createClient({ connection: RuntimeConnection.forStdio() });
            await owner.start();
            await writer.start();
            await writer.rpc.plugins.builtin.set({ paths: [] });
            // Exercise persisted MCP discovery, not a host-owned effective-plugin snapshot.
            const session = await owner.createSession({
                onPermissionRequest: approveAll,
                enableConfigDiscovery: false,
                mcpServers: {
                    peer: {
                        type: "local",
                        command: process.execPath,
                        args: [TEST_MCP_SERVER],
                        tools: ["*"],
                    },
                },
            });
            try {
                const connected = async (serverName: string) => {
                    await waitForCondition(
                        async () =>
                            (await session.rpc.mcp.list()).servers.some(
                                (server) =>
                                    server.name === serverName && server.status === "connected"
                            ),
                        { timeoutMs: 30_000, timeoutMessage: `${serverName} did not connect` }
                    ).catch(async (error: unknown) => {
                        const current = (await session.rpc.mcp.list()).servers.map(
                            ({ name, status }) => ({ name, status })
                        );
                        const installed = (await writer.rpc.plugins.list()).plugins.filter(
                            (plugin) => plugin.name === BROWSER_NAME
                        );
                        const discovered = (
                            await owner.rpc.mcp.discover({ workingDirectory: workDir })
                        ).servers.map(({ name, enabled }) => ({ name, enabled }));
                        throw new Error(
                            `${serverName} did not connect: ${JSON.stringify({ current, installed, discovered })}`,
                            { cause: error }
                        );
                    });
                    expect((await session.rpc.mcp.listTools({ serverName })).tools).toEqual(
                        expect.arrayContaining([expect.objectContaining({ name: "get_env" })])
                    );
                };
                const absent = async (serverName: string) => {
                    expect(
                        (await session.rpc.mcp.list()).servers.some(
                            (server) => server.name === serverName
                        )
                    ).toBe(false);
                };
                await connected("peer");
                await connected(BROWSER_NAME);

                const installed = await writer.rpc.plugins.install({ source: shadow });
                expect(installed.plugin.name).toBe(BROWSER_NAME);
                expect(installed.plugin.enabled).toBe(true);
                await session.rpc.mcp.reload();
                await connected(SHADOW_SERVER);
                await absent(BROWSER_NAME);

                await writer.rpc.plugins.disable({ names: [BROWSER_NAME] });
                await session.rpc.mcp.reload();
                await absent(SHADOW_SERVER);
                await absent(BROWSER_NAME);

                await writer.rpc.plugins.uninstall({ name: BROWSER_NAME });
                expect(
                    JSON.parse(readFileSync(join(env.COPILOT_HOME, "settings.json"), "utf8"))
                        .enabledPlugins[BROWSER_NAME]
                ).toBe(true);
                await session.rpc.mcp.reload();
                await connected(BROWSER_NAME);
                await absent(SHADOW_SERVER);
            } finally {
                await session.disconnect();
            }
        }
    );
});
