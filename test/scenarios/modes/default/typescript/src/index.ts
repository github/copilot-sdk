import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
    });

    const response = await session.sendAndWait({
      prompt: "Use the grep tool to search for the word 'SDK' in README.md and show the matching lines.",
    });

    if (response) {
      console.log(`Response: ${response.data.content}`);
    }

    console.log("Default mode test complete");

    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
