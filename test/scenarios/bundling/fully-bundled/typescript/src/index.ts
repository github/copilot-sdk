import { CopilotClient , RuntimeConnection } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
  });

  try {
    const session = await client.createSession({ model: "claude-haiku-4.5" });

    const response = await session.sendAndWait({
      prompt: "What is the capital of France?",
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
