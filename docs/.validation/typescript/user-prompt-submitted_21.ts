// Source: hooks/user-prompt-submitted.md:85
const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input, invocation) => {
      console.log(`[${invocation.sessionId}] User: ${input.prompt}`);
      return null; // Pass through unchanged
    },
  },
});