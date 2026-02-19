using GitHub.Copilot.SDK;

var endpoint = Environment.GetEnvironmentVariable("AZURE_OPENAI_ENDPOINT");
var apiKey = Environment.GetEnvironmentVariable("AZURE_OPENAI_API_KEY");
var model = Environment.GetEnvironmentVariable("AZURE_OPENAI_MODEL") ?? "gpt-4.1";
var apiVersion = Environment.GetEnvironmentVariable("AZURE_API_VERSION") ?? "2024-10-21";

if (string.IsNullOrEmpty(endpoint) || string.IsNullOrEmpty(apiKey))
{
    Console.Error.WriteLine("Required: AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY");
    return 1;
}

await using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
});

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = model,
    Provider = new ProviderConfig
    {
        Type = "azure",
        BaseUrl = endpoint,
        ApiKey = apiKey,
        Azure = new AzureOptions
        {
            ApiVersion = apiVersion,
        },
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

return 0;
