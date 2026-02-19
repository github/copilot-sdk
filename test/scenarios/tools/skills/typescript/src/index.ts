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
    const skillsDir = path.resolve(__dirname, "../../sample-skills");

    const session = await client.createSession({
      model: "claude-haiku-4.5",
      skillDirectories: [skillsDir],
      onPermissionRequest: async () => ({ kind: "approved" as const }),
      hooks: {
        onPreToolUse: async () => ({ permissionDecision: "allow" as const }),
      },
    });

    const response = await session.sendAndWait({
      prompt: "Use the greeting skill to greet someone named Alice.",
    });

    if (response) {
      console.log(response.data.content);
    }

    console.log("\nSkill directories configured successfully");

    await session.destroy();
  } finally {
    await client.stop();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
