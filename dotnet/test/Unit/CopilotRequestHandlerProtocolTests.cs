/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading.Channels;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class CopilotRequestHandlerProtocolTests
{
    private static readonly TimeSpan s_timeout = TimeSpan.FromSeconds(5);

    [Fact]
    public async Task HttpResponse_ReadsAheadAndCoalescesUnderOneOutstandingDataRpc()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await source.PushAsync("first"u8.ToArray());
        await peer.BeginRequestAsync();
        var head = await peer.NextMethodAsync();
        Assert.Equal("llmInference.httpResponseStart", Method(head));
        await peer.AcknowledgeAsync(head);

        var first = await peer.NextMethodAsync();
        Assert.Equal("first"u8.ToArray(), Data(first));

        var expectedReadAhead = new byte[32 * 1024];
        for (var i = 0; i < 32; i++)
        {
            var fragment = Enumerable.Repeat((byte)i, 1024).ToArray();
            fragment.CopyTo(expectedReadAhead, i * fragment.Length);
            await source.PushAsync(fragment);
        }

        await source.WaitForBytesAsync(5 + (32 * 1024));
        await source.PushAsync("last"u8.ToArray());
        await Task.Delay(TimeSpan.FromMilliseconds(100));
        Assert.Equal(5 + (32 * 1024), source.BytesRead);
        await peer.AssertNoMethodAsync();

        await peer.AcknowledgeAsync(first);
        var combined = await peer.NextMethodAsync();
        Assert.Equal(expectedReadAhead, Data(combined));

        await source.CompleteAsync();
        await peer.AcknowledgeAsync(combined);
        var last = await peer.NextMethodAsync();
        Assert.Equal("last"u8.ToArray(), Data(last));
        await peer.AcknowledgeAsync(last);

        var end = await peer.NextMethodAsync();
        Assert.True(end.GetProperty("params").GetProperty("end").GetBoolean());
        Assert.False(end.GetProperty("params").TryGetProperty("error", out _));
        await peer.AcknowledgeAsync(end);
        await source.Disposed.WaitAsync(s_timeout);
    }

    [Fact]
    public async Task HttpResponse_UpstreamErrorFollowsBufferedBytesAndOutstandingAck()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await source.PushAsync("first"u8.ToArray());
        await peer.BeginRequestAsync();
        await peer.AcknowledgeAsync(await peer.NextMethodAsync());
        var first = await peer.NextMethodAsync();

        await source.PushAsync("partial"u8.ToArray());
        await source.FailAsync(new IOException("upstream failed"));
        await source.WaitForReadsAsync(3);
        await peer.AssertNoMethodAsync();

        await peer.AcknowledgeAsync(first);
        var partial = await peer.NextMethodAsync();
        Assert.Equal("partial"u8.ToArray(), Data(partial));
        await peer.AssertNoMethodAsync();

        await peer.AcknowledgeAsync(partial);
        var error = await peer.NextMethodAsync();
        Assert.True(error.GetProperty("params").GetProperty("end").GetBoolean());
        Assert.Equal("upstream failed", error.GetProperty("params").GetProperty("error").GetProperty("message").GetString());
        await peer.AcknowledgeAsync(error);
        await source.Disposed.WaitAsync(s_timeout);
    }

    [Fact]
    public async Task HttpResponse_RuntimeCancellationDisposesSourceWithoutWaitingForDataAck()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await source.PushAsync("first"u8.ToArray());
        await peer.BeginRequestAsync();
        await peer.AcknowledgeAsync(await peer.NextMethodAsync());
        var first = await peer.NextMethodAsync();
        Assert.Equal("first"u8.ToArray(), Data(first));

        await peer.CancelRequestAsync();
        var error = await peer.NextMethodAsync();
        Assert.True(error.GetProperty("params").GetProperty("end").GetBoolean());
        Assert.Equal("cancelled", error.GetProperty("params").GetProperty("error").GetProperty("code").GetString());
        await source.Disposed.WaitAsync(s_timeout);
        await peer.AcknowledgeAsync(error);
    }

    [Fact]
    public async Task HttpResponse_RuntimeCancellationWhileSourceIsIdleReportsCancellation()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await peer.BeginRequestAsync();
        await peer.AcknowledgeAsync(await peer.NextMethodAsync());
        await source.ReadStarted.WaitAsync(s_timeout);

        await peer.CancelRequestAsync();
        var error = await peer.NextMethodAsync();
        Assert.True(error.GetProperty("params").GetProperty("end").GetBoolean());
        Assert.Equal("cancelled", error.GetProperty("params").GetProperty("error").GetProperty("code").GetString());
        await source.Disposed.WaitAsync(s_timeout);
        await peer.AcknowledgeAsync(error);
    }

    [Fact]
    public async Task HttpResponse_RejectedDataRpcDisposesSourceAndReportsError()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await source.PushAsync("first"u8.ToArray());
        await peer.BeginRequestAsync();
        await peer.AcknowledgeAsync(await peer.NextMethodAsync());
        var first = await peer.NextMethodAsync();

        await peer.RejectAsync(first, "write rejected");
        await source.Disposed.WaitAsync(s_timeout);
        var error = await peer.NextMethodAsync();
        Assert.Contains(
            "write rejected",
            error.GetProperty("params").GetProperty("error").GetProperty("message").GetString(),
            StringComparison.Ordinal);
        await peer.AcknowledgeAsync(error);
    }

    [Fact]
    public async Task HttpResponse_ConnectionLossDisposesSourceWithOutstandingDataRpc()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await source.PushAsync("first"u8.ToArray());
        await peer.BeginRequestAsync();
        await peer.AcknowledgeAsync(await peer.NextMethodAsync());
        Assert.Equal("first"u8.ToArray(), Data(await peer.NextMethodAsync()));

        peer.CloseConnection();
        await source.Disposed.WaitAsync(s_timeout);
    }

    [Fact]
    public async Task HttpResponse_ConnectionLossDisposesIdleSource()
    {
        var source = new ControlledResponseStream();
        await using var peer = await ProtocolPeer.StartAsync(new StreamRequestHandler(source));

        await peer.BeginRequestAsync();
        await peer.AcknowledgeAsync(await peer.NextMethodAsync());
        await source.ReadStarted.WaitAsync(s_timeout);

        peer.CloseConnection();
        await source.Disposed.WaitAsync(s_timeout);
    }

    private static string? Method(JsonElement message) =>
        message.GetProperty("method").GetString();

    private static byte[] Data(JsonElement message)
    {
        Assert.Equal("llmInference.httpResponseChunk", Method(message));
        var parameters = message.GetProperty("params");
        Assert.False(parameters.GetProperty("end").GetBoolean());
        Assert.True(parameters.GetProperty("binary").GetBoolean());
        return Convert.FromBase64String(parameters.GetProperty("data").GetString()!);
    }

    private sealed class StreamRequestHandler(Stream source) : CopilotRequestHandler
    {
        protected override Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx) =>
            Task.FromResult(CreateResponse(source));

        private static HttpResponseMessage CreateResponse(Stream source) =>
            new(HttpStatusCode.OK)
            {
                Content = new StreamContent(source),
            };
    }

    private sealed class ControlledResponseStream : Stream
    {
        private readonly Channel<ReadItem> _items = Channel.CreateBounded<ReadItem>(
            new BoundedChannelOptions(1)
            {
                SingleReader = true,
                SingleWriter = true,
                FullMode = BoundedChannelFullMode.Wait,
            });
        private readonly TaskCompletionSource _disposed = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _readStarted = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private int _readCount;
        private int _bytesRead;
        private int _isDisposed;
        private byte[]? _pendingData;
        private int _pendingOffset;

        internal int ReadCount => Volatile.Read(ref _readCount);

        internal int BytesRead => Volatile.Read(ref _bytesRead);

        internal Task Disposed => _disposed.Task;

        internal Task ReadStarted => _readStarted.Task;

        public override bool CanRead => true;

        public override bool CanSeek => false;

        public override bool CanWrite => false;

        public override long Length => throw new NotSupportedException();

        public override long Position
        {
            get => throw new NotSupportedException();
            set => throw new NotSupportedException();
        }

        internal ValueTask PushAsync(byte[] data) =>
            _items.Writer.WriteAsync(new ReadItem(data, Error: null, End: false));

        internal ValueTask FailAsync(Exception error) =>
            _items.Writer.WriteAsync(new ReadItem(Data: null, Error: error, End: false));

        internal ValueTask CompleteAsync() =>
            _items.Writer.WriteAsync(new ReadItem(Data: null, Error: null, End: true));

        internal async Task WaitForReadsAsync(int expected)
        {
            using var cts = new CancellationTokenSource(s_timeout);
            while (ReadCount < expected)
            {
                await Task.Delay(TimeSpan.FromMilliseconds(10), cts.Token);
            }
        }

        internal async Task WaitForBytesAsync(int expected)
        {
            using var cts = new CancellationTokenSource(s_timeout);
            while (BytesRead < expected)
            {
                await Task.Delay(TimeSpan.FromMilliseconds(10), cts.Token);
            }
        }

        public override int Read(byte[] buffer, int offset, int count) =>
            throw new NotSupportedException();

        public override Task<int> ReadAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            ReadCoreAsync(buffer.AsMemory(offset, count), cancellationToken).AsTask();

        public override ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken = default) =>
            ReadCoreAsync(buffer, cancellationToken);

        public override void Flush()
        {
        }

        public override long Seek(long offset, SeekOrigin origin) =>
            throw new NotSupportedException();

        public override void SetLength(long value) =>
            throw new NotSupportedException();

        public override void Write(byte[] buffer, int offset, int count) =>
            throw new NotSupportedException();

        protected override void Dispose(bool disposing)
        {
            if (disposing && Interlocked.Exchange(ref _isDisposed, 1) == 0)
            {
                _items.Writer.TryComplete();
                _disposed.TrySetResult();
            }

            base.Dispose(disposing);
        }

        private async ValueTask<int> ReadCoreAsync(Memory<byte> buffer, CancellationToken cancellationToken)
        {
            if (_pendingData is not null)
            {
                return CopyPendingData(buffer);
            }

            _readStarted.TrySetResult();
            var item = await _items.Reader.ReadAsync(cancellationToken);
            Interlocked.Increment(ref _readCount);
            if (item.Error is not null)
            {
                throw item.Error;
            }

            if (item.End)
            {
                return 0;
            }

            Assert.NotNull(item.Data);
            _pendingData = item.Data;
            _pendingOffset = 0;
            return CopyPendingData(buffer);
        }

        private int CopyPendingData(Memory<byte> buffer)
        {
            var available = _pendingData!.Length - _pendingOffset;
            var count = Math.Min(available, buffer.Length);
            _pendingData.AsMemory(_pendingOffset, count).CopyTo(buffer);
            _pendingOffset += count;
            if (_pendingOffset == _pendingData.Length)
            {
                _pendingData = null;
                _pendingOffset = 0;
            }

            Interlocked.Add(ref _bytesRead, count);
            return count;
        }

        private sealed record ReadItem(byte[]? Data, Exception? Error, bool End);
    }

    private sealed class ProtocolPeer : IAsyncDisposable
    {
        private readonly CopilotClient _client;
        private readonly TcpClient _tcpClient;
        private readonly NetworkStream _stream;
        private readonly CancellationTokenSource _disposeCts = new();
        private readonly Channel<JsonElement> _messages = Channel.CreateUnbounded<JsonElement>();
        private readonly Task _readLoop;
        private int _nextRequestId = 100_000;

        private ProtocolPeer(CopilotClient client, TcpClient tcpClient)
        {
            _client = client;
            _tcpClient = tcpClient;
            _stream = tcpClient.GetStream();
            _readLoop = ReadLoopAsync();
        }

        internal static async Task<ProtocolPeer> StartAsync(CopilotRequestHandler handler)
        {
            using var listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            var endpoint = (IPEndPoint)listener.LocalEndpoint;
            var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForUri($"http://127.0.0.1:{endpoint.Port}"),
                RequestHandler = handler,
            });
            var startTask = client.StartAsync();
            var tcpClient = await listener.AcceptTcpClientAsync().WaitAsync(s_timeout);
            var stream = tcpClient.GetStream();

            var connect = await ReadFrameAsync(stream, CancellationToken.None).WaitAsync(s_timeout);
            Assert.Equal("connect", Method(connect));
            await WriteResultAsync(
                stream,
                connect,
                writer =>
                {
                    writer.WriteStartObject();
                    writer.WriteBoolean("ok", true);
                    writer.WriteNumber("protocolVersion", 3);
                    writer.WriteString("version", "test");
                    writer.WriteEndObject();
                },
                CancellationToken.None);

            var setProvider = await ReadFrameAsync(stream, CancellationToken.None).WaitAsync(s_timeout);
            Assert.Equal("llmInference.setProvider", Method(setProvider));
            await WriteResultAsync(
                stream,
                setProvider,
                writer =>
                {
                    writer.WriteStartObject();
                    writer.WriteBoolean("success", true);
                    writer.WriteEndObject();
                },
                CancellationToken.None);

            await startTask.WaitAsync(s_timeout);
            return new ProtocolPeer(client, tcpClient);
        }

        internal async Task BeginRequestAsync()
        {
            await WriteRequestAsync(
                "llmInference.httpRequestStart",
                writer =>
                {
                    writer.WriteString("requestId", "test");
                    writer.WriteString("method", "GET");
                    writer.WriteString("url", "http://unused.test");
                    writer.WriteStartObject("headers");
                    writer.WriteEndObject();
                });
            await WriteRequestAsync(
                "llmInference.httpRequestChunk",
                writer =>
                {
                    writer.WriteString("requestId", "test");
                    writer.WriteString("data", string.Empty);
                    writer.WriteBoolean("end", true);
                });
        }

        internal Task CancelRequestAsync() =>
            WriteRequestAsync(
                "llmInference.httpRequestChunk",
                writer =>
                {
                    writer.WriteString("requestId", "test");
                    writer.WriteString("data", string.Empty);
                    writer.WriteBoolean("cancel", true);
                });

        internal async Task<JsonElement> NextMethodAsync(CancellationToken cancellationToken = default)
        {
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            timeout.CancelAfter(s_timeout);
            while (true)
            {
                var message = await _messages.Reader.ReadAsync(timeout.Token);
                if (message.TryGetProperty("method", out _))
                {
                    return message;
                }
            }
        }

        internal async Task AssertNoMethodAsync()
        {
            using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(100));
            await Assert.ThrowsAnyAsync<OperationCanceledException>(
                () => NextMethodAsync(cts.Token));
        }

        internal Task AcknowledgeAsync(JsonElement request) =>
            WriteResultAsync(
                _stream,
                request,
                writer =>
                {
                    writer.WriteStartObject();
                    writer.WriteBoolean("accepted", true);
                    writer.WriteEndObject();
                },
                _disposeCts.Token);

        internal Task RejectAsync(JsonElement request, string message) =>
            WriteFrameAsync(
                _stream,
                writer =>
                {
                    writer.WriteStartObject();
                    writer.WriteString("jsonrpc", "2.0");
                    writer.WritePropertyName("id");
                    request.GetProperty("id").WriteTo(writer);
                    writer.WriteStartObject("error");
                    writer.WriteNumber("code", -32603);
                    writer.WriteString("message", message);
                    writer.WriteEndObject();
                    writer.WriteEndObject();
                },
                _disposeCts.Token);

        internal void CloseConnection() => _tcpClient.Dispose();

        public async ValueTask DisposeAsync()
        {
            _disposeCts.Cancel();
            _tcpClient.Dispose();
            try
            {
                await _readLoop;
            }
            catch (Exception ex) when (ex is OperationCanceledException or IOException or ObjectDisposedException)
            {
                // These exceptions are expected when the protocol peer is torn down.
            }

            await _client.ForceStopAsync();
            await _client.DisposeAsync();
            _disposeCts.Dispose();
        }

        private async Task WriteRequestAsync(string method, Action<Utf8JsonWriter> writeParams)
        {
            var id = Interlocked.Increment(ref _nextRequestId);
            await WriteFrameAsync(
                _stream,
                writer =>
                {
                    writer.WriteStartObject();
                    writer.WriteString("jsonrpc", "2.0");
                    writer.WriteNumber("id", id);
                    writer.WriteString("method", method);
                    writer.WriteStartObject("params");
                    writeParams(writer);
                    writer.WriteEndObject();
                    writer.WriteEndObject();
                },
                _disposeCts.Token);
        }

        private async Task ReadLoopAsync()
        {
            try
            {
                while (!_disposeCts.IsCancellationRequested)
                {
                    await _messages.Writer.WriteAsync(
                        await ReadFrameAsync(_stream, _disposeCts.Token),
                        _disposeCts.Token);
                }
            }
            catch (Exception ex) when (ex is OperationCanceledException or IOException or ObjectDisposedException)
            {
                // Connection closure is the expected way to stop the read loop.
            }
            finally
            {
                _messages.Writer.TryComplete();
            }
        }

        private static async Task<JsonElement> ReadFrameAsync(Stream stream, CancellationToken cancellationToken)
        {
            using var header = new MemoryStream();
            var current = new byte[1];
            while (true)
            {
                var read = await stream.ReadAsync(current, cancellationToken);
                if (read == 0)
                {
                    throw new EndOfStreamException();
                }

                header.WriteByte(current[0]);
                if (header.Length >= 4)
                {
                    var bytes = header.GetBuffer();
                    var length = (int)header.Length;
                    if (bytes[length - 4] == '\r'
                        && bytes[length - 3] == '\n'
                        && bytes[length - 2] == '\r'
                        && bytes[length - 1] == '\n')
                    {
                        break;
                    }
                }
            }

            var headerText = Encoding.ASCII.GetString(header.GetBuffer(), 0, (int)header.Length);
            var contentLengthLine = headerText
                .Split("\r\n", StringSplitOptions.RemoveEmptyEntries)
                .Single(line => line.StartsWith("Content-Length:", StringComparison.OrdinalIgnoreCase));
            var contentLength = int.Parse(contentLengthLine["Content-Length:".Length..].Trim());
            var body = new byte[contentLength];
            await stream.ReadExactlyAsync(body, cancellationToken);
            using var document = JsonDocument.Parse(body);
            return document.RootElement.Clone();
        }

        private static Task WriteResultAsync(
            Stream stream,
            JsonElement request,
            Action<Utf8JsonWriter> writeResult,
            CancellationToken cancellationToken) =>
            WriteFrameAsync(
                stream,
                writer =>
                {
                    writer.WriteStartObject();
                    writer.WriteString("jsonrpc", "2.0");
                    writer.WritePropertyName("id");
                    request.GetProperty("id").WriteTo(writer);
                    writer.WritePropertyName("result");
                    writeResult(writer);
                    writer.WriteEndObject();
                },
                cancellationToken);

        private static async Task WriteFrameAsync(
            Stream stream,
            Action<Utf8JsonWriter> writeMessage,
            CancellationToken cancellationToken)
        {
            using var bodyStream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(bodyStream))
            {
                writeMessage(writer);
            }

            var body = bodyStream.ToArray();
            var header = Encoding.ASCII.GetBytes($"Content-Length: {body.Length}\r\n\r\n");
            await stream.WriteAsync(header, cancellationToken);
            await stream.WriteAsync(body, cancellationToken);
            await stream.FlushAsync(cancellationToken);
        }
    }
}
#endif
