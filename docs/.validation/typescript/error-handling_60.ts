// Source: hooks/error-handling.md:188
const ERROR_MESSAGES: Record<string, string> = {
  "model_call": "There was an issue communicating with the AI model. Please try again.",
  "tool_execution": "A tool failed to execute. Please check your inputs and try again.",
  "system": "A system error occurred. Please try again later.",
  "user_input": "There was an issue with your input. Please check and try again.",
};

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input) => {
      const friendlyMessage = ERROR_MESSAGES[input.errorContext];
      
      if (friendlyMessage) {
        return {
          userNotification: friendlyMessage,
        };
      }
      
      return null;
    },
  },
});