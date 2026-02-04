// Source: hooks/error-handling.md:326
const sessionContext = new Map<string, { lastTool?: string; lastPrompt?: string }>();

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input, invocation) => {
      const ctx = sessionContext.get(invocation.sessionId) || {};
      ctx.lastTool = input.toolName;
      sessionContext.set(invocation.sessionId, ctx);
      return { permissionDecision: "allow" };
    },
    
    onUserPromptSubmitted: async (input, invocation) => {
      const ctx = sessionContext.get(invocation.sessionId) || {};
      ctx.lastPrompt = input.prompt.substring(0, 100);
      sessionContext.set(invocation.sessionId, ctx);
      return null;
    },
    
    onErrorOccurred: async (input, invocation) => {
      const ctx = sessionContext.get(invocation.sessionId);
      
      console.error(`Error in session ${invocation.sessionId}:`);
      console.error(`  Error: ${input.error}`);
      console.error(`  Context: ${input.errorContext}`);
      if (ctx?.lastTool) {
        console.error(`  Last tool: ${ctx.lastTool}`);
      }
      if (ctx?.lastPrompt) {
        console.error(`  Last prompt: ${ctx.lastPrompt}...`);
      }
      
      return null;
    },
  },
});