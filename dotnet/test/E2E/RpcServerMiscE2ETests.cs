/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot;
using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for miscellaneous server-scoped RPC methods, including account auth state,
/// user.settings get/set/reload, agentRegistry.spawn, runtime.shutdown, sessions.open, and the
/// session-scoped session.extensions.sendAttachmentsToMessage.
///
/// Several of these are intentionally exercised at the wiring/guard boundary because the meaningful
/// "happy path" requires capabilities the SDK host does not expose (a registered agent-registry
/// delegate, an extension-owned connection). For those we assert the method reaches the runtime and
/// enforces its documented guard rather than failing as an unknown method.
/// </summary>
public class RpcServerMiscE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_server_misc", output)
{
    [Fact]
    public async Task Should_Reload_User_Settings()
    {
        await Client.StartAsync();

        // Drops the runtime's in-memory user-settings cache so the next read observes disk. Returns
        // no value; success is simply completing without error.
        await Client.Rpc.User.Settings.ReloadAsync();
    }

    [Fact]
    public async Task Should_Get_Set_And_Clear_User_Settings()
    {
        await Client.StartAsync();

        var before = await Client.Rpc.User.Settings.GetAsync();
        Assert.NotNull(before.Settings);
        Assert.NotEmpty(before.Settings);
        Assert.All(before.Settings, setting =>
        {
            Assert.False(string.IsNullOrWhiteSpace(setting.Key));
            Assert.True(
                setting.Value.Value.ValueKind != System.Text.Json.JsonValueKind.Undefined
                    || setting.Value.Default.ValueKind != System.Text.Json.JsonValueKind.Undefined,
                $"Setting '{setting.Key}' should expose either a value or a default.");
        });

        var settingToToggle = before.Settings.First(setting =>
            setting.Value.Value.ValueKind is System.Text.Json.JsonValueKind.True or System.Text.Json.JsonValueKind.False);
        var settingKey = settingToToggle.Key;
        var toggledValue = settingToToggle.Value.Value.ValueKind != System.Text.Json.JsonValueKind.True;

        var set = await Client.Rpc.User.Settings.SetAsync(ParseSettingJson(settingKey, toggledValue ? "true" : "false"));
        Assert.NotNull(set.ShadowedKeys);
        Assert.DoesNotContain(settingKey, set.ShadowedKeys);

        await Client.Rpc.User.Settings.ReloadAsync();
        var afterSet = await Client.Rpc.User.Settings.GetAsync();
        var updatedSetting = Assert.Contains(settingKey, afterSet.Settings);
        Assert.False(updatedSetting.IsDefault);
        Assert.Equal(toggledValue, updatedSetting.Value.GetBoolean());

        var clear = await Client.Rpc.User.Settings.SetAsync(ParseSettingJson(settingKey, "null"));
        Assert.NotNull(clear.ShadowedKeys);

        await Client.Rpc.User.Settings.ReloadAsync();
        var afterClear = await Client.Rpc.User.Settings.GetAsync();
        var clearedSetting = Assert.Contains(settingKey, afterClear.Settings);
        Assert.True(clearedSetting.IsDefault);
    }

    [Fact]
    public async Task Should_Login_List_GetCurrentAuth_And_Logout_Account()
    {
        var (client, home) = await CreateIsolatedClientAsync(autoInjectGitHubToken: false);
        var login = $"rpc-account-{Guid.NewGuid():N}";
        var token = $"rpc-account-token-{Guid.NewGuid():N}";

        try
        {
            await Ctx.SetCopilotUserByTokenAsync(token, new CopilotUserConfig(
                Login: login,
                CopilotPlan: "individual_pro",
                Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
                AnalyticsTrackingId: "rpc-account-tracking-id"));

            var initial = await client.Rpc.Account.GetCurrentAuthAsync();
            Assert.Null(initial.AuthInfo);

            var loginResult = await client.Rpc.Account.LoginAsync("https://github.com", login, token);
            Assert.NotNull(loginResult);

            var current = await client.Rpc.Account.GetCurrentAuthAsync();
            Assert.Null(current.AuthErrors);
            var authInfo = Assert.IsType<AuthInfoUser>(current.AuthInfo);
            Assert.Equal("https://github.com", authInfo.Host);
            Assert.Equal(login, authInfo.Login);

            var users = await client.Rpc.Account.GetAllUsersAsync();
            Assert.All(users, user => Assert.False(string.IsNullOrWhiteSpace(user.AuthInfo.Type)));
            var account = users.FirstOrDefault(user =>
                user.AuthInfo is AuthInfoUser userAuth
                && string.Equals(userAuth.Login, login, StringComparison.Ordinal));
            if (account is not null)
            {
                Assert.Equal(token, account.Token);
            }

            var logout = await client.Rpc.Account.LogoutAsync(authInfo);
            Assert.False(logout.HasMoreUsers);

            var afterLogout = await client.Rpc.Account.GetCurrentAuthAsync();
            Assert.Null(afterLogout.AuthInfo);
        }
        finally
        {
            await client.DisposeAsync();
            TryDeleteDirectory(home);
        }
    }

