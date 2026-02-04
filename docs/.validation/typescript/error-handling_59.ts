// Source: hooks/error-handling.md:162
import { captureException } from "@sentry/node"; // or your monitoring service

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      captureException(new Error(input.error), {
        tags: {
          sessionId: invocation.sessionId,
          errorContext: input.errorContext,
        },
        extra: {
          error: input.error,
          recoverable: input.recoverable,
          cwd: input.cwd,
        },
      });
      
      return null;
    },
  },
});