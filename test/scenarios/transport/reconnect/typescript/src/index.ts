import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    cliUrl: process.env.COPILOT_CLI_URL || "localhost:3000",
  });

  try {
    // First session
    console.log("--- Session 1 ---");
    const session1 = await client.createSession({ model: "claude-haiku-4.5" });

    const response1 = await session1.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response1?.data.content) {
      console.log(response1.data.content);
    } else {
      console.error("No response content received for session 1");
      process.exit(1);
    }

    await session1.destroy();
    console.log("Session 1 destroyed\n");

    // Second session — tests that the server accepts new sessions
    console.log("--- Session 2 ---");
    const session2 = await client.createSession({ model: "claude-haiku-4.5" });

    const response2 = await session2.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response2?.data.content) {
      console.log(response2.data.content);
    } else {
      console.error("No response content received for session 2");
      process.exit(1);
    }

    await session2.destroy();
    console.log("Session 2 destroyed");

    console.log("\nReconnect test passed — both sessions completed successfully");
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
