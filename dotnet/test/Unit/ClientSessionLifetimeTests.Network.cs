/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text;
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private const string ResolveProxyMethod = "network.resolveProxy";
    private const string AuthenticateNegotiateMethod = "network.authenticateNegotiate";
    private const string AuthenticateBasicMethod = "network.authenticateBasic";

    [Fact]
    public async Task Network_Registers_All_Callbacks_And_Answers_Concurrent_Lookups()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        var calls = new List<string>();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Network = new NetworkConfig
            {
                ResolveProxy = async (request, _) =>
                {
                    lock (calls)
                    {
                        calls.Add(request.Url);
                    }
                    await Task.Yield();
                    return request.Url.StartsWith("https:", StringComparison.Ordinal) ? "PROXY proxy.example:8080; DIRECT" : null;
                },
                AuthenticateNegotiate = async (request, _) =>
                {
                    lock (calls)
                    {
                        calls.Add(request.Spn);
                    }
                    await Task.Yield();
                    return Convert.ToBase64String(Encoding.UTF8.GetBytes(request.Spn));
                },
                AuthenticateBasic = async (request, _) =>
                {
                    lock (calls)
                    {
                        calls.Add($"{request.Proxy} {request.Realm} {request.PreviousRejected}");
                    }
                    await Task.Yield();
                    return new NetworkAuthenticateBasicResult { Username = "user", Password = "secret" };
                },
            }
        });

        await client.StartAsync();
        var timeout = TimeSpan.FromSeconds(5);
        var responses = await Task.WhenAll(
            server.SendRequestAsync(ResolveProxyMethod, new() { ["url"] = "https://api.example.com/" }).WaitAsync(timeout),
            server.SendRequestAsync(ResolveProxyMethod, new() { ["url"] = "http://example.com/path" }).WaitAsync(timeout),
            server.SendRequestAsync(AuthenticateNegotiateMethod, new() { ["spn"] = "HTTP/proxy.example" }).WaitAsync(timeout),
            server.SendRequestAsync(
                AuthenticateBasicMethod,
                new() { ["proxy"] = "http://proxy.example:8080", ["realm"] = "corp", ["previousRejected"] = true }).WaitAsync(timeout));

        var registration = Assert.Single(server.Requests, request => request.Method == "network.setProvider");
        Assert.Equal("""{"authenticateBasic":{},"authenticateNegotiate":{},"resolveProxy":{}}""", registration.Params.GetRawText());
        Assert.Equal(
            ["HTTP/proxy.example", "http://example.com/path", "http://proxy.example:8080 corp True", "https://api.example.com/"],
            calls.Order(StringComparer.Ordinal));
        Assert.Equal(
            [
                """{"proxy":"PROXY proxy.example:8080; DIRECT"}""",
                "{}",
                $$"""{"token":"{{Convert.ToBase64String(Encoding.UTF8.GetBytes("HTTP/proxy.example"))}}"}""",
                """{"password":"secret","username":"user"}""",
            ],
            responses.Select(response => response.GetRawText()));
    }

    [Theory]
    [InlineData(false, null, false, false, false, null)]
    [InlineData(true, null, false, false, false, null)]
    [InlineData(true, "runtime", false, false, false, null)]
    [InlineData(true, null, true, false, false, """{"resolveProxy":{}}""")]
    [InlineData(true, "runtime", false, true, false, """{"authenticateNegotiate":{},"scope":"runtime"}""")]
    [InlineData(true, null, false, false, true, """{"authenticateBasic":{}}""")]
    [InlineData(true, "connection", true, false, false, """{"resolveProxy":{},"scope":"connection"}""")]
    public async Task Network_Registers_Only_Configured_Callbacks_And_Scope(
        bool configured,
        string? scope,
        bool resolveProxy,
        bool authenticateNegotiate,
        bool authenticateBasic,
        string? expectedRegistration)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Network = configured
                ? new NetworkConfig
                {
                    Scope = scope is null ? null : new NetworkScope(scope),
                    ResolveProxy = resolveProxy ? (_, _) => Task.FromResult<string?>(null) : null,
                    AuthenticateNegotiate = authenticateNegotiate ? (_, _) => Task.FromResult<string?>(null) : null,
                    AuthenticateBasic = authenticateBasic ? (_, _) => Task.FromResult<NetworkAuthenticateBasicResult?>(null) : null,
                }
                : null
        });

        await client.StartAsync();
        var timeout = TimeSpan.FromSeconds(5);
        var proxyLookup = server.SendRequestAsync(ResolveProxyMethod, new() { ["url"] = "https://api.example.com/" }).WaitAsync(timeout);
        var negotiateLookup = server.SendRequestAsync(AuthenticateNegotiateMethod, new() { ["spn"] = "HTTP/proxy" }).WaitAsync(timeout);
        var basicLookup = server.SendRequestAsync(AuthenticateBasicMethod, NetworkRequestParams(AuthenticateBasicMethod)).WaitAsync(timeout);
        var answered = new List<bool>();
        foreach (var lookup in new[] { proxyLookup, negotiateLookup, basicLookup })
        {
            try
            {
                await lookup;
                answered.Add(true);
            }
            catch (InvalidOperationException)
            {
                answered.Add(false);
            }
        }

        string[] expectedRegistrations = expectedRegistration is null ? [] : [expectedRegistration];
        Assert.Equal(
            expectedRegistrations,
            server.Requests.Where(request => request.Method == "network.setProvider").Select(request => request.Params.GetRawText()));
        Assert.Equal([resolveProxy, authenticateNegotiate, authenticateBasic], answered);
    }

    [Fact]
    public async Task Network_Registers_Before_Sessions_Requested_During_Startup()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
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
            Network = new NetworkConfig { AuthenticateNegotiate = (_, _) => Task.FromResult<string?>(null) }
        });
        var timeout = TimeSpan.FromSeconds(5);

        var starting = client.StartAsync();
        await server.RequestsChanged.WaitForAsync(() => server.Requests.Any(request => request.Method == "connect"), timeout);
        var creating = client.CreateSessionAsync(new SessionConfig());
        try
        {
            // A reply on the same transport acknowledges preceding client writes
            // while connect is held, without relying on a quiet observation window.
            await server.SendRequestAsync(AuthenticateNegotiateMethod,
                NetworkRequestParams(AuthenticateNegotiateMethod)).WaitAsync(timeout);
            Assert.DoesNotContain(server.Requests, request => request.Method == "session.create");
        }
        finally
        {
            releaseConnect.SetResult();
        }
        await starting.WaitAsync(timeout);
        await using var session = await creating.WaitAsync(timeout);

        string[] expectedOrder = ["connect", "network.setProvider", "session.create"];
        Assert.Equal(
            expectedOrder,
            server.Requests.Select(request => request.Method).Where(method => expectedOrder.Contains(method)));
    }

    [Fact]
    public async Task Network_SetProvider_Preserves_Nullable_Positional_Calls()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await client.StartAsync();
        server.ClearRequests();

        await client.Rpc.Network.SetProviderAsync(null);
        await client.Rpc.Network.SetProviderAsync(null, cancellationToken: CancellationToken.None);
        await client.Rpc.Network.SetProviderWithOptionsAsync(new NetworkSetProviderRequest
        {
            AuthenticateBasic = new NetworkAuthenticateBasicOptions(),
        });

        Assert.Equal(
            ["{}", "{}", """{"authenticateBasic":{}}"""],
            server.Requests.Where(request => request.Method == "network.setProvider").Select(request => request.Params.GetRawText()));
    }

    [Fact]
    public async Task Network_Answers_Missing_Values_And_Rejects_Callback_Failures()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        var fail = false;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Network = new NetworkConfig
            {
                ResolveProxy = (_, _) => fail ? throw new InvalidOperationException("lookup failed") : Task.FromResult<string?>(null),
                AuthenticateNegotiate = (_, _) => fail ? throw new InvalidOperationException("lookup failed") : Task.FromResult<string?>(null),
                AuthenticateBasic = (_, _) => fail ? throw new InvalidOperationException("lookup failed") : Task.FromResult<NetworkAuthenticateBasicResult?>(null),
            }
        });
        await client.StartAsync();
        var timeout = TimeSpan.FromSeconds(5);

        var missing = await Task.WhenAll(
            server.SendRequestAsync(ResolveProxyMethod, new() { ["url"] = "https://api.example.com/" }).WaitAsync(timeout),
            server.SendRequestAsync(AuthenticateNegotiateMethod, new() { ["spn"] = "HTTP/proxy" }).WaitAsync(timeout),
            server.SendRequestAsync(AuthenticateBasicMethod, NetworkRequestParams(AuthenticateBasicMethod)).WaitAsync(timeout));
        fail = true;
        var proxyError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync(ResolveProxyMethod, new() { ["url"] = "https://api.example.com/" }).WaitAsync(timeout));
        var negotiateError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync(AuthenticateNegotiateMethod, new() { ["spn"] = "HTTP/proxy" }).WaitAsync(timeout));
        var basicError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync(AuthenticateBasicMethod, NetworkRequestParams(AuthenticateBasicMethod)).WaitAsync(timeout));

        Assert.Equal(["{}", "{}", "{}"], missing.Select(response => response.GetRawText()));
        Assert.Equal(
            [("lookup failed", -32603), ("lookup failed", -32603), ("lookup failed", -32603)],
            new[] { proxyError, negotiateError, basicError }.Select(error => (error.Message, ErrorCode(error))));
    }

    [Theory]
    [InlineData(ResolveProxyMethod, "Proxy resolution callback was cancelled.")]
    [InlineData(AuthenticateNegotiateMethod, "Negotiate authentication callback was cancelled.")]
    [InlineData(AuthenticateBasicMethod, "Basic authentication callback was cancelled.")]
    public async Task Network_Rejects_Callback_Local_Cancellation_Promptly(string method, string expectedMessage)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Network = AllNetworkCallbacks(async _ =>
            {
                using var callbackTimeout = new CancellationTokenSource(TimeSpan.FromMilliseconds(10));
                await Task.Delay(Timeout.InfiniteTimeSpan, callbackTimeout.Token);
                return null;
            })
        });
        await client.StartAsync();

        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync(method, NetworkRequestParams(method)).WaitAsync(TimeSpan.FromSeconds(5)));

        Assert.Equal((expectedMessage, -32603), (error.Message, ErrorCode(error)));
    }

    [Theory]
    [InlineData(ResolveProxyMethod)]
    [InlineData(AuthenticateNegotiateMethod)]
    [InlineData(AuthenticateBasicMethod)]
    public async Task Network_Runtime_Cancellation_Cancels_Blocking_Callback_Without_Blocking_Reader_And_Ignores_Late_Result(string method)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        var timeout = TimeSpan.FromSeconds(5);
        var started = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        var observedCancellation = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            // Simulates a lookup that blocks (for example on a credential prompt) until cancelled before returning
            // a task, then answers anyway.
            Network = AllNetworkCallbacks(cancellationToken =>
            {
                started.SetResult(cancellationToken.IsCancellationRequested);
                observedCancellation.SetResult(cancellationToken.WaitHandle.WaitOne(timeout));
                return Task.FromResult<string?>("late");
            })
        });
        await client.StartAsync();

        var lookup = server.SendRequestAsync(method, NetworkRequestParams(method));
        var cancelledBeforeRuntimeCancel = await started.Task.WaitAsync(timeout);
        await client.PingAsync().WaitAsync(timeout);
        await server.CancelLatestRequestAsync();
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() => lookup.WaitAsync(timeout));

        Assert.False(cancelledBeforeRuntimeCancel);
        Assert.True(await observedCancellation.Task.WaitAsync(timeout));
        Assert.Equal(("Request cancelled", -32800), (error.Message, ErrorCode(error)));
    }

    [Theory]
    [InlineData(ResolveProxyMethod)]
    [InlineData(AuthenticateNegotiateMethod)]
    [InlineData(AuthenticateBasicMethod)]
    public async Task Network_Connection_Close_Cancels_Callback(string method)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = NetworkSetProviderResponse;
        var timeout = TimeSpan.FromSeconds(5);
        var started = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Network = AllNetworkCallbacks(async cancellationToken =>
            {
                started.SetResult(cancellationToken);
                try
                {
                    await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken);
                }
                catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
                {
                    // Observed here rather than by a registration this callback disposes on exit: the
                    // delay's cancellation runs first and can resume the callback inline, which would
                    // unregister that callback before it runs.
                    cancelled.TrySetResult();
                    throw;
                }
                return null;
            })
        });
        await client.StartAsync();

        var lookup = server.SendRequestAsync(method, NetworkRequestParams(method));
        _ = lookup.ContinueWith(
            static task => _ = task.Exception,
            CancellationToken.None,
            TaskContinuationOptions.OnlyOnFaulted | TaskContinuationOptions.ExecuteSynchronously,
            TaskScheduler.Default);
        var token = await started.Task.WaitAsync(timeout);
        await client.PingAsync().WaitAsync(timeout);
        Assert.False(token.IsCancellationRequested);

        server.CloseConnection();

        await cancelled.Task.WaitAsync(timeout);
        Assert.True(token.IsCancellationRequested);
    }

    [Fact]
    public async Task Network_Callbacks_Queued_After_Cancellation_Are_Not_Started()
    {
        var invoked = new List<string>();
        var config = new NetworkConfig
        {
            ResolveProxy = (_, _) =>
            {
                invoked.Add(ResolveProxyMethod);
                return Task.FromResult<string?>("PROXY proxy.example:8080");
            },
            AuthenticateNegotiate = (_, _) =>
            {
                invoked.Add(AuthenticateNegotiateMethod);
                return Task.FromResult<string?>("token");
            },
            AuthenticateBasic = (_, _) =>
            {
                invoked.Add(AuthenticateBasicMethod);
                return Task.FromResult<NetworkAuthenticateBasicResult?>(new() { Username = "user" });
            },
        };
        // The adapter is internal; the client invokes it on the thread pool, where a request
        // cancelled before a worker picks it up cannot be reached deterministically.
        var handler = CreateNetworkAdapter(config);
        using var cancellation = new CancellationTokenSource();
        cancellation.Cancel();

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            handler.ResolveProxyAsync(new NetworkResolveProxyRequest { Url = "https://api.example.com/" }, cancellation.Token));
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            handler.AuthenticateNegotiateAsync(new NetworkAuthenticateNegotiateRequest { Spn = "HTTP/proxy" }, cancellation.Token));
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            handler.AuthenticateBasicAsync(BasicRequest(), cancellation.Token));

        Assert.Empty(invoked);
    }

    [Fact]
    public async Task Network_Discards_Answers_Given_As_The_Request_Ends()
    {
        var timeout = TimeSpan.FromSeconds(5);
        var outcomes = new List<string>();
        foreach (var method in new[] { ResolveProxyMethod, AuthenticateNegotiateMethod, AuthenticateBasicMethod })
        {
            var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
            using var release = new ManualResetEventSlim();
            var handler = CreateNetworkAdapter(AllNetworkCallbacks(_ =>
            {
                started.TrySetResult();
                // Ignores the request's cancellation, to answer after it. Only the cancellation below (or a failing
                // test's cleanup) releases it, so it never answers before the request ends.
                release.Wait(CancellationToken.None);
                return Task.FromResult<string?>("late");
            }));
            try
            {
                // The dispatcher's token is cancelled when the runtime cancels the request or the connection closes.
                using var closed = new CancellationTokenSource();
                Task lookup = method switch
                {
                    ResolveProxyMethod => handler.ResolveProxyAsync(new NetworkResolveProxyRequest { Url = "https://api.example.com/" }, closed.Token),
                    AuthenticateNegotiateMethod => handler.AuthenticateNegotiateAsync(new NetworkAuthenticateNegotiateRequest { Spn = "HTTP/proxy" }, closed.Token),
                    _ => handler.AuthenticateBasicAsync(BasicRequest(), closed.Token),
                };
                await started.Task.WaitAsync(timeout);
                // Registered after the adapter started waiting, so it runs first when the token is cancelled. It lets
                // the callback answer and holds back the adapter's own cancellation until the lookup has ended: the
                // callback answers as the request ends, before the adapter's wait observes the cancellation.
                using var answerOnCancel = closed.Token.Register(() =>
                {
                    release.Set();
                    SpinWait.SpinUntil(() => lookup.IsCompleted, timeout);
                });

                closed.Cancel();

                try
                {
                    await lookup.WaitAsync(timeout);
                    outcomes.Add($"{method} answered");
                }
                catch (OperationCanceledException)
                {
                    outcomes.Add($"{method} cancelled");
                }
            }
            finally
            {
                release.Set();
            }
        }

        Assert.Equal(
            [$"{ResolveProxyMethod} cancelled", $"{AuthenticateNegotiateMethod} cancelled", $"{AuthenticateBasicMethod} cancelled"],
            outcomes);
    }

    [Fact]
    public async Task Network_Stops_Waiting_For_Callbacks_That_Ignore_Cancellation()
    {
        var ignored = new TaskCompletionSource<string?>(TaskCreationOptions.RunContinuationsAsynchronously);
        var unobserved = new List<Exception>();
        EventHandler<UnobservedTaskExceptionEventArgs> onUnobserved = (_, args) =>
        {
            lock (unobserved)
            {
                unobserved.AddRange(args.Exception.Flatten().InnerExceptions.Where(error => error.Message == "late failure"));
            }
        };
        TaskScheduler.UnobservedTaskException += onUnobserved;
        try
        {
            await CancelCallbacksThatIgnoreCancellationAsync(ignored.Task);
            // The callbacks' late failure must not be reported as an unobserved task exception.
            ignored.SetException(new InvalidOperationException("late failure"));
            for (var attempt = 0; attempt < 10; attempt++)
            {
                await Task.Delay(10);
                GC.Collect();
                GC.WaitForPendingFinalizers();
            }
        }
        finally
        {
            TaskScheduler.UnobservedTaskException -= onUnobserved;
        }

        lock (unobserved)
        {
            Assert.Empty(unobserved);
        }
    }

    /// <summary>
    /// Starts every callback, each returning <paramref name="ignored"/> and ignoring cancellation, then cancels them and
    /// checks that the requests end anyway. Separate so nothing in the test keeps the abandoned callbacks reachable.
    /// </summary>
    [System.Runtime.CompilerServices.MethodImpl(System.Runtime.CompilerServices.MethodImplOptions.NoInlining)]
    private static async Task CancelCallbacksThatIgnoreCancellationAsync(Task<string?> ignored)
    {
        var timeout = TimeSpan.FromSeconds(5);
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var handler = CreateNetworkAdapter(AllNetworkCallbacks(_ =>
        {
            started.TrySetResult();
            return ignored;
        }));
        // The dispatcher's token is cancelled when the runtime cancels the request or the connection closes.
        using var closed = new CancellationTokenSource();
        var lookups = new Task[]
        {
            handler.ResolveProxyAsync(new NetworkResolveProxyRequest { Url = "https://api.example.com/" }, closed.Token),
            handler.AuthenticateNegotiateAsync(new NetworkAuthenticateNegotiateRequest { Spn = "HTTP/proxy" }, closed.Token),
            handler.AuthenticateBasicAsync(BasicRequest(), closed.Token),
        };
        await started.Task.WaitAsync(timeout);

        closed.Cancel();

        foreach (var lookup in lookups)
        {
            await Assert.ThrowsAnyAsync<OperationCanceledException>(() => lookup.WaitAsync(timeout));
        }
    }

    private static INetworkHandler CreateNetworkAdapter(NetworkConfig config) => (INetworkHandler)Activator.CreateInstance(
        typeof(NetworkConfig).Assembly.GetType("GitHub.Copilot.NetworkAdapter", throwOnError: true)!,
        System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic,
        binder: null,
        args: [config],
        culture: null)!;

    /// <summary>
    /// Configures every network callback with the same lookup, which receives the request's cancellation token; Basic
    /// authentication answers with the lookup's result as the user name.
    /// </summary>
    private static NetworkConfig AllNetworkCallbacks(Func<CancellationToken, Task<string?>> lookup) => new()
    {
        ResolveProxy = (_, cancellationToken) => lookup(cancellationToken),
        AuthenticateNegotiate = (_, cancellationToken) => lookup(cancellationToken),
        AuthenticateBasic = async (_, cancellationToken) =>
            await lookup(cancellationToken) is { } username ? new NetworkAuthenticateBasicResult { Username = username } : null,
    };

    private static NetworkAuthenticateBasicRequest BasicRequest() => new() { Proxy = "http://proxy:8080", PreviousRejected = false };

    private static Dictionary<string, object?> NetworkRequestParams(string method) => method switch
    {
        ResolveProxyMethod => new() { ["url"] = "https://api.example.com/" },
        AuthenticateNegotiateMethod => new() { ["spn"] = "HTTP/proxy" },
        _ => new() { ["proxy"] = "http://proxy:8080", ["previousRejected"] = false },
    };

    private static int ErrorCode(Exception error) => ((JsonElement)error.Data["error"]!).GetProperty("code").GetInt32();

    private static object? NetworkSetProviderResponse(RpcRequestRecord request) => request.Method == "network.setProvider"
        ? null
        : throw new InvalidOperationException($"Unexpected RPC method '{request.Method}'.");

    private sealed partial class FakeCopilotServer
    {
        public Task CancelLatestRequestAsync()
        {
            var stream = _stream ?? throw new InvalidOperationException("Client is not connected.");
            return WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["method"] = "$/cancelRequest",
                ["params"] = new Dictionary<string, object?> { ["id"] = Volatile.Read(ref _nextRequestId) }
            }, _cts.Token);
        }
    }
}
#endif
