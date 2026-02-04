// Source: hooks/user-prompt-submitted.md:198
const BLOCKED_PATTERNS = [
  /password\s*[:=]/i,
  /api[_-]?key\s*[:=]/i,
  /secret\s*[:=]/i,
];

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      for (const pattern of BLOCKED_PATTERNS) {
        if (pattern.test(input.prompt)) {
          // Replace the prompt with a warning message
          return {
            modifiedPrompt: "[Content blocked: Please don't include sensitive credentials in your prompts. Use environment variables instead.]",
            suppressOutput: true,
          };
        }
      }
      return null;
    },
  },
});