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
import { z } from "zod";

const server = new McpServer({ name: "env-echo", version: "1.0.0" });

server.tool(
    "get_env",
    "Returns the value of the specified environment variable.",
    { name: z.string().describe("Environment variable name") },
    async ({ name }) => ({
        content: [{ type: "text", text: process.env[name] ?? "" }],
    }),
);

const transport = new StdioServerTransport();
await server.connect(transport);

