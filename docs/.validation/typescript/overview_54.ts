// Source: hooks/overview.md:174
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      console.log(`[${new Date().toISOString()}] Tool: ${input.toolName}, Args: ${JSON.stringify(input.toolArgs)}`);
      return { permissionDecision: "allow" };
    },
    onPostToolUse: async (input) => {
      console.log(`[${new Date().toISOString()}] Result: ${JSON.stringify(input.toolResult)}`);
      return null;
    },
  },
});