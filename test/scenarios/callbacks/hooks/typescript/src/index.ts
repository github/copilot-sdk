import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const hookLog: string[] = [];

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && { cliPath: process.env.COPILOT_CLI_PATH }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      onPermissionRequest: async () => ({ kind: "approved" as const }),
      hooks: {
        onSessionStart: async () => {
          hookLog.push("onSessionStart");
        },
        onSessionEnd: async () => {
          hookLog.push("onSessionEnd");
        },
        onPreToolUse: async (input) => {
          hookLog.push(`onPreToolUse:${input.toolName}`);
          return { permissionDecision: "allow" as const };
        },
        onPostToolUse: async (input) => {
          hookLog.push(`onPostToolUse:${input.toolName}`);
        },
        onUserPromptSubmitted: async (input) => {
          hookLog.push("onUserPromptSubmitted");
          return input;
        },
        onErrorOccurred: async (input) => {
          hookLog.push(`onErrorOccurred:${input.error}`);
        },
      },
    });

    const response = await session.sendAndWait({
      prompt: "List the files in the current directory using the glob tool with pattern '*.md'.",
    });

    if (response) {
      console.log(response.data.content);
    }

    await session.destroy();

    console.log("\n--- Hook execution log ---");
    for (const entry of hookLog) {
      console.log(`  ${entry}`);
    }
    console.log(`\nTotal hooks fired: ${hookLog.length}`);
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
