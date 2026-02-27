import { CopilotClient } from "@github/copilot-sdk";

const OLLAMA_BASE_URL = process.env.OLLAMA_BASE_URL ?? "http://localhost:11434/v1";
const OLLAMA_MODEL = process.env.OLLAMA_MODEL ?? "llama3.2:3b";

const COMPACT_SYSTEM_PROMPT =
  "You are a compact local assistant. Keep answers short, concrete, and under 80 words.";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
  });

  try {
    const session = await client.createSession({
      model: OLLAMA_MODEL,
      provider: {
        type: "openai",
        baseUrl: OLLAMA_BASE_URL,
      },
      // Use a compact replacement prompt and no tools to minimize request context.
      systemMessage: { mode: "replace", content: COMPACT_SYSTEM_PROMPT },
      availableTools: [],
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
