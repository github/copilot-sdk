// Source: guides/skills.md:118
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
    SkillDirectories = new List<string>
    {
        "./skills/code-review",
        "./skills/documentation",
        "~/.copilot/skills",  // User-level skills
    },
});

// Copilot now has access to skills in those directories
await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "Review this code for security issues"
});