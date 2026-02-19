import { CopilotClient, defineTool } from "@github/copilot-sdk";
import { z } from "zod";

// In-memory virtual filesystem
const virtualFs = new Map<string, string>();

const createFile = defineTool("create_file", {
  description: "Create or overwrite a file at the given path with the provided content",
  parameters: z.object({
    path: z.string().describe("File path"),
    content: z.string().describe("File content"),
  }),
  handler: async (args) => {
    virtualFs.set(args.path, args.content);
    return `Created ${args.path} (${args.content.length} bytes)`;
  },
});

const readFile = defineTool("read_file", {
  description: "Read the contents of a file at the given path",
  parameters: z.object({
    path: z.string().describe("File path"),
  }),
  handler: async (args) => {
    const content = virtualFs.get(args.path);
    if (content === undefined) return `Error: file not found: ${args.path}`;
    return content;
  },
});

const listFiles = defineTool("list_files", {
  description: "List all files in the virtual filesystem",
  parameters: z.object({}),
  handler: async () => {
    if (virtualFs.size === 0) return "No files";
    return [...virtualFs.keys()].join("\n");
  },
});

async function main() {
  const client = new CopilotClient({
    ...(process.env.COPILOT_CLI_PATH && {
      cliPath: process.env.COPILOT_CLI_PATH,
    }),
    githubToken: process.env.GITHUB_TOKEN,
  });

  try {
    const session = await client.createSession({
      model: "claude-haiku-4.5",
      // Remove all built-in tools — only our custom virtual FS tools are available
      availableTools: [],
      tools: [createFile, readFile, listFiles],
      onPermissionRequest: async () => ({ kind: "approved" as const }),
      hooks: {
        onPreToolUse: async () => ({ permissionDecision: "allow" as const }),
      },
    });

    const response = await session.sendAndWait({
      prompt:
        "Create a file called plan.md with a brief 3-item project plan for building a CLI tool. " +
        "Then read it back and tell me what you wrote.",
    });

    if (response) {
      console.log(response.data.content);
    }

    // Dump the virtual filesystem to prove nothing touched disk
    console.log("\n--- Virtual filesystem contents ---");
    for (const [path, content] of virtualFs) {
      console.log(`\n[${path}]`);
      console.log(content);
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
