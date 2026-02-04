// Source: hooks/user-prompt-submitted.md:171
const SHORTCUTS: Record<string, string> = {
  "/fix": "Please fix the errors in the code",
  "/explain": "Please explain this code in detail",
  "/test": "Please write unit tests for this code",
  "/refactor": "Please refactor this code to improve readability and maintainability",
};

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      for (const [shortcut, expansion] of Object.entries(SHORTCUTS)) {
        if (input.prompt.startsWith(shortcut)) {
          const rest = input.prompt.slice(shortcut.length).trim();
          return {
            modifiedPrompt: `${expansion}${rest ? `: ${rest}` : ""}`,
          };
        }
      }
      return null;
    },
  },
});