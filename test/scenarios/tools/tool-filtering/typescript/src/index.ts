import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant. You have access to a limited set of tools. When asked about your tools, list exactly which tools you have available.",
      },
      availableTools: ["grep", "glob", "view"],
    });

    const response = await session.sendAndWait({
      prompt: "What tools do you have available? List each one by name.",
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
