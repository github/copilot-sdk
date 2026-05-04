#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile } from "fs/promises";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const configIndex = process.argv.indexOf("--config");
if (configIndex === -1 || !process.argv[configIndex + 1]) {
    console.error("Usage: test-mcp-elicitation-server.mjs --config <path-to-config.json>");
    process.exit(1);
}

const configPath = process.argv[configIndex + 1];
const requests = JSON.parse(await readFile(configPath, "utf-8"));

const server = new McpServer({
    name: "test-elicitation-server",
    version: "1.0.0",
});

server.registerTool(
    "request_user_input",
    {
        description: "Request structured input from the user via an elicitation form",
        inputSchema: {},
    },
    async () => {
        const results = [];

        for (const request of requests) {
            const result = await server.server.elicitInput(request);
            results.push({ action: result.action, content: result.content });

            if (result.action !== "accept") {
                break;
            }
        }

        return {
            content: [{ type: "text", text: JSON.stringify({ results }) }],
        };
    },
);

const transport = new StdioServerTransport();
await server.connect(transport);
