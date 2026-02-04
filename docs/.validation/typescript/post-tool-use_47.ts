// Source: hooks/post-tool-use.md:161
const SENSITIVE_PATTERNS = [
  /api[_-]?key["\s:=]+["']?[\w-]+["']?/gi,
  /password["\s:=]+["']?[\w-]+["']?/gi,
  /secret["\s:=]+["']?[\w-]+["']?/gi,
];

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      if (typeof input.toolResult === "string") {
        let redacted = input.toolResult;
        for (const pattern of SENSITIVE_PATTERNS) {
          redacted = redacted.replace(pattern, "[REDACTED]");
        }
        
        if (redacted !== input.toolResult) {
          return { modifiedResult: redacted };
        }
      }
      return null;
    },
  },
});