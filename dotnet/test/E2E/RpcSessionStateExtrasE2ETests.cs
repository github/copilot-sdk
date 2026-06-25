/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for session-scoped RPC methods that were previously untested:
/// model.list, metadata.activity, permissions.getAllowAll/setAllowAll, plan.readSqlTodos,
/// telemetry.getEngagementId, tools.getCurrentMetadata, and the session-scoped plugins.reload.
/// </summary>
public class RpcSessionStateExtrasE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_session_state_extras", output)
{
    [Fact]
    public async Task Should_List_Models_For_Session()
    {
        // model.list resolves models through the session's own auth context, which requires the
        // GitHub token -> user resolution to be served by the proxy (a fresh shared client does not
        // route token resolution there). Use a dedicated authenticated client like the server-scoped
        // models.list coverage does.
        const string token = "rpc-session-model-list-token";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Model = "claude-sonnet-4.5",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var result = await session.Rpc.Model.ListAsync();

        Assert.NotNull(result.List);
        Assert.NotEmpty(result.List);
        // The configured model must be present in the returned catalog.
        Assert.Contains(result.List, model => model.GetRawText().Contains("claude-sonnet-4.5", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Report_Session_Activity_When_Idle()
    {
        await using var session = await CreateSessionAsync();

        var activity = await session.Rpc.Metadata.ActivityAsync();

        // A freshly created session that has not been sent any work is idle: no active turns or
        // tasks, and nothing to abort.
        Assert.False(activity.HasActiveWork, "Expected a freshly created session to report no active work.");
        Assert.False(activity.Abortable, "Expected a freshly created session to have nothing abortable.");
    }

    [Fact]
    public async Task Should_Get_And_Set_AllowAll_Permissions()
    {
        await using var session = await CreateSessionAsync();

        try
        {
            var initial = await session.Rpc.Permissions.GetAllowAllAsync();
            Assert.False(initial.Enabled, "Allow-all should be disabled on a fresh session.");

            var enable = await session.Rpc.Permissions.SetAllowAllAsync(true);
            Assert.True(enable.Success);
            Assert.True(enable.Enabled);
            Assert.True((await session.Rpc.Permissions.GetAllowAllAsync()).Enabled);

            var disable = await session.Rpc.Permissions.SetAllowAllAsync(false);
            Assert.True(disable.Success);
            Assert.False(disable.Enabled);
            Assert.False((await session.Rpc.Permissions.GetAllowAllAsync()).Enabled);
        }
        finally
        {
            await session.Rpc.Permissions.SetAllowAllAsync(false);
        }
    }

    [Fact]
    public async Task Should_Read_Empty_Sql_Todos_For_Fresh_Session()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Plan.ReadSqlTodosAsync();

        // A fresh session has never written to its SQL todos table, so the query returns an empty
        // (but non-null) row set rather than failing.
        Assert.NotNull(result.Rows);
        Assert.Empty(result.Rows);
    }

    [Fact]
    public async Task Should_Get_Telemetry_Engagement_Id()
    {
        await using var session = await CreateSessionAsync();

        var result = await session.Rpc.Telemetry.GetEngagementIdAsync();

        // The engagement id is optional (null until telemetry assigns one), but the call must
        // round-trip without error and return a result object.
        Assert.NotNull(result);
    }

    [Fact]
    public async Task Should_Get_Current_Tool_Metadata_After_Initialization()
    {
        await using var session = await CreateSessionAsync();

        // getCurrentMetadata returns the tool snapshot captured for the most recent turn; it is null
        // until the session has processed a turn. Drive one real turn so the runtime computes and
        // records the current tool metadata.
        var answer = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
        Assert.NotNull(answer);

        var result = await session.Rpc.Tools.GetCurrentMetadataAsync();

        Assert.NotNull(result.Tools);
        Assert.NotEmpty(result.Tools!);
        Assert.All(result.Tools!, tool =>
        {
            Assert.False(string.IsNullOrWhiteSpace(tool.Name));
            Assert.NotNull(tool.Description);
        });
    }

    [Fact]
    public async Task Should_Reload_Session_Plugins()
    {
        await using var session = await CreateSessionAsync();

        // Reloading refreshes the session's plugin set; with no plugins configured it is a no-op
        // that must still complete successfully and leave the plugin list queryable.
        await session.Rpc.Plugins.ReloadAsync();

        var plugins = await session.Rpc.Plugins.ListAsync();
        Assert.NotNull(plugins.Plugins);
        Assert.All(plugins.Plugins, plugin => Assert.False(string.IsNullOrWhiteSpace(plugin.Name)));
    }

    private CopilotClient CreateAuthenticatedClient(string token)
    {
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_DEBUG_GITHUB_API_URL"] = Ctx.ProxyUrl,
        };

        return Ctx.CreateClient(options: new CopilotClientOptions
        {
            Environment = env,
            GitHubToken = token,
        });
    }

    private async Task ConfigureAuthenticatedUserAsync(string token)
    {
        await Ctx.SetCopilotUserByTokenAsync(token, new CopilotUserConfig(
            Login: "rpc-session-extras-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "rpc-session-extras-tracking-id"));
    }
}
