/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import type { CustomAgentConfig, MCPLocalServerConfig, MCPServerConfig } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("MCP Servers and Custom Agents", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    describe("MCP Servers", () => {
        it("should accept MCP server configuration on session create", async () => {
            const mcpServers: Record<string, MCPServerConfig> = {
                "test-server": {
                    type: "local",
                    command: "echo",
                    args: ["hello"],
                    tools: ["*"],
                } as MCPLocalServerConfig,
            };

            const session = await client.createSession({
                mcpServers,
            });

            expect(session.sessionId).toBeDefined();

            // Simple interaction to verify session works
            const message = await session.sendAndWait({
                prompt: "What is 2+2?",
            });
            expect(message?.data.content).toContain("4");

            await session.destroy();
        });

        it("should accept MCP server configuration on session resume", async () => {
            // Create a session first
            const session1 = await client.createSession();
            const sessionId = session1.sessionId;
            await session1.sendAndWait({ prompt: "What is 1+1?" });

            // Resume with MCP servers
            const mcpServers: Record<string, MCPServerConfig> = {
                "test-server": {
                    type: "local",
                    command: "echo",
                    args: ["hello"],
                    tools: ["*"],
                } as MCPLocalServerConfig,
            };

            const session2 = await client.resumeSession(sessionId, {
                mcpServers,
            });

            expect(session2.sessionId).toBe(sessionId);

            const message = await session2.sendAndWait({
                prompt: "What is 3+3?",
            });
            expect(message?.data.content).toContain("6");

            await session2.destroy();
        });

        it("should handle multiple MCP servers", async () => {
            const mcpServers: Record<string, MCPServerConfig> = {
                server1: {
                    type: "local",
                    command: "echo",
                    args: ["server1"],
                    tools: ["*"],
                } as MCPLocalServerConfig,
                server2: {
                    type: "local",
                    command: "echo",
                    args: ["server2"],
                    tools: ["*"],
                } as MCPLocalServerConfig,
            };

            const session = await client.createSession({
                mcpServers,
            });

            expect(session.sessionId).toBeDefined();
            await session.destroy();
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
                customAgents,
            });

            expect(session.sessionId).toBeDefined();

            // Simple interaction to verify session works
            const message = await session.sendAndWait({
                prompt: "What is 5+5?",
            });
            expect(message?.data.content).toContain("10");

            await session.destroy();
        });

        it("should accept custom agent configuration on session resume", async () => {
            // Create a session first
            const session1 = await client.createSession();
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
                customAgents,
            });

            expect(session2.sessionId).toBe(sessionId);

            const message = await session2.sendAndWait({
                prompt: "What is 6+6?",
            });
            expect(message?.data.content).toContain("12");

            await session2.destroy();
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
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await session.destroy();
        });

        it("should handle custom agent with MCP servers", async () => {
            const customAgents: CustomAgentConfig[] = [
                {
                    name: "mcp-agent",
                    displayName: "MCP Agent",
                    description: "An agent with its own MCP servers",
                    prompt: "You are an agent with MCP servers.",
                    mcpServers: {
                        "agent-server": {
                            type: "local",
                            command: "echo",
                            args: ["agent-mcp"],
                            tools: ["*"],
                        } as MCPLocalServerConfig,
                    },
                },
            ];

            const session = await client.createSession({
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await session.destroy();
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
                customAgents,
            });

            expect(session.sessionId).toBeDefined();
            await session.destroy();
        });
    });

    describe("Combined Configuration", () => {
        it("should accept both MCP servers and custom agents", async () => {
            const mcpServers: Record<string, MCPServerConfig> = {
                "shared-server": {
                    type: "local",
                    command: "echo",
                    args: ["shared"],
                    tools: ["*"],
                } as MCPLocalServerConfig,
            };

            const customAgents: CustomAgentConfig[] = [
                {
                    name: "combined-agent",
                    displayName: "Combined Agent",
                    description: "An agent using shared MCP servers",
                    prompt: "You are a combined test agent.",
                },
            ];

            const session = await client.createSession({
                mcpServers,
                customAgents,
            });

            expect(session.sessionId).toBeDefined();

            const message = await session.sendAndWait({
                prompt: "What is 7+7?",
            });
            expect(message?.data.content).toContain("14");

            await session.destroy();
        });
    });
});
