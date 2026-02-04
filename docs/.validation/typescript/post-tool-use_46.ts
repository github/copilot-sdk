// Source: hooks/post-tool-use.md:87
const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input, invocation) => {
      console.log(`[${invocation.sessionId}] Tool: ${input.toolName}`);
      console.log(`  Args: ${JSON.stringify(input.toolArgs)}`);
      console.log(`  Result: ${JSON.stringify(input.toolResult)}`);
      return null; // Pass through unchanged
    },
  },
});