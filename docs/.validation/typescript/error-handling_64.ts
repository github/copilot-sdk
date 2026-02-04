// Source: hooks/error-handling.md:302
const CRITICAL_CONTEXTS = ["system", "model_call"];

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      if (CRITICAL_CONTEXTS.includes(input.errorContext) && !input.recoverable) {
        await sendAlert({
          level: "critical",
          message: `Critical error in session ${invocation.sessionId}`,
          error: input.error,
          context: input.errorContext,
          timestamp: new Date(input.timestamp).toISOString(),
        });
      }
      
      return null;
    },
  },
});