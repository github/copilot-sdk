/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using Xunit;

using GitHub.Copilot.Rpc;

namespace GitHub.Copilot.Test.Unit;

#pragma warning disable GHCP001 // GitHub telemetry forwarding is experimental.

public sealed class GitHubTelemetryTests
{
    [Fact]
    public async Task CreateSession_Opts_Into_Forwarding_When_Handler_Provided()
    {
        await using var server = await FakeTelemetryServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            OnGitHubTelemetry = _ => Task.CompletedTask,
        });
        await client.StartAsync();

        await client.CreateSessionAsync(new SessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll });

        var createParams = server.LastCreateParams ?? throw new InvalidOperationException("session.create was not captured.");
        Assert.True(createParams.TryGetProperty("enableGitHubTelemetryForwarding", out var flag));
        Assert.True(flag.GetBoolean());
    }

    [Fact]
    public async Task ResumeSession_Opts_Into_Forwarding_When_Handler_Provided()
    {
        await using var server = await FakeTelemetryServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            OnGitHubTelemetry = _ => Task.CompletedTask,
        });
        await client.StartAsync();

        await client.ResumeSessionAsync("session-1", new ResumeSessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll });

        var resumeParams = server.LastResumeParams ?? throw new InvalidOperationException("session.resume was not captured.");
        Assert.True(resumeParams.TryGetProperty("enableGitHubTelemetryForwarding", out var flag));
        Assert.True(flag.GetBoolean());
    }

    [Fact]
    public async Task CreateSession_Does_Not_Opt_In_Without_Handler()
    {
        await using var server = await FakeTelemetryServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
        });
        await client.StartAsync();

        await client.CreateSessionAsync(new SessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll });

        var createParams = server.LastCreateParams ?? throw new InvalidOperationException("session.create was not captured.");
        var optedIn = createParams.TryGetProperty("enableGitHubTelemetryForwarding", out var flag)
            && flag.ValueKind == JsonValueKind.True;
        Assert.False(optedIn);
    }

    [Fact]
    public async Task GitHubTelemetry_Event_Is_Forwarded_To_OnGitHubTelemetry()
    {
        var received = new TaskCompletionSource<GitHubTelemetryNotification>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var server = await FakeTelemetryServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            OnGitHubTelemetry = notification =>
            {
                received.TrySetResult(notification);
                return Task.CompletedTask;
            },
        });
        await client.StartAsync();

        await server.SendGitHubTelemetryEventAsync(new Dictionary<string, object?>
        {
            ["sessionId"] = "session-1",
            ["restricted"] = false,
            ["event"] = new Dictionary<string, object?>
            {
                ["kind"] = "tool_call_executed",
                ["properties"] = new Dictionary<string, object?> { ["tool"] = "shell" },
                ["metrics"] = new Dictionary<string, object?> { ["duration_ms"] = 42 },
                ["session_id"] = "session-1",
            },
        });

        var notification = await received.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.Equal("session-1", notification.SessionId);
        Assert.False(notification.Restricted);
        Assert.Equal("tool_call_executed", notification.Event.Kind);
        Assert.Equal("shell", notification.Event.Properties["tool"]);
        Assert.Equal(42, notification.Event.Metrics["duration_ms"]);
        Assert.Equal("session-1", notification.Event.SessionId);
    }

    [Fact]
    public async Task GitHubTelemetry_Event_Maps_Restricted_And_ClientInfo()
    {
        var received = new TaskCompletionSource<GitHubTelemetryNotification>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var server = await FakeTelemetryServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            OnGitHubTelemetry = notification =>
            {
                received.TrySetResult(notification);
                return Task.CompletedTask;
            },
        });
        await client.StartAsync();

        await server.SendGitHubTelemetryEventAsync(new Dictionary<string, object?>
        {
            ["sessionId"] = "session-2",
            ["restricted"] = true,
            ["event"] = new Dictionary<string, object?>
            {
                ["kind"] = "model_call",
                ["properties"] = new Dictionary<string, object?> { ["model"] = "gpt-5" },
                ["metrics"] = new Dictionary<string, object?> { ["tokens"] = 128 },
                ["session_id"] = "session-2",
                ["client"] = new Dictionary<string, object?>
                {
                    ["cli_version"] = "1.2.3",
                    ["os_platform"] = "win32",
                    ["os_arch"] = "x64",
                    ["node_version"] = "20.0.0",
                    ["is_staff"] = false,
                },
            },
        });

        var notification = await received.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.True(notification.Restricted);

        var clientInfo = notification.Event.Client;
        Assert.NotNull(clientInfo);
        Assert.Equal("1.2.3", clientInfo!.CliVersion);
        Assert.Equal("win32", clientInfo.OsPlatform);
        Assert.Equal("x64", clientInfo.OsArch);
        Assert.Equal("20.0.0", clientInfo.NodeVersion);
        Assert.Equal(false, clientInfo.IsStaff);
    }

    private sealed class FakeTelemetryServer : IAsyncDisposable
    {
        private readonly TcpListener _listener;
        private readonly CancellationTokenSource _cts = new();
        private readonly SemaphoreSlim _writeLock = new(1, 1);
        private readonly TaskCompletionSource<Stream> _connected = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly Task _serverTask;

        private FakeTelemetryServer(TcpListener listener)
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

        public JsonElement? LastCreateParams { get; private set; }

        public JsonElement? LastResumeParams { get; private set; }

        public static Task<FakeTelemetryServer> StartAsync()
        {
            var listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            return Task.FromResult(new FakeTelemetryServer(listener));
        }

        public async Task SendGitHubTelemetryEventAsync(Dictionary<string, object?> notificationParams)
        {
            var stream = await _connected.Task.WaitAsync(_cts.Token);

            // Send a genuine JSON-RPC notification (no "id"), exactly as the runtime
            // does via sendNotification. This exercises the real notification dispatch
            // path rather than masking it behind a request that carries an id.
            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["method"] = "gitHubTelemetry.event",
                ["params"] = notificationParams,
            }, _cts.Token);
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
                // Expected during teardown: the listener/socket is torn down while the
                // server loop is still awaiting I/O. Observe the exception and move on.
                _ = ex;
            }

            _cts.Dispose();
            _writeLock.Dispose();
        }

        private async Task RunAsync()
        {
            using var tcpClient = await _listener.AcceptTcpClientAsync(_cts.Token);
            using var stream = tcpClient.GetStream();
            _connected.TrySetResult(stream);

            while (!_cts.Token.IsCancellationRequested)
            {
                using var message = await ReadMessageAsync(stream, _cts.Token);
                if (message is null)
                {
                    return;
                }

                // Inbound messages without a "method" are responses to our own
                // server-initiated requests (e.g. session.* the SDK answers); the
                // SDK never replies to the gitHubTelemetry.event notification.
                if (!message.RootElement.TryGetProperty("method", out _))
                {
                    continue;
                }

                await HandleRequestAsync(stream, message.RootElement, _cts.Token);
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

            object? result = method switch
            {
                "connect" => new Dictionary<string, object?>
                {
                    ["ok"] = true,
                    ["protocolVersion"] = 3,
                    ["version"] = "test",
                },
                "session.create" => CaptureCreate(request),
                "session.resume" => CaptureResume(request),
                "session.send" => new Dictionary<string, object?> { ["messageId"] = "message-1" },
                "session.destroy" => new Dictionary<string, object?>(),
                "runtime.shutdown" => new Dictionary<string, object?>(),
                _ => throw new InvalidOperationException($"Unexpected RPC method '{method}'."),
            };

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["result"] = result,
            }, cancellationToken);
        }

        private Dictionary<string, object?> CaptureCreate(JsonElement request)
        {
            LastCreateParams = request.TryGetProperty("params", out var p) ? p.Clone() : null;
            return SessionResult(LastCreateParams);
        }

        private Dictionary<string, object?> CaptureResume(JsonElement request)
        {
            LastResumeParams = request.TryGetProperty("params", out var p) ? p.Clone() : null;
            return SessionResult(LastResumeParams);
        }

        private static Dictionary<string, object?> SessionResult(JsonElement? paramsElement)
        {
            string sessionId = "session-1";
            if (paramsElement is { ValueKind: JsonValueKind.Object } p
                && p.TryGetProperty("sessionId", out var sidProp)
                && sidProp.ValueKind == JsonValueKind.String
                && sidProp.GetString() is string sid
                && !string.IsNullOrEmpty(sid))
            {
                sessionId = sid;
            }

            return new Dictionary<string, object?>
            {
                ["sessionId"] = sessionId,
                ["workspacePath"] = null,
                ["capabilities"] = null,
            };
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

#pragma warning restore GHCP001
#endif
