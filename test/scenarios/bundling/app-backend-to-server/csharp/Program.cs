using System.Text.Json;
using GitHub.Copilot.SDK;

var port = Environment.GetEnvironmentVariable("PORT") ?? "8080";
var cliUrl = Environment.GetEnvironmentVariable("CLI_URL")
    ?? Environment.GetEnvironmentVariable("COPILOT_CLI_URL")
    ?? "localhost:3000";

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls($"http://0.0.0.0:{port}");
var app = builder.Build();

app.MapPost("/chat", async (HttpContext ctx) =>
{
    var body = await JsonSerializer.DeserializeAsync<JsonElement>(ctx.Request.Body);
    var prompt = body.TryGetProperty("prompt", out var p) ? p.GetString() : null;
    if (string.IsNullOrEmpty(prompt))
    {
        ctx.Response.StatusCode = 400;
        await ctx.Response.WriteAsJsonAsync(new { error = "Missing 'prompt' in request body" });
        return;
    }

    using var client = new CopilotClient(new CopilotClientOptions { CliUrl = cliUrl });
    await client.StartAsync();

    try
    {
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Model = "claude-haiku-4.5",
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = prompt,
        });

        if (response?.Data?.Content != null)
        {
            await ctx.Response.WriteAsJsonAsync(new { response = response.Data.Content });
        }
        else
        {
            ctx.Response.StatusCode = 502;
            await ctx.Response.WriteAsJsonAsync(new { error = "No response content from Copilot CLI" });
        }
    }
    finally
    {
        await client.StopAsync();
    }
});

Console.WriteLine($"Listening on port {port}");
app.Run();
