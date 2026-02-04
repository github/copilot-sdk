// Source: auth/index.md:279
await using var client = new CopilotClient(new CopilotClientOptions
{
    UseLoggedInUser = false,  // Only use explicit tokens
});