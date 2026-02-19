import { CopilotClient } from "@github/copilot-sdk";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      availableTools: [],
      systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant. Answer questions about attached files concisely.",
      },
    });

    const sampleFile = path.resolve(__dirname, "../../sample-data.txt");

    const response = await session.sendAndWait({
      prompt: "What languages are listed in the attached file?",
      attachments: [{ type: "file", path: sampleFile }],
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
