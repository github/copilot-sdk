import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  const model = process.env.ANTHROPIC_MODEL || "claude-sonnet-4-20250514";

  if (!apiKey) {
    console.error("Required: ANTHROPIC_API_KEY");
    process.exit(1);
  }

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
  });

  try {
    const session = await client.createSession({
      model,
      provider: {
        type: "anthropic",
        baseUrl: process.env.ANTHROPIC_BASE_URL || "https://api.anthropic.com",
        apiKey,
      },
      availableTools: [],
      systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant. Answer concisely.",
      },
    });

    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response) {
      console.log(response.data.content);
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
