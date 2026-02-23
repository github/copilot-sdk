import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const endpoint = process.env.AZURE_OPENAI_ENDPOINT;
  const apiKey = process.env.AZURE_OPENAI_API_KEY;
  const model = process.env.AZURE_OPENAI_MODEL || "claude-haiku-4.5";

  if (!endpoint || !apiKey) {
    console.error("Required: AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY");
    process.exit(1);
  }

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
  });

  try {
    const session = await client.createSession({
      model,
      provider: {
        type: "azure",
        baseUrl: endpoint,
        apiKey,
        azure: {
          apiVersion: process.env.AZURE_API_VERSION || "2024-10-21",
        },
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
