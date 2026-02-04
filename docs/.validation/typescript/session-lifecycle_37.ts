// Source: hooks/session-lifecycle.md:388
const sessionData: Record<string, { prompts: number; tools: number; startTime: number }> = {};

const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      sessionData[invocation.sessionId] = { 
        prompts: 0, 
        tools: 0, 
        startTime: input.timestamp 
      };
      return null;
    },
    onUserPromptSubmitted: async (_, invocation) => {
      sessionData[invocation.sessionId].prompts++;
      return null;
    },
    onPreToolUse: async (_, invocation) => {
      sessionData[invocation.sessionId].tools++;
      return { permissionDecision: "allow" };
    },
    onSessionEnd: async (input, invocation) => {
      const data = sessionData[invocation.sessionId];
      console.log(`
Session Summary:
  ID: ${invocation.sessionId}
  Duration: ${(input.timestamp - data.startTime) / 1000}s
  Prompts: ${data.prompts}
  Tool calls: ${data.tools}
  End reason: ${input.reason}
      `.trim());
      
      delete sessionData[invocation.sessionId];
      return null;
    },
  },
});