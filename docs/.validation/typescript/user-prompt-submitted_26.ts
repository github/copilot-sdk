// Source: hooks/user-prompt-submitted.md:246
interface UserPreferences {
  codeStyle: "concise" | "verbose";
  preferredLanguage: string;
  experienceLevel: "beginner" | "intermediate" | "expert";
}

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      const prefs: UserPreferences = await loadUserPreferences();
      
      const contextParts = [];
      
      if (prefs.codeStyle === "concise") {
        contextParts.push("User prefers concise code with minimal comments.");
      } else {
        contextParts.push("User prefers verbose code with detailed comments.");
      }
      
      if (prefs.experienceLevel === "beginner") {
        contextParts.push("Explain concepts in simple terms.");
      }
      
      return {
        additionalContext: contextParts.join(" "),
      };
    },
  },
});