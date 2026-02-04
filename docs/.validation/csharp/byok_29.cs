// Source: auth/byok.md:143
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5.2-codex",  // Your deployment name
    Provider = new ProviderConfig
    {
        Type = "openai",
        BaseUrl = "https://your-resource.openai.azure.com/openai/v1/",
        WireApi = "responses",  // Use "completions" for older models
        ApiKey = Environment.GetEnvironmentVariable("FOUNDRY_API_KEY"),
    },
});

var response = await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "What is 2+2?",
});
Console.WriteLine(response?.Data.Content);