// Source: hooks/session-lifecycle.md:87
const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      console.log(`Session ${invocation.sessionId} started (${input.source})`);
      
      const projectInfo = await detectProjectType(input.cwd);
      
      return {
        additionalContext: `
This is a ${projectInfo.type} project.
Main language: ${projectInfo.language}
Package manager: ${projectInfo.packageManager}
        `.trim(),
      };
    },
  },
});