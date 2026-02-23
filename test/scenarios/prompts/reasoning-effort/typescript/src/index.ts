import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    // Test with "low" reasoning effort
    const session = await client.createSession({
      model: "claude-opus-4.6",
      reasoningEffort: "low",
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
      console.log(`Reasoning effort: low`);
      console.log(`Response: ${response.data.content}`);
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
