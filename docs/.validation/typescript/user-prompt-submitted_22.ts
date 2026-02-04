// Source: hooks/user-prompt-submitted.md:151
const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      const projectInfo = await getProjectInfo();
      
      return {
        additionalContext: `
Project: ${projectInfo.name}
Language: ${projectInfo.language}
Framework: ${projectInfo.framework}
        `.trim(),
      };
    },
  },
});