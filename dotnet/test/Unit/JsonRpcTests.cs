/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

/// <summary>
/// Behavior tests for the SDK's hand-rolled JSON-RPC transport (params shape, serializer
/// metadata, request/response routing, error propagation). Reflection is used to force
/// every generated <c>JsonSerializable</c> registration on the <see cref="GitHub.Copilot.Rpc.RpcJsonSerializerContext"/>,
/// which guards against regressions in the C# code generator (<c>scripts/codegen/csharp.ts</c>)
/// silently dropping a registration. Functional behavior of individual RPC methods lives
/// in the <c>Rpc*Tests</c> classes; this file owns transport- and serializer-shape concerns.
/// </summary>
public class JsonRpcTests
{
    [Fact]
    public async Task JsonRpc_Handles_Positional_Named_And_Single_Object_Params()
    {
        using var pair = JsonRpcReflectionPair.Create();

        pair.Server.SetLocalRpcMethod(
            "positional",
            (Func<string, int, CancellationToken, ValueTask<string>>)HandleNameAndCount);
        pair.Server.SetLocalRpcMethod(
            "named",
            (Func<string, int, CancellationToken, ValueTask<string>>)HandleNameAndCount);
        pair.Server.SetLocalRpcMethod(
            "single",
            (Func<SingleObjectRequest, CancellationToken, ValueTask<SingleObjectResponse>>)HandleSingleObject,
            singleObjectParam: true);

        pair.StartListening();

        Assert.Equal("Mona:2", await pair.Client.InvokeAsync<string>("positional", ["Mona", 2]));
        Assert.Equal("Octo:3", await pair.Client.InvokeAsync<string>("named", [new NamedParams { Name = "Octo", Count = 3 }]));

        var response = await pair.Client.InvokeAsync<SingleObjectResponse>(
            "single",
            [new SingleObjectRequest { Value = "value" }]);
        Assert.Equal("VALUE", response.Value);

        static ValueTask<string> HandleNameAndCount(string name, int count, CancellationToken cancellationToken) =>
            ValueTask.FromResult($"{name}:{count}");

        static ValueTask<SingleObjectResponse> HandleSingleObject(SingleObjectRequest request, CancellationToken cancellationToken) =>
            ValueTask.FromResult(new SingleObjectResponse { Value = request.Value.ToUpperInvariant() });
    }

    [Fact]
    public async Task JsonRpc_Returns_Errors_For_Missing_Method_And_Invalid_Params()
    {
        using var pair = JsonRpcReflectionPair.Create();

        pair.Server.SetLocalRpcMethod(
            "single",
            (Func<SingleObjectRequest, CancellationToken, ValueTask<SingleObjectResponse>>)HandleSingleObject,
            singleObjectParam: true);

        pair.StartListening();

        var missing = await Assert.ThrowsAnyAsync<Exception>(() =>
            pair.Client.InvokeAsync<string>("missing", args: null));
        Assert.Contains("Method not found: missing", missing.Message, StringComparison.Ordinal);
        Assert.Equal(-32601, GetRemoteErrorCode(missing));

        var invalidParams = await Assert.ThrowsAnyAsync<Exception>(() =>
            pair.Client.InvokeAsync<SingleObjectResponse>("single", ["not", "an", "object"]));
        Assert.Contains("Expected JSON object", invalidParams.Message, StringComparison.Ordinal);
        Assert.Equal(-32603, GetRemoteErrorCode(invalidParams));

        static ValueTask<SingleObjectResponse> HandleSingleObject(SingleObjectRequest request, CancellationToken cancellationToken) =>
            ValueTask.FromResult(new SingleObjectResponse { Value = request.Value });
    }

