// Source: auth/index.md:138
using GitHub.Copilot.SDK;

await using var client = new CopilotClient(new CopilotClientOptions
{
    GithubToken = userAccessToken,     // Token from OAuth flow
    UseLoggedInUser = false,           // Don't use stored CLI credentials
});