// Source: hooks/session-lifecycle.md:276
const sessionStartTimes = new Map<string, number>();

const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      sessionStartTimes.set(invocation.sessionId, input.timestamp);
      return null;
    },
    onSessionEnd: async (input, invocation) => {
      const startTime = sessionStartTimes.get(invocation.sessionId);
      const duration = startTime ? input.timestamp - startTime : 0;
      
      await recordMetrics({
        sessionId: invocation.sessionId,
        duration,
        endReason: input.reason,
      });
      
      sessionStartTimes.delete(invocation.sessionId);
      return null;
    },
  },
});