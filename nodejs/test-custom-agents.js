import { CopilotClient } from "./dist/index.js";

const customAgents = [
    {
        name: "test-agent",
        displayName: "Test Agent",
        description: "A test agent for SDK testing",
        prompt: "You are a helpful test agent.",
        infer: true,
    },
];

const client = new CopilotClient();

try {
    console.log("Creating session with custom agents...");
    const session = await client.createSession({
        model: "gpt-4o-mini",
        customAgents: customAgents,
    });

    console.log(`Session created: ${session.sessionId}`);

    console.log("\nAsking assistant to list custom agents...");
    const message = await session.sendAndWait({
        prompt: "List all custom agents available. Be specific about user-defined or user-provided custom agents that were configured for this session.",
    });

    console.log("\n=== Assistant Response ===");
    console.log(message?.data.content || "No content");
    console.log("==========================\n");

    // Check if the response mentions our custom agent
    const content = (message?.data.content || "").toLowerCase();
    if (content.includes("test-agent") || content.includes("test agent")) {
        console.log("✅ SUCCESS: Custom agent 'test-agent' is visible to the assistant!");
    } else if (content.includes("user-defined") || content.includes("user-provided")) {
        console.log("⚠️  PARTIAL: Response mentions user-defined/provided agents but doesn't specifically list test-agent");
    } else {
        console.log("❌ FAIL: Custom agent 'test-agent' is NOT visible to the assistant");
    }

    await session.destroy();
    await client.stop();
} catch (error) {
    console.error("Error:", error);
    await client.stop();
    process.exit(1);
}
