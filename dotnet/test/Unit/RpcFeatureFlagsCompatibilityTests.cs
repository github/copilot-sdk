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
    public async Task FeatureFlags_Read_Preserves_Nullable_Positional_Calls()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method == "session.featureFlags.read"
            ? new Dictionary<string, object?>
            {
                ["snapshot"] = new Dictionary<string, object?> { ["active"] = true },
                ["flags"] = new Dictionary<string, object?>(),
                ["overrides"] = Array.Empty<object?>(),
            }
            : throw new InvalidOperationException($"Unexpected feature flag request '{request.Method}'.");
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        await session.Rpc.FeatureFlags.ReadAsync(null);
        await session.Rpc.FeatureFlags.ReadAsync(null, cancellationToken: CancellationToken.None);
        await session.Rpc.FeatureFlags.ReadWithOptionsAsync(new SessionFeatureFlagsReadRequest
        {
            DisplayOnly = true,
            ServiceId = "original-generation",
        });

        var requests = server.Requests.ToArray();
        Assert.Equal(3, requests.Length);
        Assert.All(requests, request =>
        {
            Assert.Equal("session.featureFlags.read", request.Method);
            Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
            Assert.False(request.Params.TryGetProperty("waitForExpResponse", out _));
        });
        Assert.All(requests.Take(2), request =>
        {
            Assert.False(request.Params.TryGetProperty("displayOnly", out _));
        });
        Assert.True(requests[2].Params.GetProperty("displayOnly").GetBoolean());
        Assert.Equal("original-generation", requests[2].Params.GetProperty("serviceId").GetString());
    }
}
#endif
