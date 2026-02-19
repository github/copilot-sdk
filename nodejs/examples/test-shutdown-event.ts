/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient } from "../src/index.js";

console.log("🚀 Testing session.shutdown event\n");

// Create client
const client = new CopilotClient({ logLevel: "info" });
const session = await client.createSession();
console.log(`✅ Session created: ${session.sessionId}\n`);

// Track if we receive the shutdown event
let shutdownEventReceived = false;

// Listen to all events
session.on((event) => {
    console.log(`📢 Event [${event.type}]`);
    if (event.type === "session.shutdown") {
        shutdownEventReceived = true;
        console.log("✅ SHUTDOWN EVENT RECEIVED!");
        console.log("   Data:", JSON.stringify(event.data, null, 2));
    }
});

// Send a simple message
console.log("💬 Sending message...");
const result = await session.sendAndWait({ prompt: "What is 2+2?" });
console.log("📝 Response:", result?.data.content);

// Clean up
console.log("\n🔄 Destroying session...");
await session.destroy();

// Give a bit more time for any delayed events
await new Promise(resolve => setTimeout(resolve, 200));

console.log("\n" + (shutdownEventReceived ? "✅ SUCCESS: session.shutdown event was received!" : "❌ FAILURE: session.shutdown event was NOT received!"));

await client.stop();
console.log("\n✅ Done!");
