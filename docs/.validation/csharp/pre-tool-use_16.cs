// Source: hooks/pre-tool-use.md:149
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnPreToolUse = (input, invocation) =>
        {
            Console.WriteLine($"[{invocation.SessionId}] Calling {input.ToolName}");
            Console.WriteLine($"  Args: {input.ToolArgs}");
            return Task.FromResult<PreToolUseHookOutput?>(
                new PreToolUseHookOutput { PermissionDecision = "allow" }
            );
        },
    },
});