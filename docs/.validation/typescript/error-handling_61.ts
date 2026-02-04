// Source: hooks/error-handling.md:215
const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input) => {
      // Suppress tool execution errors that are recoverable
      if (input.errorContext === "tool_execution" && input.recoverable) {
        console.log(`Suppressed recoverable error: ${input.error}`);
        return { suppressOutput: true };
      }
      return null;
    },
  },
});