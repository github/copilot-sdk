// Source: hooks/post-tool-use.md:298
const NOISY_TOOLS = ["list_directory", "search_codebase"];

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      if (NOISY_TOOLS.includes(input.toolName)) {
        // Summarize instead of showing full result
        const items = Array.isArray(input.toolResult) 
          ? input.toolResult 
          : input.toolResult?.items || [];
        
        return {
          modifiedResult: {
            summary: `Found ${items.length} items`,
            firstFew: items.slice(0, 5),
          },
        };
      }
      return null;
    },
  },
});