    [Fact]
    public async Task JsonRpc_Preserves_Remote_Error_Data()
    {
        using var pair = JsonRpcReflectionPair.Create();

        pair.Server.SetLocalRpcMethod(
            "structuredError",
            (Func<string, int, CancellationToken, ValueTask<string>>)((_, _, _) => throw CreateLocalRpcInvocationException(
                -32603,
                "No handler implemented for this canvas action",
                CreateJsonElement("""{"code":"canvas_action_no_handler","message":"No handler implemented for this canvas action"}"""))));
        pair.Server.SetLocalRpcMethod(
            "nullErrorData",
            (Func<string, int, CancellationToken, ValueTask<string>>)((_, _, _) => throw CreateLocalRpcInvocationException(
                -32603,
                "Null error data",
                CreateJsonElement("null"))));
        pair.Server.SetLocalRpcMethod(
            "omittedErrorData",
            (Func<string, int, CancellationToken, ValueTask<string>>)((_, _, _) => throw CreateLocalRpcInvocationException(
                -32603,
                "Omitted error data")));

        pair.StartListening();

        var structured = await Assert.ThrowsAnyAsync<Exception>(() =>
            pair.Client.InvokeAsync<string>("structuredError", ["invoke", 1]));
        Assert.Equal(-32603, GetRemoteErrorCode(structured));

        var data = GetRemoteErrorData(structured);
        Assert.NotNull(data);
        Assert.Equal("canvas_action_no_handler", data.Value.GetProperty("code").GetString());
        Assert.Equal("No handler implemented for this canvas action", data.Value.GetProperty("message").GetString());

        var nullData = await Assert.ThrowsAnyAsync<Exception>(() =>
            pair.Client.InvokeAsync<string>("nullErrorData", ["invoke", 1]));
        Assert.Equal(JsonValueKind.Null, GetRemoteErrorData(nullData)?.ValueKind);

        var omittedData = await Assert.ThrowsAnyAsync<Exception>(() =>
            pair.Client.InvokeAsync<string>("omittedErrorData", ["invoke", 1]));
        Assert.Null(GetRemoteErrorData(omittedData));
    }

    [Fact]
    public async Task JsonRpc_Cancels_And_Disposes_Pending_Requests()
    {
        using var pair = JsonRpcReflectionPair.Create(startServer: false);

        using var cts = new CancellationTokenSource();
        var canceled = pair.Client.InvokeAsync<string>("never", args: null, cts.Token);
        cts.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => canceled);

