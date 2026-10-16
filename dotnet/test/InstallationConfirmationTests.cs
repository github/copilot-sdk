/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization.Metadata;
using InstallationDecision = GitHub.Copilot.Rpc.InstallationDecision;
using InstallationReviewMcp = GitHub.Copilot.Rpc.InstallationReviewMcp;
using InstallationsConfirmRequest = GitHub.Copilot.Rpc.InstallationsConfirmRequest;
using McpInstallationReviewInstall = GitHub.Copilot.Rpc.McpInstallationReviewInstall;
using Xunit;

namespace GitHub.Copilot.Test;

public class InstallationConfirmationTests
{
    private static readonly TimeSpan Wait = TimeSpan.FromSeconds(5);
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    [Fact]
    public async Task Presents_Typed_Review_And_Echoes_Challenge_And_Fingerprint()
    {
        InstallationsConfirmRequest? received = null;
        InstallationConfirmationContext? context = null;
        InstallationConfirmationHandler handler = (request, incomingContext) =>
        {
            received = request;
            context = incomingContext;
            request.ConfirmationId = "mutated-by-handler";
            request.ReviewFingerprint = "mutated-by-handler";
            return ValueTask.FromResult(InstallationDecision.Confirm);
        };
        var peer = await Peer.ConnectAsync(handler);
        try
        {
            await peer.ConfirmAsync(101, Request("operation-a"));

            var response = await peer.ReadFrameAsync();

            Assert.Equal("challenge-operation-a", response.GetProperty("result").GetProperty("confirmationId").GetString());
            Assert.Equal("fingerprint-operation-a", response.GetProperty("result").GetProperty("reviewFingerprint").GetString());
            Assert.Equal("confirm", response.GetProperty("result").GetProperty("decision").GetString());
            Assert.NotNull(received);
            Assert.Equal("operation-a", received.OperationId);
            Assert.Equal("original-session", received.PolicySessionId);
            Assert.False(context!.CancellationToken.IsCancellationRequested);
            var review = Assert.IsType<InstallationReviewMcp>(received.Review);
            var install = Assert.IsType<McpInstallationReviewInstall>(review.Review);
            Assert.Equal("https://example.test/mcp", install.EffectiveConfiguration?.Url);
            Assert.Equal("eu", install.EffectiveConfiguration?.Headers["X-Region"]);
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    [Fact]
    public async Task Same_Session_Reviews_Finish_Out_Of_Order_Without_Blocking_Other_Rpcs()
    {
        var handler = new ControlledInstallationHandler();
        var peer = await Peer.ConnectAsync(handler.HandleAsync);
        try
        {
            await peer.ConfirmAsync(101, Request("a"));
            await peer.ConfirmAsync(102, Request("b"));
            var first = await handler.NextReviewAsync();
            var second = await handler.NextReviewAsync();
            var reviewA = first.Request.OperationId == "a" ? first : second;
            var reviewB = first.Request.OperationId == "b" ? first : second;

            Assert.Equal(reviewA.Request.PolicySessionId, reviewB.Request.PolicySessionId);
            await peer.SendAsync(new JsonObject
            {
                ["jsonrpc"] = "2.0",
                ["id"] = 103,
                ["method"] = "gitHubToken.getToken",
                ["params"] = new JsonObject
                {
                    ["registrationId"] = "unknown",
                    ["host"] = "github.com",
                    ["reason"] = "initial",
                },
            });
            Assert.Equal(103, (await peer.ReadFrameAsync()).GetProperty("id").GetInt32());

            reviewB.Decision.SetResult(InstallationDecision.Decline);
            var declined = await peer.ReadFrameAsync();
            Assert.Equal(102, declined.GetProperty("id").GetInt32());
            Assert.Equal("decline", declined.GetProperty("result").GetProperty("decision").GetString());

            reviewA.Decision.SetResult(InstallationDecision.Confirm);
            var confirmed = await peer.ReadFrameAsync();
            Assert.Equal(101, confirmed.GetProperty("id").GetInt32());
            Assert.Equal("challenge-a", confirmed.GetProperty("result").GetProperty("confirmationId").GetString());
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    [Fact]
    public async Task Cancel_Request_Retires_Only_Cancelled_Review_And_Drops_Late_Decision()
    {
        var handler = new ControlledInstallationHandler();
        var peer = await Peer.ConnectAsync(handler.HandleAsync);
        try
        {
            await peer.ConfirmAsync(301, Request("a"));
            var reviewA = await handler.NextReviewAsync();
            await peer.CancelAsync(301);
            var cancelled = await peer.ReadFrameAsync();
            Assert.Equal(301, cancelled.GetProperty("id").GetInt32());
            Assert.Equal(-32800, cancelled.GetProperty("error").GetProperty("code").GetInt32());
            Assert.True(reviewA.Context.CancellationToken.IsCancellationRequested);
            reviewA.Decision.SetResult(InstallationDecision.Confirm);

            await peer.ConfirmAsync(302, Request("b"));
            var reviewB = await handler.NextReviewAsync();
            await peer.CancelAsync(301);
            await peer.CancelAsync(999);
            await peer.CancelAsync("302");
            await peer.SendAsync(new JsonObject
            {
                ["jsonrpc"] = "2.0",
                ["id"] = 303,
                ["method"] = "gitHubToken.getToken",
                ["params"] = new JsonObject
                {
                    ["registrationId"] = "unknown",
                    ["host"] = "github.com",
                    ["reason"] = "initial",
                },
            });
            Assert.Equal(303, (await peer.ReadFrameAsync()).GetProperty("id").GetInt32());
            Assert.False(reviewB.Context.CancellationToken.IsCancellationRequested);

            reviewB.Decision.SetResult(InstallationDecision.Confirm);
            var response = await peer.ReadFrameAsync();
            Assert.Equal(302, response.GetProperty("id").GetInt32());
            Assert.Equal("confirm", response.GetProperty("result").GetProperty("decision").GetString());
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    [Fact]
    public async Task Cancel_Request_Rejects_Decision_Completed_By_Cancellation()
    {
        var received = new TaskCompletionSource<InstallationConfirmationContext>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        InstallationConfirmationHandler handler = (_request, context) =>
        {
            received.SetResult(context);
            return new ValueTask<InstallationDecision>(CompleteAfterCancellationAsync(context.CancellationToken));
        };
        var peer = await Peer.ConnectAsync(handler);
        try
        {
            await peer.ConfirmAsync(351, Request("cancel-race"));
            var context = await WithTimeoutAsync(received.Task);

            await peer.CancelAsync(351);

            await WaitForCancellationAsync(context.CancellationToken);
            var response = await peer.ReadFrameAsync();
            Assert.Equal(351, response.GetProperty("id").GetInt32());
            Assert.Equal(-32800, response.GetProperty("error").GetProperty("code").GetInt32());
            Assert.False(response.TryGetProperty("result", out _));
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    [Fact]
    public async Task Connection_Close_Cancels_All_Outstanding_Reviews()
    {
        var handler = new ControlledInstallationHandler();
        var peer = await Peer.ConnectAsync(handler.HandleAsync);
        try
        {
            await peer.ConfirmAsync(361, Request("a"));
            await peer.ConfirmAsync(362, Request("b"));
            var first = await handler.NextReviewAsync();
            var second = await handler.NextReviewAsync();

            peer.CloseTransport();

            await WaitForCancellationAsync(first.Context.CancellationToken);
            await WaitForCancellationAsync(second.Context.CancellationToken);
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    [Fact]
    public async Task Connection_Close_Is_Distinct_And_Does_Not_Affect_Another_Connection()
    {
        var firstHandler = new ControlledInstallationHandler();
        var secondHandler = new ControlledInstallationHandler();
        var first = await Peer.ConnectAsync(firstHandler.HandleAsync);
        var second = await Peer.ConnectAsync(secondHandler.HandleAsync);
        try
        {
            await first.ConfirmAsync(401, Request("a"));
            var firstReview = await firstHandler.NextReviewAsync();
            await second.ConfirmAsync(401, Request("b"));
            var secondReview = await secondHandler.NextReviewAsync();

            first.CloseTransport();
            await WaitForCancellationAsync(firstReview.Context.CancellationToken);
            Assert.False(secondReview.Context.CancellationToken.IsCancellationRequested);

            firstReview.Decision.SetResult(InstallationDecision.Confirm);
            secondReview.Decision.SetResult(InstallationDecision.Cancel);
            var response = await second.ReadFrameAsync();
            Assert.Equal("cancel", response.GetProperty("result").GetProperty("decision").GetString());
        }
        finally
        {
            await first.DisposeAsync();
            await second.DisposeAsync();
        }
    }

    [Fact]
    public async Task Stopping_Client_Retires_Pending_Review()
    {
        var handler = new ControlledInstallationHandler();
        var peer = await Peer.ConnectAsync(handler.HandleAsync);
        try
        {
            await peer.ConfirmAsync(450, Request("a"));
            var review = await handler.NextReviewAsync();

            await peer.Client.ForceStopAsync();

            await WaitForCancellationAsync(review.Context.CancellationToken);
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    [Fact]
    public async Task Missing_Handler_Invalid_Review_Handler_Errors_And_Unknown_Decisions_Never_Approve()
    {
        var missing = await Peer.ConnectAsync(handler: null);
        try
        {
            await missing.ConfirmAsync(501, Request("a"));
            var response = await missing.ReadFrameAsync();
            Assert.Equal(501, response.GetProperty("id").GetInt32());
            Assert.True(response.TryGetProperty("error", out _));
            Assert.False(response.TryGetProperty("result", out _));
        }
        finally
        {
            await missing.DisposeAsync();
        }

        var invokedForInvalid = false;
        var invalid = await Peer.ConnectAsync((_request, _context) =>
        {
            invokedForInvalid = true;
            return ValueTask.FromResult(InstallationDecision.Confirm);
        });
        try
        {
            var invalidReview = Request("invalid");
            invalidReview["review"] = 42;
            await invalid.ConfirmAsync(502, invalidReview);
            var response = await invalid.ReadFrameAsync();
            Assert.Equal(502, response.GetProperty("id").GetInt32());
            Assert.True(response.TryGetProperty("error", out _));
            Assert.False(invokedForInvalid);
        }
        finally
        {
            await invalid.DisposeAsync();
        }

        var throwing = await Peer.ConnectAsync((_request, _context) =>
            throw new InvalidOperationException("review unavailable"));
        try
        {
            await throwing.ConfirmAsync(503, Request("throws"));
            var response = await throwing.ReadFrameAsync();
            Assert.Equal(503, response.GetProperty("id").GetInt32());
            Assert.True(response.TryGetProperty("error", out _));
            Assert.False(response.TryGetProperty("result", out _));
        }
        finally
        {
            await throwing.DisposeAsync();
        }

        var unknown = await Peer.ConnectAsync((_request, _context) =>
            ValueTask.FromResult(new InstallationDecision("future")));
        try
        {
            await unknown.ConfirmAsync(504, Request("unknown"));
            var response = await unknown.ReadFrameAsync();
            Assert.Equal(504, response.GetProperty("id").GetInt32());
            Assert.True(response.TryGetProperty("error", out _));
            Assert.False(response.TryGetProperty("result", out _));
        }
        finally
        {
            await unknown.DisposeAsync();
        }
    }

    [Fact]
    public async Task Optional_Legacy_Session_Is_Not_Inferred()
    {
        InstallationsConfirmRequest? received = null;
        var peer = await Peer.ConnectAsync((request, _context) =>
        {
            received = request;
            return ValueTask.FromResult(InstallationDecision.Decline);
        });
        try
        {
            var request = Request("legacy");
            request.Remove("policySessionId");
            await peer.ConfirmAsync(601, request);
            var response = await peer.ReadFrameAsync();

            Assert.Null(received!.PolicySessionId);
            Assert.Equal("decline", response.GetProperty("result").GetProperty("decision").GetString());
        }
        finally
        {
            await peer.DisposeAsync();
        }
    }

    private static JsonObject Request(string operation)
    {
        var request = JsonNode.Parse(InstallationConfirmationJson)!.AsObject();
        request["operationId"] = operation;
        request["confirmationId"] = $"challenge-{operation}";
        request["reviewFingerprint"] = $"fingerprint-{operation}";
        return request;
    }

    private static async Task WaitForCancellationAsync(CancellationToken cancellationToken)
    {
        if (cancellationToken.IsCancellationRequested)
        {
            return;
        }

        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var registration = cancellationToken.Register(
            static state => ((TaskCompletionSource)state!).TrySetResult(),
            cancelled);
        await WithTimeoutAsync(cancelled.Task);
    }

    private static async Task<InstallationDecision> CompleteAfterCancellationAsync(CancellationToken cancellationToken)
    {
        await WaitForCancellationAsync(cancellationToken);
        return InstallationDecision.Confirm;
    }

    private static async Task WithTimeoutAsync(Task task)
    {
        var timeout = Task.Delay(Wait);
        var completed = await Task.WhenAny(task, timeout);
        Assert.Same(task, completed);
        await task;
    }

    private static async Task<T> WithTimeoutAsync<T>(Task<T> task)
    {
        var timeout = Task.Delay(Wait);
        var completed = await Task.WhenAny(task, timeout);
        Assert.Same(task, completed);
        return await task;
    }

    private sealed class ControlledInstallationHandler
    {
        private readonly AsyncQueue<Review> _reviews = new();

        public ValueTask<InstallationDecision> HandleAsync(
            InstallationsConfirmRequest request,
            InstallationConfirmationContext context)
        {
            var decision = new TaskCompletionSource<InstallationDecision>(
                TaskCreationOptions.RunContinuationsAsynchronously);
            _reviews.Enqueue(new Review(request, context, decision));
            return new ValueTask<InstallationDecision>(decision.Task);
        }

        public Task<Review> NextReviewAsync() => WithTimeoutAsync(_reviews.DequeueAsync());
    }

    private sealed record Review(
        InstallationsConfirmRequest Request,
        InstallationConfirmationContext Context,
        TaskCompletionSource<InstallationDecision> Decision);

    private sealed class AsyncQueue<T>
    {
        private readonly Queue<T> _items = new();
        private readonly Queue<TaskCompletionSource<T>> _waiters = new();

        public void Enqueue(T item)
        {
            TaskCompletionSource<T>? waiter = null;
            lock (_items)
            {
                if (_waiters.Count > 0)
                {
                    waiter = _waiters.Dequeue();
                }
                else
                {
                    _items.Enqueue(item);
                    return;
                }
            }

            waiter.SetResult(item);
        }

        public Task<T> DequeueAsync()
        {
            lock (_items)
            {
                if (_items.Count > 0)
                {
                    return Task.FromResult(_items.Dequeue());
                }

                var waiter = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
                _waiters.Enqueue(waiter);
                return waiter.Task;
            }
        }
    }

    private sealed class Peer
    {
        private readonly TcpListener _listener;
        private readonly TcpClient _tcpClient;
        private readonly Stream _stream;
        private bool _disposed;

        private Peer(
            CopilotClient client,
            TcpListener listener,
            TcpClient tcpClient,
            Stream stream)
        {
            Client = client;
            _listener = listener;
            _tcpClient = tcpClient;
            _stream = stream;
        }

        public CopilotClient Client { get; }

        public static async Task<Peer> ConnectAsync(InstallationConfirmationHandler? handler)
        {
            var listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            var port = ((IPEndPoint)listener.LocalEndpoint).Port;
            var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForUri($"127.0.0.1:{port}"),
                InstallationConfirmationHandler = handler,
            });
            var start = client.StartAsync();
            var tcpClient = await WithTimeoutAsync(listener.AcceptTcpClientAsync());
            var stream = tcpClient.GetStream();
            var connect = await InstallationConfirmationTests.ReadFrameAsync(stream);
            Assert.Equal("connect", connect.GetProperty("method").GetString());
            await InstallationConfirmationTests.WriteFrameAsync(stream, new JsonObject
            {
                ["jsonrpc"] = "2.0",
                ["id"] = connect.GetProperty("id").GetInt64(),
                ["result"] = new JsonObject
                {
                    ["ok"] = true,
                    ["protocolVersion"] = 3,
                    ["version"] = "installation-confirmation-test",
                },
            });
            await WithTimeoutAsync(start);
            return new Peer(client, listener, tcpClient, stream);
        }

        public Task SendAsync(JsonObject message) => WriteFrameAsync(_stream, message);

        public Task ConfirmAsync(long id, JsonObject request) => SendAsync(new JsonObject
        {
            ["jsonrpc"] = "2.0",
            ["id"] = id,
            ["method"] = "installations.confirm",
            ["params"] = request,
        });

        public Task CancelAsync(long id) => SendAsync(new JsonObject
        {
            ["jsonrpc"] = "2.0",
            ["method"] = "$/cancelRequest",
            ["params"] = new JsonObject { ["id"] = id },
        });

        public Task CancelAsync(string id) => SendAsync(new JsonObject
        {
            ["jsonrpc"] = "2.0",
            ["method"] = "$/cancelRequest",
            ["params"] = new JsonObject { ["id"] = id },
        });

        public Task<JsonElement> ReadFrameAsync() => InstallationConfirmationTests.ReadFrameAsync(_stream);

        public void CloseTransport()
        {
            _tcpClient.Dispose();
        }

        public async Task DisposeAsync()
        {
            if (_disposed)
            {
                return;
            }

            _disposed = true;
            try
            {
                await Client.ForceStopAsync();
            }
            catch (ObjectDisposedException ex)
            {
                Debug.WriteLine($"Ignoring expected ObjectDisposedException during peer teardown: {ex.Message}");
            }
            catch (InvalidOperationException ex)
            {
                Debug.WriteLine($"Ignoring expected InvalidOperationException during peer teardown: {ex.Message}");
            }
            catch (IOException ex)
            {
                Debug.WriteLine($"Ignoring expected IOException during peer teardown: {ex.Message}");
            }
            catch (SocketException ex)
            {
                Debug.WriteLine($"Ignoring expected SocketException during peer teardown: {ex.Message}");
            }

            _stream.Dispose();
            _tcpClient.Dispose();
            _listener.Stop();
        }
    }

    private static async Task WriteFrameAsync(Stream stream, object value)
    {
        var json = JsonSerializer.SerializeToUtf8Bytes(value, JsonOptions);
        var header = Encoding.ASCII.GetBytes($"Content-Length: {json.Length}\r\n\r\n");
        await stream.WriteAsync(header.AsMemory());
        await stream.WriteAsync(json.AsMemory());
        await stream.FlushAsync();
    }

    private static async Task<JsonElement> ReadFrameAsync(Stream stream)
    {
        return await WithTimeoutAsync(ReadFrameUntimedAsync(stream));
    }

    private static async Task<JsonElement> ReadFrameUntimedAsync(Stream stream)
    {
        var header = new List<byte>();
        var singleByte = new byte[1];
        while (!EndsWithHeaderTerminator(header))
        {
            var read = await stream.ReadAsync(singleByte.AsMemory());
            if (read == 0)
            {
                throw new EndOfStreamException("Stream ended while reading JSON-RPC headers.");
            }

            header.Add(singleByte[0]);
        }

        var headerText = Encoding.ASCII.GetString(header.ToArray());
        var contentLength = headerText
            .Split(["\r\n"], StringSplitOptions.RemoveEmptyEntries)
            .Select(line => line.StartsWith("Content-Length: ", StringComparison.Ordinal)
                ? int.Parse(line["Content-Length: ".Length..], System.Globalization.CultureInfo.InvariantCulture)
                : (int?)null)
            .First(length => length.HasValue)!.Value;
        var body = new byte[contentLength];
        var offset = 0;
        while (offset < body.Length)
        {
            var read = await stream.ReadAsync(body.AsMemory(offset));
            if (read == 0)
            {
                throw new EndOfStreamException("Stream ended while reading a JSON-RPC body.");
            }

            offset += read;
        }

        using var document = JsonDocument.Parse(body);
        return document.RootElement.Clone();
    }

    private static bool EndsWithHeaderTerminator(List<byte> header)
    {
        return header.Count >= 4 &&
            header[^4] == (byte)'\r' &&
            header[^3] == (byte)'\n' &&
            header[^2] == (byte)'\r' &&
            header[^1] == (byte)'\n';
    }

    private const string InstallationConfirmationJson = """
        {
          "confirmationId": "confirmation-a",
          "operationId": "operation-a",
          "policySessionId": "original-session",
          "expiresAt": "2026-09-24T03:00:00Z",
          "reviewFingerprint": "fingerprint-a",
          "review": {
            "resource": "mcp",
            "review": {
              "action": "install",
              "identity": {
                "canonicalName": "io.example/server",
                "serverName": "example"
              },
              "provenance": {
                "authority": "cards.example.test",
                "validatedAt": "2026-09-24T02:59:00Z",
                "cardDigest": {
                  "algorithm": "sha256-rfc8785",
                  "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                },
                "mediaType": "application/mcp-server-card+json"
              },
              "target": { "scope": "user", "configKey": "example" },
              "policy": { "decision": "allowed", "source": "none" },
              "selectedChoice": {
                "choiceId": "remote-choice",
                "installMethod": "remote",
                "endpoint": "https://example.test/mcp",
                "transport": "streamable-http",
                "requiredValues": [],
                "secretPlaceholders": []
              },
              "configurationChange": {
                "operation": "add",
                "scope": "user",
                "configKey": "example",
                "changedFields": ["type", "url", "headers", "tools"],
                "secretReferences": []
              },
              "inputs": [],
              "suppliedSecrets": [],
              "secretStorage": "keychain",
              "effectiveConfiguration": {
                "transport": "streamable-http",
                "url": "https://example.test/mcp",
                "headers": { "X-Region": "eu" },
                "tools": ["*"]
              }
            }
          }
        }
        """;
}
