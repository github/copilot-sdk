// Source: hooks/error-handling.md:263
interface ErrorStats {
  count: number;
  lastOccurred: number;
  contexts: string[];
}

const errorStats = new Map<string, ErrorStats>();

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      const key = `${input.errorContext}:${input.error.substring(0, 50)}`;
      
      const existing = errorStats.get(key) || {
        count: 0,
        lastOccurred: 0,
        contexts: [],
      };
      
      existing.count++;
      existing.lastOccurred = input.timestamp;
      existing.contexts.push(invocation.sessionId);
      
      errorStats.set(key, existing);
      
      // Alert if error is recurring
      if (existing.count >= 5) {
        console.warn(`Recurring error detected: ${key} (${existing.count} times)`);
      }
      
      return null;
    },
  },
});