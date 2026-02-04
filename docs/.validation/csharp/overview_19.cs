// Source: hooks/overview.md:127
using GitHub.Copilot.SDK;

var client = new CopilotClient();

var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnPreToolUse = (input, invocation) =>
        {
            Console.WriteLine($"Tool called: {input.ToolName}");
            return Task.FromResult<PreToolUseHookOutput?>(
                new PreToolUseHookOutput { PermissionDecision = "allow" }
            );
        },
        OnPostToolUse = (input, invocation) =>
        {
            Console.WriteLine($"Tool result: {input.ToolResult}");
            return Task.FromResult<PostToolUseHookOutput?>(null);
        },
        OnSessionStart = (input, invocation) =>
        {
            return Task.FromResult<SessionStartHookOutput?>(
                new SessionStartHookOutput { AdditionalContext = "User prefers concise answers." }
            );
        },
    },
});