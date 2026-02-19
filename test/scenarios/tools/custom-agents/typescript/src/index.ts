import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      customAgents: [
        {
          name: "researcher",
          displayName: "Research Agent",
          description: "A research agent that can only read and search files, not modify them",
          tools: ["grep", "glob", "view"],
          prompt: "You are a research assistant. You can search and read files but cannot modify anything. When asked about your capabilities, list the tools you have access to.",
        },
      ],
    });

    const response = await session.sendAndWait({
      prompt: "What custom agents are available? Describe the researcher agent and its capabilities.",
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
