/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Collections;
using System.Reflection;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.SDK.Rpc;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class CoverageTargetedTests
{
    [Fact]
    public void Public_Dto_Properties_Can_Be_Set_And_Read()
    {
        var exercisedProperties = 0;
        var assembly = typeof(CopilotClient).Assembly;
        var candidateTypes = assembly
            .GetTypes()
            .Where(type =>
                type is { IsClass: true, IsAbstract: false, IsPublic: true } &&
                type.Namespace?.StartsWith("GitHub.Copilot.SDK", StringComparison.Ordinal) == true &&
                type.GetConstructor(Type.EmptyTypes) is not null)
            .OrderBy(type => type.FullName, StringComparer.Ordinal);

        foreach (var type in candidateTypes)
        {
            var instance = Activator.CreateInstance(type)!;

            foreach (var property in type.GetProperties(BindingFlags.Instance | BindingFlags.Public))
            {
                if (property.GetIndexParameters().Length != 0)
                {
                    continue;
                }

                if (property.SetMethod?.IsPublic == true &&
                    TryCreateSampleValue(property.PropertyType, [], out var sampleValue))
                {
                    property.SetValue(instance, sampleValue);
                }

                if (property.GetMethod?.IsPublic == true)
                {
                    _ = property.GetValue(instance);
                    exercisedProperties++;
                }
            }
        }

        Assert.True(exercisedProperties > 1_000, $"Expected to exercise many DTO properties, but only exercised {exercisedProperties}.");
    }

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

    [Fact]
    public async Task SessionFsProvider_Converts_Exceptions_To_Rpc_Errors()
    {
        var handler = (ISessionFsHandler)new ThrowingSessionFsProvider(new FileNotFoundException("missing"));

        AssertFsError((await handler.ReadFileAsync(new SessionFsReadFileRequest { Path = "missing.txt" })).Error);
        AssertFsError(await handler.WriteFileAsync(new SessionFsWriteFileRequest { Path = "missing.txt", Content = "content" }));
        AssertFsError(await handler.AppendFileAsync(new SessionFsAppendFileRequest { Path = "missing.txt", Content = "content" }));

        var exists = await handler.ExistsAsync(new SessionFsExistsRequest { Path = "missing.txt" });
        Assert.False(exists.Exists);

        AssertFsError((await handler.StatAsync(new SessionFsStatRequest { Path = "missing.txt" })).Error);
        AssertFsError(await handler.MkdirAsync(new SessionFsMkdirRequest { Path = "missing-dir" }));
        AssertFsError((await handler.ReaddirAsync(new SessionFsReaddirRequest { Path = "missing-dir" })).Error);
        AssertFsError((await handler.ReaddirWithTypesAsync(new SessionFsReaddirWithTypesRequest { Path = "missing-dir" })).Error);
        AssertFsError(await handler.RmAsync(new SessionFsRmRequest { Path = "missing.txt" }));
        AssertFsError(await handler.RenameAsync(new SessionFsRenameRequest { Src = "missing.txt", Dest = "dest.txt" }));

        var unknown = (ISessionFsHandler)new ThrowingSessionFsProvider(new InvalidOperationException("bad path"));
        var unknownError = await unknown.WriteFileAsync(new SessionFsWriteFileRequest { Path = "bad.txt", Content = "content" });
        Assert.Equal(SessionFsErrorCode.UNKNOWN, unknownError!.Code);

        static void AssertFsError(SessionFsError? error)
        {
            Assert.NotNull(error);
            Assert.Equal(SessionFsErrorCode.ENOENT, error.Code);
            Assert.Contains("missing", error.Message, StringComparison.OrdinalIgnoreCase);
        }
    }

    [Fact]
    public void TelemetryHelpers_Restores_W3C_Trace_Context()
    {
        using var parent = new Activity("parent");
        parent.SetIdFormat(ActivityIdFormat.W3C);
        parent.TraceStateString = "state=value";
        parent.Start();

        var traceContext = InvokeTelemetryHelper<(string? Traceparent, string? Tracestate)>("GetTraceContext");
        Assert.Equal(parent.Id, traceContext.Traceparent);
        Assert.Equal("state=value", traceContext.Tracestate);

        parent.Stop();
        using var restored = InvokeTelemetryHelper<Activity?>(
            "RestoreTraceContext",
            traceContext.Traceparent,
            traceContext.Tracestate);

        Assert.NotNull(restored);
        Assert.Equal(parent.Id, restored.ParentId);
        Assert.Equal("state=value", restored.TraceStateString);

        Assert.Null(InvokeTelemetryHelper<Activity?>("RestoreTraceContext", "not-a-traceparent", null));
    }

    private static int GetRemoteErrorCode(Exception exception)
    {
        var property = exception.GetType().GetProperty("ErrorCode", BindingFlags.Instance | BindingFlags.Public);
        Assert.NotNull(property);
        return (int)property.GetValue(exception)!;
    }

    private static T InvokeTelemetryHelper<T>(string name, params object?[] args)
    {
        var helperType = typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.SDK.TelemetryHelpers", throwOnError: true)!;
        var method = helperType.GetMethod(name, BindingFlags.Static | BindingFlags.NonPublic)!;
        return (T)method.Invoke(null, args)!;
    }

    private static bool TryCreateSampleValue(Type type, HashSet<Type> visited, out object? value)
    {
        var nullableType = Nullable.GetUnderlyingType(type);
        if (nullableType is not null)
        {
            return TryCreateSampleValue(nullableType, visited, out value);
        }

        if (type == typeof(string))
        {
            value = "value";
            return true;
        }

        if (type == typeof(bool))
        {
            value = true;
            return true;
        }

        if (type == typeof(int))
        {
            value = 1;
            return true;
        }

        if (type == typeof(long))
        {
            value = 1L;
            return true;
        }

        if (type == typeof(double))
        {
            value = 1.0;
            return true;
        }

        if (type == typeof(DateTimeOffset))
        {
            value = DateTimeOffset.UnixEpoch;
            return true;
        }

        if (type == typeof(DateTime))
        {
            value = DateTime.UnixEpoch;
            return true;
        }

        if (type == typeof(TimeSpan))
        {
            value = TimeSpan.FromMilliseconds(1);
            return true;
        }

        if (type == typeof(JsonElement))
        {
            using var document = JsonDocument.Parse("""{"value":1}""");
            value = document.RootElement.Clone();
            return true;
        }

        if (type == typeof(object))
        {
            value = "value";
            return true;
        }

        if (type.IsEnum)
        {
            var values = Enum.GetValues(type);
            value = values.Length > 0 ? values.GetValue(0) : Activator.CreateInstance(type);
            return true;
        }

        if (type.IsArray)
        {
            var elementType = type.GetElementType()!;
            if (!TryCreateSampleValue(elementType, visited, out var elementValue))
            {
                elementValue = elementType.IsValueType ? Activator.CreateInstance(elementType) : null;
            }

            var array = Array.CreateInstance(elementType, 1);
            array.SetValue(elementValue, 0);
            value = array;
            return true;
        }

        if (TryCreateGenericCollection(type, visited, out value))
        {
            return true;
        }

        if (!type.IsValueType && type.GetConstructor(Type.EmptyTypes) is not null && visited.Add(type))
        {
            value = Activator.CreateInstance(type);
            visited.Remove(type);
            return true;
        }

        value = type.IsValueType ? Activator.CreateInstance(type) : null;
        return true;
    }

    private static bool TryCreateGenericCollection(Type type, HashSet<Type> visited, out object? value)
    {
        var dictionaryInterface = type.GetInterfaces()
            .Append(type)
            .FirstOrDefault(candidate =>
                candidate.IsGenericType &&
                (candidate.GetGenericTypeDefinition() == typeof(IDictionary<,>) ||
                 candidate.GetGenericTypeDefinition() == typeof(IReadOnlyDictionary<,>)) &&
                candidate.GetGenericArguments()[0] == typeof(string));

        if (dictionaryInterface is not null)
        {
            var valueType = dictionaryInterface.GetGenericArguments()[1];
            TryCreateSampleValue(valueType, visited, out var sampleValue);
            var dictionary = (IDictionary)Activator.CreateInstance(typeof(Dictionary<,>).MakeGenericType(typeof(string), valueType))!;
            dictionary["key"] = sampleValue;
            value = dictionary;
            return true;
        }

        var enumerableInterface = type.GetInterfaces()
            .Append(type)
            .FirstOrDefault(candidate =>
                candidate.IsGenericType &&
                (candidate.GetGenericTypeDefinition() == typeof(IList<>) ||
                 candidate.GetGenericTypeDefinition() == typeof(IReadOnlyList<>) ||
                 candidate.GetGenericTypeDefinition() == typeof(IEnumerable<>)));

        if (enumerableInterface is not null)
        {
            var elementType = enumerableInterface.GetGenericArguments()[0];
            TryCreateSampleValue(elementType, visited, out var sampleValue);
            var list = (IList)Activator.CreateInstance(typeof(List<>).MakeGenericType(elementType))!;
            list.Add(sampleValue);
            value = list;
            return true;
        }

        value = null;
        return false;
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

    private sealed class ThrowingSessionFsProvider(Exception exception) : SessionFsProvider
    {
        protected override Task<string> ReadFileAsync(string path, CancellationToken cancellationToken) =>
            Task.FromException<string>(exception);

        protected override Task WriteFileAsync(string path, string content, int? mode, CancellationToken cancellationToken) =>
            Task.FromException(exception);

        protected override Task AppendFileAsync(string path, string content, int? mode, CancellationToken cancellationToken) =>
            Task.FromException(exception);

        protected override Task<bool> ExistsAsync(string path, CancellationToken cancellationToken) =>
            Task.FromException<bool>(exception);

        protected override Task<SessionFsStatResult> StatAsync(string path, CancellationToken cancellationToken) =>
            Task.FromException<SessionFsStatResult>(exception);

        protected override Task MkdirAsync(string path, bool recursive, int? mode, CancellationToken cancellationToken) =>
            Task.FromException(exception);

        protected override Task<IList<string>> ReaddirAsync(string path, CancellationToken cancellationToken) =>
            Task.FromException<IList<string>>(exception);

        protected override Task<IList<SessionFsReaddirWithTypesEntry>> ReaddirWithTypesAsync(string path, CancellationToken cancellationToken) =>
            Task.FromException<IList<SessionFsReaddirWithTypesEntry>>(exception);

        protected override Task RmAsync(string path, bool recursive, bool force, CancellationToken cancellationToken) =>
            Task.FromException(exception);

        protected override Task RenameAsync(string src, string dest, CancellationToken cancellationToken) =>
            Task.FromException(exception);
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
            typeof(CopilotClient).Assembly.GetType("GitHub.Copilot.SDK.JsonRpc", throwOnError: true)!;

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

        public override async ValueTask<int> ReadAsync(Memory<byte> destination, CancellationToken cancellationToken = default)
        {
            while (true)
            {
                lock (_gate)
                {
                    if (_buffer.Count > 0)
                    {
                        var count = Math.Min(destination.Length, _buffer.Count);
                        for (var i = 0; i < count; i++)
                        {
                            destination.Span[i] = _buffer.Dequeue();
                        }

                        return count;
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

        public override ValueTask WriteAsync(ReadOnlyMemory<byte> source, CancellationToken cancellationToken = default)
        {
            var peer = _peer ?? throw new ObjectDisposedException(nameof(InMemoryDuplexStream));
            peer.Enqueue(source.Span);
            return ValueTask.CompletedTask;
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
