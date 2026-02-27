using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll
});

using var _ = session.On(evt =>
{
    Console.ForegroundColor = ConsoleColor.Blue;
    switch (evt)
    {
        case AssistantReasoningEvent reasoning:
            Console.WriteLine($"[reasoning: {reasoning.Data.Content}]");
            break;
        case ToolExecutionStartEvent tool:
            Console.WriteLine($"[tool: {tool.Data.ToolName}]");
            break;
    }
    Console.ResetColor();
});

Console.WriteLine("Chat with Copilot (Ctrl+C to exit)\n");

while (true)
{
    Console.Write("You: ");
    var input = Console.ReadLine()?.Trim();
    if (string.IsNullOrEmpty(input)) continue;
    Console.WriteLine();

    var reply = await session.SendAndWaitAsync(new MessageOptions { Prompt = input });
    Console.WriteLine($"\nAssistant: {reply?.Data.Content}\n");
}
