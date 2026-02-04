// Source: hooks/session-lifecycle.md:135
const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      if (input.source === "resume") {
        // Load previous session state
        const previousState = await loadSessionState(invocation.sessionId);
        
        return {
          additionalContext: `
Session resumed. Previous context:
- Last topic: ${previousState.lastTopic}
- Open files: ${previousState.openFiles.join(", ")}
          `.trim(),
        };
      }
      return null;
    },
  },
});