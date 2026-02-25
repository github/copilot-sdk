import { CopilotClient, defineTool, approveAll } from "@github/copilot-sdk";
import { z } from "zod";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      onPermissionRequest: approveAll,
      tools: [
        defineTool("grep", {
          description: "A custom grep implementation that overrides the built-in",
          parameters: z.object({
            query: z.string().describe("Search query"),
          }),
          handler: ({ query }) => `CUSTOM_GREP_RESULT: ${query}`,
        }),
      ],
    });

    const response = await session.sendAndWait({
      prompt: "Use grep to search for the word 'hello'",
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
