/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Reflection;
using System.Text;
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

    [Theory]
    [InlineData(false, false)]
    [InlineData(false, true)]
    [InlineData(true, false)]
    [InlineData(true, true)]
    public async Task JsonRpc_Dispose_During_Send_Does_Not_Mask_Error(bool blockFlush, bool failSend)
    {
        var sendError = failSend ? new IOException("send failed") : null;
        using var sendStream = new GatedSendStream(blockFlush, sendError);
        using var rpc = new JsonRpcReflection(sendStream, Stream.Null);
        var pending = rpc.InvokeAsync<string>("pending", args: null);
        var shutdownError = new IOException("connection stopped");
        var timeout = TimeSpan.FromSeconds(5);

        try
        {
            await sendStream.Blocked.WaitAsync(timeout);
            rpc.Dispose(shutdownError);
            Assert.True(rpc.Completion.IsCompleted);
            Assert.False(pending.IsCompleted);
        }
        finally
        {
            sendStream.Unblock();
        }

        var error = await Record.ExceptionAsync(() => pending.WaitAsync(timeout));
        Assert.Same(sendError ?? shutdownError, error);
        if (!failSend)
        {
            Assert.Equal(
                CreateFrame("""{"jsonrpc":"2.0","id":1,"method":"pending"}"""),
                sendStream.ToArray());
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task JsonRpc_Dispose_With_Queued_Send_Does_Not_Write_Another_Frame(bool cancelQueued)
    {
        using var sendStream = new GatedSendStream();
        using var rpc = new JsonRpcReflection(sendStream, Stream.Null);
        using var cancellation = new CancellationTokenSource();
        var active = rpc.InvokeAsync<string>("active", args: null);
        Task<string>? queued = null;
        var shutdownError = Assert.Throws<IOException>((Action)(() => throw new IOException("connection stopped")));
        var originalStackTrace = shutdownError.StackTrace!;
        var timeout = TimeSpan.FromSeconds(5);

        try
        {
            await sendStream.Blocked.WaitAsync(timeout);
            queued = rpc.InvokeAsync<string>("queued", args: null, cancellation.Token);
            Assert.False(queued.IsCompleted);
            if (cancelQueued)
            {
                cancellation.Cancel();
                // Finish canceling the lock wait before shutdown competes with it.
                await Assert.ThrowsAnyAsync<OperationCanceledException>(() => queued.WaitAsync(timeout));
            }
            rpc.Dispose(shutdownError);
        }
        finally
        {
            sendStream.Unblock();
        }

        var activeError = await Record.ExceptionAsync(() => active.WaitAsync(timeout));
        var queuedError = await Record.ExceptionAsync(() => queued!.WaitAsync(timeout));
        cancellation.Cancel();
        Assert.Same(shutdownError, activeError);
        if (cancelQueued)
        {
            Assert.IsAssignableFrom<OperationCanceledException>(queuedError);
        }
        else
        {
            Assert.Same(shutdownError, queuedError);
            Assert.Contains(originalStackTrace, queuedError.StackTrace, StringComparison.Ordinal);
            var lateError = await Record.ExceptionAsync(() =>
                rpc.InvokeAsync<string>("afterShutdown", args: null).WaitAsync(timeout));
            Assert.Same(shutdownError, lateError);
            Assert.Contains(originalStackTrace, lateError.StackTrace, StringComparison.Ordinal);
        }
        Assert.Equal(
            CreateFrame("""{"jsonrpc":"2.0","id":1,"method":"active"}"""),
            sendStream.ToArray());
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task JsonRpc_Cancels_Incoming_Handler_Token_When_Connection_Disposed(bool requestHasId)
    {
        using var pair = JsonRpcReflectionPair.Create();
        var handlerStarted = new TaskCompletionSource<CancellationToken>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        var handlerDispatched = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);

        pair.Server.SetLocalRpcMethod("waitForShutdown", (Func<CancellationToken, Task>)(async cancellationToken =>
        {
            await handlerDispatched.Task;
            handlerStarted.SetResult(cancellationToken);
            await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken);
        }));
        pair.Server.SetLocalRpcMethod("handlerDispatched", (Action)(() => handlerDispatched.SetResult()));

        if (requestHasId)
        {
            await pair.SendClientFrameAsync(CreateRequestFrame(1, "waitForShutdown", "{}"));
        }
        else
        {
            await pair.SendClientFrameAsync(CreateNotificationFrame("waitForShutdown", "{}"));
        }
        // Keep the handler suspended until dispatch returns, before testing shutdown cancellation.
        await pair.SendClientFrameAsync(CreateNotificationFrame("handlerDispatched", "{}"));

        var handlerToken = await WaitForCancellationAsync(handlerStarted.Task);
        Assert.False(handlerToken.IsCancellationRequested);

        pair.Server.Dispose();

        await WaitForCancellationAsync(handlerToken);
    }

    [Fact]
    public async Task JsonRpc_Invokes_Task_Returning_Handler_To_Completion()
    {
        using var pair = JsonRpcReflectionPair.Create();
        pair.StartListening();

        var observed = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        pair.Server.SetLocalRpcMethod("voidAsync", (Func<string, int, CancellationToken, Task>)(async (name, count, cancellationToken) =>
        {
            await Task.Yield();
            observed.SetResult($"{name}:{count}");
        }));

        await pair.Client.InvokeAsync<object?>("voidAsync", ["Mona", 2]).WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal("Mona:2", await observed.Task.WaitAsync(TimeSpan.FromSeconds(5)));
    }

    [Fact]
    public async Task JsonRpc_Awaits_Declared_Task_Handler_With_Generic_Runtime_Task()
    {
        using var pair = JsonRpcReflectionPair.Create();
        pair.Server.SetLocalRpcMethod(
            "genericRuntimeTask",
            (Func<SingleObjectRequest, Task>)(_ => Task.FromResult(1)),
            singleObjectParam: true);
        pair.StartListening();

        Assert.Null(await pair.Client.InvokeAsync<object?>(
            "genericRuntimeTask",
            [new SingleObjectRequest { Value = "value" }]));
    }

    [Fact]
    public async Task JsonRpc_Dispose_Completes_Cleanup_When_Cancellation_Callback_Throws()
    {
        using var pair = JsonRpcReflectionPair.Create(startServer: false);
        using var registration = pair.Client.RegisterDisposeCallback(
            () => throw new InvalidOperationException("callback failed"));
        var pending = pair.Client.InvokeAsync<string>("stillPending", args: null);

        var exception = Assert.Throws<AggregateException>(() => pair.Client.Dispose());

        Assert.Contains(
            exception.InnerExceptions,
            inner => inner is InvalidOperationException { Message: "callback failed" });
        await Assert.ThrowsAnyAsync<ObjectDisposedException>(() => pending);
        Assert.True(pair.Client.Completion.IsCompleted);
        Assert.False(pair.Client.Completion.IsFaulted);
        Assert.False(pair.Client.Completion.IsCanceled);
        pair.Client.Dispose();
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task JsonRpc_Process_Exit_Reports_Connection_Lost(bool requestBeforeExit)
    {
        using var pair = JsonRpcReflectionPair.Create(startServer: false);
        await using var client = new CopilotClient();
        var pending = requestBeforeExit
            ? pair.Client.InvokeAsync<string>("pendingAtExit", args: null)
            : null;

        pair.Client.NotifyProcessExit(client);
        pending ??= pair.Client.InvokeAsync<string>("afterExit", args: null);

        var exception = await Assert.ThrowsAnyAsync<IOException>(() => pending);
        Assert.Equal("ConnectionLostException", exception.GetType().Name);
        Assert.Equal("The JSON-RPC connection was lost.", exception.Message);
        Assert.True(pair.Client.Completion.IsCompleted);
    }

    [Theory]
    [InlineData("""{"code":-32000,"message":"boom \ud83d\ude00"}""", typeof(RemoteRpcException))]
    [InlineData("""{"code":-32000,"message":"boom \ud83d"}""", typeof(InvalidOperationException))]
    [InlineData("""{"code":-32000,"message":42}""", typeof(InvalidOperationException))]
    [InlineData("""{"code":2147483648,"message":"boom"}""", typeof(FormatException))]
    [InlineData("\"boom\"", typeof(InvalidOperationException))]
    public async Task JsonRpc_Unreadable_Error_Response_Fails_Its_Request_And_Keeps_Reading(string error, Type expectedException)
    {
        var errorFrame = CreateFrame($$"""{"jsonrpc":"2.0","id":1,"error":{{error}}}""");
        var nextFrame = CreateResponseFrame(2, "carried");
        using var receiveStream = new MemoryStream(CombineFrames([errorFrame, nextFrame]));
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);

        var failed = rpc.InvokeAsync<string>("first", args: null);
        var next = rpc.InvokeAsync<string>("second", args: null);
        rpc.StartListening();

        var exception = await Assert.ThrowsAnyAsync<Exception>(() => failed.WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.IsType(expectedException, exception);
        Assert.Equal("carried", await next.WaitAsync(TimeSpan.FromSeconds(5)));
    }

    [Theory]
    [InlineData(1024 * 1024, 1, 0)]
    [InlineData(2 * 1024 * 1024, 128, 8192)]
    public async Task JsonRpc_Does_Not_Retain_Oversized_Receive_Buffer(int headerPaddingLength, int responseCount, int payloadLength)
    {
        var oversizedFrame = CreateResponseFrame(
            long.MaxValue,
            "ignored",
            headerPaddingLength: headerPaddingLength);
        var responses = Enumerable.Range(1, responseCount).Select(i => $"{i}:{new string('x', payloadLength)}").ToArray();
        var carriedFrames = CombineFrames(responses.Select((value, i) => CreateResponseFrame(i + 1, value)));
        using var receiveStream = new CoalescedFramesThenWaitStream(oversizedFrame, carriedFrames);
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);

        var carriedResponse = Task.WhenAll(responses.Select(_ => rpc.InvokeAsync<string>("pending", args: null)));
        rpc.StartListening();

        var responseCompleted = await Task.WhenAny(
            carriedResponse,
            Task.Delay(TimeSpan.FromSeconds(5), CancellationToken.None));
        Assert.Same(carriedResponse, responseCompleted);
        Assert.Equal(responses, await carriedResponse);
        Assert.True(receiveStream.FramesWereCoalesced);

        var readCompleted = await Task.WhenAny(
            receiveStream.PostFrameReadBufferSize,
            Task.Delay(TimeSpan.FromSeconds(5), CancellationToken.None));
        Assert.Same(receiveStream.PostFrameReadBufferSize, readCompleted);
        Assert.InRange(await receiveStream.PostFrameReadBufferSize, 1, 1024 * 1024);
    }

    [Theory]
    [InlineData("null")]
    [InlineData("\"server-id\"")]
    public async Task JsonRpc_Invalid_Response_Id_Does_Not_End_Read_Loop(string invalidId)
    {
        var invalidFrame = CreateFrame(
            $$"""{"jsonrpc":"2.0","id":{{invalidId}},"result":"ignored"}""");
        var validFrame = CreateResponseFrame(1, "carried");
        using var receiveStream = new MemoryStream(CombineFrames([invalidFrame, validFrame]));
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);

        var response = rpc.InvokeAsync<string>("pending", args: null);
        rpc.StartListening();

        Assert.Equal("carried", await response.WaitAsync(TimeSpan.FromSeconds(5)));
        await rpc.Completion.WaitAsync(TimeSpan.FromSeconds(5));
    }

    [Fact]
    public async Task JsonRpc_JsonElement_Params_Remain_Valid_After_Message_Disposal()
    {
        var payloads = new[]
        {
            "null",
            "false",
            "42",
            "\"text\"",
            """[1,{"nested":[null,true]}]""",
            """{"result":{"rows":[{"content":"preserved"}]}}""",
        };
        using var receiveStream = new MemoryStream(CombineFrames(
            payloads.Select(payload => CreateNotificationFrame("payload", $$"""{"payload":{{payload}}}"""))));
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);
        var collector = new JsonElementCollector(payloads.Length);
        rpc.SetLocalRpcMethod("payload", (Action<JsonElement?>)collector.Handle);

        rpc.StartListening();
        await collector.Completion.WaitAsync(TimeSpan.FromSeconds(5));
        await rpc.Completion.WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal(payloads, collector.Payloads.Select(payload => payload?.GetRawText() ?? "null"));
    }

    [Theory]
    [InlineData(7)]
    [InlineData(int.MaxValue)]
    public async Task JsonRpc_Consumes_Coalesced_Frames_After_Invalid_Json_And_Buffer_Compaction(int chunkSize)
    {
        var frames = new List<byte[]>
        {
            CreateResponseFrame(0, "ignored", headerPaddingLength: 4096),
            CreateFrame("{invalid"),
        };
        frames.AddRange(Enumerable.Range(0, 100).Select(i => CreateNotificationFrame("payload", $$"""{"payload":{{i}}}""")));
        frames.Add(CreateNotificationFrame("payload", $$"""{"payload":"{{new string('x', 8192)}}"}"""));
        frames.Add(CreateNotificationFrame("payload", """{"payload":"last"}"""));
        using var receiveStream = new FragmentedReadStream(CombineFrames(frames), chunkSize);
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);
        var collector = new JsonElementCollector(102);
        rpc.SetLocalRpcMethod("payload", (Action<JsonElement?>)collector.Handle);

        rpc.StartListening();
        await rpc.Completion.WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal(102, collector.Payloads.Count);
        Assert.Equal(Enumerable.Range(0, 100), collector.Payloads.Take(100).Select(p => p!.Value.GetInt32()));
        Assert.Equal(new string('x', 8192), collector.Payloads[100]!.Value.GetString());
        Assert.Equal("last", collector.Payloads[101]!.Value.GetString());
    }

    [Theory]
    [InlineData("Content-Len")]
    [InlineData("Content-Length: 100\r\n\r\n{}")]
    public async Task JsonRpc_Truncated_Frame_Closes_Connection(string frame)
    {
        using var receiveStream = new FragmentedReadStream(Encoding.ASCII.GetBytes(frame), 7);
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);
        var pending = rpc.InvokeAsync<JsonElement>("pending", args: null);

        rpc.StartListening();

        var error = await Assert.ThrowsAnyAsync<IOException>(() => pending.WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.Equal("ConnectionLostException", error.GetType().Name);
        await rpc.Completion.WaitAsync(TimeSpan.FromSeconds(5));
    }

    [Fact]
    public async Task JsonRpc_Malformed_Session_Event_Does_Not_Block_Unknown_Or_Known_Events()
    {
        var malformed = CreateNotificationFrame(
            "session.event",
            """{"sessionId":"session-1","event":42}""");
        var malformedDiscriminator = CreateNotificationFrame(
            "session.event",
            """{"sessionId":"session-1","event":{"type":false}}""");
        var duplicateDiscriminator = CreateNotificationFrame(
            "session.event",
            """{"sessionId":"session-1","event":{"type":"session.idle","data":{},"type":"session.idle"}}""");
        var unexpectedMetadata = CreateNotificationFrame(
            "session.event",
            """{"sessionId":"session-1","event":{"type":"future.event","$id":"1"}}""");
        var unknown = CreateNotificationFrame(
            "session.event",
            """
            {
                "sessionId":"session-1",
                "event":{
                    "id":"11111111-1111-1111-1111-111111111111",
                    "timestamp":"2026-09-19T00:00:00Z",
                    "parentId":null,
                    "type":"future.event",
                    "data":{"nested":[null,true,{"value":42}]}
                }
            }
            """);
        var known = CreateNotificationFrame(
            "session.event",
            """
            {
                "sessionId":"session-1",
                "event":{
                    "id":"22222222-2222-2222-2222-222222222222",
                    "timestamp":"2026-09-19T00:00:01Z",
                    "parentId":null,
                    "type":"tool.execution_start",
                    "data":{
                        "toolCallId":"tool-1",
                        "toolName":"view",
                        "arguments":{"path":"README.md","nested":[1,{"value":true}]}
                    }
                }
            }
            """);

        using var receiveStream = new MemoryStream(CombineFrames(
            [malformed, malformedDiscriminator, duplicateDiscriminator, unexpectedMetadata, unknown, known]));
        using var rpc = new JsonRpcReflection(Stream.Null, receiveStream);
        var collector = new SessionEventCollector(expectedCount: 2);
        rpc.SetLocalRpcMethod("session.event", (Action<string, SessionEvent?>)collector.Handle);

        rpc.StartListening();
        await collector.Completion.WaitAsync(TimeSpan.FromSeconds(5));
        await rpc.Completion.WaitAsync(TimeSpan.FromSeconds(5));

        Assert.All(collector.SessionIds, sessionId => Assert.Equal("session-1", sessionId));
        var unknownEvent = Assert.IsType<SessionEvent>(collector.Events[0]);
        Assert.Equal("unknown", unknownEvent.Type);

        var toolEvent = Assert.IsType<ToolExecutionStartEvent>(collector.Events[1]);
        Assert.Equal("README.md", toolEvent.Data.Arguments?.GetProperty("path").GetString());
        Assert.True(toolEvent.Data.Arguments?.GetProperty("nested")[1].GetProperty("value").GetBoolean());
    }

    private static byte[] CreateResponseFrame(long id, string result, int headerPaddingLength = 0)
    {
        using var bodyStream = new MemoryStream();
        using (var writer = new Utf8JsonWriter(bodyStream))
        {
            writer.WriteStartObject();
            writer.WriteString("jsonrpc", "2.0");
            writer.WriteNumber("id", id);
            writer.WriteString("result", result);
            writer.WriteEndObject();
        }

        var body = bodyStream.ToArray();
        var paddingHeader = headerPaddingLength > 0
            ? $"X-Padding: {new string('x', headerPaddingLength)}\r\n"
            : string.Empty;
        var header = Encoding.ASCII.GetBytes($"{paddingHeader}Content-Length: {body.Length}\r\n\r\n");
        var frame = new byte[header.Length + body.Length];
        header.CopyTo(frame, 0);
        body.CopyTo(frame, header.Length);
        return frame;
    }

    private static byte[] CreateNotificationFrame(string method, string paramsJson)
        => CreateFrame($$"""{"jsonrpc":"2.0","method":"{{method}}","params":{{paramsJson}}}""");

    private static byte[] CreateRequestFrame(long id, string method, string paramsJson)
        => CreateFrame($$"""{"jsonrpc":"2.0","id":{{id}},"method":"{{method}}","params":{{paramsJson}}}""");

    private static byte[] CreateFrame(string json)
    {
        var body = Encoding.UTF8.GetBytes(json);
        var header = Encoding.ASCII.GetBytes($"Content-Length: {body.Length}\r\n\r\n");
        var frame = new byte[header.Length + body.Length];
        header.CopyTo(frame, 0);
        body.CopyTo(frame, header.Length);
        return frame;
    }

    private static byte[] CombineFrames(IEnumerable<byte[]> frames)
    {
        using var stream = new MemoryStream();
        foreach (var frame in frames)
        {
            stream.Write(frame);
        }
        return stream.ToArray();
    }

    private static async Task WaitForCancellationAsync(CancellationToken token)
    {
        if (token.IsCancellationRequested)
        {
            return;
        }

        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var registration = token.Register(
            static state => ((TaskCompletionSource)state!).TrySetResult(),
            cancelled);
        var completed = await Task.WhenAny(cancelled.Task, Task.Delay(TimeSpan.FromSeconds(5), CancellationToken.None));
        Assert.Same(cancelled.Task, completed);
    }

    private static async Task<T> WaitForCancellationAsync<T>(Task<T> task)
    {
        var completed = await Task.WhenAny(task, Task.Delay(TimeSpan.FromSeconds(5), CancellationToken.None));
        Assert.Same(task, completed);
        return await task;
    }

    private static int GetRemoteErrorCode(Exception exception)
    {
        var property = exception.GetType().GetProperty("ErrorCode", BindingFlags.Instance | BindingFlags.Public);
        Assert.NotNull(property);
        return (int)property.GetValue(exception)!;
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

        public async Task SendClientFrameAsync(byte[] frame)
        {
            await _clientStream.WriteAsync(frame.AsMemory());
            await _clientStream.FlushAsync();
        }

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

        private static readonly JsonSerializerOptions SerializerOptions = CreateSerializerOptions();

        private static JsonSerializerOptions CreateSerializerOptions()
        {
            var property = typeof(CopilotClient).GetProperty(
                "SerializerOptionsForMessageFormatter",
                BindingFlags.Static | BindingFlags.NonPublic)!;
            var options = new JsonSerializerOptions((JsonSerializerOptions)property.GetValue(null)!);
            options.TypeInfoResolverChain.Add(new DefaultJsonTypeInfoResolver());
            return options;
        }

        private readonly object _instance;

        public JsonRpcReflection(Stream stream)
            : this(stream, stream)
        {
        }

        public JsonRpcReflection(Stream sendStream, Stream receiveStream)
        {
            _instance = Activator.CreateInstance(
                JsonRpcType,
                BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic,
                binder: null,
                args: [sendStream, receiveStream, SerializerOptions, null],
                culture: null)!;
        }

        public Task Completion => (Task)JsonRpcType.GetProperty(nameof(Completion))!.GetValue(_instance)!;

        public void StartListening() => JsonRpcType.GetMethod(nameof(StartListening))!.Invoke(_instance, null);

        public void SetLocalRpcMethod(string methodName, Delegate handler, bool singleObjectParam = false) =>
            JsonRpcType.GetMethod("SetLocalRpcMethod")!.Invoke(_instance, [methodName, handler, singleObjectParam]);

        public CancellationTokenRegistration RegisterDisposeCallback(Action callback)
        {
            var disposeCts = (CancellationTokenSource)JsonRpcType
                .GetField("_disposeCts", BindingFlags.Instance | BindingFlags.NonPublic)!
                .GetValue(_instance)!;
            return disposeCts.Token.Register(callback);
        }

        public async Task<T> InvokeAsync<T>(string methodName, object?[]? args, CancellationToken cancellationToken = default)
        {
            var method = JsonRpcType
                .GetMethod("InvokeAsync")!
                .MakeGenericMethod(typeof(T));

            // Pass null for the optional onResponseInline parameter.
            var task = (Task<T>)method.Invoke(_instance, [methodName, args, cancellationToken, null])!;
            return await task.ConfigureAwait(false);
        }

        public void Dispose() => ((IDisposable)_instance).Dispose();

        public void Dispose(Exception reason) =>
            JsonRpcType.GetMethod("Dispose", BindingFlags.Instance | BindingFlags.NonPublic)!
                .Invoke(_instance, [reason]);

        public void NotifyProcessExit(CopilotClient client) =>
            typeof(CopilotClient)
                .GetMethod("DisposeRpcAfterProcessExit", BindingFlags.Instance | BindingFlags.NonPublic)!
                .Invoke(client, [_instance]);
    }

    private sealed class JsonElementCollector(int expectedCount)
    {
        private readonly TaskCompletionSource _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public List<JsonElement?> Payloads { get; } = [];

        public Task Completion => _completion.Task;

        public void Handle(JsonElement? payload)
        {
            Payloads.Add(payload);
            if (Payloads.Count == expectedCount)
            {
                _completion.TrySetResult();
            }
        }
    }

    private sealed class SessionEventCollector(int expectedCount)
    {
        private readonly TaskCompletionSource _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public List<SessionEvent?> Events { get; } = [];

        public List<string> SessionIds { get; } = [];

        public Task Completion => _completion.Task;

        public void Handle(string sessionId, SessionEvent? @event)
        {
            SessionIds.Add(sessionId);
            Events.Add(@event);
            if (Events.Count == expectedCount)
            {
                _completion.TrySetResult();
            }
        }
    }

    private sealed class GatedSendStream(bool blockFlush = false, Exception? sendError = null) : MemoryStream
    {
        private readonly TaskCompletionSource _blocked = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _release = new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task Blocked => _blocked.Task;

        public void Unblock() => _release.TrySetResult();

        public override Task WriteAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            WriteCoreAsync(buffer.AsMemory(offset, count), cancellationToken).AsTask();

#if NET8_0_OR_GREATER
        public override
#else
        internal
#endif
        ValueTask WriteAsync(ReadOnlyMemory<byte> source, CancellationToken cancellationToken = default) =>
            WriteCoreAsync(source, cancellationToken);

        private async ValueTask WriteCoreAsync(ReadOnlyMemory<byte> source, CancellationToken cancellationToken)
        {
            Assert.False(cancellationToken.CanBeCanceled);
            if (!blockFlush)
            {
                await BlockAsync();
            }

            var bytes = source.ToArray();
            base.Write(bytes, 0, bytes.Length);
        }

        public override Task FlushAsync(CancellationToken cancellationToken)
        {
            Assert.False(cancellationToken.CanBeCanceled);
            return blockFlush ? BlockAsync() : Task.CompletedTask;
        }

        private async Task BlockAsync()
        {
            _blocked.TrySetResult();
            await _release.Task;
            if (sendError is not null)
            {
                throw sendError;
            }
        }
    }

    private sealed class FragmentedReadStream(byte[] bytes, int chunkSize) : MemoryStream(bytes)
    {
        public override Task<int> ReadAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            base.ReadAsync(buffer, offset, Math.Min(count, chunkSize), cancellationToken);

#if NET8_0_OR_GREATER
        public override ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken = default) =>
            base.ReadAsync(buffer.Slice(0, Math.Min(buffer.Length, chunkSize)), cancellationToken);
