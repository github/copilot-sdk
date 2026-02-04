// Source: compatibility.md:129
const session = await client.createSession({
  onPermissionRequest: async (request) => {
    // Auto-approve everything (equivalent to --yolo)
    return { approved: true };
    
    // Or implement custom logic
    if (request.kind === "shell") {
      return { approved: request.command.startsWith("git") };
    }
    return { approved: true };
  },
});