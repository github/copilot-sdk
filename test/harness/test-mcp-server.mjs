#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Minimal MCP server that exposes a `get_env` tool and deterministic prompts.
 * Returns the value of a named environment variable from this process.
 * Used by SDK E2E tests to verify that literal env values reach MCP server subprocesses.
 *
 * Usage: npx tsx test-mcp-server.mjs
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { existsSync } from "node:fs";
import { setTimeout } from "node:timers/promises";
import {
    GetPromptRequestSchema,
    ListPromptsRequestSchema,
    McpError,
    ErrorCode,
} from "@modelcontextprotocol/sdk/types.js";
import { appendFile, readFile } from "node:fs/promises";
import { z } from "zod";

function getArgument(name) {
    const index = process.argv.indexOf(name);
    return index === -1 ? undefined : process.argv[index + 1];
}

const startupMarkerPath = getArgument("--startup-marker");
const diagnosticStderr = getArgument("--diagnostic-stderr");
const serverName = getArgument("--server-name") ?? "env-echo";
const toolName = getArgument("--tool-name") ?? "get_env";
const server = new McpServer({ name: serverName, version: "1.0.0" });
const fixtures = JSON.parse(await readFile(new URL("./mcp-prompt-fixtures.json", import.meta.url), "utf8"));
// Fixtures contain SDK extension bags; MCP sends those entries as ordinary object fields.
function toMcpObject(value) {
    const { additionalProperties, ...fields } = value;
    return { ...fields, ...additionalProperties };
}

function toMcpPromptPage(page) {
    return {
        ...toMcpObject(page),
        prompts: page.prompts.map((prompt) => ({
            ...toMcpObject(prompt),
            ...(prompt.arguments ? { arguments: prompt.arguments.map(toMcpObject) } : {}),
            ...(prompt.icons ? { icons: prompt.icons.map(toMcpObject) } : {}),
        })),
    };
}

const prompts = {
    firstPage: toMcpPromptPage(fixtures.firstPage),
    secondPage: toMcpPromptPage(fixtures.secondPage),
    richPrompt: {
        ...toMcpObject(fixtures.richPrompt),
        messages: fixtures.richPrompt.messages.map(toMcpObject),
    },
};
let promptsChanged = false;

server.server.registerCapabilities({ prompts: { listChanged: true } });
server.server.setRequestHandler(ListPromptsRequestSchema, async ({ params }) => {
    if (params?.cursor === undefined) {
        return prompts.firstPage;
    }
    if (params.cursor !== prompts.firstPage.nextCursor) {
        throw new McpError(ErrorCode.InvalidParams, "Unknown prompt cursor");
    }
    return {
        ...prompts.secondPage,
        prompts: [...prompts.secondPage.prompts, ...(promptsChanged ? [{ name: "added" }] : [])],
    };
});
// Use the protocol handler so unknown content and extension fields reach clients unchanged.
server.server.setRequestHandler(GetPromptRequestSchema, async ({ params }) => {
    if (params.name === "rich") {
        if (params.arguments?.topic === undefined) {
            throw new McpError(ErrorCode.InvalidParams, "Missing required argument: topic");
        }
        return prompts.richPrompt;
    }
    if (params.name === "echo") {
        return {
            messages: [{ role: "user", content: { type: "text", text: JSON.stringify(params.arguments ?? null) } }],
        };
    }
    if (params.name === "refresh") {
        promptsChanged = true;
        await server.server.notification({ method: "notifications/prompts/list_changed" });
        return { messages: [] };
    }
    throw new McpError(ErrorCode.InvalidParams, `Unknown prompt: ${params.name}`);
});

server.tool(
    toolName,
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
const startupGate = getArgument("--startup-gate");
while (startupGate && !existsSync(startupGate)) {
    await setTimeout(20);
}
if (process.argv.includes("--fail-startup")) {
    throw new Error("MCP startup failed as requested by the test");
}
if (diagnosticStderr) {
    console.error(diagnosticStderr);
}
await server.connect(transport);
