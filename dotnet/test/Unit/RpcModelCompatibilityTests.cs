/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public void Existing_Model_Switch_Signature_And_Defaults_Are_Preserved()
    {
        Type[] types =
        [
            typeof(string), typeof(AutoTier?), typeof(string), typeof(ReasoningSummary?),
            typeof(Verbosity?), typeof(ModelCapabilitiesOverride), typeof(ContextTier?),
            typeof(ModelChangeSource?), typeof(bool?), typeof(string), typeof(bool?),
            typeof(string), typeof(string), typeof(bool?), typeof(ModelPickerPersistenceRequest),
            typeof(CancellationToken),
        ];
        var method = typeof(ModelApi).GetMethod("SwitchToAsync", types);
        Assert.NotNull(method);
        var parameters = method.GetParameters();
        Assert.False(parameters[0].IsOptional);
        Assert.Equal("cancellationToken", parameters[^1].Name);
        Assert.All(parameters.Skip(1), parameter =>
        {
            Assert.True(parameter.IsOptional);
            Assert.True(parameter.HasDefaultValue);
            Assert.Null(parameter.DefaultValue);
        });
    }

    [Fact]
    public async Task Existing_Positional_Model_Switch_Calls_Preserve_Wire_Values()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        await session.Rpc.Model.SwitchToAsync("auto", AutoTier.Balance);
        await session.Rpc.Model.SwitchToAsync(
            "auto", AutoTier.Efficiency, "high", null, null, null, null, null,
            true, "compact", true, "repo", "global", true, null, CancellationToken.None);

        var requests = server.Requests.ToArray();
        Assert.Equal(2, requests.Length);
        foreach (var request in requests)
        {
            Assert.Equal("session.model.switchTo", request.Method);
            Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
            Assert.Equal("auto", request.Params.GetProperty("modelId").GetString());
            Assert.False(request.Params.TryGetProperty("providerId", out _));
        }
        Assert.Equal("balance", requests[0].Params.GetProperty("autoTier").GetString());
        Assert.False(requests[0].Params.TryGetProperty("reasoningEffort", out _));
        Assert.Equal("efficiency", requests[1].Params.GetProperty("autoTier").GetString());
        Assert.Equal("high", requests[1].Params.GetProperty("reasoningEffort").GetString());
        Assert.True(requests[1].Params.GetProperty("deferIfModelChangeQueued").GetBoolean());
        Assert.Equal("compact", requests[1].Params.GetProperty("compactionDecision").GetString());
        Assert.True(requests[1].Params.GetProperty("runCompactionPreflight").GetBoolean());
        Assert.Equal("repo", requests[1].Params.GetProperty("repoScope").GetString());
        Assert.Equal("global", requests[1].Params.GetProperty("modelChangeScope").GetString());
        Assert.True(requests[1].Params.GetProperty("requireAvailable").GetBoolean());
    }

    [Fact]
    public async Task Provider_Qualified_Model_Switch_Uses_Request_With_Sdk_Owned_Session()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var result = await session.Rpc.Model.SwitchToAsync(new ModelSwitchToRequest
        {
            ModelId = "shared-model",
            ProviderId = "provider-a",
            ReasoningEffort = "high",
        }, CancellationToken.None);

        Assert.Null(typeof(ModelSwitchToRequest).GetProperty("SessionId"));
        var request = Assert.Single(server.Requests);
        Assert.Equal("session.model.switchTo", request.Method);
        Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
        Assert.Equal("shared-model", request.Params.GetProperty("modelId").GetString());
        Assert.Equal("provider-a", request.Params.GetProperty("providerId").GetString());
        Assert.Equal("high", request.Params.GetProperty("reasoningEffort").GetString());
        Assert.False(request.Params.TryGetProperty("autoTier", out _));
        // The fake reports "auto"; the SDK must return the server's result, not the requested id.
        Assert.Equal("auto", result.ModelId);
    }
}
#endif
