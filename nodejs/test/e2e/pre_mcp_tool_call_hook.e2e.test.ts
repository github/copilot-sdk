/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { MCPStdioServerConfig, PreMcpToolCallHookInput } from "../../src/types.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const TEST_MCP_META_ECHO_SERVER = resolve(
    __dirname,
    "../../../test/harness/test-mcp-meta-echo-server.mjs"
);
const TEST_HARNESS_DIR = dirname(TEST_MCP_META_ECHO_SERVER);

describe("pre_mcp_tool_call_hook", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should set meta via preMcpToolCall hook", async () => {
        const hookInputs: PreMcpToolCallHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: {
                "meta-echo": {
                    command: "node",
                    args: [TEST_MCP_META_ECHO_SERVER],
                    cwd: TEST_HARNESS_DIR,
                    tools: ["*"],
                } as MCPStdioServerConfig,
            },
            hooks: {
                onPreMcpToolCall: async (input, _invocation) => {
                    hookInputs.push(input);
                    return { metaToUse: { injected: "by-hook", source: "test" } };
                },
            },
        });

        const message = await session.sendAndWait({
            prompt: "Use the meta-echo/echo_meta tool with value 'test-set'. Reply with just the raw tool result.",
        });

        expect(message).not.toBeNull();
        expect(message!.data.content).toContain("injected");
        expect(message!.data.content).toContain("by-hook");

        expect(hookInputs.length).toBeGreaterThan(0);
        expect(hookInputs[0].serverName).toBe("meta-echo");
        expect(hookInputs[0].toolName).toBe("echo_meta");
        expect(hookInputs[0].workingDirectory).toBeDefined();
        expect(hookInputs[0].timestamp).toBeInstanceOf(Date);

        await session.disconnect();
    });

    it("should replace meta via preMcpToolCall hook", async () => {
        const hookInputs: PreMcpToolCallHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: {
                "meta-echo": {
                    command: "node",
                    args: [TEST_MCP_META_ECHO_SERVER],
                    cwd: TEST_HARNESS_DIR,
                    tools: ["*"],
                } as MCPStdioServerConfig,
            },
            hooks: {
                onPreMcpToolCall: async (input, _invocation) => {
                    hookInputs.push(input);
                    return { metaToUse: { completely: "replaced" } };
                },
            },
        });

        const message = await session.sendAndWait({
            prompt: "Use the meta-echo/echo_meta tool with value 'test-replace'. Reply with just the raw tool result.",
        });

        expect(message).not.toBeNull();
        expect(message!.data.content).toContain("completely");
        expect(message!.data.content).toContain("replaced");

        expect(hookInputs.length).toBeGreaterThan(0);
        expect(hookInputs[0].serverName).toBe("meta-echo");
        expect(hookInputs[0].toolName).toBe("echo_meta");

        await session.disconnect();
    });

    it("should remove meta via preMcpToolCall hook", async () => {
        const hookInputs: PreMcpToolCallHookInput[] = [];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            mcpServers: {
                "meta-echo": {
                    command: "node",
                    args: [TEST_MCP_META_ECHO_SERVER],
                    cwd: TEST_HARNESS_DIR,
                    tools: ["*"],
                } as MCPStdioServerConfig,
            },
            hooks: {
                onPreMcpToolCall: async (input, _invocation) => {
                    hookInputs.push(input);
                    return { metaToUse: null };
                },
            },
        });

        const message = await session.sendAndWait({
            prompt: "Use the meta-echo/echo_meta tool with value 'test-remove'. Reply with just the raw tool result.",
        });

        expect(message).not.toBeNull();
        expect(message!.data.content).toContain('"meta":null');
        expect(message!.data.content).toContain("test-remove");

        expect(hookInputs.length).toBeGreaterThan(0);
        expect(hookInputs[0].serverName).toBe("meta-echo");
        expect(hookInputs[0].toolName).toBe("echo_meta");

        await session.disconnect();
    });
});
