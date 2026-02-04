// Source: hooks/pre-tool-use.md:190
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      // Add a default timeout to all shell commands
      if (input.toolName === "shell" && input.toolArgs) {
        const args = input.toolArgs as { command: string; timeout?: number };
        return {
          permissionDecision: "allow",
          modifiedArgs: {
            ...args,
            timeout: args.timeout ?? 30000, // Default 30s timeout
          },
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});