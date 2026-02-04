// Source: hooks/error-handling.md:142
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnErrorOccurred = (input, invocation) =>
        {
            Console.Error.WriteLine($"[{invocation.SessionId}] Error: {input.Error}");
            Console.Error.WriteLine($"  Context: {input.ErrorContext}");
            Console.Error.WriteLine($"  Recoverable: {input.Recoverable}");
            return Task.FromResult<ErrorOccurredHookOutput?>(null);
        },
    },
});