/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import type { CopilotSession, MCPServerConfig, MCPStdioServerConfig } from "../../src/index.js";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { formatError, waitForCondition } from "./harness/sdkTestHelper.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const TEST_MCP_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-server.mjs");
const TEST_HARNESS_DIR = dirname(TEST_MCP_SERVER);

describe("Session-scoped MCP lifecycle RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    function createTestMcpServers(...serverNames: string[]): Record<string, MCPServerConfig> {
        return Object.fromEntries(
            serverNames.map((name) => [
                name,
                {
                    type: "local",
                    command: "node",
                    args: [TEST_MCP_SERVER],
                    workingDirectory: TEST_HARNESS_DIR,
                    tools: ["*"],
                } as MCPStdioServerConfig,
            ])
        );
    }

    async function createSessionWithMcp(serverName: string): Promise<CopilotSession> {
        return client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: createTestMcpServers(serverName),
        });
    }

    async function waitForMcpServerStatus(
        session: CopilotSession,
        serverName: string,
        expectedStatus = "connected"
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
                timeoutMs: 60_000,
                intervalMs: 200,
                timeoutMessage: `${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}`,
            }
        );
    }

    async function waitForMcpRunning(
        session: CopilotSession,
        serverName: string,
        expectedRunning: boolean
    ): Promise<void> {
        await waitForCondition(
            async () =>
                (await session.rpc.mcp.isServerRunning({ serverName })).running === expectedRunning,
            {
                timeoutMs: 60_000,
                intervalMs: 200,
                timeoutMessage: `${serverName} running=${expectedRunning}`,
            }
        );
    }

    function missingName(prefix: string): string {
        return `${prefix}-${randomUUID().replace(/-/g, "")}`;
    }

    function assertNotUnhandledMethod(message: string): void {
        expect(message.toLowerCase()).not.toContain("unhandled method");
    }

    it(
        "should list tools and report running status for connected server",
        { timeout: 120_000 },
        async () => {
            const serverName = "rpc-lifecycle-list-server";
            const session = await createSessionWithMcp(serverName);
            try {
                await waitForMcpServerStatus(session, serverName);

                const tools = await session.rpc.mcp.listTools({ serverName });
                expect(tools.tools.length).toBeGreaterThan(0);
                for (const tool of tools.tools) {
                    expect(tool.name).toBeTruthy();
                }

                expect((await session.rpc.mcp.isServerRunning({ serverName })).running).toBe(true);
                expect(
                    (
                        await session.rpc.mcp.isServerRunning({
                            serverName: missingName("missing"),
                        })
                    ).running
                ).toBe(false);
            } finally {
                await session.disconnect();
            }
        }
    );

    it("should throw when listing tools for unconnected server", { timeout: 120_000 }, async () => {
        const serverName = "rpc-lifecycle-unconnected-host";
        const session = await createSessionWithMcp(serverName);
        try {
            await waitForMcpServerStatus(session, serverName);

            await expect(
                session.rpc.mcp.listTools({ serverName: missingName("missing") })
            ).rejects.toSatisfy((error: unknown) => {
                const message = formatError(error);
                assertNotUnhandledMethod(message);
                expect(message.toLowerCase()).toContain("not connected");
                return true;
            });
        } finally {
            await session.disconnect();
        }
    });

    it("should stop running mcp server", { timeout: 180_000 }, async () => {
        const serverName = "rpc-lifecycle-stop-server";
        const session = await createSessionWithMcp(serverName);
        try {
            await waitForMcpServerStatus(session, serverName);
            expect((await session.rpc.mcp.isServerRunning({ serverName })).running).toBe(true);

            await session.rpc.mcp.stopServer({ serverName });

            await waitForMcpRunning(session, serverName, false);
        } finally {
            await session.disconnect();
        }
    });
});
