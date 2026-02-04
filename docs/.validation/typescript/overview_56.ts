// Source: hooks/overview.md:211
const session = await client.createSession({
  hooks: {
    onSessionStart: async () => {
      const userPrefs = await loadUserPreferences();
      return {
        additionalContext: `User preferences: ${JSON.stringify(userPrefs)}`,
      };
    },
  },
});