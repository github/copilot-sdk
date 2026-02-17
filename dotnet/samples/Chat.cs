using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync();

using var _ = session.On(evt =>
{
    Console.ForegroundColor = ConsoleColor.Blue;
    var output = evt switch
    {
        AssistantReasoningEvent reasoning => $"[reasoning: {reasoning.Data.Content}]",
        ToolExecutionStartEvent toolStart => $"[tool: {toolStart.Data.ToolName} {toolStart.Data.Arguments}]",
        _ => null
    };
    if (output != null) Console.WriteLine(output);
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
