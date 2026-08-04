#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Minimal MCP server that exposes a `get_env` tool.
 * Returns the value of a named environment variable from this process.
 * Used by SDK E2E tests to verify that literal env values reach MCP server subprocesses.
 *
 * Usage: npx tsx test-mcp-server.mjs
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { appendFile } from "node:fs/promises";
import { z } from "zod";

function getArgument(name) {
    const index = process.argv.indexOf(name);
    return index === -1 ? undefined : process.argv[index + 1];
}

const startupMarkerPath = getArgument("--startup-marker");
const serverName = getArgument("--server-name") ?? "env-echo";
const server = new McpServer({ name: serverName, version: "1.0.0" });

server.tool(
    "get_env",
    "Returns the value of the specified environment variable.",
    { name: z.string().describe("Environment variable name") },
    async ({ name }) => ({
        content: [{ type: "text", text: process.env[name] ?? "" }],
    }),
);

const transport = new StdioServerTransport();
if (startupMarkerPath) {
    await appendFile(startupMarkerPath, `${serverName}\n`);
}
await server.connect(transport);
