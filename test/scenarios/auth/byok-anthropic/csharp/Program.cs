using GitHub.Copilot.SDK;

var apiKey = Environment.GetEnvironmentVariable("ANTHROPIC_API_KEY");
var model = Environment.GetEnvironmentVariable("ANTHROPIC_MODEL") ?? "claude-sonnet-4-20250514";
var baseUrl = Environment.GetEnvironmentVariable("ANTHROPIC_BASE_URL") ?? "https://api.anthropic.com";

if (string.IsNullOrEmpty(apiKey))
{
    Console.Error.WriteLine("Missing ANTHROPIC_API_KEY.");
    return 1;
}

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
});

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = model,
        Provider = new ProviderConfig
        {
            Type = "anthropic",
            BaseUrl = baseUrl,
            ApiKey = apiKey,
        },
        AvailableTools = [],
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = "You are a helpful assistant. Answer concisely.",
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }
}
finally
{
    await client.StopAsync();
}
return 0;

