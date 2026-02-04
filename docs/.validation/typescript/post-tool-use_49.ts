// Source: hooks/post-tool-use.md:215
const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      // If a file read returned an error, add helpful context
      if (input.toolName === "read_file" && input.toolResult?.error) {
        return {
          additionalContext: "Tip: If the file doesn't exist, consider creating it or checking the path.",
        };
      }
      
      // If shell command failed, add debugging hint
      if (input.toolName === "shell" && input.toolResult?.exitCode !== 0) {
        return {
          additionalContext: "The command failed. Check if required dependencies are installed.",
        };
      }
      
      return null;
    },
  },
});