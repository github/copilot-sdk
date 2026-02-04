// Source: hooks/overview.md:191
const BLOCKED_TOOLS = ["shell", "bash", "exec"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (BLOCKED_TOOLS.includes(input.toolName)) {
        return {
          permissionDecision: "deny",
          permissionDecisionReason: "Shell access is not permitted",
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});