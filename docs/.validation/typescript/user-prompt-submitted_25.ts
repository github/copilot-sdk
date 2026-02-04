// Source: hooks/user-prompt-submitted.md:225
const MAX_PROMPT_LENGTH = 10000;

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      if (input.prompt.length > MAX_PROMPT_LENGTH) {
        // Truncate the prompt and add context
        return {
          modifiedPrompt: input.prompt.substring(0, MAX_PROMPT_LENGTH),
          additionalContext: `Note: The original prompt was ${input.prompt.length} characters and was truncated to ${MAX_PROMPT_LENGTH} characters.`,
        };
      }
      return null;
    },
  },
});