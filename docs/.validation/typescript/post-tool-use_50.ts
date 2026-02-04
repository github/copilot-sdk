// Source: hooks/post-tool-use.md:241
const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      if (input.toolResult?.error && input.toolResult?.stack) {
        // Remove internal stack trace details
        return {
          modifiedResult: {
            error: input.toolResult.error,
            // Keep only first 3 lines of stack
            stack: input.toolResult.stack.split("\n").slice(0, 3).join("\n"),
          },
        };
      }
      return null;
    },
  },
});