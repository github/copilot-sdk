using GitHub.Copilot;
using GitHub.Copilot.Rpc;

var inputLog = new List<string>();

using var client = new CopilotClient();

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        OnPermissionRequest = (request, invocation) =>
            Task.FromResult<PermissionDecision>(PermissionDecision.ApproveOnce()),
        OnUserInputRequest = (request, invocation) =>
        {
            inputLog.Add($"question: {request.Question}");
            return Task.FromResult(new UserInputResponse { Answer = "Paris", WasFreeform = true });
        },
        Hooks = new SessionHooks
        {
            OnPreToolUse = (input, invocation) =>
                Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" }),
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "I want to learn about a city. Use the ask_user tool to ask me which city I'm interested in. Then tell me about that city.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    Console.WriteLine("\n--- User input log ---");
    foreach (var entry in inputLog)
    {
        Console.WriteLine($"  {entry}");
    }
    Console.WriteLine($"\nTotal user input requests: {inputLog.Count}");
}
finally
{
    await client.StopAsync();
}
