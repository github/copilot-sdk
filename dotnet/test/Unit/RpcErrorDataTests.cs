/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Globalization;
using System.Net;
using System.Net.Sockets;
using System.Text;
#endif
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class RpcErrorDataTests
{
    private const string ErrorMessage = "Request failed";
    private const int ErrorCode = -32042;

    public static TheoryData<string?, JsonValueKind?> ErrorPayloads => new()
    {
        { """{"privateDetail":"payload-only-marker","nested":{"items":[1,false,null]}}""", JsonValueKind.Object },
        { """[{"value":"payload-only-marker"},[1,true],null]""", JsonValueKind.Array },
        { "{}", JsonValueKind.Object },
        { "[]", JsonValueKind.Array },
        { "\"payload-only-marker\"", JsonValueKind.String },
        { "\"\"", JsonValueKind.String },
        { "9007199254740993", JsonValueKind.Number },
        { "1.234567890123456789", JsonValueKind.Number },
        { "0", JsonValueKind.Number },
        { "true", JsonValueKind.True },
        { "false", JsonValueKind.False },
        { "null", JsonValueKind.Null },
        { null, null },
    };

    // The constructor contract also ships in netstandard2.0, exercised by net472.
    [Theory]
    [MemberData(nameof(ErrorPayloads))]
    public void Constructor_Preserves_Data_After_Document_Disposal(string? data, JsonValueKind? kind)
    {
        var inner = new IOException("original cause");
        RemoteRpcException error;
        using (var document = data is null ? null : JsonDocument.Parse(data))
        {
            error = new RemoteRpcException(ErrorMessage, ErrorCode, document?.RootElement, inner);
        }

        Assert.Equal(ErrorCode, error.ErrorCode);
        Assert.Equal(ErrorMessage, error.Message);
        Assert.Same(inner, error.InnerException);
        Assert.DoesNotContain("payload-only-marker", error.ToString());
        Assert.Equal(kind.HasValue, error.ErrorData.HasValue);
        if (kind.HasValue)
        {
            Assert.Equal(kind.Value, error.ErrorData!.Value.ValueKind);
            Assert.Equal(data, error.ErrorData.Value.GetRawText());
        }
    }

    [Fact]
    public void Constructor_Rejects_Undefined_Data()
    {
        Assert.Throws<InvalidOperationException>(() =>
            new RemoteRpcException(ErrorMessage, ErrorCode, default(JsonElement)));
    }

#if NET8_0_OR_GREATER
    [Theory]
    [MemberData(nameof(ErrorPayloads))]
    public async Task Session_Create_Preserves_Remote_Error_And_Data(string? data, JsonValueKind? kind)
    {
        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        var dataMember = data is null ? "" : ",\"data\":" + data;
        await using var server = new FakeCopilotServer("session.create",
            $$"""{"code":{{ErrorCode}},"message":"{{ErrorMessage}}"{{dataMember}}}""");
        IOException error;
        await using (var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) }))
        {
            error = await Assert.ThrowsAsync<IOException>(() =>
                client.CreateSessionAsync(new SessionConfig(), timeout.Token));

            // Processing a later response ensures the error response document has been disposed.
            var ping = await client.PingAsync(cancellationToken: timeout.Token);
            Assert.Equal("pong", ping.Message);
        }

        var remote = Assert.IsType<RemoteRpcException>(error.InnerException);
        Assert.Same(remote, error.GetBaseException());
        Assert.Equal(ErrorCode, remote.ErrorCode);
        Assert.Equal(ErrorMessage, remote.Message);
        Assert.Equal($"Communication error with Copilot CLI: {ErrorMessage}", error.Message);
        Assert.Equal($"GitHub.Copilot.RemoteRpcException: {ErrorMessage}", remote.ToString().Split(Environment.NewLine)[0]);
        Assert.Equal($"System.IO.IOException: {error.Message}", error.ToString().Split(Environment.NewLine)[0]);
        Assert.DoesNotContain("payload-only-marker", remote.ToString());
        Assert.DoesNotContain("payload-only-marker", error.ToString());
        Assert.Equal(kind.HasValue, remote.ErrorData.HasValue);
        if (kind.HasValue)
        {
            var payload = remote.ErrorData!.Value;
            Assert.Equal(kind.Value, payload.ValueKind);
            Assert.Equal(data, payload.GetRawText());
        }
    }

    [Fact]
    public async Task Successful_Response_Is_Not_An_Error()
    {
        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        await using var server = new FakeCopilotServer("session.create", null);
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var response = await client.PingAsync(cancellationToken: timeout.Token);

        Assert.Equal("pong", response.Message);
    }

    [Fact]
    public async Task Connection_Loss_Is_Not_A_Remote_Error()
    {
        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        await using var server = new FakeCopilotServer("ping", null);
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var error = await Assert.ThrowsAsync<IOException>(() => client.PingAsync(cancellationToken: timeout.Token));

        Assert.NotNull(error.InnerException);
        Assert.IsNotType<RemoteRpcException>(error.InnerException);
        Assert.Equal("Communication error with Copilot CLI: The JSON-RPC connection was lost.", error.Message);
    }

    private sealed class FakeCopilotServer : IAsyncDisposable
    {
        private readonly TcpListener _listener = new(IPAddress.Loopback, 0);
        private readonly CancellationTokenSource _cts = new(TimeSpan.FromSeconds(15));
        private readonly Task _serverTask;
        private readonly string _method;
        private readonly string? _error;

        public FakeCopilotServer(string method, string? error)
        {
            _method = method;
            _error = error;
            _listener.Start();
            Url = $"http://127.0.0.1:{((IPEndPoint)_listener.LocalEndpoint).Port}";
            _serverTask = RunAsync();
        }

        public string Url { get; }

        public async ValueTask DisposeAsync()
        {
            using var cancellation = _cts;
            _cts.Cancel();
            try
            {
                await _serverTask.WaitAsync(TimeSpan.FromSeconds(5));
            }
            catch (OperationCanceledException ex) when (ex.CancellationToken == _cts.Token)
            {
                // Canceling a pending accept/read/write is the expected shutdown path.
                return;
            }
            finally
            {
                _listener.Stop();
            }
        }

        private async Task RunAsync()
        {
            using var connection = await _listener.AcceptTcpClientAsync(_cts.Token);
            using var stream = connection.GetStream();
            while (!_cts.IsCancellationRequested)
            {
                using var request = await ReadMessageAsync(stream, _cts.Token);
                if (request is null)
                {
                    return;
                }
                if (!request.RootElement.TryGetProperty("id", out var id))
                {
                    continue;
                }
                var method = request.RootElement.GetProperty("method").GetString();
                string response;
                if (method == _method)
                {
                    if (_error is null)
                    {
                        return;
                    }
                    response = $$"""{"jsonrpc":"2.0","id":{{id.GetRawText()}},"error":{{_error}}}""";
                }
                else
                {
                    var result = method switch
                    {
                        "connect" => """{"ok":true,"protocolVersion":3,"version":"test"}""",
                        "ping" => """{"message":"pong"}""",
                        _ => throw new InvalidOperationException($"Unexpected method: {method}"),
                    };
                    response = $$"""{"jsonrpc":"2.0","id":{{id.GetRawText()}},"result":{{result}}}""";
                }
                var body = Encoding.UTF8.GetBytes(response);
                var header = Encoding.ASCII.GetBytes($"Content-Length: {body.Length}\r\n\r\n");
                await stream.WriteAsync(header, _cts.Token);
                await stream.WriteAsync(body, _cts.Token);
            }
        }

        private static async Task<JsonDocument?> ReadMessageAsync(Stream stream, CancellationToken cancellationToken)
        {
            var header = new List<byte>();
            var buffer = new byte[1];
            while (true)
            {
                if (await stream.ReadAsync(buffer, cancellationToken) == 0)
                {
                    return null;
                }
                header.Add(buffer[0]);
                if (header.Count >= 4 && header[^4] == '\r' && header[^3] == '\n' &&
                    header[^2] == '\r' && header[^1] == '\n')
                {
                    break;
                }
            }
            var length = Encoding.ASCII.GetString([.. header])
                .Split("\r\n", StringSplitOptions.RemoveEmptyEntries)
                .Select(line => line.Split(':', 2))
                .Where(parts => parts[0].Equals("Content-Length", StringComparison.OrdinalIgnoreCase))
                .Select(parts => int.Parse(parts[1].Trim(), CultureInfo.InvariantCulture))
                .Single();
            var body = new byte[length];
            await stream.ReadExactlyAsync(body, cancellationToken);
            return JsonDocument.Parse(body);
        }
    }
#endif
}
