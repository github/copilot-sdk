using GitHub.Copilot.SDK;

var baseUrl = Environment.GetEnvironmentVariable("OLLAMA_BASE_URL") ?? "http://localhost:11434/v1";
var model = Environment.GetEnvironmentVariable("OLLAMA_MODEL") ?? "llama3.2:3b";

var compactSystemPrompt =
    "You are a compact local assistant. Keep answers short, concrete, and under 80 words.";

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
        },
        AvailableTools = [],
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = compactSystemPrompt,
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
