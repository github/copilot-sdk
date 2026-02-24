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
        await Client.StartAsync();
        var authStatus = await Client.GetAuthStatusAsync();
        if (!authStatus.IsAuthenticated)
        {
            // Skip if not authenticated - models.list requires auth
            return;
        }

        var result = await Client.Rpc.Models.ListAsync();
        Assert.NotNull(result.Models);
    }

    // account.getQuota is defined in schema but not yet implemented in CLI
    [Fact(Skip = "account.getQuota not yet implemented in CLI")]
    public async Task Should_Call_Rpc_Account_GetQuota_When_Authenticated()
    {
        await Client.StartAsync();
        var authStatus = await Client.GetAuthStatusAsync();
        if (!authStatus.IsAuthenticated)
        {
            // Skip if not authenticated - account.getQuota requires auth
            return;
        }

        var result = await Client.Rpc.Account.GetQuotaAsync();
        Assert.NotNull(result.QuotaSnapshots);
    }

    // session.model.getCurrent is defined in schema but not yet implemented in CLI
    [Fact(Skip = "session.model.getCurrent not yet implemented in CLI")]
    public async Task Should_Call_Session_Rpc_Model_GetCurrent()
    {
        var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var result = await session.Rpc.Model.GetCurrentAsync();
        Assert.NotNull(result.ModelId);
        Assert.NotEmpty(result.ModelId);
    }

    // session.model.switchTo is defined in schema but not yet implemented in CLI
    [Fact(Skip = "session.model.switchTo not yet implemented in CLI")]
    public async Task Should_Call_Session_Rpc_Model_SwitchTo()
    {
        var session = await CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        // Get initial model
        var before = await session.Rpc.Model.GetCurrentAsync();
        Assert.NotNull(before.ModelId);

        // Switch to a different model
        var result = await session.Rpc.Model.SwitchToAsync(modelId: "gpt-4.1");
        Assert.Equal("gpt-4.1", result.ModelId);

        // Verify the switch persisted
        var after = await session.Rpc.Model.GetCurrentAsync();
        Assert.Equal("gpt-4.1", after.ModelId);
    }

    [Fact]
    public async Task Should_Get_And_Set_Session_Mode()
    {
        var session = await CreateSessionAsync();

        // Get initial mode (default should be interactive)
        var initial = await session.Rpc.Mode.GetAsync();
        Assert.Equal(SessionModeGetResultMode.Interactive, initial.Mode);

        // Switch to plan mode
        var planResult = await session.Rpc.Mode.SetAsync(SessionModeGetResultMode.Plan);
        Assert.Equal(SessionModeGetResultMode.Plan, planResult.Mode);

        // Verify mode persisted
        var afterPlan = await session.Rpc.Mode.GetAsync();
        Assert.Equal(SessionModeGetResultMode.Plan, afterPlan.Mode);

        // Switch back to interactive
        var interactiveResult = await session.Rpc.Mode.SetAsync(SessionModeGetResultMode.Interactive);
        Assert.Equal(SessionModeGetResultMode.Interactive, interactiveResult.Mode);
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
    public async Task Should_Create_List_And_Read_Workspace_Files()
    {
        var session = await CreateSessionAsync();

        // Initially no files
        var initialFiles = await session.Rpc.Workspace.ListFilesAsync();
        Assert.Empty(initialFiles.Files);

        // Create a file
        var fileContent = "Hello, workspace!";
        await session.Rpc.Workspace.CreateFileAsync("test.txt", fileContent);

        // List files
        var afterCreate = await session.Rpc.Workspace.ListFilesAsync();
        Assert.Contains("test.txt", afterCreate.Files);

        // Read file
        var readResult = await session.Rpc.Workspace.ReadFileAsync("test.txt");
        Assert.Equal(fileContent, readResult.Content);

        // Create nested file
        await session.Rpc.Workspace.CreateFileAsync("subdir/nested.txt", "Nested content");

        var afterNested = await session.Rpc.Workspace.ListFilesAsync();
        Assert.Contains("test.txt", afterNested.Files);
        Assert.Contains(afterNested.Files, f => f.Contains("nested.txt"));
    }
}
