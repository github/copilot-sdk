// Source: hooks/error-handling.md:88
const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      console.error(`[${invocation.sessionId}] Error: ${input.error}`);
      console.error(`  Context: ${input.errorContext}`);
      console.error(`  Recoverable: ${input.recoverable}`);
      return null;
    },
  },
});