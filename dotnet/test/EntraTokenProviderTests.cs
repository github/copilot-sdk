/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json.Nodes;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test;

public class EntraTokenProviderTests
{
    [Fact]
    public void Null_Configuration_Remains_Disabled()
    {
        EntraAuthOptions? config = null;
        var options = new CopilotClientOptions { Entra = config };
        Assert.Null(options.Entra);

        config = new EntraAuthOptions { ClientId = "existing-client" };
        options.Entra = config;
        Assert.Equal("existing-client", options.Entra!.ClientId);
        Assert.Null(options.Entra.TokenProvider);
    }

    [Fact]
    public async Task Presents_Typed_Identity_And_Resource_Requests()
    {
        var observed = new List<EntraTokenAcquireRequest>();
        EntraTokenProvider provider = (request, cancellation) =>
        {
            Assert.False(cancellation.IsCancellationRequested);
            observed.Add(request);
            return ValueTask.FromResult<EntraTokenAcquireResult>(request is EntraTokenAcquireRequestIdentity
                ? new EntraTokenAcquireResultIdentity { AccountId = "host-account", Username = "display-name" }
                : new EntraTokenAcquireResultToken { AccountId = "host-account", AccessToken = "resource-token" });
        };
        var peer = await InstallationConfirmationTests.Peer.ConnectAsync(null,
            new EntraAuthOptions { ClientId = "host-client", TokenProvider = provider });
        try
        {
            Assert.Empty(observed);
            await peer.SendAsync(Request(101, "identity"));
            var identity = (await peer.ReadFrameAsync()).GetProperty("result");
            Assert.Equal("identity", identity.GetProperty("kind").GetString());
            Assert.Equal("host-account", identity.GetProperty("accountId").GetString());
            var identityRequest = Assert.IsType<EntraTokenAcquireRequestIdentity>(observed[0]);
            Assert.Equal("host-client", identityRequest.ClientId);
            Assert.Equal(EntraTokenAcquireReason.SignIn, identityRequest.Reason);

            await peer.SendAsync(Request(102, "token"));
            Assert.Equal("resource-token", (await peer.ReadFrameAsync()).GetProperty("result").GetProperty("accessToken").GetString());
            var token = Assert.IsType<EntraTokenAcquireRequestToken>(observed[1]);
            Assert.Equal("host-account", token.AccountId);
            Assert.Equal("resource-client", token.ClientId);
            Assert.Equal("resource", token.Audience);
            Assert.Equal("tenant", token.Tenant);
            Assert.True(token.SkipCachedAccessToken);
            Assert.Equal(["resource/.default"], token.Scopes);
            Assert.Equal(EntraTokenAcquireReason.Renewal, token.Reason);
        }
        finally { await peer.DisposeAsync(); }
    }

    [Fact]
    public async Task Cancellation_Rejects_A_Cancellation_Time_Success()
    {
        var entered = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        EntraTokenProvider provider = async (request, cancellation) =>
        {
            if (request is EntraTokenAcquireRequestToken) return new EntraTokenAcquireResultUnavailable();
            entered.SetResult(cancellation);
            try { await Task.Delay(Timeout.InfiniteTimeSpan, cancellation); }
            catch (OperationCanceledException) when (cancellation.IsCancellationRequested) { }
            return new EntraTokenAcquireResultIdentity { AccountId = "late-account" };
        };
        var peer = await InstallationConfirmationTests.Peer.ConnectAsync(null,
            new EntraAuthOptions { ClientId = "host-client", TokenProvider = provider });
        try
        {
            await peer.SendAsync(Request(201, "identity"));
            var cancellation = await entered.Task.WaitAsync(TimeSpan.FromSeconds(5));
            await peer.CancelAsync(201);
            var response = await peer.ReadFrameAsync();
            Assert.Equal(-32800, response.GetProperty("error").GetProperty("code").GetInt32());
            Assert.True(cancellation.IsCancellationRequested);
            await peer.SendAsync(Request(202, "token"));
            var next = await peer.ReadFrameAsync();
            Assert.Equal(202, next.GetProperty("id").GetInt32());
            Assert.Equal("unavailable", next.GetProperty("result").GetProperty("kind").GetString());
        }
        finally { await peer.DisposeAsync(); }
    }

    [Fact]
    public async Task Original_Connection_Close_Cancels_Host_Acquisition()
    {
        var entered = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        EntraTokenProvider provider = async (_, cancellation) =>
        {
            entered.SetResult(cancellation);
            await Task.Delay(Timeout.InfiniteTimeSpan, cancellation);
            return new EntraTokenAcquireResultCancelled();
        };
        var peer = await InstallationConfirmationTests.Peer.ConnectAsync(null,
            new EntraAuthOptions { ClientId = "host-client", TokenProvider = provider });
        try
        {
            await peer.SendAsync(Request(301, "identity"));
            var cancellation = await entered.Task.WaitAsync(TimeSpan.FromSeconds(5));
            var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
            using var registration = cancellation.Register(() => cancelled.TrySetResult());
            peer.CloseTransport();
            await cancelled.Task.WaitAsync(TimeSpan.FromSeconds(5));
        }
        finally { await peer.DisposeAsync(); }
    }

    [Fact]
    public async Task Callback_Errors_Do_Not_Expose_Credentials()
    {
        EntraTokenProvider provider = (_, _) => throw new InvalidOperationException("credential-from-host-exception");
        var peer = await InstallationConfirmationTests.Peer.ConnectAsync(null,
            new EntraAuthOptions { ClientId = "host-client", TokenProvider = provider });
        try
        {
            await peer.SendAsync(Request(401, "identity"));
            var error = (await peer.ReadFrameAsync()).GetProperty("error");
            Assert.Equal(-32603, error.GetProperty("code").GetInt32());
            Assert.Equal("Entra token provider failed.", error.GetProperty("message").GetString());
        }
        finally { await peer.DisposeAsync(); }
    }

    private static JsonObject Request(long id, string kind)
    {
        var request = new JsonObject
        {
            ["kind"] = kind,
            ["clientId"] = "host-client",
            ["reason"] = "signIn",
        };
        if (kind == "token")
        {
            request["accountId"] = "host-account";
            request["clientId"] = "resource-client";
            request["reason"] = "renewal";
            request["scopes"] = new JsonArray("resource/.default");
            request["audience"] = "resource";
            request["tenant"] = "tenant";
            request["skipCachedAccessToken"] = true;
        }
        return new JsonObject
        {
            ["jsonrpc"] = "2.0",
            ["id"] = id,
            ["method"] = "entraToken.getToken",
            ["params"] = new JsonObject { ["request"] = request },
        };
    }
}
