import { CopilotClient } from "@github/copilot-sdk";

const PIRATE_PROMPT = `You are a pirate. Always respond in pirate speak. Say 'Arrr!' in every response. Use nautical terms and pirate slang throughout.`;

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      systemMessage: { mode: "replace", content: PIRATE_PROMPT },
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
