/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { appendFile, rename, writeFile } from "node:fs/promises";
import { joinSession } from "@github/copilot-sdk/extension";

const url = process.env.EXTENSION_HTTP_MCP_URL;
const statusPath = process.env.EXTENSION_HTTP_MCP_STATUS;
const callsPath = process.env.EXTENSION_HTTP_MCP_CALLS;
if (!url || !statusPath || !callsPath) {
    throw new Error("HTTP MCP extension fixture requires its URL, status path, and calls path");
}

async function recordStatus(status) {
    const pendingPath = `${statusPath}.pending`;
    await writeFile(pendingPath, JSON.stringify(status));
    await rename(pendingPath, statusPath);
}

const mcpServers = {
    extensionprobe: { type: "http", url, tools: ["http_probe"], timeout: 5000 },
};

try {
    const session = await joinSession({
        tools: [
            {
                name: "extension_direct_probe",
                description: "Record a marker in the extension process and return it.",
                parameters: {
                    type: "object",
                    properties: { marker: { type: "string" } },
                    required: ["marker"],
                    additionalProperties: false,
                },
                handler: async ({ marker }) => {
                    if (typeof marker !== "string") {
                        throw new Error("Direct probe requires a string marker");
                    }
                    await appendFile(callsPath, `${JSON.stringify({ marker })}\n`);
                    return `EXTENSION_DIRECT_REPLY_${marker}`;
                },
            },
        ],
        mcpServers,
    });
    await recordStatus({
        state: "joined",
        sessionId: session.sessionId,
        sdkPath: process.env.COPILOT_SDK_PATH,
        mcpServers,
    });
} catch (error) {
    await recordStatus({
        state: "failed",
        error: error instanceof Error ? error.message : String(error),
    });
    throw error;
}
