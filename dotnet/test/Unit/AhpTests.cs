/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Collections.Concurrent;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using System.Text;
using System.Text.Json.Nodes;
using System.Threading.Channels;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class AhpTests
{
    private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(5);

    [Fact]
    public async Task RoutesOpaqueMessagesAndReassemblesUtf8Fragments()
    {
        await using var server = new Server();
        await using var client = server.CreateClient();
        await using var first = await client.CreateAhpEndpointAsync();
        await using var second = await client.CreateAhpEndpointAsync();
        var create1 = await server.NextAsync("ahp.createEndpoint");
        var create2 = await server.NextAsync("ahp.createEndpoint");
        Assert.NotEqual(create1["params"]!["endpointId"]!.GetValue<string>(), create2["params"]!["endpointId"]!.GetValue<string>());
        var firstTransport = new Transport();
        var secondTransport = new Transport();
        await using var connection1 = first.AcceptConnection(firstTransport);
        await using var connection2 = second.AcceptConnection(secondTransport);
        var open1 = await server.NextAsync("ahp.openConnection");
        var open2 = await server.NextAsync("ahp.openConnection");
        Assert.True(Guid.TryParse(open1["params"]!["connectionId"]!.GetValue<string>(), out _));
        await connection1.ReceiveAsync("opaque text: not SDK-parsed JSON");
        Assert.Equal("opaque text: not SDK-parsed JSON", (await server.NextAsync("ahp.receive"))["params"]!["message"]!.GetValue<string>());
        var text = "{\"text\":\"Bert 🐧\"}";
        var bytes = Encoding.UTF8.GetBytes(text);
        var split = Array.IndexOf(bytes, (byte)0xF0) + 2;
        await connection2.ReceiveChunkAsync(bytes.AsMemory(0, split), false);
        Assert.Throws<InvalidOperationException>(() => { _ = connection2.ReceiveAsync("interleaved"); });
        await connection2.ReceiveChunkAsync(bytes.AsMemory(split), true);
        Assert.Equal(text, (await server.NextAsync("ahp.receive"))["params"]!["message"]!.GetValue<string>());
        var firstReply = await server.CallbackAsync(open1, "first");
        var secondReply = await server.CallbackAsync(open2, "second");
        Assert.Empty(Assert.IsType<JsonObject>(firstReply["result"]));
        Assert.Empty(Assert.IsType<JsonObject>(secondReply["result"]));
        Assert.Equal("first", Assert.Single(firstTransport.Messages));
        Assert.Equal("second", Assert.Single(secondTransport.Messages));
        await server.CloseAsync(open1, "runtime failure");
        Assert.Equal("runtime failure", (await Assert.ThrowsAsync<IOException>(() => connection1.Closed.WaitAsync(Timeout))).Message);
        Assert.False(connection2.Closed.IsCompleted);
    }

    [Fact]
    public async Task EarlyCloseSendsFreshCompensationAfterLateOpenAcknowledgement()
    {
        await using var server = new Server("ahp.openConnection");
        await using var client = server.CreateClient();
        await using var endpoint = await client.CreateAhpEndpointAsync();
        var connection = endpoint.AcceptConnection(new Transport());
        var open = await server.NextAsync("ahp.openConnection");
        await connection.EndAsync().WaitAsync(Timeout);
        await server.NextAsync("ahp.closeConnection");
        await server.ReplyAsync(open);
        var compensation = await server.NextAsync("ahp.closeConnection");
        Assert.Equal(open["params"]!["connectionId"]!.GetValue<string>(), compensation["params"]!["connectionId"]!.GetValue<string>());
        await connection.EndAsync();
        Assert.True(connection.Closed.IsCompletedSuccessfully);
    }

    [Fact]
    public async Task CanceledCreationCompensatesLateAcknowledgement()
    {
        await using var server = new Server("ahp.createEndpoint");
        await using var client = server.CreateClient();
        using var cancellation = new CancellationTokenSource();
        var creation = client.CreateAhpEndpointAsync(cancellation.Token);
        var request = await server.NextAsync("ahp.createEndpoint");
        cancellation.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => creation.WaitAsync(Timeout));
        AssertRegistryEmpty(client);
        await server.NextAsync("ahp.disposeEndpoint");
        await server.ReplyAsync(request);
        var disposal = await server.NextAsync("ahp.disposeEndpoint");
        Assert.Equal(request["params"]!["endpointId"]!.GetValue<string>(), disposal["params"]!["endpointId"]!.GetValue<string>());
    }

    [Fact]
    public async Task RpcLossCancelsInitialCreationAndActiveConnections()
    {
        await using (var server = new Server("ahp.createEndpoint"))
        await using (var client = server.CreateClient())
        {
            var creation = client.CreateAhpEndpointAsync();
            await server.NextAsync("ahp.createEndpoint");
            server.Disconnect();
            await Assert.ThrowsAnyAsync<Exception>(() => creation.WaitAsync(Timeout));
            AssertRegistryEmpty(client);
        }
        await using (var server = new Server())
        await using (var client = server.CreateClient())
        {
            var endpoint = await client.CreateAhpEndpointAsync();
            var connection = endpoint.AcceptConnection(new Transport());
            await server.NextAsync("ahp.openConnection");
            server.Disconnect();
            await Assert.ThrowsAnyAsync<IOException>(() => connection.Closed.WaitAsync(Timeout));
            using var deadline = new CancellationTokenSource(Timeout);
            while (RegistryCount(client) != 0) await Task.Delay(10, deadline.Token);
            AssertRegistryEmpty(client);
            Assert.Throws<ObjectDisposedException>(() => endpoint.AcceptConnection(new Transport()));
        }
    }

    [Fact]
    public async Task DisposalReleasesRegistriesDespiteBlockedSendAndCloseCallbacks()
    {
        await using var server = new Server();
        await using var client = server.CreateClient();
        var endpoint = await client.CreateAhpEndpointAsync();
        var entered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var blocked = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var transport = new Transport
        {
            Send = (_, _) => { entered.TrySetResult(); return blocked.Task; },
            Close = _ => blocked.Task
        };
        var connection = endpoint.AcceptConnection(transport);
        var open = await server.NextAsync("ahp.openConnection");
        var callback = server.CallbackAsync(open, "blocked");
        await entered.Task.WaitAsync(Timeout);
        await endpoint.DisposeAsync().AsTask().WaitAsync(Timeout);
        AssertRegistryEmpty(client);
        Assert.Null(typeof(AhpConnection).GetField("_transport", BindingFlags.NonPublic | BindingFlags.Instance)!.GetValue(connection));
        await connection.Closed.WaitAsync(Timeout);
        var response = await callback.WaitAsync(Timeout);
        Assert.NotNull(response["error"]);
        blocked.TrySetResult();
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task IncomingQueueEnforcesByteAndMessageBounds(bool byteLimit)
    {
        await using var server = new Server("ahp.openConnection");
        await using var client = server.CreateClient();
        await using var endpoint = await client.CreateAhpEndpointAsync();
        var connection = endpoint.AcceptConnection(new Transport());
        var open = await server.NextAsync("ahp.openConnection");
        var pending = new List<Task>();
        if (byteLimit) pending.Add(connection.ReceiveAsync(new string('x', 8 * 1024 * 1024)));
        else for (var i = 0; i < 64; i++) pending.Add(connection.ReceiveAsync("{}"));
        await Assert.ThrowsAsync<IOException>(() => connection.ReceiveAsync("x"));
        await Assert.ThrowsAsync<IOException>(() => connection.Closed);
        foreach (var work in pending) await Assert.ThrowsAnyAsync<Exception>(() => work.WaitAsync(Timeout));
        await server.ReplyAsync(open);
    }

    [Fact]
    public async Task InvalidUtf8AndOversizedFragmentsCloseConnection()
    {
        await using var server = new Server();
        await using var client = server.CreateClient();
        await using var endpoint = await client.CreateAhpEndpointAsync();
        var malformed = endpoint.AcceptConnection(new Transport());
        await Assert.ThrowsAsync<DecoderFallbackException>(() => malformed.ReceiveChunkAsync(new byte[] { 0xF0, 0x9F }, true));
        await Assert.ThrowsAsync<DecoderFallbackException>(() => malformed.Closed);
        var oversized = endpoint.AcceptConnection(new Transport());
        await oversized.ReceiveChunkAsync(new byte[8 * 1024 * 1024], false);
        await Assert.ThrowsAsync<IOException>(() => oversized.ReceiveChunkAsync(new byte[1], false));
        await Assert.ThrowsAsync<IOException>(() => oversized.Closed);
    }

    [Fact]
    public async Task OutgoingQueueOverflowCancelsBlockedCallbacks()
    {
        await using var server = new Server();
        await using var client = server.CreateClient();
        await using var endpoint = await client.CreateAhpEndpointAsync();
        var entered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var blocked = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var connection = endpoint.AcceptConnection(new Transport
        {
            Send = (_, _) => { entered.TrySetResult(); return blocked.Task; }
        });
        var open = await server.NextAsync("ahp.openConnection");
        var callbacks = new List<Task<JsonObject>> { server.CallbackAsync(open, "blocked") };
        await entered.Task.WaitAsync(Timeout);
        for (var i = 0; i < 64; i++) callbacks.Add(server.CallbackAsync(open, "{}"));
        await Assert.ThrowsAsync<IOException>(() => connection.Closed.WaitAsync(Timeout));
        var results = await Task.WhenAll(callbacks).WaitAsync(Timeout);
        Assert.All(results, response => Assert.NotNull(response["error"]));
        blocked.TrySetResult();
    }

    [Fact]
    public async Task UserCancellationHandlersCannotBlockLocalDisposal()
    {
        await using var server = new Server();
        await using var client = server.CreateClient();
        var endpoint = await client.CreateAhpEndpointAsync();
        var entered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var blocked = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var releaseCancellation = new ManualResetEventSlim();
        var connection = endpoint.AcceptConnection(new Transport
        {
            Send = (_, token) =>
            {
                token.Register(() => releaseCancellation.Wait());
                entered.TrySetResult();
                return blocked.Task;
            }
        });
        var open = await server.NextAsync("ahp.openConnection");
        var callback = server.CallbackAsync(open, "blocked");
        try
        {
            await entered.Task.WaitAsync(Timeout);
            await endpoint.DisposeAsync().AsTask().WaitAsync(Timeout);
            await connection.Closed.WaitAsync(Timeout);
            AssertRegistryEmpty(client);
            Assert.NotNull((await callback.WaitAsync(Timeout))["error"]);
        }
        finally
        {
            releaseCancellation.Set();
            blocked.TrySetResult();
        }
    }

    [Fact]
    public async Task AdmissionDeadlineClosesUnacknowledgedOpen()
    {
        await using var server = new Server("ahp.openConnection");
        await using var client = server.CreateClient();
        await using var endpoint = await client.CreateAhpEndpointAsync();
        var connection = endpoint.AcceptConnection(new Transport());
        var open = await server.NextAsync("ahp.openConnection");
        await Assert.ThrowsAsync<TimeoutException>(() => connection.Closed.WaitAsync(TimeSpan.FromSeconds(15)));
        Assert.Null(typeof(AhpConnection).GetField("_transport", BindingFlags.NonPublic | BindingFlags.Instance)!.GetValue(connection));
        await server.NextAsync("ahp.closeConnection");
        await server.ReplyAsync(open);
        await server.NextAsync("ahp.closeConnection");
    }

    [Fact]
    public async Task ReceivesAndSendsAreSerializedIndependently()
    {
        await using var server = new Server("ahp.receive");
        await using var client = server.CreateClient();
        await using var endpoint = await client.CreateAhpEndpointAsync();
        var sendEntered = Channel.CreateUnbounded<string>();
        var sendGate = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var transport = new Transport
        {
            Send = (message, _) => { sendEntered.Writer.TryWrite(message); return message == "one" ? sendGate.Task : Task.CompletedTask; }
        };
        var connection = endpoint.AcceptConnection(transport);
        var open = await server.NextAsync("ahp.openConnection");
        var firstReceive = connection.ReceiveAsync("one");
        var request1 = await server.NextAsync("ahp.receive");
        var secondReceive = connection.ReceiveAsync("two");
        var firstSend = server.CallbackAsync(open, "one");
        Assert.Equal("one", await sendEntered.Reader.ReadAsync().AsTask().WaitAsync(Timeout));
        var secondSend = server.CallbackAsync(open, "two");
        await server.ReplyAsync(request1);
        await firstReceive;
        var request2 = await server.NextAsync("ahp.receive");
        Assert.Equal("two", request2["params"]!["message"]!.GetValue<string>());
        Assert.False(sendEntered.Reader.TryRead(out _));
        await server.ReplyAsync(request2);
        await secondReceive;
        sendGate.TrySetResult();
        await Task.WhenAll(firstSend, secondSend).WaitAsync(Timeout);
        Assert.Equal("two", await sendEntered.Reader.ReadAsync().AsTask().WaitAsync(Timeout));
        await connection.EndAsync();
    }

    private static void AssertRegistryEmpty(CopilotClient client)
        => Assert.Equal(0, RegistryCount(client));

    private static int RegistryCount(CopilotClient client)
    {
        var registry = typeof(CopilotClient).GetField("_ahpEndpoints", BindingFlags.Instance | BindingFlags.NonPublic)!.GetValue(client)!;
        return (int)registry.GetType().GetProperty("Count")!.GetValue(registry)!;
    }

    private sealed class Transport : IAhpTransport
    {
        public ConcurrentQueue<string> Messages { get; } = new();
        public Func<string, CancellationToken, Task>? Send { get; init; }
        public Func<Exception?, Task>? Close { get; init; }
        public Task SendAsync(string message, CancellationToken cancellationToken)
        {
            Messages.Enqueue(message);
            return Send?.Invoke(message, cancellationToken) ?? Task.CompletedTask;
        }
        public Task CloseAsync(Exception? error = null) => Close?.Invoke(error) ?? Task.CompletedTask;
    }

    private sealed class Server : IAsyncDisposable
    {
        private readonly TcpListener _listener = new(IPAddress.Loopback, 0);
        private readonly CancellationTokenSource _lifetime = new();
        private readonly SemaphoreSlim _write = new(1, 1);
        private readonly Channel<JsonObject> _requests = Channel.CreateUnbounded<JsonObject>();
        private readonly ConcurrentDictionary<int, TaskCompletionSource<JsonObject>> _callbacks = new();
        private readonly HashSet<string> _held;
        private readonly Task _reader;
        private NetworkStream? _stream;
        private int _nextCallback;

        public Server(params string[] held)
        {
            _held = new(held);
            _listener.Start();
            _reader = ReadAsync();
        }

        public CopilotClient CreateClient() => new(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri($"127.0.0.1:{((IPEndPoint)_listener.LocalEndpoint).Port}")
        });

        public async Task<JsonObject> NextAsync(string method)
        {
            using var timeout = new CancellationTokenSource(Timeout);
            while (await _requests.Reader.WaitToReadAsync(timeout.Token))
            {
                var request = await _requests.Reader.ReadAsync(timeout.Token);
                if (request["method"]!.GetValue<string>() == method) return request;
            }
            throw new IOException("Server closed");
        }

        public Task ReplyAsync(JsonObject request) => WriteAsync(new JsonObject
        {
            ["jsonrpc"] = "2.0",
            ["id"] = request["id"]!.DeepClone(),
            ["result"] = request["method"]!.GetValue<string>() == "connect"
                ? new JsonObject { ["ok"] = true, ["protocolVersion"] = 3, ["version"] = "test" }
                : new JsonObject()
        });

        public async Task<JsonObject> CallbackAsync(JsonObject open, string message)
        {
            var id = Interlocked.Increment(ref _nextCallback);
            var completion = new TaskCompletionSource<JsonObject>(TaskCreationOptions.RunContinuationsAsynchronously);
            _callbacks[id] = completion;
            var parameters = (JsonObject)open["params"]!.DeepClone();
            parameters["message"] = message;
            await WriteAsync(new JsonObject { ["jsonrpc"] = "2.0", ["id"] = id, ["method"] = "ahpTransport.send", ["params"] = parameters });
            return await completion.Task.WaitAsync(Timeout);
        }

        public Task CloseAsync(JsonObject open, string? error)
        {
            var parameters = (JsonObject)open["params"]!.DeepClone();
            if (error is not null) parameters["error"] = error;
            return WriteAsync(new JsonObject { ["jsonrpc"] = "2.0", ["method"] = "ahpTransport.closed", ["params"] = parameters });
        }

        private async Task WriteAsync(JsonObject message)
        {
            var bytes = Encoding.UTF8.GetBytes(message.ToJsonString());
            await _write.WaitAsync(_lifetime.Token);
            try
            {
                await _stream!.WriteAsync(Encoding.ASCII.GetBytes($"Content-Length: {bytes.Length}\r\n\r\n"), _lifetime.Token);
                await _stream.WriteAsync(bytes, _lifetime.Token);
            }
            finally { _write.Release(); }
        }

        private async Task ReadAsync()
        {
            try
            {
                using var socket = await _listener.AcceptTcpClientAsync(_lifetime.Token);
                _stream = socket.GetStream();
                var one = new byte[1];
                while (!_lifetime.IsCancellationRequested)
                {
                    var header = new StringBuilder();
                    while (!header.ToString().EndsWith("\r\n\r\n", StringComparison.Ordinal))
                    {
                        await _stream.ReadExactlyAsync(one, _lifetime.Token);
                        header.Append((char)one[0]);
                    }
                    var length = int.Parse(header.ToString().Split(':')[1].Trim(), System.Globalization.CultureInfo.InvariantCulture);
                    var body = new byte[length];
                    await _stream.ReadExactlyAsync(body, _lifetime.Token);
                    var message = JsonNode.Parse(body)!.AsObject();
                    if (message["method"] is { } method)
                    {
                        if (message["id"] is null) continue;
                        await _requests.Writer.WriteAsync(message, _lifetime.Token);
                        if (!_held.Contains(method.GetValue<string>())) await ReplyAsync(message);
                    }
                    else if (message["id"] is { } id && _callbacks.TryRemove(id.GetValue<int>(), out var completion))
                    {
                        completion.TrySetResult(message);
                    }
                }
            }
            catch (Exception error) when (error is OperationCanceledException or IOException or ObjectDisposedException or SocketException) { }
        }

        public void Disconnect() => _stream?.Dispose();

        public async ValueTask DisposeAsync()
        {
            _lifetime.Cancel();
            Disconnect();
            _listener.Stop();
            await _reader.WaitAsync(Timeout);
            _lifetime.Dispose();
            _write.Dispose();
        }
    }
}
#endif
