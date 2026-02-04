// Source: hooks/pre-tool-use.md:170
const BLOCKED_TOOLS = ["shell", "bash", "write_file", "delete_file"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (BLOCKED_TOOLS.includes(input.toolName)) {
        return {
          permissionDecision: "deny",
          permissionDecisionReason: `Tool '${input.toolName}' is not permitted in this environment`,
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});