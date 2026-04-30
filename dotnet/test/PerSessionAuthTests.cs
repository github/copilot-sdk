/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class PerSessionAuthTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "per-session-auth", output)
{
    /// <summary>
    /// Creates a client with COPILOT_DEBUG_GITHUB_API_URL redirected to the proxy
    /// so per-session auth token resolution (fetchCopilotUser) is intercepted.
    /// </summary>
    private CopilotClient CreateAuthTestClient()
    {
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_DEBUG_GITHUB_API_URL"] = Ctx.ProxyUrl,
        };
        return Ctx.CreateClient(options: new CopilotClientOptions { Environment = env });
    }

    private async Task SetupCopilotUsersAsync()
    {
        await Ctx.SetCopilotUserByTokenAsync("token-alice", new CopilotUserConfig(
            Login: "alice",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "alice-tracking-id"
        ));

        await Ctx.SetCopilotUserByTokenAsync("token-bob", new CopilotUserConfig(
            Login: "bob",
            CopilotPlan: "business",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "bob-tracking-id"
        ));
    }

    private CopilotClient? _authClient;

    private CopilotClient AuthClient => _authClient ??= CreateAuthTestClient();

    [Fact]
    public async Task ShouldAuthenticateWithGitHubToken()
    {
        await SetupCopilotUsersAsync();

        await using var session = await AuthClient.CreateSessionAsync(new SessionConfig
        {
            GitHubToken = "token-alice",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var status = await session.Rpc.Auth.GetStatusAsync();
        Assert.True(status.IsAuthenticated);
        Assert.Equal("alice", status.Login);
    }

    [Fact]
    public async Task ShouldIsolateAuthBetweenSessions()
    {
        await SetupCopilotUsersAsync();

        await using var sessionA = await AuthClient.CreateSessionAsync(new SessionConfig
        {
            GitHubToken = "token-alice",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        await using var sessionB = await AuthClient.CreateSessionAsync(new SessionConfig
        {
            GitHubToken = "token-bob",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var statusA = await sessionA.Rpc.Auth.GetStatusAsync();
        Assert.True(statusA.IsAuthenticated);
        Assert.Equal("alice", statusA.Login);

        var statusB = await sessionB.Rpc.Auth.GetStatusAsync();
        Assert.True(statusB.IsAuthenticated);
        Assert.Equal("bob", statusB.Login);
    }

    [Fact]
    public async Task ShouldBeUnauthenticatedWithoutToken()
    {
        await using var session = await AuthClient.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var status = await session.Rpc.Auth.GetStatusAsync();
        Assert.False(status.IsAuthenticated);
        Assert.Null(status.Login);
    }

    [Fact]
    public async Task ShouldFailWithInvalidToken()
    {
        await SetupCopilotUsersAsync();

        var ex = await Assert.ThrowsAnyAsync<Exception>(() => AuthClient.CreateSessionAsync(new SessionConfig
        {
            GitHubToken = "invalid-token",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        }));
        Assert.Contains("401 Unauthorized", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }
}
