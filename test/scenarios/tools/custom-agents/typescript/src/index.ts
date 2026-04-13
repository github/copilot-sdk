import { CopilotClient, defineTool } from "@github/copilot-sdk";
import { z } from "zod";

const analyzeCodebase = defineTool({
    name: "analyze-codebase",
    description: "Performs deep analysis of the codebase, generating extensive context",
    parameters: z.object({ query: z.string().describe("The analysis query") }),
    handler: async ({ query }) => {
        return `Analysis result for: ${query}`;
    },
});

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      tools: [analyzeCodebase],
      defaultAgent: {
        excludedTools: ["analyze-codebase"],
      },
      customAgents: [
        {
          name: "researcher",
          displayName: "Research Agent",
          description: "A research agent that can only read and search files, not modify them",
          tools: ["grep", "glob", "view", "analyze-codebase"],
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

    await session.disconnect();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
