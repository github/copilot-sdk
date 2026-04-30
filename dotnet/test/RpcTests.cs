/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class RpcTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "session", output)
{
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

    private async Task ConfigureAuthenticatedUserAsync(
        string token,
        IReadOnlyDictionary<string, CopilotUserQuotaSnapshot>? quotaSnapshots = null)
    {
        await Ctx.SetCopilotUserByTokenAsync(token, new CopilotUserConfig(
            Login: "rpc-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "rpc-user-tracking-id",
            QuotaSnapshots: quotaSnapshots));
    }

    private static async Task<Exception> AssertRpcFailureAsync(Func<Task> action, string expectedMessage)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.Contains(expectedMessage, ex.ToString(), StringComparison.OrdinalIgnoreCase);
        return ex;
    }

    [Fact]
    public async Task Should_Call_Rpc_Ping_With_Typed_Params_And_Result()
    {
        await Client.StartAsync();
        var result = await Client.Rpc.PingAsync(message: "typed rpc test");
        Assert.Equal("pong: typed rpc test", result.Message);
        Assert.True(result.Timestamp >= 0);
    }

    [Fact]
    public async Task Should_Call_Rpc_Models_List_With_Typed_Result()
    {
        const string token = "rpc-models-token";
        await ConfigureAuthenticatedUserAsync(token);
        await using var client = CreateAuthenticatedClient(token);
        await client.StartAsync();

        var result = await client.Rpc.Models.ListAsync();
        Assert.NotNull(result.Models);
        Assert.Contains(result.Models, model => model.Id == "claude-sonnet-4.5");
    }

    [Fact]
    public async Task Should_Call_Rpc_Account_GetQuota_When_Authenticated()
    {
        const string token = "rpc-quota-token";
        await ConfigureAuthenticatedUserAsync(
            token,
            new Dictionary<string, CopilotUserQuotaSnapshot>
            {
                ["chat"] = new(
                    Entitlement: 100,
                    OverageCount: 2,
                    OveragePermitted: true,
                    PercentRemaining: 75,
                    TimestampUtc: "2026-04-30T00:00:00Z"),
            });
        await using var client = CreateAuthenticatedClient(token);
        await client.StartAsync();

        var result = await client.Rpc.Account.GetQuotaAsync(gitHubToken: token);
        var chatQuota = Assert.Contains("chat", result.QuotaSnapshots);
        Assert.Equal(100, chatQuota.EntitlementRequests);
        Assert.Equal(25, chatQuota.UsedRequests);
        Assert.Equal(75, chatQuota.RemainingPercentage);
        Assert.Equal(2, chatQuota.Overage);
        Assert.True(chatQuota.UsageAllowedWithExhaustedQuota);
        Assert.True(chatQuota.OverageAllowedWithExhaustedQuota);
        Assert.Equal("2026-04-30T00:00:00Z", chatQuota.ResetDate);
    }

    [Fact]
    public async Task Should_Call_Session_Rpc_Model_GetCurrent()
    {
        var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var result = await session.Rpc.Model.GetCurrentAsync();
        Assert.NotNull(result.ModelId);
        Assert.NotEmpty(result.ModelId);
    }

    [Fact]
    public async Task Should_Call_Session_Rpc_Model_SwitchTo()
    {
        var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var before = await session.Rpc.Model.GetCurrentAsync();
        Assert.NotNull(before.ModelId);

        var result = await session.Rpc.Model.SwitchToAsync(modelId: "gpt-4.1", reasoningEffort: "high");
        Assert.NotNull(result.ModelId);
        Assert.NotEmpty(result.ModelId);
    }

    [Fact]
    public async Task Should_Call_Rpc_Tools_List_With_Typed_Result()
    {
        await Client.StartAsync();

        var result = await Client.Rpc.Tools.ListAsync();

        Assert.NotNull(result.Tools);
        Assert.NotEmpty(result.Tools);
        Assert.All(result.Tools, tool => Assert.False(string.IsNullOrWhiteSpace(tool.Name)));
    }

    [Fact]
    public async Task Should_Call_Get_Last_Session_Id()
    {
        await Client.StartAsync();

        var result = await Client.GetLastSessionIdAsync();

        Assert.Null(result);
    }

    [Fact]
    public async Task Should_Get_Session_Mode()
    {
        var session = await CreateSessionAsync();

        var mode = await session.Rpc.Mode.GetAsync();
        Assert.Equal(SessionMode.Interactive, mode);
    }

    [Fact]
    public async Task Should_Set_Session_Mode()
    {
        var session = await CreateSessionAsync();

        await session.Rpc.Mode.SetAsync(SessionMode.Plan);
        await session.Rpc.Mode.SetAsync(SessionMode.Interactive);
    }

    [Fact]
    public async Task Should_Read_Update_And_Delete_Plan()
    {
        var session = await CreateSessionAsync();

        // Initially plan should not exist
        var initial = await session.Rpc.Plan.ReadAsync();
        Assert.False(initial.Exists);
        Assert.Null(initial.Content);

        // Create/update plan
        var planContent = "# Test Plan\n\n- Step 1\n- Step 2";
        await session.Rpc.Plan.UpdateAsync(planContent);

        // Verify plan exists and has correct content
        var afterUpdate = await session.Rpc.Plan.ReadAsync();
        Assert.True(afterUpdate.Exists);
        Assert.Equal(planContent, afterUpdate.Content);

        // Delete plan
        await session.Rpc.Plan.DeleteAsync();

        // Verify plan is deleted
        var afterDelete = await session.Rpc.Plan.ReadAsync();
        Assert.False(afterDelete.Exists);
        Assert.Null(afterDelete.Content);
    }

    [Fact]
    public async Task Should_Call_Workspace_File_Rpc_Methods()
    {
        var session = await CreateSessionAsync();

        var initial = await session.Rpc.Workspaces.ListFilesAsync();
        Assert.NotNull(initial.Files);

        await session.Rpc.Workspaces.CreateFileAsync("test.txt", "Hello, workspace!");

        var afterCreate = await session.Rpc.Workspaces.ListFilesAsync();
        Assert.Contains("test.txt", afterCreate.Files);

        var file = await session.Rpc.Workspaces.ReadFileAsync("test.txt");
        Assert.Equal("Hello, workspace!", file.Content);

        var workspace = await session.Rpc.Workspaces.GetWorkspaceAsync();
        Assert.NotNull(workspace);
    }
}
