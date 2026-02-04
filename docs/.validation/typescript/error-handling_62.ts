// Source: hooks/error-handling.md:232
const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input) => {
      if (input.errorContext === "tool_execution") {
        return {
          userNotification: `
The tool failed. Here are some recovery suggestions:
- Check if required dependencies are installed
- Verify file paths are correct
- Try a simpler approach
          `.trim(),
        };
      }
      
      if (input.errorContext === "model_call" && input.error.includes("rate")) {
        return {
          errorHandling: "retry",
          retryCount: 3,
          userNotification: "Rate limit hit. Retrying...",
        };
      }
      
      return null;
    },
  },
});