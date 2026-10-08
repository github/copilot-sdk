/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn } from "node:child_process";
import { dirname, resolve } from "node:path";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";
import { describe, expect, it, onTestFinished } from "vitest";
import type { CopilotSession, MCPServerConfig } from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { stopChildProcess, waitForCondition } from "./harness/sdkTestHelper.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const TEST_MCP_SESSION_EXPIRY_SERVER = resolve(
    __dirname,
    "../../../test/harness/test-mcp-session-expiry-server.mjs"
);

interface McpServerStats {
    initializations: number;
    toolsListRequests: number;
    toolCalls: number;
    activeSessions: number;
    pings: number;
    expiredRequests: number;
    expiredRequestLog: { method: string; elapsedMs: number }[];
    protocolVersions: string[];
    expiredSessionProbes: number;
}

describe("MCP remote session closure", async () => {
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            env: {
                COPILOT_MCP_APPS: "true",
                MCP_APPS: "true",
            },
        },
    });

    it(
        "reconnects after an idle Streamable HTTP session expires",
        { timeout: 180_000 },
        async () => {
            const remoteServer = await startSessionExpiryMcpServer();
            const serverName = "remote-server";
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                // `session.mcp.apps.callTool` below requires the negotiated
                // `mcp-apps` capability, which only this opt-in requests.
                enableMcpApps: true,
                mcpServers: {
                    [serverName]: {
                        type: "http",
                        url: `${remoteServer.url}/mcp`,
                        tools: ["*"],
                    } as MCPServerConfig,
                },
            });
            onTestFinished(() => disconnectSession(session));
            expect(session.capabilities.ui?.mcpApps).toBe(true);

            await waitForMcpServerStatus(session, serverName, "connected");
            expect((await remoteServer.stats()).initializations).toBe(1);

            // The server drops the session the way an idle Streamable HTTP endpoint
            // expires one: every later request for it answers 404. Await an answered
            // probe or reinitialization before budgeting recovery, not the idle delay.
            await remoteServer.expireSessions();

            await waitForCondition(
                async () => {
                    const stats = await remoteServer.stats();
                    return stats.expiredSessionProbes >= 1 || stats.initializations >= 2;
                },
                {
                    timeoutMs: 120_000,
                    intervalMs: 250,
                    timeoutMessage:
                        "remote MCP session expiry was neither probed nor re-initialized",
                }
            );
            let lastStats: McpServerStats | undefined;
            try {
                await waitForCondition(
                    async () => {
                        lastStats = await remoteServer.stats();
                        return lastStats.initializations >= 2;
                    },
                    {
                        timeoutMs: 120_000,
                        intervalMs: 250,
                        timeoutMessage:
                            "remote MCP server was never re-initialized after session expiry",
                    }
                );
            } catch (error) {
                const details = JSON.stringify(lastStats);
                throw new Error(`MCP reconnection failed; last server stats: ${details}`, {
                    cause: error,
                });
            }
            await waitForMcpServerStatus(session, serverName, "connected");

            const result = await session.rpc.mcp.apps.callTool({
                serverName,
                originServerName: serverName,
                toolName: "remote_ping",
                arguments: {},
            });
            expect(result.content).toEqual([{ type: "text", text: "remote pong" }]);

            const finalStats = await remoteServer.stats();
            expect(finalStats.toolCalls).toBe(1);
        }
    );
});

async function waitForMcpServerStatus(
    session: CopilotSession,
    serverName: string,
    expectedStatus: string
): Promise<void> {
    let lastStatus = "<not listed>";
    await waitForCondition(
        async () => {
            const result = await session.rpc.mcp.list();
            const server = result.servers.find((entry) => entry.name === serverName);
            lastStatus = server?.status ?? "<not listed>";
            return server?.status === expectedStatus;
        },
        {
            timeoutMs: 120_000,
            intervalMs: 200,
            timeoutMessage: `${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}`,
        }
    );
}

async function disconnectSession(session: CopilotSession): Promise<void> {
    try {
        await session.disconnect();
    } catch {
        // The session may already be gone when the test finished.
    }
}

async function startSessionExpiryMcpServer(): Promise<{
    url: string;
    stats: () => Promise<McpServerStats>;
    expireSessions: () => Promise<void>;
}> {
    const child = spawn(process.execPath, [TEST_MCP_SESSION_EXPIRY_SERVER], {
        stdio: ["ignore", "pipe", "pipe"],
    });
    onTestFinished(() => stopChildProcess(child));

    const stderr: string[] = [];
    child.stderr.on("data", (chunk) => stderr.push(String(chunk)));

    const url = await new Promise<string>((resolvePromise, reject) => {
        const rl = createInterface({ input: child.stdout });
        const timeout = setTimeout(() => {
            rl.close();
            reject(
                new Error(`Timed out waiting for session-expiry MCP server. ${stderr.join("")}`)
            );
        }, 30_000);

        child.once("exit", (code, signal) => {
            clearTimeout(timeout);
            rl.close();
            reject(
                new Error(
                    `Session-expiry MCP server exited before listening. code=${code} signal=${signal} ${stderr.join("")}`
                )
            );
        });

        rl.on("line", (line) => {
            const match = /^Listening: (.+)$/.exec(line);
            if (!match) {
                return;
            }
            clearTimeout(timeout);
            rl.close();
            resolvePromise(match[1]);
        });
    });

    return {
        url,
        stats: async () => {
            const response = await fetch(`${url}/__stats`);
            if (!response.ok) {
                throw new Error(`Failed to read MCP server stats: ${response.status}`);
            }
            return (await response.json()) as McpServerStats;
        },
        expireSessions: async () => {
            const response = await fetch(`${url}/__expire`, { method: "POST" });
            if (!response.ok) {
                throw new Error(`Failed to expire MCP sessions: ${response.status}`);
            }
        },
    };
}
