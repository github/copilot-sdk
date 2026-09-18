#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
    CallToolRequestSchema,
    ListToolsRequestSchema,
    ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

const resourceUri = "ui://scenario/app";
const server = new Server(
    { name: "scenario-mcp-app", version: "1.0.0" },
    { capabilities: { resources: {}, tools: {} } }
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: "app_visible",
            description: "Visible to MCP App views.",
            inputSchema: {
                type: "object",
                properties: { value: { type: "string" } },
                required: ["value"],
            },
            _meta: { "ui.visibility": ["model", "app"] },
        },
    ],
}));

server.setRequestHandler(CallToolRequestSchema, async request => ({
    content: [
        {
            type: "text",
            text: `APP_VISIBLE:${request.params.arguments?.value ?? ""}`,
        },
    ],
}));

server.setRequestHandler(ReadResourceRequestSchema, async request => {
    if (request.params.uri !== resourceUri) {
        throw new Error(`Unknown resource: ${request.params.uri}`);
    }

    return {
        contents: [
            {
                uri: resourceUri,
                mimeType: "text/html",
                text: "<html><body>SCENARIO_MCP_APP</body></html>",
                _meta: {
                    "ui.csp": {
                        connectDomains: ["https://api.example.test"],
                    },
                },
            },
        ],
    };
});

await server.connect(new StdioServerTransport());
