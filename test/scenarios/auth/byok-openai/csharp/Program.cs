using GitHub.Copilot.SDK;

var apiKey = Environment.GetEnvironmentVariable("OPENAI_API_KEY");
var model = Environment.GetEnvironmentVariable("OPENAI_MODEL") ?? "claude-haiku-4.5";
var baseUrl = Environment.GetEnvironmentVariable("OPENAI_BASE_URL") ?? "https://api.openai.com/v1";

if (string.IsNullOrEmpty(apiKey))
{
    Console.Error.WriteLine("Missing OPENAI_API_KEY.");
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
            Type = "openai",
            BaseUrl = baseUrl,
            ApiKey = apiKey,
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

