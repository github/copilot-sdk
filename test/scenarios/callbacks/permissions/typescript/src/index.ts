import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const permissionLog: string[] = [];

  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && {
      cliPath: process.env.COPILOT_CLI_PATH,
    }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      onPermissionRequest: async (request) => {
        permissionLog.push(`approved:${request.toolName}`);
        return { kind: "approved" as const };
      },
      hooks: {
        onPreToolUse: async () => ({ permissionDecision: "allow" as const }),
      },
    });

    const response = await session.sendAndWait({
      prompt:
        "List the files in the current directory using glob with pattern '*.md'.",
    });

    if (response) {
      console.log(response.data.content);
    }

    await session.destroy();

    console.log("\n--- Permission request log ---");
    for (const entry of permissionLog) {
      console.log(`  ${entry}`);
    }
    console.log(`\nTotal permission requests: ${permissionLog.length}`);
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
