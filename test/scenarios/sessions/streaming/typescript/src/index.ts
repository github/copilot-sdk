import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-sonnet-4.6",
      streaming: true,
    });

    let chunkCount = 0;
    session.on("event", (event: { type: string }) => {
      if (event.type === "assistant.message.chunk") {
        chunkCount++;
      }
    });

    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
    });

    if (response) {
      console.log(response.data.content);
    }
    console.log(`\nStreaming chunks received: ${chunkCount}`);

    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
