// Source: hooks/session-lifecycle.md:159
const session = await client.createSession({
  hooks: {
    onSessionStart: async () => {
      const preferences = await loadUserPreferences();
      
      const contextParts = [];
      
      if (preferences.language) {
        contextParts.push(`Preferred language: ${preferences.language}`);
      }
      if (preferences.codeStyle) {
        contextParts.push(`Code style: ${preferences.codeStyle}`);
      }
      if (preferences.verbosity === "concise") {
        contextParts.push("Keep responses brief and to the point.");
      }
      
      return {
        additionalContext: contextParts.join("\n"),
      };
    },
  },
});