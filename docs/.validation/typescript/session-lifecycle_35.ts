// Source: hooks/session-lifecycle.md:339
const sessionResources = new Map<string, { tempFiles: string[] }>();

const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      sessionResources.set(invocation.sessionId, { tempFiles: [] });
      return null;
    },
    onSessionEnd: async (input, invocation) => {
      const resources = sessionResources.get(invocation.sessionId);
      
      if (resources) {
        // Clean up temp files
        for (const file of resources.tempFiles) {
          await fs.unlink(file).catch(() => {});
        }
        sessionResources.delete(invocation.sessionId);
      }
      
      console.log(`Session ${invocation.sessionId} ended: ${input.reason}`);
      return null;
    },
  },
});