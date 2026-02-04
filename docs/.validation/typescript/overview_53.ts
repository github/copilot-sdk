// Source: hooks/overview.md:27
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      console.log(`Tool called: ${input.toolName}`);
      // Allow all tools
      return { permissionDecision: "allow" };
    },
    onPostToolUse: async (input) => {
      console.log(`Tool result: ${JSON.stringify(input.toolResult)}`);
      return null; // No modifications
    },
    onSessionStart: async (input) => {
      return { additionalContext: "User prefers concise answers." };
    },
  },
});