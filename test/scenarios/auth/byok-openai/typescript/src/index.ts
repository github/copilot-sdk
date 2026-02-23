import { CopilotClient } from "@github/copilot-sdk";

const OPENAI_BASE_URL = process.env.OPENAI_BASE_URL ?? "https://api.openai.com/v1";
const OPENAI_MODEL = process.env.OPENAI_MODEL ?? "claude-haiku-4.5";
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;

if (!OPENAI_API_KEY) {
  console.error("Missing OPENAI_API_KEY.");
  process.exit(1);
}

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
  });

  try {
    const session = await client.createSession({
      model: OPENAI_MODEL,
      provider: {
        type: "openai",
        baseUrl: OPENAI_BASE_URL,
        apiKey: OPENAI_API_KEY,
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
