// Source: hooks/post-tool-use.md:141
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnPostToolUse = (input, invocation) =>
        {
            Console.WriteLine($"[{invocation.SessionId}] Tool: {input.ToolName}");
            Console.WriteLine($"  Args: {input.ToolArgs}");
            Console.WriteLine($"  Result: {input.ToolResult}");
            return Task.FromResult<PostToolUseHookOutput?>(null);
        },
    },
});