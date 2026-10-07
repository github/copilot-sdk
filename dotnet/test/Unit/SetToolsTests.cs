/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
#pragma warning disable GHCP001 // Live tool replacement is intentionally experimental.

using Microsoft.Extensions.AI;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization.Metadata;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class SetToolsTests
{
    [Fact]
    public async Task SetToolsAsync_Sends_Complete_Payload_And_Empty_Descriptions()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        await session.SetToolsAsync([
            CopilotTool.DefineTool(
                (int code) => $"fruit:{code}",
                new CopilotToolOptions
                {
                    OverridesBuiltInTool = true,
                    SkipPermission = true,
                    IsTerminal = true,
                    Defer = CopilotToolDefer.Never,
                    Metadata = new Dictionary<string, JsonNode?> { ["owner"] = "test" },
                },
                new AIFunctionFactoryOptions
                {
                    Name = "lookup_fruit",
                    Description = "Looks up the fruit for a numeric code",
                }),
            AIFunctionFactory.Create(() => "empty", new AIFunctionFactoryOptions
            {
                Name = "empty_description",
                Description = string.Empty,
            }),
        ]);

        var request = Assert.Single(server.Requests, request => request.Method == "session.tools.set");
        Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
        var tools = request.Params.GetProperty("tools").EnumerateArray().ToList();
        Assert.Collection(
            tools,
            fruit =>
            {
                Assert.Equal("lookup_fruit", fruit.GetProperty("name").GetString());
                Assert.Equal("Looks up the fruit for a numeric code", fruit.GetProperty("description").GetString());
                Assert.Equal("object", fruit.GetProperty("parameters").GetProperty("type").GetString());
                Assert.True(fruit.GetProperty("parameters").GetProperty("properties").TryGetProperty("code", out _));
                Assert.True(fruit.GetProperty("overridesBuiltInTool").GetBoolean());
                Assert.True(fruit.GetProperty("skipPermission").GetBoolean());
                Assert.True(fruit.GetProperty("isTerminal").GetBoolean());
                Assert.Equal("never", fruit.GetProperty("defer").GetString());
                Assert.Equal("test", fruit.GetProperty("metadata").GetProperty("owner").GetString());
            },
            empty =>
            {
                Assert.Equal("empty_description", empty.GetProperty("name").GetString());
                Assert.Equal(string.Empty, empty.GetProperty("description").GetString());
                Assert.False(empty.TryGetProperty("overridesBuiltInTool", out _));
                Assert.False(empty.TryGetProperty("skipPermission", out _));
                Assert.False(empty.TryGetProperty("isTerminal", out _));
                Assert.False(empty.TryGetProperty("defer", out _));
                Assert.False(empty.TryGetProperty("metadata", out _));
            });
    }

    [Fact]
    public async Task SetToolsAsync_Switches_Handlers_After_Acceptance()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        var releaseSet = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeSetToolsResponseAsync = _ => releaseSet.Task;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [Tool("replaceable_tool", "old")],
        });
        server.ClearRequests();

        var replacement = session.SetToolsAsync([Tool("replaceable_tool", "new")]);
        await server.WaitForRequestAsync("session.tools.set");

        var oldResult = await InvokeToolAsync(server, session, "replaceable_tool", "old-request");
        Assert.Equal("old", oldResult);

        releaseSet.SetResult();
        await replacement;

        var newResult = await InvokeToolAsync(server, session, "replaceable_tool", "new-request");
        Assert.Equal("new", newResult);
    }

    [Fact]
    public async Task SetToolsAsync_Rejection_Leaves_Handlers_Unchanged()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        server.RejectNextSetTools("invalid tool name");
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [Tool("stable_tool", "old")],
        });

        await Assert.ThrowsAsync<IOException>(() => session.SetToolsAsync([Tool("stable_tool", "new")]));

        var result = await InvokeToolAsync(server, session, "stable_tool", "stable-request");
        Assert.Equal("old", result);
    }

    [Fact]
    public async Task SetToolsAsync_Empty_Set_Removes_Handlers()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [Tool("removed_tool", "old")],
        });

        await session.SetToolsAsync([]);

        var setRequest = server.Requests.Last(request => request.Method == "session.tools.set");
        Assert.Empty(setRequest.Params.GetProperty("tools").EnumerateArray());
        Assert.Null(GetTool(session, "removed_tool"));
    }

    [Fact]
    public async Task SetToolsAsync_Serializes_Concurrent_Replacements_And_Allows_Later_After_Rejection()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        var releaseFirst = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var setCall = 0;
        server.BeforeSetToolsResponseAsync = _ => Interlocked.Increment(ref setCall) == 1 ? releaseFirst.Task : Task.CompletedTask;
        server.RejectSetToolsCall(1, "first failed");
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [Tool("ordered_tool", "initial")],
        });
        server.ClearRequests();

        var first = session.SetToolsAsync([Tool("ordered_tool", "first")]);
        await server.WaitForRequestAsync("session.tools.set");
        var second = session.SetToolsAsync([Tool("ordered_tool", "second")]);
        await Task.Delay(100);
        Assert.Single(server.Requests, request => request.Method == "session.tools.set");

        releaseFirst.SetResult();
        await Assert.ThrowsAsync<IOException>(() => first);
        await second;

        var setRequests = server.Requests.Where(request => request.Method == "session.tools.set").ToList();
        Assert.Equal(2, setRequests.Count);
        Assert.Equal("first failed", server.RejectedMessages.Single());
        var result = await InvokeToolAsync(server, session, "ordered_tool", "ordered-request");
        Assert.Equal("second", result);
    }

    [Fact]
    public async Task SetToolsAsync_Cancellation_After_Request_Was_Sent_Still_Installs_Accepted_Replacement()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        var releaseSet = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeSetToolsResponseAsync = _ => releaseSet.Task;
        using var acceptanceContext = new DeferredSynchronizationContext();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(() =>
            {
                acceptanceContext.Resume();
                return "old";
            }, new AIFunctionFactoryOptions { Name = "cancel_tool", Description = "Returns old" })],
        });

        try
        {
            using var cts = new CancellationTokenSource();
            var replacement = acceptanceContext.Run(() => session.SetToolsAsync([Tool("cancel_tool", "new")], cts.Token));
            await server.WaitForRequestAsync("session.tools.set");
            await cts.CancelAsync();
            await acceptanceContext.RunNextAsync();
            await Assert.ThrowsAnyAsync<OperationCanceledException>(() => replacement);

            releaseSet.SetResult();
            await acceptanceContext.WaitForPendingAsync();
            using var acceptanceTimeout = new CancellationTokenSource(TimeSpan.FromSeconds(5));
            string? result;
            try
            {
                result = await InvokeToolUntilAsync(server, session, "cancel_tool", "cancel-request", "new", acceptanceTimeout.Token);
            }
            catch (OperationCanceledException ex) when (acceptanceTimeout.IsCancellationRequested)
            {
                throw new TimeoutException(ex.Message, ex);
            }
            Assert.Equal("new", result);
        }
        finally
        {
            releaseSet.TrySetResult();
            acceptanceContext.Resume();
        }
    }

    [Fact]
    public async Task InvokeToolUntilAsync_Rejects_Expected_Result_After_Deadline()
    {
        using var deadline = new CancellationTokenSource();
        await using var server = await SetToolsFakeServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(() =>
            {
                deadline.Cancel();
                return "new";
            }, new AIFunctionFactoryOptions { Name = "late_tool", Description = "Returns after the wait deadline" })],
        });

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            InvokeToolUntilAsync(server, session, "late_tool", "late-request", "new", deadline.Token));

        var response = await server.WaitForRequestAsync("session.tools.handlePendingToolCall", "late-request-0");
        Assert.Equal("new", response.Params.GetProperty("result").GetProperty("textResultForLlm").GetString());
    }

    [Fact]
    public async Task SetToolsAsync_Cancellation_While_Queued_Does_Not_Send_Request()
    {
        await using var server = await SetToolsFakeServer.StartAsync();
        var releaseSet = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeSetToolsResponseAsync = _ => releaseSet.Task;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [Tool("queued_tool", "old")],
        });
        server.ClearRequests();

        var first = session.SetToolsAsync([Tool("queued_tool", "first")]);
        await server.WaitForRequestAsync("session.tools.set");

        using var cts = new CancellationTokenSource();
        var queued = session.SetToolsAsync([Tool("queued_tool", "queued")], cts.Token);
        await cts.CancelAsync();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => queued);

        using var preCanceled = new CancellationTokenSource();
        await preCanceled.CancelAsync();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            session.SetToolsAsync([Tool("queued_tool", "pre-canceled")], preCanceled.Token));

        releaseSet.SetResult();
        await first;

        Assert.Single(server.Requests, request => request.Method == "session.tools.set");
        var result = await InvokeToolAsync(server, session, "queued_tool", "queued-request");
        Assert.Equal("first", result);
    }

    private static AIFunction Tool(string name, string result)
    {
        return AIFunctionFactory.Create(
            () => result,
            new AIFunctionFactoryOptions { Name = name, Description = $"Returns {result}" });
    }

    private static AIFunction? GetTool(CopilotSession session, string name)
    {
        var method = typeof(CopilotSession).GetMethod("GetTool", BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("GetTool method was not found.");
        return (AIFunction?)method.Invoke(session, [name]);
    }

    private static async Task<string?> InvokeToolAsync(
        SetToolsFakeServer server,
        CopilotSession session,
        string toolName,
        string requestId,
        CancellationToken cancellationToken = default)
    {
        await SendToolRequestAsync(server, session, toolName, requestId, cancellationToken);
        var request = await server.WaitForRequestAsync("session.tools.handlePendingToolCall", requestId, cancellationToken);
        return request.Params.GetProperty("result").GetProperty("textResultForLlm").GetString();
    }

    /// <summary>
    /// Invokes <paramref name="toolName"/> until the replacement handler answers it, or a timeout expires.
    /// </summary>
    /// <remarks>
    /// Writing a <c>session.tools.set</c> response happens before the client has read it, completed the RPC,
    /// and published the replacement handlers. Tests that
    /// await a successful <c>SetToolsAsync</c> task complete after that publish and need no polling; the
    /// cancellation test cannot, because the caller's await threw and the replacement task runs detached.
    /// Polling does not weaken the assertion: a replacement that never installs still fails, on the timeout.
    /// </remarks>
    private static async Task<string?> InvokeToolUntilAsync(
        SetToolsFakeServer server,
        CopilotSession session,
        string toolName,
        string requestId,
        string expected,
        CancellationToken cancellationToken)
    {
        var attempt = 0;
        string? result = null;
        try
        {
            while (true)
            {
                // Each invocation needs its own id: the server records one pending call per request id.
                result = await InvokeToolAsync(server, session, toolName, $"{requestId}-{attempt++}", cancellationToken);
                cancellationToken.ThrowIfCancellationRequested();
                if (result == expected)
                {
                    return result;
                }

                Assert.Equal("old", result);
                await Task.Delay(20, cancellationToken);
            }
        }
        catch (OperationCanceledException ex) when (cancellationToken.IsCancellationRequested)
        {
            throw new OperationCanceledException(
                $"Accepted replacement did not install '{expected}'; last tool result was '{result ?? "<no response>"}'.",
                ex,
                cancellationToken);
        }
    }

    private static Task SendToolRequestAsync(
        SetToolsFakeServer server,
        CopilotSession session,
        string toolName,
        string requestId,
        CancellationToken cancellationToken = default)
    {
        using var arguments = JsonDocument.Parse("{}");
        return server.SendSessionEventAsync(session.SessionId, "external_tool.requested", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["requestId"] = requestId,
            ["toolCallId"] = requestId + "-call",
            ["toolName"] = toolName,
            ["arguments"] = arguments.RootElement.Clone(),
        }, cancellationToken);
    }

    private sealed record RpcRequestRecord(string Method, JsonElement Params);

    private sealed class DeferredSynchronizationContext : SynchronizationContext, IDisposable
    {
        private readonly object _gate = new();
        private readonly Queue<(SendOrPostCallback Callback, object? State)> _callbacks = new();
        private readonly SemaphoreSlim _available = new(0);
        private bool _resumed;

        public Task Run(Func<Task> action)
        {
            var previous = Current;
            SetSynchronizationContext(this);
            try
            {
                return action();
            }
            finally
            {
                SetSynchronizationContext(previous);
            }
        }

        public override void Post(SendOrPostCallback callback, object? state)
        {
            lock (_gate)
            {
                if (!_resumed)
                {
                    _callbacks.Enqueue((callback, state));
                    _available.Release();
                    return;
                }
            }
            ThreadPool.QueueUserWorkItem(_ => callback(state));
        }

        public async Task WaitForPendingAsync()
        {
            if (!await _available.WaitAsync(TimeSpan.FromSeconds(5)))
            {
                throw new TimeoutException("The client did not post its tool-replacement continuation.");
            }
        }

        public async Task RunNextAsync()
        {
            await WaitForPendingAsync();
            (SendOrPostCallback Callback, object? State) next;
            lock (_gate)
            {
                next = _callbacks.Dequeue();
            }
            next.Callback(next.State);
        }

        public void Resume()
        {
            (SendOrPostCallback Callback, object? State)[] pending;
            lock (_gate)
            {
                _resumed = true;
                pending = _callbacks.ToArray();
                _callbacks.Clear();
            }
            foreach (var (callback, state) in pending)
            {
                ThreadPool.QueueUserWorkItem(_ => callback(state));
            }
        }

        public void Dispose()
        {
            Resume();
            _available.Dispose();
        }
    }

    private sealed class SetToolsFakeServer : IAsyncDisposable
    {
        private static readonly JsonSerializerOptions s_jsonOptions = new(JsonSerializerDefaults.Web)
        {
            TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
        };
        private readonly TcpListener _listener;
        private readonly CancellationTokenSource _cts = new();
        private readonly SemaphoreSlim _writeLock = new(1, 1);
        private readonly List<RpcRequestRecord> _requests = [];
        private readonly object _requestsLock = new();
        private readonly Task _serverTask;
        private NetworkStream? _stream;
        private string? _nextSetToolsRejection;
        private readonly Dictionary<int, string> _setToolsRejections = [];
        private int _setToolsCalls;

        private SetToolsFakeServer(TcpListener listener)
        {
            _listener = listener;
            _serverTask = RunAsync();
        }

        public string Url
        {
            get
            {
                var endpoint = (IPEndPoint)_listener.LocalEndpoint;
                return $"http://127.0.0.1:{endpoint.Port}";
            }
        }

        public Func<RpcRequestRecord, Task>? BeforeSetToolsResponseAsync { get; set; }

        public List<string> RejectedMessages { get; } = [];

        public IReadOnlyList<RpcRequestRecord> Requests
        {
            get
            {
                lock (_requestsLock)
                {
                    return _requests.ToArray();
                }
            }
        }

        public static Task<SetToolsFakeServer> StartAsync()
        {
            var listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            return Task.FromResult(new SetToolsFakeServer(listener));
        }

        public void ClearRequests()
        {
            lock (_requestsLock)
            {
                _requests.Clear();
            }
        }

        public void RejectNextSetTools(string message)
        {
            _nextSetToolsRejection = message;
        }

        public void RejectSetToolsCall(int callNumber, string message)
        {
            _setToolsRejections[callNumber] = message;
        }

        public async Task<RpcRequestRecord> WaitForRequestAsync(
            string method,
            string? requestId = null,
            CancellationToken cancellationToken = default)
        {
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            timeout.CancelAfter(TimeSpan.FromSeconds(5));
            while (!timeout.IsCancellationRequested)
            {
                cancellationToken.ThrowIfCancellationRequested();
                var request = Requests.FirstOrDefault(request =>
                    request.Method == method &&
                    (requestId is null || request.Params.GetProperty("requestId").GetString() == requestId));
                if (request is not null)
                {
                    return request;
                }

                await Task.Delay(20, cancellationToken);
            }

            cancellationToken.ThrowIfCancellationRequested();
            throw new TimeoutException($"Timed out waiting for RPC method '{method}'.");
        }

        public async Task SendSessionEventAsync(
            string sessionId,
            string type,
            Dictionary<string, object?> data,
            CancellationToken cancellationToken = default)
        {
            using var linkedCancellation = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, _cts.Token);
            var stream = _stream ?? throw new InvalidOperationException("Client is not connected.");
            var evt = new Dictionary<string, object?>
            {
                ["id"] = Guid.NewGuid().ToString(),
                ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                ["parentId"] = null,
                ["type"] = type,
                ["data"] = data,
            };
            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["method"] = "session.event",
                ["params"] = new Dictionary<string, object?>
                {
                    ["sessionId"] = sessionId,
                    ["event"] = evt,
                },
            }, linkedCancellation.Token);
        }

        public async ValueTask DisposeAsync()
        {
            _cts.Cancel();
            _listener.Stop();
            try
            {
                await _serverTask;
            }
            catch (Exception ex) when (ex is OperationCanceledException or ObjectDisposedException or IOException or SocketException)
            {
            }

            _cts.Dispose();
            _writeLock.Dispose();
        }

        private async Task RunAsync()
        {
            using var tcpClient = await _listener.AcceptTcpClientAsync(_cts.Token);
            using var stream = tcpClient.GetStream();
            _stream = stream;

            while (!_cts.Token.IsCancellationRequested)
            {
                using var message = await ReadMessageAsync(stream, _cts.Token);
                if (message is null)
                {
                    return;
                }

                var root = message.RootElement;
                if (!root.TryGetProperty("method", out _) || !root.TryGetProperty("id", out var id))
                {
                    continue;
                }

                _ = HandleRequestAsync(stream, root, id.Clone(), _cts.Token);
            }
        }

        private async Task HandleRequestAsync(Stream stream, JsonElement request, JsonElement id, CancellationToken cancellationToken)
        {
            var method = request.GetProperty("method").GetString()!;
            var parameters = request.TryGetProperty("params", out var rawParams)
                ? rawParams.Clone()
                : JsonDocument.Parse("{}").RootElement.Clone();
            var requestRecord = new RpcRequestRecord(method, parameters);
            lock (_requestsLock)
            {
                _requests.Add(requestRecord);
            }

            if (method == "session.tools.set")
            {
                var callNumber = Interlocked.Increment(ref _setToolsCalls);
                if (BeforeSetToolsResponseAsync is { } beforeResponse)
                {
                    await beforeResponse(requestRecord);
                }

                var rejection = _nextSetToolsRejection;
                _nextSetToolsRejection = null;
                if (rejection is null && _setToolsRejections.TryGetValue(callNumber, out var configuredRejection))
                {
                    rejection = configuredRejection;
                }

                if (rejection is not null)
                {
                    RejectedMessages.Add(rejection);
                    await WriteErrorAsync(stream, id, -32602, rejection, cancellationToken);
                    return;
                }
            }

            var result = method switch
            {
                "connect" => new Dictionary<string, object?>
                {
                    ["ok"] = true,
                    ["protocolVersion"] = 3,
                    ["version"] = "test",
                },
                "session.create" => new Dictionary<string, object?>
                {
                    ["sessionId"] = parameters.TryGetProperty("sessionId", out var sid) && sid.ValueKind == JsonValueKind.String
                        ? sid.GetString()
                        : Guid.NewGuid().ToString(),
                    ["workspacePath"] = null,
                    ["capabilities"] = null,
                },
                "session.detach" => new Dictionary<string, object?> { ["success"] = true },
                "session.tools.set" => new Dictionary<string, object?>(),
                "session.tools.handlePendingToolCall" => new Dictionary<string, object?> { ["success"] = true },
                _ => throw new InvalidOperationException($"Unexpected RPC method '{method}'."),
            };

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["result"] = result,
            }, cancellationToken);
        }

        private Task WriteErrorAsync(Stream stream, JsonElement id, int code, string message, CancellationToken cancellationToken)
        {
            return WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["error"] = new Dictionary<string, object?>
                {
                    ["code"] = code,
                    ["message"] = message,
                },
            }, cancellationToken);
        }

        private async Task WriteMessageAsync(Stream stream, object payload, CancellationToken cancellationToken)
        {
            var body = JsonSerializer.SerializeToUtf8Bytes(payload, s_jsonOptions);
            var header = Encoding.ASCII.GetBytes($"Content-Length: {body.Length}\r\n\r\n");
            await _writeLock.WaitAsync(cancellationToken);
            try
            {
                await stream.WriteAsync(header, cancellationToken);
                await stream.WriteAsync(body, cancellationToken);
                await stream.FlushAsync(cancellationToken);
            }
            finally
            {
                _writeLock.Release();
            }
        }

        private static async Task<JsonDocument?> ReadMessageAsync(Stream stream, CancellationToken cancellationToken)
        {
            var header = new List<byte>();
            var buffer = new byte[1];
            while (true)
            {
                var read = await stream.ReadAsync(buffer, cancellationToken);
                if (read == 0)
                {
                    return null;
                }

                header.Add(buffer[0]);
                if (header.Count >= 4 &&
                    header[^4] == '\r' && header[^3] == '\n' && header[^2] == '\r' && header[^1] == '\n')
                {
                    break;
                }
            }

            var headerText = Encoding.ASCII.GetString(header.ToArray());
            var lengthLine = headerText.Split("\r\n", StringSplitOptions.RemoveEmptyEntries)
                .Single(line => line.StartsWith("Content-Length:", StringComparison.OrdinalIgnoreCase));
            var length = int.Parse(lengthLine["Content-Length:".Length..].Trim(), System.Globalization.CultureInfo.InvariantCulture);
            var body = new byte[length];
            var offset = 0;
            while (offset < length)
            {
                var read = await stream.ReadAsync(body.AsMemory(offset, length - offset), cancellationToken);
                if (read == 0)
                {
                    return null;
                }

                offset += read;
            }

            return JsonDocument.Parse(body);
        }
    }
}

#pragma warning restore GHCP001
#endif
