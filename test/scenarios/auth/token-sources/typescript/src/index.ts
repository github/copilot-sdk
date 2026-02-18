import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  // Demonstrate token source resolution
  // Priority: explicit githubToken > COPILOT_GITHUB_TOKEN > GH_TOKEN > GITHUB_TOKEN > gh CLI
  const tokenSource =
    process.env.COPILOT_GITHUB_TOKEN ? "COPILOT_GITHUB_TOKEN" :
    process.env.GH_TOKEN ? "GH_TOKEN" :
    process.env.GITHUB_TOKEN ? "GITHUB_TOKEN" :
    "gh CLI or stored OAuth";

  const token =
    process.env.COPILOT_GITHUB_TOKEN ||
    process.env.GH_TOKEN ||
    process.env.GITHUB_TOKEN;

  console.log(`Token source resolved: ${tokenSource}`);

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    ...(token && { githubToken: token }),
  });

  try {
    const session = await client.createSession({
      model: "gpt-4.1",
      availableTools: [],
      systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant. Answer concisely.",
      },
    });

    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response) {
      console.log(response.data.content);
    }

    console.log("\nAuth test passed — token resolved successfully");

    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
