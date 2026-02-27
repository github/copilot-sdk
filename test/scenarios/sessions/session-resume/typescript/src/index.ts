import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    // 1. Create a session
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      availableTools: [],
    });

    // 2. Send the secret word
    await session.sendAndWait({
      prompt: "Remember this: the secret word is PINEAPPLE.",
    });

    // 3. Get the session ID (don't destroy — resume needs the session to persist)
    const sessionId = session.sessionId;

    // 4. Resume the session with the same ID
    const resumed = await client.resumeSession(sessionId);
    console.log("Session resumed");

    // 5. Ask for the secret word
    const response = await resumed.sendAndWait({
      prompt: "What was the secret word I told you?",
    });

    if (response) {
      console.log(response.data.content);
    }

    await resumed.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
