/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Net;
using System.Net.Sockets;
using System.Diagnostics;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Text;
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class ClientSessionLifetimeTests
{
    [Fact]
    public async Task StopAsync_Requests_Runtime_Shutdown_For_Owned_Process()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(client, process);

        await client.StopAsync();

        Assert.Equal(1, server.RuntimeShutdownCount);
    }

    [Fact]
    public async Task StopAsync_Does_Not_Throw_When_Runtime_Shutdown_Fails()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.FailRuntimeShutdown();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(client, process);

        await client.StopAsync();

        Assert.Equal(1, server.RuntimeShutdownCount);
    }

    [Fact]
    public async Task ForceStopAsync_And_External_Stop_Do_Not_Request_Runtime_Shutdown()
    {
        await using var forceServer = await FakeCopilotServer.StartAsync();
        await using var forceClient = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(forceServer.Url) });
        await forceClient.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(forceClient, process);

        await forceClient.ForceStopAsync();

        Assert.Equal(0, forceServer.RuntimeShutdownCount);

        await using var externalServer = await FakeCopilotServer.StartAsync();
        await using var externalClient = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(externalServer.Url) });
        await externalClient.StartAsync();

        await externalClient.StopAsync();

        Assert.Equal(0, externalServer.RuntimeShutdownCount);
    }

    [Fact]
    public async Task Dropped_Session_Remains_Rooted_By_Client()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var weakSession = await CreateDroppedSessionAsync(client);

        ForceCollect();

        Assert.True(
            weakSession.TryGetTarget(out _),
            "CopilotClient should root created sessions until they are explicitly disposed or the client stops.");
        AssertSessionCount(client, sessions: 1);
        GC.KeepAlive(client);
    }

    [Fact]
    public async Task Disposed_Session_Is_Removed_From_Client()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        await session.DisposeAsync();

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task Disposing_Session_Remains_Rooted_Until_Destroy_Completes()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.DelayDestroy();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        var disposeTask = session.DisposeAsync().AsTask();
        await server.DestroyStarted;

        AssertSessionCount(client, sessions: 1);

        server.CompleteDestroy();
        await disposeTask;

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task StopAsync_Removes_Rooted_Sessions()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        _ = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        await client.StopAsync();

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task StopAsync_Keeps_Session_Rooted_Until_Destroy_Completes()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.DelayDestroy();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        _ = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        var stopTask = client.StopAsync();
        await server.DestroyStarted;

        AssertSessionCount(client, sessions: 1);

        server.CompleteDestroy();
        await stopTask;

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task ResumeSessionAsync_Throws_When_Same_Client_Already_Tracks_Session()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var sessionId = "same-session-id";
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() => client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        }));
        Assert.Contains(sessionId, exception.Message);
        AssertSessionCount(client, sessions: 1);
    }

    [Fact]
    public async Task Generated_Session_Rpc_Throws_When_Session_Disposed()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        await session.DisposeAsync();

        await Assert.ThrowsAsync<ObjectDisposedException>(() => session.Rpc.Model.GetCurrentAsync());
    }

    [MethodImpl(MethodImplOptions.NoInlining)]
    private static async Task<WeakReference<CopilotSession>> CreateDroppedSessionAsync(CopilotClient client)
    {
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        return new WeakReference<CopilotSession>(session);
    }

    private static void ForceCollect()
    {
        GC.Collect();
        GC.WaitForPendingFinalizers();
        GC.Collect();
    }

    private static void AssertSessionCount(CopilotClient client, int sessions)
    {
        Assert.Equal(sessions, GetPrivateDictionaryCount(client, "_sessions"));
    }

    private static int GetPrivateDictionaryCount(CopilotClient client, string fieldName)
    {
        var field = typeof(CopilotClient).GetField(fieldName, BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException($"Field '{fieldName}' was not found.");
        var dictionary = field.GetValue(client)
            ?? throw new InvalidOperationException($"Field '{fieldName}' was null.");
        var count = dictionary.GetType().GetProperty("Count")
            ?? throw new InvalidOperationException($"Field '{fieldName}' does not expose Count.");

        return (int)count.GetValue(dictionary)!;
    }

    private static async Task ReplaceConnectionCliProcessAsync(CopilotClient client, Process process)
    {
        var field = typeof(CopilotClient).GetField("_connectionTask", BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("_connectionTask field was not found.");
        var connectionTask = (Task)field.GetValue(client)!;
        await connectionTask;

        var resultProperty = connectionTask.GetType().GetProperty(nameof(Task<object>.Result))
            ?? throw new InvalidOperationException("Connection task result property was not found.");
        var connection = resultProperty.GetValue(connectionTask)!;
        var connectionType = connection.GetType();
        var rpc = connectionType.GetProperty("Rpc")!.GetValue(connection);
        var networkStream = connectionType.GetProperty("NetworkStream")!.GetValue(connection);
        var constructor = connectionType.GetConstructors(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public).Single();
        var updatedConnection = constructor.Invoke([rpc, process, networkStream, null]);
        var fromResult = typeof(Task).GetMethod(nameof(Task.FromResult))!.MakeGenericMethod(connectionType);
        field.SetValue(client, fromResult.Invoke(null, [updatedConnection]));
    }

    private static Process StartExitedProcess()
    {
        var startInfo = OperatingSystem.IsWindows()
            ? new ProcessStartInfo(Environment.GetEnvironmentVariable("COMSPEC") ?? "cmd.exe", "/c exit 0")
            : new ProcessStartInfo("/bin/sh", "-c \"exit 0\"");
        startInfo.UseShellExecute = false;
        var process = Process.Start(startInfo)
            ?? throw new InvalidOperationException("Failed to start test process.");
        process.WaitForExit();
        return process;
    }

    private sealed class FakeCopilotServer : IAsyncDisposable
    {
        private readonly TcpListener _listener;
        private readonly CancellationTokenSource _cts = new();
        private readonly SemaphoreSlim _writeLock = new(1, 1);
        private readonly TaskCompletionSource _destroyStarted = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _allowDestroy = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly Task _serverTask;
        private string? _lastSessionId;
        private bool _delayDestroy;
        private bool _failRuntimeShutdown;

        private FakeCopilotServer(TcpListener listener)
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

        public static Task<FakeCopilotServer> StartAsync()
        {
            var listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            return Task.FromResult(new FakeCopilotServer(listener));
        }

        public Task DestroyStarted => _destroyStarted.Task;

        public int RuntimeShutdownCount { get; private set; }

        public void DelayDestroy()
        {
            _delayDestroy = true;
        }

        public void CompleteDestroy()
        {
            _allowDestroy.TrySetResult();
        }

        public void FailRuntimeShutdown()
        {
            _failRuntimeShutdown = true;
        }

        public async ValueTask DisposeAsync()
        {
            _allowDestroy.TrySetResult();
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

            while (!_cts.Token.IsCancellationRequested)
            {
                using var request = await ReadMessageAsync(stream, _cts.Token);
                if (request is null)
                {
                    return;
                }

                await HandleRequestAsync(stream, request.RootElement, _cts.Token);
            }
        }

        private async Task HandleRequestAsync(Stream stream, JsonElement request, CancellationToken cancellationToken)
        {
            if (!request.TryGetProperty("id", out var idElement))
            {
                return;
            }

            var id = idElement.Clone();
            var method = request.GetProperty("method").GetString();
            if (method == "runtime.shutdown" && _failRuntimeShutdown)
            {
                RuntimeShutdownCount++;
                await WriteMessageAsync(stream, new Dictionary<string, object?>
                {
                    ["jsonrpc"] = "2.0",
                    ["id"] = id,
                    ["error"] = new Dictionary<string, object?>
                    {
                        ["code"] = -32000,
                        ["message"] = "runtime shutdown failed"
                    }
                }, cancellationToken);
                return;
            }

            object? result = method switch
            {
                "connect" => new Dictionary<string, object?>
                {
                    ["ok"] = true,
                    ["protocolVersion"] = 3,
                    ["version"] = "test"
                },
                "session.create" => CreateSessionResult(request),
                "session.resume" => CreateSessionResult(request),
                "session.send" => new Dictionary<string, object?>
                {
                    ["messageId"] = "message-1"
                },
                "session.delete" => new Dictionary<string, object?>
                {
                    ["success"] = true
                },
                "session.destroy" => await DestroySessionAsync(cancellationToken),
                "runtime.shutdown" => HandleRuntimeShutdown(),
                _ => throw new InvalidOperationException($"Unexpected RPC method '{method}'.")
            };

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["result"] = result
            }, cancellationToken);
        }

        private Dictionary<string, object?> CreateSessionResult(JsonElement request)
        {
            string? sessionId = null;
            if (request.TryGetProperty("params", out var paramsProp)
                && paramsProp.ValueKind == JsonValueKind.Object
                && paramsProp.TryGetProperty("sessionId", out var sidProp)
                && sidProp.ValueKind == JsonValueKind.String)
            {
                sessionId = sidProp.GetString();
            }
            if (string.IsNullOrEmpty(sessionId))
            {
                sessionId = Guid.NewGuid().ToString();
            }
            _lastSessionId = sessionId;

            return new Dictionary<string, object?>
            {
                ["sessionId"] = _lastSessionId,
                ["workspacePath"] = null,
                ["capabilities"] = null
            };
        }

        private async Task<Dictionary<string, object?>> DestroySessionAsync(CancellationToken cancellationToken)
        {
            if (_delayDestroy)
            {
                _destroyStarted.TrySetResult();
                await _allowDestroy.Task.WaitAsync(cancellationToken);
            }

            return [];
        }

        private Dictionary<string, object?> HandleRuntimeShutdown()
        {
            RuntimeShutdownCount++;
            return [];
        }

        private async Task WriteMessageAsync(Stream stream, object payload, CancellationToken cancellationToken)
        {
            using var bodyStream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(bodyStream))
            {
                WriteJsonValue(writer, payload);
            }

            var body = bodyStream.ToArray();
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

        private static void WriteJsonValue(Utf8JsonWriter writer, object? value)
        {
            switch (value)
            {
                case null:
                    writer.WriteNullValue();
                    break;

                case string stringValue:
                    writer.WriteStringValue(stringValue);
                    break;

                case bool boolValue:
                    writer.WriteBooleanValue(boolValue);
                    break;

                case int intValue:
                    writer.WriteNumberValue(intValue);
                    break;

                case long longValue:
                    writer.WriteNumberValue(longValue);
                    break;

                case JsonElement jsonElement:
                    jsonElement.WriteTo(writer);
                    break;

                case Dictionary<string, object?> dictionary:
                    writer.WriteStartObject();
                    foreach (var (propertyName, propertyValue) in dictionary)
                    {
                        writer.WritePropertyName(propertyName);
                        WriteJsonValue(writer, propertyValue);
                    }
                    writer.WriteEndObject();
                    break;

                case object?[] array:
                    writer.WriteStartArray();
                    foreach (var item in array)
                    {
                        WriteJsonValue(writer, item);
                    }
                    writer.WriteEndArray();
                    break;

                default:
                    throw new InvalidOperationException($"Unexpected JSON value type '{value.GetType().Name}'.");
            }
        }

        private static async Task<JsonDocument?> ReadMessageAsync(Stream stream, CancellationToken cancellationToken)
        {
            var headerBytes = new List<byte>();
            while (true)
            {
                var value = await ReadByteAsync(stream, cancellationToken);
                if (value < 0)
                {
                    return null;
                }

                headerBytes.Add((byte)value);
                var count = headerBytes.Count;
                if (count >= 4 &&
                    headerBytes[count - 4] == '\r' &&
                    headerBytes[count - 3] == '\n' &&
                    headerBytes[count - 2] == '\r' &&
                    headerBytes[count - 1] == '\n')
                {
                    break;
                }
            }

            var header = Encoding.ASCII.GetString([.. headerBytes]);
            var contentLength = header
                .Split(["\r\n"], StringSplitOptions.RemoveEmptyEntries)
                .Select(line => line.Split(':', 2))
                .Where(parts => parts.Length == 2 && parts[0].Equals("Content-Length", StringComparison.OrdinalIgnoreCase))
                .Select(parts => int.Parse(parts[1].Trim(), System.Globalization.CultureInfo.InvariantCulture))
                .Single();

            var body = new byte[contentLength];
            var offset = 0;
            while (offset < body.Length)
            {
                var read = await stream.ReadAsync(body.AsMemory(offset, body.Length - offset), cancellationToken);
                if (read == 0)
                {
                    return null;
                }

                offset += read;
            }

            return JsonDocument.Parse(body);
        }

        private static async Task<int> ReadByteAsync(Stream stream, CancellationToken cancellationToken)
        {
            var buffer = new byte[1];
            var read = await stream.ReadAsync(buffer, cancellationToken);
            return read == 0 ? -1 : buffer[0];
        }
    }
}
#endif
