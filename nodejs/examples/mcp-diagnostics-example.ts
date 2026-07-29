/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { approveAll, CopilotClient } from "@github/copilot-sdk";

console.log("🚀 Starting MCP diagnostics example\n");

await using client = new CopilotClient({ logLevel: "info" });
await using session = await client.createSession({
    onPermissionRequest: approveAll,
    mcpServers: {
        filesystem: {
            type: "local",
            command: "npx",
            args: ["-y", "@modelcontextprotocol/server-filesystem", "."],
            tools: ["*"],
        },
    },
    onMcpDiagnostic: (diagnostic) => {
        switch (diagnostic.detail.kind) {
            case "wire_message":
                console.log(
                    `↔️ ${diagnostic.serverName} ${diagnostic.detail.direction} ${diagnostic.detail.method ?? ""}`
                );
                if (diagnostic.detail.truncated) {
                    console.log("   Captured payload was clipped.");
                }
                break;
            case "http_exchange":
                console.log(
                    `🌐 ${diagnostic.serverName} ${diagnostic.detail.phase} ${diagnostic.detail.statusCode ?? ""}`
                );
                break;
            case "server_log":
                console.error(`📋 ${diagnostic.serverName}: ${diagnostic.detail.line}`);
                break;
            case "process_lifecycle":
                console.log(
                    `⚙️ ${diagnostic.serverName} ${diagnostic.detail.command ?? ""} ${diagnostic.detail.exitCode ?? ""}`
                );
                break;
        }
    },
});

console.log(`✅ Session created: ${session.sessionId}\n`);
console.log("💬 Sending message...");
const result = await session.sendAndWait("Use the filesystem MCP server to list files here.");
console.log("📝 Response:", result?.data.content);
console.log("✅ Done!");
