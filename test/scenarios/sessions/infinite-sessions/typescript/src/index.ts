import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      availableTools: [],
      systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant. Answer concisely in one sentence.",
      },
      infiniteSessions: {
        enabled: true,
        backgroundCompactionThreshold: 0.80,
        bufferExhaustionThreshold: 0.95,
      },
    });

    const prompts = [
      "What is the capital of France?",
      "What is the capital of Japan?",
      "What is the capital of Brazil?",
    ];

    for (const prompt of prompts) {
      const response = await session.sendAndWait({ prompt });
      if (response) {
        console.log(`Q: ${prompt}`);
        console.log(`A: ${response.data.content}\n`);
      }
    }

    console.log("Infinite sessions test complete — all messages processed successfully");

    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
