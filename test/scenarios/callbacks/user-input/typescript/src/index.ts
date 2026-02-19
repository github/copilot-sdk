import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const inputLog: string[] = [];

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      onPermissionRequest: async () => ({ kind: "approved" as const }),
      onUserInputRequest: async (request) => {
        inputLog.push(`question: ${request.question}`);
        return { answer: "Paris", wasFreeform: true };
      },
      hooks: {
        onPreToolUse: async () => ({ permissionDecision: "allow" as const }),
      },
    });

    const response = await session.sendAndWait({
      prompt: "I want to learn about a city. Use the ask_user tool to ask me which city I'm interested in. Then tell me about that city.",
    });

    if (response) {
      console.log(response.data.content);
    }

    await session.destroy();

    console.log("\n--- User input log ---");
    for (const entry of inputLog) {
      console.log(`  ${entry}`);
    }
    console.log(`\nTotal user input requests: ${inputLog.length}`);
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