        var pending = pair.Client.InvokeAsync<string>("stillPending", args: null);
        pair.Client.Dispose();
        await Assert.ThrowsAnyAsync<ObjectDisposedException>(() => pending);
    }

    private static int GetRemoteErrorCode(Exception exception)
    {
        var property = exception.GetType().GetProperty("ErrorCode", BindingFlags.Instance | BindingFlags.Public);
        Assert.NotNull(property);
        return (int)property.GetValue(exception)!;
    }

    private static JsonElement? GetRemoteErrorData(Exception exception)
    {
        var property = exception.GetType().GetProperty("ErrorData", BindingFlags.Instance | BindingFlags.Public);
        Assert.NotNull(property);
        return property.GetValue(exception) is JsonElement value ? value : null;
    }

    private static JsonElement CreateJsonElement(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }

    private static Exception CreateLocalRpcInvocationException(int code, string message, JsonElement? data = null)
    {
        var type = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.LocalRpcInvocationException", throwOnError: true)!;
        return (Exception)Activator.CreateInstance(
            type,
            BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic,
            binder: null,
            args: [code, message, data],
            culture: null)!;
    }

    private sealed class NamedParams
    {
        public string Name { get; set; } = string.Empty;

        public int Count { get; set; }
    }

    private sealed class SingleObjectRequest
    {
        public string Value { get; set; } = string.Empty;
    }

    private sealed class SingleObjectResponse
    {
        public string Value { get; set; } = string.Empty;
    }

    private sealed class JsonRpcReflectionPair : IDisposable
    {
        private readonly InMemoryDuplexStream _clientStream;
        private readonly InMemoryDuplexStream _serverStream;

        private JsonRpcReflectionPair(InMemoryDuplexStream clientStream, InMemoryDuplexStream serverStream)
        {
            _clientStream = clientStream;
            _serverStream = serverStream;
            Client = new JsonRpcReflection(clientStream);
            Server = new JsonRpcReflection(serverStream);
        }

        public JsonRpcReflection Client { get; }

        public JsonRpcReflection Server { get; }

        public static JsonRpcReflectionPair Create(bool startServer = true)
        {
            var (clientStream, serverStream) = InMemoryDuplexStream.CreatePair();
            var pair = new JsonRpcReflectionPair(clientStream, serverStream);
            if (startServer)
            {
                pair.Server.StartListening();
            }

            return pair;
        }

        public void StartListening() => Client.StartListening();

        public void Dispose()
        {
            Client.Dispose();
            Server.Dispose();
            _clientStream.Dispose();
            _serverStream.Dispose();
        }
    }

    private sealed class JsonRpcReflection : IDisposable
    {
        private static readonly Type JsonRpcType =
            typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.JsonRpc", throwOnError: true)!;

        private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web)
        {
            TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
        };

        private readonly object _instance;

        public JsonRpcReflection(Stream stream)
        {
            _instance = Activator.CreateInstance(
                JsonRpcType,
                BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic,
                binder: null,
                args: [stream, stream, SerializerOptions, null],
                culture: null)!;
        }

        public void StartListening() => JsonRpcType.GetMethod(nameof(StartListening))!.Invoke(_instance, null);

        public void SetLocalRpcMethod(string methodName, Delegate handler, bool singleObjectParam = false) =>
            JsonRpcType.GetMethod("SetLocalRpcMethod")!.Invoke(_instance, [methodName, handler, singleObjectParam]);

        public async Task<T> InvokeAsync<T>(string methodName, object?[]? args, CancellationToken cancellationToken = default)
        {
            var method = JsonRpcType
                .GetMethod("InvokeAsync")!
                .MakeGenericMethod(typeof(T));

            var task = (Task<T>)method.Invoke(_instance, [methodName, args, cancellationToken])!;
            return await task.ConfigureAwait(false);
        }

        public void Dispose() => ((IDisposable)_instance).Dispose();
    }

    private sealed class InMemoryDuplexStream : Stream
    {
        private readonly Queue<byte> _buffer = new();
        private readonly SemaphoreSlim _dataAvailable = new(0);
        private readonly object _gate = new();
        private InMemoryDuplexStream? _peer;
        private bool _completed;

        public override bool CanRead => true;

        public override bool CanSeek => false;

        public override bool CanWrite => true;

        public override long Length => throw new NotSupportedException();

        public override long Position { get => throw new NotSupportedException(); set => throw new NotSupportedException(); }

        public static (InMemoryDuplexStream Client, InMemoryDuplexStream Server) CreatePair()
        {
            var client = new InMemoryDuplexStream();
            var server = new InMemoryDuplexStream();
            client._peer = server;
            server._peer = client;
            return (client, server);
        }

        public override void Flush()
        {
        }

        public override Task FlushAsync(CancellationToken cancellationToken) => Task.CompletedTask;

        public override int Read(byte[] buffer, int offset, int count) =>
            ReadAsync(buffer.AsMemory(offset, count)).AsTask().GetAwaiter().GetResult();

        public override Task<int> ReadAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            ReadAsync(buffer.AsMemory(offset, count), cancellationToken).AsTask();

#if NET8_0_OR_GREATER
        public override
#else
        internal
#endif
        async ValueTask<int> ReadAsync(Memory<byte> destination, CancellationToken cancellationToken = default)
        {
            while (true)
            {
                lock (_gate)
                {
                    if (_buffer.Count > 0)
                    {
                        var bytesRead = Math.Min(destination.Length, _buffer.Count);
                        var span = destination.Span;
                        for (var i = 0; i < bytesRead; i++)
                        {
                            span[i] = _buffer.Dequeue();
                        }

                        return bytesRead;
                    }

                    if (_completed)
                    {
                        return 0;
                    }
                }

                await _dataAvailable.WaitAsync(cancellationToken).ConfigureAwait(false);
            }
        }

        public override void Write(byte[] buffer, int offset, int count) =>
            WriteAsync(buffer.AsMemory(offset, count)).AsTask().GetAwaiter().GetResult();

        public override Task WriteAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            WriteAsync(buffer.AsMemory(offset, count), cancellationToken).AsTask();

#if NET8_0_OR_GREATER
        public override
#else
        internal
#endif
        ValueTask WriteAsync(ReadOnlyMemory<byte> source, CancellationToken cancellationToken = default)
        {
            var peer = _peer ?? throw new ObjectDisposedException(nameof(InMemoryDuplexStream));
            peer.Enqueue(source.Span);
            return default;
        }

        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();

        public override void SetLength(long value) => throw new NotSupportedException();

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                lock (_gate)
                {
                    _completed = true;
                }

                _dataAvailable.Release();
            }

            base.Dispose(disposing);
        }

        private void Enqueue(ReadOnlySpan<byte> source)
        {
            lock (_gate)
            {
                foreach (var value in source)
                {
                    _buffer.Enqueue(value);
                }
            }

            _dataAvailable.Release();
        }
    }
}
