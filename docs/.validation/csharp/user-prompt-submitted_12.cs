// Source: hooks/user-prompt-submitted.md:133
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnUserPromptSubmitted = (input, invocation) =>
        {
            Console.WriteLine($"[{invocation.SessionId}] User: {input.Prompt}");
            return Task.FromResult<UserPromptSubmittedHookOutput?>(null);
        },
    },
});