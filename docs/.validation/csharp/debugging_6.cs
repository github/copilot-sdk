// Source: debugging.md:61
using GitHub.Copilot.SDK;
using Microsoft.Extensions.Logging;

// Using ILogger
var loggerFactory = LoggerFactory.Create(builder =>
{
    builder.SetMinimumLevel(LogLevel.Debug);
    builder.AddConsole();
});

var client = new CopilotClient(new CopilotClientOptions
{
    LogLevel = "debug",
    Logger = loggerFactory.CreateLogger<CopilotClient>()
});