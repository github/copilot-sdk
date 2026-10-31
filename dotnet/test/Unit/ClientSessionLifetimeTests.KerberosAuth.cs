/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData("fallback", null)]
    [InlineData("always", "runtime")]
    public async Task KerberosAuth_Registers_Mode_And_Scope_And_Routes_Concurrent_Lookups(string mode, string? scope)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = KerberosAuthSetProviderResponse;
        var calls = new List<string>();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            KerberosAuth = new KerberosAuthConfig
            {
                Mode = mode == "always" ? KerberosAuthMode.Always : KerberosAuthMode.Fallback,
                Scope = scope is null ? null : new KerberosAuthScope(scope),
                Callback = async (request, _) =>
                {
                    lock (calls)
                    {
                        calls.Add(request.Spn);
                    }
                    await Task.Yield();
                    return Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes(request.Spn));
                }
            }
        });

        await client.StartAsync();
        string[] spns = ["HTTP/proxy.example", "ldap/directory.example@EXAMPLE.COM"];
        var responses = await Task.WhenAll(spns.Select(spn =>
            server.SendRequestAsync("kerberosAuth.acquire", new() { ["spn"] = spn }).WaitAsync(TimeSpan.FromSeconds(5))));

        var registration = Assert.Single(server.Requests, request => request.Method == "kerberosAuth.setProvider");
        Assert.Equal(mode, registration.Params.GetProperty("mode").GetString());
        if (scope is null)
        {
            Assert.False(registration.Params.TryGetProperty("scope", out _));
        }
        else
        {
            Assert.Equal(scope, registration.Params.GetProperty("scope").GetString());
        }
        Assert.Equal(spns.Order(), calls.Order());
        Assert.Equal(
            spns.Select(spn => Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes(spn))),
            responses.Select(response => response.GetProperty("token").GetString()));
    }

    [Fact]
    public async Task KerberosAuth_Is_Not_Registered_Without_Option()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = KerberosAuthSetProviderResponse;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        await client.StartAsync();

        Assert.DoesNotContain(server.Requests, request => request.Method == "kerberosAuth.setProvider");
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("kerberosAuth.acquire", new() { ["spn"] = "HTTP/proxy" }).WaitAsync(TimeSpan.FromSeconds(5)));
    }

    [Fact]
    public async Task KerberosAuth_Returns_No_Token_When_Unavailable_And_Rejects_Callback_Failures()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = KerberosAuthSetProviderResponse;
        var fail = false;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            KerberosAuth = new KerberosAuthConfig
            {
                Mode = KerberosAuthMode.Always,
                Callback = (_, _) => fail
                    ? throw new InvalidOperationException("lookup failed")
                    : Task.FromResult<string?>(null)
            }
        });
        await client.StartAsync();
        var timeout = TimeSpan.FromSeconds(5);

        var unavailable = await server.SendRequestAsync("kerberosAuth.acquire", new() { ["spn"] = "HTTP/proxy" }).WaitAsync(timeout);
        fail = true;
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("kerberosAuth.acquire", new() { ["spn"] = "HTTP/proxy" }).WaitAsync(timeout));

        Assert.Equal(JsonValueKind.Object, unavailable.ValueKind);
        Assert.False(unavailable.TryGetProperty("token", out _));
        Assert.Equal("lookup failed", error.Message);
        Assert.Equal(-32603, ((JsonElement)error.Data["error"]!).GetProperty("code").GetInt32());
    }

    [Fact]
    public async Task KerberosAuth_Rejects_Callback_Local_Cancellation_Promptly()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = KerberosAuthSetProviderResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            KerberosAuth = new KerberosAuthConfig
            {
                Mode = KerberosAuthMode.Always,
                Callback = async (_, _) =>
                {
                    using var callbackTimeout = new CancellationTokenSource(TimeSpan.FromMilliseconds(10));
                    await Task.Delay(Timeout.InfiniteTimeSpan, callbackTimeout.Token);
                    return null;
                }
            }
        });
        await client.StartAsync();

        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("kerberosAuth.acquire", new() { ["spn"] = "HTTP/proxy" }).WaitAsync(TimeSpan.FromSeconds(5)));

        Assert.Equal("Kerberos credential callback was cancelled.", error.Message);
        Assert.Equal(-32603, ((JsonElement)error.Data["error"]!).GetProperty("code").GetInt32());
    }

    [Fact]
    public async Task KerberosAuth_Registers_Before_Sessions_Requested_During_Startup()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = KerberosAuthSetProviderResponse;
        var releaseConnect = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, cancellationToken) =>
        {
            if (request.Method == "connect")
            {
                await releaseConnect.Task.WaitAsync(cancellationToken);
            }
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            KerberosAuth = new KerberosAuthConfig
            {
                Mode = KerberosAuthMode.Always,
                Callback = (_, _) => Task.FromResult<string?>(null)
            }
        });
        var timeout = TimeSpan.FromSeconds(5);

        var starting = client.StartAsync();
        await server.RequestsChanged.WaitForAsync(() => server.Requests.Any(request => request.Method == "connect"), timeout);
        var creating = client.CreateSessionAsync(new SessionConfig());
        // Give a session request that does not wait for startup time to reach the runtime before registration.
        await Task.Delay(TimeSpan.FromMilliseconds(200));
        releaseConnect.SetResult();
        await starting.WaitAsync(timeout);
        await using var session = await creating.WaitAsync(timeout);

        string[] expectedOrder = ["connect", "kerberosAuth.setProvider", "session.create"];
        Assert.Equal(
            expectedOrder,
            server.Requests.Select(request => request.Method).Where(method => expectedOrder.Contains(method)));
    }

    private static Dictionary<string, object?> KerberosAuthSetProviderResponse(RpcRequestRecord request) => request.Method == "kerberosAuth.setProvider"
        ? new Dictionary<string, object?>()
        : throw new InvalidOperationException($"Unexpected RPC method '{request.Method}'.");
}
#endif
