import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    cliUrl: process.env.COPILOT_CLI_URL || "localhost:3000",
  });

  try {
    const session = await client.createSession({ model: "claude-haiku-4.5" });

    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response?.data.content) {
      console.log(response.data.content);
    } else {
      console.error("No response content received");
      process.exit(1);
    }

    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
