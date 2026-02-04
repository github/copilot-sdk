// Source: hooks/user-prompt-submitted.md:311
const TEMPLATES: Record<string, (args: string) => string> = {
  "bug:": (desc) => `I found a bug: ${desc}

Please help me:
1. Understand why this is happening
2. Suggest a fix
3. Explain how to prevent similar bugs`,

  "feature:": (desc) => `I want to implement this feature: ${desc}

Please:
1. Outline the implementation approach
2. Identify potential challenges
3. Provide sample code`,
};

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      for (const [prefix, template] of Object.entries(TEMPLATES)) {
        if (input.prompt.toLowerCase().startsWith(prefix)) {
          const args = input.prompt.slice(prefix.length).trim();
          return {
            modifiedPrompt: template(args),
          };
        }
      }
      return null;
    },
  },
});