using System.Net.Http.Json;
using System.Text.Json;
using GitHub.Copilot.SDK;

// GitHub OAuth Device Flow
var clientId = Environment.GetEnvironmentVariable("GITHUB_OAUTH_CLIENT_ID")
    ?? throw new InvalidOperationException("Missing GITHUB_OAUTH_CLIENT_ID");

var httpClient = new HttpClient();
httpClient.DefaultRequestHeaders.Add("Accept", "application/json");
httpClient.DefaultRequestHeaders.Add("User-Agent", "copilot-sdk-csharp");

// Step 1: Request device code
var deviceCodeResponse = await httpClient.PostAsync(
    "https://github.com/login/device/code",
    new FormUrlEncodedContent(new Dictionary<string, string> { { "client_id", clientId } }));
var deviceCode = await deviceCodeResponse.Content.ReadFromJsonAsync<JsonElement>();

var userCode = deviceCode.GetProperty("user_code").GetString();
var verificationUri = deviceCode.GetProperty("verification_uri").GetString();
var code = deviceCode.GetProperty("device_code").GetString();
var interval = deviceCode.GetProperty("interval").GetInt32();

Console.WriteLine($"Please visit: {verificationUri}");
Console.WriteLine($"Enter code: {userCode}");

// Step 2: Poll for access token
string? accessToken = null;
while (accessToken == null)
{
    await Task.Delay(interval * 1000);
    var tokenResponse = await httpClient.PostAsync(
        "https://github.com/login/oauth/access_token",
        new FormUrlEncodedContent(new Dictionary<string, string>
        {
            { "client_id", clientId },
            { "device_code", code! },
            { "grant_type", "urn:ietf:params:oauth:grant-type:device_code" },
        }));
    var tokenData = await tokenResponse.Content.ReadFromJsonAsync<JsonElement>();

    if (tokenData.TryGetProperty("access_token", out var token))
    {
        accessToken = token.GetString();
    }
    else if (tokenData.TryGetProperty("error", out var error))
    {
        var err = error.GetString();
        if (err == "authorization_pending") continue;
        if (err == "slow_down") { interval += 5; continue; }
        throw new Exception($"OAuth error: {err}");
    }
}

// Step 3: Verify authentication
httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {accessToken}");
var userResponse = await httpClient.GetFromJsonAsync<JsonElement>("https://api.github.com/user");
Console.WriteLine($"Authenticated as: {userResponse.GetProperty("login").GetString()}");

// Step 4: Use the token with Copilot
using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GitHubToken = accessToken,
});

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
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