    [Fact]
    public async Task Should_Report_Agent_Registry_Spawn_Gate_Closed()
    {
        await Client.StartAsync();

        // agentRegistry.spawn is gated off on the SDK host (no spawn delegate is registered). The
        // call must still reach the runtime and be rejected by that gate, proving the method is
        // wired rather than an unknown method.
        var ex = await Assert.ThrowsAnyAsync<Exception>(
            () => Client.Rpc.AgentRegistry.SpawnAsync(cwd: Path.GetTempPath()));

        var message = ex.ToString();
        Assert.DoesNotContain("Unhandled method", message, StringComparison.OrdinalIgnoreCase);
        Assert.Contains("agentRegistry.spawn", message, StringComparison.OrdinalIgnoreCase);
        Assert.True(
            message.Contains("not enabled", StringComparison.OrdinalIgnoreCase)
                || message.Contains("no delegate", StringComparison.OrdinalIgnoreCase),
            message);
    }

    [Fact]
    public async Task Should_Shut_Down_Owned_Runtime()
    {
        // runtime.shutdown must only ever target a dedicated, SDK-owned runtime — never the shared
        // fixture client whose process backs every other test.
        var client = Ctx.CreateClient();
        await client.StartAsync();

        try
        {
            // Confirm the runtime is live before shutting it down.
            await client.Rpc.User.Settings.ReloadAsync();

            await client.Rpc.Runtime.ShutdownAsync();

            // After a graceful shutdown the runtime tears down and stops serving. Poll until a
            // follow-up RPC fails rather than asserting on a single immediate call, which could race
            // shutdown propagation across the connection.
            await Harness.TestHelper.WaitForConditionAsync(
                async () =>
                {
                    try { await client.Rpc.User.Settings.ReloadAsync(); return false; }
                    catch (Exception ex) when (IsExpectedShutdownException(ex)) { return true; }
                },
                timeout: TimeSpan.FromSeconds(15),
                pollInterval: TimeSpan.FromMilliseconds(100),
                timeoutMessage: "Runtime kept serving RPCs after a graceful shutdown.");
        }
        finally
        {
            await DisposeStoppedRuntimeClientAsync(client);
        }
    }

    [Fact]
    public async Task Should_Report_Not_Found_When_Opening_Session_Without_Context()
    {
        // sessions.open with no parameters asks the runtime to resume the last session for the
        // (unspecified) context. A fresh runtime with its own empty COPILOT_HOME has no such
        // session, so the documented "not_found" outcome is returned deterministically.
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            var result = await client.Rpc.Sessions.OpenAsync();

            Assert.Equal(SessionsOpenStatus.NotFound, result.Status);
            Assert.Null(result.SessionId);
        }
        finally
        {
            await client.DisposeAsync();
            TryDeleteDirectory(home);
        }
    }

    [Fact]
    public async Task Should_Reject_Send_Attachments_From_Non_Extension_Connection()
    {
        // session.extensions.sendAttachmentsToMessage may only be called over an extension-owned
        // connection. A normal SDK session connection has no extensionId, so the runtime rejects the
        // push — confirming the method is wired and enforces its ownership guard.
        await using var session = await CreateSessionAsync();

        var ex = await Assert.ThrowsAnyAsync<Exception>(
            () => session.Rpc.Extensions.SendAttachmentsToMessageAsync(new List<PushAttachment>()));
        var message = ex.ToString();
        Assert.DoesNotContain("Unhandled method", message, StringComparison.OrdinalIgnoreCase);
        Assert.Contains("extension", message, StringComparison.OrdinalIgnoreCase);
    }

    /// <summary>
    /// Creates a started client backed by a throwaway COPILOT_HOME so its session store is empty and
    /// independent of every other test and of the shared fixture client.
    /// </summary>
    private async Task<(CopilotClient Client, string Home)> CreateIsolatedClientAsync(
        bool autoInjectGitHubToken = true)
    {
        var home = Path.Combine(Path.GetTempPath(), "copilot-e2e-misc-home-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(home);

        var env = Ctx.GetEnvironment();
        env["COPILOT_HOME"] = home;
        env["GH_CONFIG_DIR"] = home;
        env["XDG_CONFIG_HOME"] = home;
        env["XDG_STATE_HOME"] = home;
        if (!autoInjectGitHubToken)
        {
            env["GH_TOKEN"] = "";
            env["GITHUB_TOKEN"] = "";
        }

        var options = new CopilotClientOptions { Environment = env };
        if (!autoInjectGitHubToken)
        {
            options.UseLoggedInUser = false;
        }

        var client = Ctx.CreateClient(
            options: options,
            autoInjectGitHubToken: autoInjectGitHubToken);
        await client.StartAsync();
        return (client, home);
    }

    private static void TryDeleteDirectory(string path)
    {
        try
        {
            if (Directory.Exists(path))
            {
                Directory.Delete(path, recursive: true);
            }
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
            // Temp directories are reclaimed by the OS; ignore transient locks on cleanup.
        }
    }

    private static async Task DisposeStoppedRuntimeClientAsync(CopilotClient client)
    {
        try
        {
            await client.DisposeAsync();
        }
        catch (Exception ex) when (IsExpectedShutdownException(ex))
        {
            // The runtime.shutdown test intentionally stops the process before disposal.
        }
    }

    private static bool IsExpectedShutdownException(Exception ex) =>
        ex is OperationCanceledException
            or InvalidOperationException
            or ObjectDisposedException
            or IOException;

    private static System.Text.Json.JsonElement ParseJsonElement(string json)
    {
        using var document = System.Text.Json.JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }

    private static System.Text.Json.JsonElement ParseSettingJson(string key, string valueLiteral)
        => ParseJsonElement("{\"" + System.Text.Json.JsonEncodedText.Encode(key) + "\":" + valueLiteral + "}");
}
