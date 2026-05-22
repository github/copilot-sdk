#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Minimal MCP server that exposes an `echo_meta` tool.
 * Returns the value passed in along with the `_meta` received in the tools/call request.
 * Used by SDK E2E tests to verify that preMcpToolCall hook meta modifications
 * reach the MCP server subprocess.
 *
 * Usage: node test-mcp-meta-echo-server.mjs
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { ListToolsRequestSchema, CallToolRequestSchema } from "@modelcontextprotocol/sdk/types.js";

const server = new Server(
    { name: "meta-echo", version: "1.0.0" },
    { capabilities: { tools: {} } }
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: "echo_meta",
            description: "Echoes the value and the _meta received in the request.",
            inputSchema: {
                type: "object",
                properties: {
                    value: { type: "string", description: "A value to echo back" },
                },
                required: ["value"],
            },
        },
    ],
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args, _meta } = request.params;
    if (name !== "echo_meta") {
        return {
            content: [{ type: "text", text: `Unknown tool: ${name}` }],
            isError: true,
        };
    }
    const value = args?.value ?? "";
    // Filter out system-injected meta keys (progressToken from MCP SDK,
    // trace context from runtime) so tests only see hook-provided meta.
    const systemKeys = new Set(["progressToken", "traceparent", "tracestate"]);
    const hookMeta = _meta
        ? Object.fromEntries(Object.entries(_meta).filter(([k]) => !systemKeys.has(k)))
        : null;
    const resultMeta = hookMeta && Object.keys(hookMeta).length > 0 ? hookMeta : null;
    return {
        content: [
            { type: "text", text: JSON.stringify({ meta: resultMeta, value }) },
        ],
    };
});

const transport = new StdioServerTransport();
await server.connect(transport);
