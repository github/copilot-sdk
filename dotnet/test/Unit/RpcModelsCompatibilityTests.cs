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
    public void Models_List_Preserves_Exact_Legacy_Method_Signature()
    {
        var method = typeof(ServerModelsApi).GetMethod(
            nameof(ServerModelsApi.ListAsync),
            [typeof(string), typeof(string), typeof(CancellationToken)]);

        Assert.NotNull(method);
        Assert.Equal(typeof(Task<ModelList>), method.ReturnType);
        Assert.All(method.GetParameters(), parameter => Assert.False(parameter.IsOptional));
    }

    [Fact]
    public async Task Models_List_Preserves_Positional_Cancellation_And_Sends_Streamer_Mode()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method == "models.list"
            ? new Dictionary<string, object?> { ["models"] = Array.Empty<object>() }
            : throw new InvalidOperationException($"Unexpected models request '{request.Method}'.");
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await client.StartAsync();
        server.ClearRequests();
        using var cancellation = new CancellationTokenSource();

        Func<string?, string?, CancellationToken, Task<ModelList>> legacyList = client.Rpc.Models.ListAsync;
        await legacyList(null, null, cancellation.Token);
        await client.Rpc.Models.ListAsync("account", "token", cancellation.Token);
        await client.Rpc.Models.ListAsync(streamerMode: true);
        await client.Rpc.Models.ListAsync(null, null, cancellation.Token, false);
        await client.Rpc.Models.ListAsync();

        var requests = server.Requests.ToArray();
        Assert.Equal(5, requests.Length);
        Assert.All(requests, request => Assert.Equal("models.list", request.Method));
        Assert.False(requests[0].Params.TryGetProperty("streamerMode", out _));
        Assert.False(requests[1].Params.TryGetProperty("streamerMode", out _));
        Assert.Equal("account", requests[1].Params.GetProperty("selectionId").GetString());
        Assert.Equal("token", requests[1].Params.GetProperty("gitHubToken").GetString());
        Assert.True(requests[2].Params.GetProperty("streamerMode").GetBoolean());
        Assert.False(requests[3].Params.GetProperty("streamerMode").GetBoolean());
        Assert.False(requests[4].Params.TryGetProperty("streamerMode", out _));

        cancellation.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(
            () => client.Rpc.Models.ListAsync(null, null, cancellation.Token));
    }
}
#endif
