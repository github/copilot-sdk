// Source: hooks/user-prompt-submitted.md:280
const promptTimestamps: number[] = [];
const RATE_LIMIT = 10; // prompts
const RATE_WINDOW = 60000; // 1 minute

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      const now = Date.now();
      
      // Remove timestamps outside the window
      while (promptTimestamps.length > 0 && promptTimestamps[0] < now - RATE_WINDOW) {
        promptTimestamps.shift();
      }
      
      if (promptTimestamps.length >= RATE_LIMIT) {
        return {
          reject: true,
          rejectReason: `Rate limit exceeded. Please wait before sending more prompts.`,
        };
      }
      
      promptTimestamps.push(now);
      return null;
    },
  },
});