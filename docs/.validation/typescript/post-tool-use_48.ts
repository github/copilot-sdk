// Source: hooks/post-tool-use.md:189
const MAX_RESULT_LENGTH = 10000;

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      const resultStr = JSON.stringify(input.toolResult);
      
      if (resultStr.length > MAX_RESULT_LENGTH) {
        return {
          modifiedResult: {
            truncated: true,
            originalLength: resultStr.length,
            content: resultStr.substring(0, MAX_RESULT_LENGTH) + "...",
          },
          additionalContext: `Note: Result was truncated from ${resultStr.length} to ${MAX_RESULT_LENGTH} characters.`,
        };
      }
      return null;
    },
  },
});