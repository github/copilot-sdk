// Source: hooks/pre-tool-use.md:213
const ALLOWED_DIRECTORIES = ["/home/user/projects", "/tmp"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (input.toolName === "read_file" || input.toolName === "write_file") {
        const args = input.toolArgs as { path: string };
        const isAllowed = ALLOWED_DIRECTORIES.some(dir => 
          args.path.startsWith(dir)
        );
        
        if (!isAllowed) {
          return {
            permissionDecision: "deny",
            permissionDecisionReason: `Access to '${args.path}' is not permitted. Allowed directories: ${ALLOWED_DIRECTORIES.join(", ")}`,
          };
        }
      }
      return { permissionDecision: "allow" };
    },
  },
});