// Source: hooks/pre-tool-use.md:96
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input, invocation) => {
      console.log(`[${invocation.sessionId}] Calling ${input.toolName}`);
      console.log(`  Args: ${JSON.stringify(input.toolArgs)}`);
      return { permissionDecision: "allow" };
    },
  },
});