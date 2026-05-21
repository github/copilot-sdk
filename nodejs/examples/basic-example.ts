/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { z } from "zod";
import { approveAll, CopilotClient, defineTool } from "@github/copilot-sdk";

console.log("🚀 Starting Copilot SDK Example\n");

const facts: Record<string, string> = {
    javascript: "JavaScript was created in 10 days by Brendan Eich in 1995.",
    node: "Node.js lets you run JavaScript outside the browser using the V8 engine.",
};

const lookupFactTool = defineTool("lookup_fact", {
    description: "Returns a fun fact about a given topic.",
    parameters: z.object({
        topic: z.string().describe("Topic to look up (e.g. 'javascript', 'node')"),
    }),
    handler: ({ topic }) => facts[topic.toLowerCase()] ?? `No fact stored for ${topic}.`,
});

await using client = new CopilotClient({ logLevel: "info" });
await using session = await client.createSession({
    onPermissionRequest: approveAll,
    tools: [lookupFactTool],
});
console.log(`✅ Session created: ${session.sessionId}\n`);

session.on((event) => {
    console.log(`📢 Event [${event.type}]:`, JSON.stringify(event.data, null, 2));
});

console.log("💬 Sending message...");
const result1 = await session.sendAndWait("Tell me 2+2");
console.log("📝 Response:", result1?.data.content);

console.log("💬 Sending follow-up message...");
const result2 = await session.sendAndWait("Use lookup_fact to tell me about 'node'");
console.log("📝 Response:", result2?.data.content);

console.log("✅ Done!");
