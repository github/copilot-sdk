// Source: hooks/pre-tool-use.md:257
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (input.toolName === "query_database") {
        return {
          permissionDecision: "allow",
          additionalContext: "Remember: This database uses PostgreSQL syntax. Always use parameterized queries.",
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});