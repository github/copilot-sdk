/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

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
        var session = await Client.CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

        var result = await session.Rpc.Model.GetCurrentAsync();
        Assert.NotNull(result.ModelId);
        Assert.NotEmpty(result.ModelId);
    }

    // session.model.switchTo is defined in schema but not yet implemented in CLI
    [Fact(Skip = "session.model.switchTo not yet implemented in CLI")]
    public async Task Should_Call_Session_Rpc_Model_SwitchTo()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

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
}
