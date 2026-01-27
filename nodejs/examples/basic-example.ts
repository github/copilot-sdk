/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { z } from "zod";
import { CopilotClient, defineTool } from "../src/index.js";

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

// Create client - will auto-start CLI server (searches PATH for "copilot")
const client = new CopilotClient({ logLevel: "info" });
const session = await client.createSession({ tools: [lookupFactTool] });
console.log(`✅ Session created: ${session.sessionId}\n`);

// Listen to events
session.on((event) => {
    console.log(`📢 Event [${event.type}]:`, JSON.stringify(event.data, null, 2));
});

// Send a simple message
console.log("💬 Sending message...");
await session.sendAndWait({ prompt: "Tell me 2+2" });

// Send another message that uses the tool
console.log("💬 Sending follow-up message...");
await session.sendAndWait({ prompt: "Now use lookup_fact to tell me something about Node.js." });

// Clean up
await session.destroy();
await client.stop();
console.log("✅ Done!");
