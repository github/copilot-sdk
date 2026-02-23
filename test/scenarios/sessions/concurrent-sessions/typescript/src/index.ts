import { CopilotClient } from "@github/copilot-sdk";

const PIRATE_PROMPT = `You are a pirate. Always say Arrr!`;
const ROBOT_PROMPT = `You are a robot. Always say BEEP BOOP!`;

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const [session1, session2] = await Promise.all([
      client.createSession({
        model: "claude-haiku-4.5",
        systemMessage: { mode: "replace", content: PIRATE_PROMPT },
        availableTools: [],
      }),
      client.createSession({
        model: "claude-haiku-4.5",
        systemMessage: { mode: "replace", content: ROBOT_PROMPT },
        availableTools: [],
      }),
    ]);

    const [response1, response2] = await Promise.all([
      session1.sendAndWait({ prompt: "What is the capital of France?" }),
      session2.sendAndWait({ prompt: "What is the capital of France?" }),
    ]);

    if (response1) {
      console.log("Session 1 (pirate):", response1.data.content);
    }
    if (response2) {
      console.log("Session 2 (robot):", response2.data.content);
    }

    await Promise.all([session1.destroy(), session2.destroy()]);
  } finally {
    await client.stop();
    process.exit(0);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