#endif
    }

    private sealed class CoalescedFramesThenWaitStream : Stream
    {
        private readonly TaskCompletionSource<int> _postFrameReadBufferSize =
            new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly byte[] _frames;
        private readonly int _firstFrameLength;
        private int _offset;

        public CoalescedFramesThenWaitStream(byte[] firstFrame, byte[] secondFrame)
        {
            _firstFrameLength = firstFrame.Length;
            _frames = new byte[firstFrame.Length + secondFrame.Length];
            firstFrame.CopyTo(_frames, 0);
            secondFrame.CopyTo(_frames, firstFrame.Length);
        }

        public bool FramesWereCoalesced { get; private set; }

        public Task<int> PostFrameReadBufferSize => _postFrameReadBufferSize.Task;

        public override bool CanRead => true;

        public override bool CanSeek => false;

        public override bool CanWrite => false;

        public override long Length => throw new NotSupportedException();

        public override long Position { get => throw new NotSupportedException(); set => throw new NotSupportedException(); }

        public override int Read(byte[] buffer, int offset, int count) => throw new NotSupportedException();

        public override Task<int> ReadAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken) =>
            ReadCoreAsync(buffer.AsMemory(offset, count), cancellationToken).AsTask();

#if NET8_0_OR_GREATER
        public override
#else
        internal
#endif
        ValueTask<int> ReadAsync(Memory<byte> destination, CancellationToken cancellationToken = default) =>
            ReadCoreAsync(destination, cancellationToken);

        public override void Flush()
        {
        }

        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();

        public override void SetLength(long value) => throw new NotSupportedException();

        public override void Write(byte[] buffer, int offset, int count) => throw new NotSupportedException();

        private ValueTask<int> ReadCoreAsync(Memory<byte> destination, CancellationToken cancellationToken)
        {
            if (_offset >= _frames.Length)
            {
                _postFrameReadBufferSize.TrySetResult(destination.Length);
                return new ValueTask<int>(WaitForCancellationAsync(cancellationToken));
            }

            var startingOffset = _offset;
            var bytesRead = Math.Min(destination.Length, _frames.Length - _offset);
            _frames.AsMemory(_offset, bytesRead).CopyTo(destination);
            _offset += bytesRead;
            FramesWereCoalesced |= startingOffset < _firstFrameLength && _offset == _frames.Length;
            return new ValueTask<int>(bytesRead);
        }

        private static async Task<int> WaitForCancellationAsync(CancellationToken cancellationToken)
        {
            await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken).ConfigureAwait(false);
            return 0;
        }
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
