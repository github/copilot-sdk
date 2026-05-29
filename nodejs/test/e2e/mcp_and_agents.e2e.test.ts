/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import type {
    CopilotSession,
    CustomAgentConfig,
    MCPStdioServerConfig,
    MCPServerConfig,
} from "../../src/index.js";
import { approveAll, defineTool } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const TEST_MCP_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-server.mjs");
const TEST_HARNESS_DIR = dirname(TEST_MCP_SERVER);

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

async function waitForMcpServerStatus(
    session: CopilotSession,
    serverName: string,
    expectedStatus = "connected"
): Promise<void> {
    const deadline = Date.now() + 60_000;
    let lastStatus = "<not listed>";

    while (Date.now() < deadline) {
        const result = await session.rpc.mcp.list();
        const server = result.servers.find((s) => s.name === serverName);
        if (server?.status === expectedStatus) {
            return;
        }
        lastStatus = server?.status ?? "<not listed>";
        await new Promise((resolve) => setTimeout(resolve, 200));
    }

    throw new Error(`${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}`);
}

describe("MCP Servers and Custom Agents", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

    describe("MCP Servers", () => {
        it("should accept MCP server configuration on session create", async () => {
            const mcpServers = createTestMcpServers("test-server");

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                mcpServers,
            });

            expect(session.sessionId).toBeDefined();
            await waitForMcpServerStatus(session, "test-server");

            // Simple interaction to verify session works
            const message = await session.sendAndWait({
                prompt: "What is 2+2?",
            });
            expect(message?.data.content).toContain("4");

            await session.disconnect();
        });

        it("should accept MCP server configuration without args", async () => {
            const mcpServers: Record<string, MCPServerConfig> = {
                "test-server": {
                    type: "local",
                    command: "git",
                    tools: ["*"],
                } as MCPStdioServerConfig,
            };

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                mcpServers,
            });

            expect(session.sessionId).toBeDefined();

            await session.disconnect();
        });

        it("should accept MCP server configuration on session resume", async () => {
            // Create a session first
            const session1 = await client.createSession({ onPermissionRequest: approveAll });
            const sessionId = session1.sessionId;
            await session1.sendAndWait({ prompt: "What is 1+1?" });

            // Resume with MCP servers
            const mcpServers = createTestMcpServers("test-server");

            const session2 = await client.resumeSession(sessionId, {
                onPermissionRequest: approveAll,
                mcpServers,
            });

            expect(session2.sessionId).toBe(sessionId);
            await waitForMcpServerStatus(session2, "test-server");

            await session2.disconnect();
        });

        it("should handle multiple MCP servers", async () => {
            const mcpServers = createTestMcpServers("server1", "server2");

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                mcpServers,
            });

            expect(session.sessionId).toBeDefined();
            await waitForMcpServerStatus(session, "server1");
            await waitForMcpServerStatus(session, "server2");
            await session.disconnect();
        });

        it("should pass literal env values to MCP server subprocess", async () => {
            const mcpServers: Record<string, MCPServerConfig> = {
                "env-echo": {
                    type: "local",
                    command: "node",
                    args: [TEST_MCP_SERVER],
                    tools: ["*"],
                    env: { TEST_SECRET: "hunter2" },
                    workingDirectory: TEST_HARNESS_DIR,
                } as MCPStdioServerConfig,
            };

            const session = await client.createSession({
                mcpServers,
                onPermissionRequest: approveAll,
            });

            expect(session.sessionId).toBeDefined();
            await waitForMcpServerStatus(session, "env-echo");

            const message = await session.sendAndWait({
                prompt: "Use the env-echo/get_env tool to read the TEST_SECRET environment variable. Reply with just the value, nothing else.",
            });
            expect(message?.data.content).toContain("hunter2");

            await session.disconnect();
        });
    });

    describe("Custom Agents", () => {
        it("should accept custom agent configuration on session create", async () => {
            const customAgents: CustomAgentConfig[] = [
                {
                    name: "test-agent",
                    displayName: "Test Agent",
                    description: "A test agent for SDK testing",
                    prompt: "You are a helpful test agent.",
                    infer: true,
                },
            ];

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                customAgents,
            });

            expect(session.sessionId).toBeDefined();

            // Simple interaction to verify session works
            const message = await session.sendAndWait({
                prompt: "What is 5+5?",
            });
            expect(message?.data.content).toContain("10");

            await session.disconnect();
        });

        it("should accept custom agent configuration on session resume", async () => {
            // Create a session first
            const session1 = await client.createSession({ onPermissionRequest: approveAll });
            const sessionId = session1.sessionId;
            await session1.sendAndWait({ prompt: "What is 1+1?" });

            // Resume with custom agents
            const customAgents: CustomAgentConfig[] = [
                {
                    name: "resume-agent",
                    displayName: "Resume Agent",
                    description: "An agent added on resume",
                    prompt: "You are a resume test agent.",
                },
            ];

            const session2 = await client.resumeSession(sessionId, {
                onPermissionRequest: approveAll,
                customAgents,
            });

            expect(session2.sessionId).toBe(sessionId);

            const message = await session2.sendAndWait({
                prompt: "What is 6+6?",
            });
            expect(message?.data.content).toContain("12");

            await session2.disconnect();
        });

        it("should handle custom agent with tools configuration", async () => {
            const customAgents: CustomAgentConfig[] = [
                {
                    name: "tool-agent",
                    displayName: "Tool Agent",
                    description: "An agent with specific tools",
                    prompt: "You are an agent with specific tools.",
                    tools: ["bash", "edit"],
                    infer: true,
                },
            ];

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await session.disconnect();
        });

        it("should handle custom agent with MCP servers", async () => {
            const customAgents: CustomAgentConfig[] = [
                {
                    name: "mcp-agent",
                    displayName: "MCP Agent",
                    description: "An agent with its own MCP servers",
                    prompt: "You are an agent with MCP servers.",
                    mcpServers: {
                        ...createTestMcpServers("agent-server"),
                    },
                },
            ];

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await session.disconnect();
        });

        it("should handle multiple custom agents", async () => {
            const customAgents: CustomAgentConfig[] = [
                {
                    name: "agent1",
                    displayName: "Agent One",
                    description: "First agent",
                    prompt: "You are agent one.",
                },
                {
                    name: "agent2",
                    displayName: "Agent Two",
                    description: "Second agent",
                    prompt: "You are agent two.",
                    infer: false,
                },
            ];

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await session.disconnect();
        });
    });

    describe("Combined Configuration", () => {
        it("should accept both MCP servers and custom agents", async () => {
            const mcpServers = createTestMcpServers("shared-server");

            const customAgents: CustomAgentConfig[] = [
                {
                    name: "combined-agent",
                    displayName: "Combined Agent",
                    description: "An agent using shared MCP servers",
                    prompt: "You are a combined test agent.",
                },
            ];

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                mcpServers,
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await waitForMcpServerStatus(session, "shared-server");

            await session.disconnect();
        });
    });

    describe("Default Agent Tool Exclusion", () => {
        it("should hide excluded tools from default agent", async () => {
            const secretTool = defineTool("secret_tool", {
                description: "A secret tool hidden from the default agent",
                parameters: z.object({
                    input: z.string().describe("Input to process"),
                }),
                handler: ({ input }) => `SECRET:${input}`,
            });

            const session = await client.createSession({
                onPermissionRequest: approveAll,
                tools: [secretTool],
                defaultAgent: {
                    excludedTools: ["secret_tool"],
                },
            });

            // Ask about the tool — the default agent should not see it
            const message = await session.sendAndWait({
                prompt: "Do you have access to a tool called secret_tool? Answer yes or no.",
            });

            // Sanity-check the replayed response (not the actual exclusion assertion)
            expect(message?.data.content?.toLowerCase()).toContain("no");

            // The real assertion: verify the runtime excluded the tool from the CAPI request
            const exchanges = await openAiEndpoint.getExchanges();
            const toolNames = exchanges.flatMap((e) =>
                (e.request.tools ?? []).map((t) => ("function" in t ? t.function.name : ""))
            );
            expect(toolNames).not.toContain("secret_tool");

            await session.disconnect();
        });

        it("should accept defaultAgent configuration on session resume", async () => {
            const session1 = await client.createSession({ onPermissionRequest: approveAll });
            const sessionId = session1.sessionId;
            await session1.sendAndWait({ prompt: "What is 3+3?" });

            const secretTool = defineTool("secret_tool", {
                description: "A secret tool hidden from the default agent",
                parameters: z.object({
                    input: z.string().describe("Input to process"),
                }),
                handler: ({ input }) => `SECRET:${input}`,
            });

            const session2 = await client.resumeSession(sessionId, {
                onPermissionRequest: approveAll,
                tools: [secretTool],
                defaultAgent: {
                    excludedTools: ["secret_tool"],
                },
            });

            expect(session2.sessionId).toBe(sessionId);

            const message = await session2.sendAndWait({
                prompt: "What is 4+4?",
            });
            expect(message?.data.content).toContain("8");

            await session2.disconnect();
        });
    });
});
