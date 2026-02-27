import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    // MCP server config — demonstrates the configuration pattern.
    // When MCP_SERVER_CMD is set, connects to a real MCP server.
    // Otherwise, runs without MCP tools as a build/integration test.
    const mcpServers: Record<string, any> = {};
    if (process.env.MCP_SERVER_CMD) {
      mcpServers["example"] = {
        type: "stdio",
        command: process.env.MCP_SERVER_CMD,
        args: process.env.MCP_SERVER_ARGS ? process.env.MCP_SERVER_ARGS.split(" ") : [],
      };
    }

    const session = await client.createSession({
      model: "claude-haiku-4.5",
      ...(Object.keys(mcpServers).length > 0 && { mcpServers }),
      availableTools: [],
      systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant. Answer questions concisely.",
      },
    });

    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response) {
      console.log(response.data.content);
    }

    if (Object.keys(mcpServers).length > 0) {
      console.log("\nMCP servers configured: " + Object.keys(mcpServers).join(", "));
    } else {
      console.log("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)");
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
