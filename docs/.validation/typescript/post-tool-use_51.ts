// Source: hooks/post-tool-use.md:263
interface AuditEntry {
  timestamp: number;
  sessionId: string;
  toolName: string;
  args: unknown;
  result: unknown;
  success: boolean;
}

const auditLog: AuditEntry[] = [];

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input, invocation) => {
      auditLog.push({
        timestamp: input.timestamp,
        sessionId: invocation.sessionId,
        toolName: input.toolName,
        args: input.toolArgs,
        result: input.toolResult,
        success: !input.toolResult?.error,
      });
      
      // Optionally persist to database/file
      await saveAuditLog(auditLog);
      
      return null;
    },
  },
});