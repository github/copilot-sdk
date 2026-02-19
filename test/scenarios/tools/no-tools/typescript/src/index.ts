import { CopilotClient } from "@github/copilot-sdk";

const SYSTEM_PROMPT = `You are a minimal assistant with no tools available.
You cannot execute code, read files, edit files, search, or perform any actions.
You can only respond with text based on your training data.
If asked about your capabilities or tools, clearly state that you have no tools available.`;

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      systemMessage: { mode: "replace", content: SYSTEM_PROMPT },
      availableTools: [],
    });

    const response = await session.sendAndWait({
      prompt: "Use the bash tool to run 'echo hello'.",
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
