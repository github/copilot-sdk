/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Text;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot;

/// <summary>Application-owned transport for opaque AHP JSON text. The application owns listening and authentication.</summary>
public interface IAhpTransport
{
    /// <summary>Sends one complete text message. Honor cancellation when possible.</summary>
    Task SendAsync(string message, CancellationToken cancellationToken);

    /// <summary>Closes the physical transport, optionally because of an error.</summary>
    Task CloseAsync(Exception? error = null);
}

public sealed partial class CopilotClient
{
    private readonly ConcurrentDictionary<string, AhpEndpoint> _ahpEndpoints = new();

    /// <summary>Creates a runtime-owned AHP endpoint without creating an HTTP or WebSocket listener.</summary>
    /// <remarks>
    /// Authenticate callers before accepting connections. AHP exposes all live local sessions in the runtime engine,
    /// not only this client's sessions. Client ownership isolates endpoint lifecycle, not session visibility.
    /// </remarks>
    public async Task<AhpEndpoint> CreateAhpEndpointAsync(CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken).ConfigureAwait(false);
        var endpoint = new AhpEndpoint(connection.Rpc, id => _ahpEndpoints.TryRemove(id, out _), _logger);
        _ahpEndpoints[endpoint.Id] = endpoint;
        if (_disposed || connection.Rpc.Completion.IsCompleted)
        {
            endpoint.Retire(new IOException("AHP runtime connection closed"));
        }
        await endpoint.InitializeAsync(cancellationToken).ConfigureAwait(false);
        return endpoint;
    }

    private void RegisterAhpHandlers(JsonRpc rpc)
    {
        rpc.SetLocalRpcMethod("ahpTransport.send",
            (Func<AhpSendRequest, ValueTask<AhpEmptyResult>>)SendAhpAsync, singleObjectParam: true);
        rpc.SetLocalRpcMethod("ahpTransport.closed",
            (Action<AhpClosedNotification>)CloseAhp, singleObjectParam: true);
    }

    private async ValueTask<AhpEmptyResult> SendAhpAsync(AhpSendRequest request)
    {
        if (!_ahpEndpoints.TryGetValue(request.EndpointId, out var endpoint) ||
            !endpoint.Connections.TryGetValue(request.ConnectionId, out var connection))
        {
            throw new InvalidOperationException("Unknown AHP connection");
        }
        try
        {
            await connection.SendAsync(request.Message).ConfigureAwait(false);
        }
        catch (OperationCanceledException error)
        {
            // Logical AHP cancellation must reply with an error, not cancel the outer RPC dispatch.
            throw new IOException("AHP transport send was canceled", error);
        }
        return new();
    }

    private void CloseAhp(AhpClosedNotification notification)
    {
        if (_ahpEndpoints.TryGetValue(notification.EndpointId, out var endpoint) &&
            endpoint.Connections.TryGetValue(notification.ConnectionId, out var connection))
        {
            connection.Retire(notification.Error is null ? null : new IOException(notification.Error));
        }
    }

    private void RetireAhpEndpoints(JsonRpc? rpc = null)
    {
        foreach (var endpoint in _ahpEndpoints.Values)
        {
            if (rpc is null || ReferenceEquals(endpoint.Rpc, rpc))
            {
                endpoint.Retire(new IOException("AHP runtime connection closed"));
            }
        }

    }

    internal static bool IsRecoverableAhpFailure(Exception exception) =>
        IsRecoverableConnectionCleanupFailure(exception);
}

/// <summary>An AHP endpoint whose listener, authentication, and transport framing belong to the application.</summary>
public sealed class AhpEndpoint : IAsyncDisposable
{
    internal static readonly TimeSpan Deadline = TimeSpan.FromSeconds(10);
    internal readonly ConcurrentDictionary<string, AhpConnection> Connections = new();
    internal readonly string Id = Guid.NewGuid().ToString();
    internal readonly JsonRpc Rpc;
    internal readonly ILogger Logger;
    private readonly object _gate = new();
    private readonly CancellationTokenSource _lifetime = new();
    private Action<string>? _remove;
    private bool _active = true;
    private Task? _disposal;

    internal AhpEndpoint(JsonRpc rpc, Action<string> remove, ILogger logger)
    {
        Rpc = rpc;
        _remove = remove;
        Logger = logger;
    }

    internal async Task InitializeAsync(CancellationToken cancellationToken)
    {
        using var linked = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, _lifetime.Token);
        try
        {
            linked.Token.ThrowIfCancellationRequested();
            var creation = RequestAsync("ahp.createEndpoint", cancellationToken: CancellationToken.None);
            _ = CompensateAsync(creation, Rpc, Id, null, Logger, _lifetime.Token);
            await creation.WaitAsync(Deadline, linked.Token).ConfigureAwait(false);
        }
        catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error))
        {
            Retire(error);
            _ = IgnoreDisposalFailureAsync(Rpc, Id, Logger);
            throw;
        }
    }

    private static async Task IgnoreDisposalFailureAsync(JsonRpc rpc, string endpointId, ILogger logger)
    {
        try
        {
            using var timeout = new CancellationTokenSource(Deadline);
            await rpc.InvokeAsync<AhpEmptyResult>("ahp.disposeEndpoint",
                [new AhpRequest { EndpointId = endpointId }], timeout.Token)
                .WaitAsync(Deadline, timeout.Token).ConfigureAwait(false);
        }
        catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error))
        {
            if (logger.IsEnabled(LogLevel.Debug))
                logger.LogDebug(error, "Failed to dispose AHP endpoint {EndpointId} after unsuccessful creation", endpointId);
        }
    }

    internal Task<AhpEmptyResult> RequestAsync(string method, string? connectionId = null,
        string? message = null, CancellationToken cancellationToken = default) =>
        Rpc.InvokeAsync<AhpEmptyResult>(method,
            [new AhpRequest { EndpointId = Id, ConnectionId = connectionId, Message = message }], cancellationToken);

    // The late-ack observer holds only wire identifiers and a token, not endpoint/transport registries.
    internal static async Task CompensateAsync(Task admission, JsonRpc rpc, string endpointId,
        string? connectionId, ILogger logger, CancellationToken lifetime)
    {
        try
        {
            await admission.ConfigureAwait(false);
            if (lifetime.IsCancellationRequested)
            {
                using var timeout = new CancellationTokenSource(Deadline);
                await rpc.InvokeAsync<AhpEmptyResult>(
                    connectionId is null ? "ahp.disposeEndpoint" : "ahp.closeConnection",
                    [new AhpRequest { EndpointId = endpointId, ConnectionId = connectionId }],
                    timeout.Token).WaitAsync(Deadline, timeout.Token).ConfigureAwait(false);
            }
        }
        catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error))
        {
            if (logger.IsEnabled(LogLevel.Debug))
                logger.LogDebug(error, "AHP admission or compensating cleanup failed for endpoint {EndpointId}, connection {ConnectionId}",
                    endpointId, connectionId);
        }
    }

    /// <summary>Accepts a physical connection synchronously; messages may arrive before runtime admission completes.</summary>
    public AhpConnection AcceptConnection(IAhpTransport transport)
    {
        ArgumentNullException.ThrowIfNull(transport);
        lock (_gate)
        {
            ObjectDisposedException.ThrowIf(!_active, this);
            var connection = new AhpConnection(this, transport);
            Connections[connection.Id] = connection;
            connection.Open();
            return connection;
        }
    }

    internal void Retire(Exception? error = null)
    {
        AhpConnection[] connections;
        lock (_gate)
        {
            if (!_active) return;
            _active = false;
            _remove?.Invoke(Id);
            _remove = null;
            connections = Connections.Values.ToArray();
            Connections.Clear();
        }
        _lifetime.Cancel();
        foreach (var connection in connections) connection.Retire(error);
    }

    /// <summary>Releases local connections immediately, then disposes the runtime endpoint with a bounded deadline.</summary>
    public ValueTask DisposeAsync()
    {
        lock (_gate)
        {
            if (_disposal is not null) return new(_disposal);
            if (!_active) return default;
            Retire();
            _disposal = DisposeRemoteAsync();
            return new(_disposal);
        }
    }

    private async Task DisposeRemoteAsync()
    {
        using var timeout = new CancellationTokenSource(Deadline);
        await RequestAsync("ahp.disposeEndpoint", cancellationToken: timeout.Token)
            .WaitAsync(Deadline, timeout.Token).ConfigureAwait(false);
    }
}

/// <summary>One physical AHP connection. Messages are opaque JSON text interpreted only by the runtime.</summary>
public sealed class AhpConnection : IAsyncDisposable
{
    private const int MaxBytes = 8 * 1024 * 1024;
    private const int MaxMessages = 64;
    private static readonly UTF8Encoding s_utf8 = new(false, true);
    private readonly object _gate = new();
    private readonly AhpEndpoint _endpoint;
    private readonly CancellationTokenSource _lifetime = new();
    private readonly TaskCompletionSource _closed = new(TaskCreationOptions.RunContinuationsAsynchronously);
    private IAhpTransport? _transport;
    private Task _opening = Task.CompletedTask;
    private Task _inbound = Task.CompletedTask;
    private Task _outbound = Task.CompletedTask;
    private Task _closeWork = Task.CompletedTask;
    private Task? _ending;
    private int _incomingBytes, _incomingMessages, _outgoingBytes, _outgoingMessages;
    private MemoryStream? _chunks;
    internal readonly string Id = Guid.NewGuid().ToString();

    internal AhpConnection(AhpEndpoint endpoint, IAhpTransport transport)
    {
        _endpoint = endpoint;
        _transport = transport;
        _ = ObserveAsync(_closed.Task, endpoint.Logger, "connection closure");
    }

    /// <summary>Completes when locally closed; faults on transport, protocol, or runtime connection failure.</summary>
    public Task Closed => _closed.Task;

    internal void Open()
    {
        _opening = _endpoint.RequestAsync("ahp.openConnection", Id);
        _ = AhpEndpoint.CompensateAsync(_opening, _endpoint.Rpc, _endpoint.Id, Id, _endpoint.Logger, _lifetime.Token);
        _ = ObserveOpeningAsync();
    }

    private async Task ObserveOpeningAsync()
    {
        try { await _opening.WaitAsync(AhpEndpoint.Deadline, _lifetime.Token).ConfigureAwait(false); }
        catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error)) { Fail(error); }
    }

    private void AssertActive()
    {
        ObjectDisposedException.ThrowIf(_transport is null, this);
    }

    /// <summary>Admits one complete text message, not waiting for completion of the AHP operation.</summary>
    public Task ReceiveAsync(string message, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(message);
        lock (_gate)
        {
            AssertActive();
            if (_chunks is not null) throw new InvalidOperationException("An AHP fragmented message is still in progress");
            return Enqueue(message, outbound: false, cancellationToken);
        }
    }

    /// <summary>Accepts WebSocket text fragments, preserving UTF-8 characters across fragment boundaries.</summary>
    public Task ReceiveChunkAsync(ReadOnlyMemory<byte> bytes, bool endOfMessage, CancellationToken cancellationToken = default)
    {
        lock (_gate)
        {
            AssertActive();
            cancellationToken.ThrowIfCancellationRequested();
            if (_incomingBytes + (_chunks?.Length ?? 0) + bytes.Length > MaxBytes)
            {
                var error = new IOException("AHP incoming messages exceed the 8 MiB limit");
                Fail(error);
                return Task.FromException(error);
            }
            _chunks ??= new MemoryStream();
            _chunks.Write(bytes.Span);
            if (!endOfMessage) return Task.CompletedTask;
            try
            {
                var message = s_utf8.GetString(_chunks.GetBuffer(), 0, checked((int)_chunks.Length));
                _chunks.Dispose();
                _chunks = null;
                return Enqueue(message, outbound: false, cancellationToken);
            }
            catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error))
            {
                Fail(error);
                return Task.FromException(error);
            }
        }
    }

    internal Task SendAsync(string message)
    {
        lock (_gate)
        {
            AssertActive();
            return Enqueue(message, outbound: true, CancellationToken.None);
        }
    }

    private Task Enqueue(string message, bool outbound, CancellationToken cancellationToken)
    {
        int bytes;
        try { bytes = s_utf8.GetByteCount(message); }
        catch (EncoderFallbackException error)
        {
            Fail(error);
            return Task.FromException(error);
        }
        var pendingBytes = outbound ? _outgoingBytes : _incomingBytes;
        var pendingMessages = outbound ? _outgoingMessages : _incomingMessages;
        if (pendingBytes + (long)bytes > MaxBytes || pendingMessages >= MaxMessages)
        {
            var error = new IOException("AHP messages exceed the 8 MiB / 64 pending message limit");
            Fail(error);
            return Task.FromException(error);
        }
        if (outbound) { _outgoingBytes += bytes; _outgoingMessages++; }
        else { _incomingBytes += bytes; _incomingMessages++; }
        var prior = outbound ? _outbound : _inbound;
        var work = RunQueuedAsync(prior, message, bytes, outbound, cancellationToken);
        if (outbound) _outbound = ObserveAsync(work, _endpoint.Logger, "outgoing message");
        else _inbound = ObserveAsync(work, _endpoint.Logger, "incoming message");
        return work;
    }

    private async Task RunQueuedAsync(Task prior, string message, int bytes, bool outbound, CancellationToken cancellationToken)
    {
        // Never call application callbacks while holding the queue lock or on the RPC reader.
        await Task.Yield();
        using var linked = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, _lifetime.Token);
        linked.CancelAfter(AhpEndpoint.Deadline);
        try
        {
            await prior.WaitAsync(AhpEndpoint.Deadline, linked.Token).ConfigureAwait(false);
            linked.Token.ThrowIfCancellationRequested();
            if (outbound)
            {
                IAhpTransport transport;
                lock (_gate)
                {
                    AssertActive();
                    transport = _transport!;
                }
                await InvokeSendAsync(transport, message, _endpoint.Logger, linked.Token)
                    .WaitAsync(AhpEndpoint.Deadline, linked.Token).ConfigureAwait(false);
            }
            else
            {
                await _opening.WaitAsync(AhpEndpoint.Deadline, linked.Token).ConfigureAwait(false);
                linked.Token.ThrowIfCancellationRequested();
                await _endpoint.RequestAsync("ahp.receive", Id, message, linked.Token)
                    .WaitAsync(AhpEndpoint.Deadline, linked.Token).ConfigureAwait(false);
            }
        }
        catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error))
        {
            Fail(error);
            throw;
        }
        finally
        {
            lock (_gate)
            {
                if (outbound) { _outgoingBytes -= bytes; _outgoingMessages--; }
                else { _incomingBytes -= bytes; _incomingMessages--; }
            }
        }
    }

    // These isolated closures must not capture the connection/client: an arbitrary callback can hang forever.
    private static async Task InvokeSendAsync(IAhpTransport transport, string message, ILogger logger, CancellationToken token)
    {
        var callbackCancellation = new CancellationTokenSource();
        // User cancellation handlers can block too; they never run on the SDK's lifetime cancellation path.
        using var registration = token.Register(() =>
            _ = ObserveAsync(Task.Run(callbackCancellation.Cancel, CancellationToken.None), logger, "send cancellation callback"));
        try
        {
            await Task.Run(() => transport.SendAsync(message, callbackCancellation.Token), CancellationToken.None)
                .WaitAsync(AhpEndpoint.Deadline, token).ConfigureAwait(false);
        }
        finally
        {
            // Do not dispose the source while a user cancellation handler may still be running.
            _ = ObserveAsync(Task.Run(callbackCancellation.Cancel, CancellationToken.None), logger, "send cancellation callback");
        }
    }

    private static async Task InvokeCloseAsync(IAhpTransport transport, Exception? error)
    {
        await Task.Run(() => transport.CloseAsync(error)).WaitAsync(AhpEndpoint.Deadline).ConfigureAwait(false);
    }

    // Observing a task does not replace its original fault: message/close callers and Closed still receive it.
    private static async Task ObserveAsync(Task task, ILogger logger, string operation)
    {
        try { await task.ConfigureAwait(false); }
        catch (Exception error) when (CopilotClient.IsRecoverableAhpFailure(error))
        {
            if (logger.IsEnabled(LogLevel.Debug))
                logger.LogDebug(error, "AHP {Operation} failed", operation);
        }
    }

    internal void Retire(Exception? error = null)
    {
        lock (_gate)
        {
            if (_transport is null) return;
            var transport = _transport;
            _transport = null;
            _endpoint.Connections.TryRemove(Id, out _);
            _chunks?.Dispose();
            _chunks = null;
            _lifetime.Cancel();
            _closeWork = InvokeCloseAsync(transport, error);
            _ = ObserveAsync(_closeWork, _endpoint.Logger, "transport close");
            if (error is null) _closed.TrySetResult();
            else _closed.TrySetException(error);
        }
    }

    private void Fail(Exception error)
    {
        lock (_gate)
        {
            if (_transport is null) return;
            Retire(error);
            _ = ObserveAsync(EndAsync(), _endpoint.Logger, "connection cleanup");
        }
    }

    /// <summary>Ends this connection, releasing local state before bounded runtime and transport cleanup.</summary>
    public Task EndAsync()
    {
        lock (_gate)
        {
            Retire();
            return _ending ??= EndRemoteAsync();
        }
    }

    private async Task EndRemoteAsync()
    {
        using var timeout = new CancellationTokenSource(AhpEndpoint.Deadline);
        await Task.WhenAll(_endpoint.RequestAsync("ahp.closeConnection", Id, cancellationToken: timeout.Token)
                .WaitAsync(AhpEndpoint.Deadline, timeout.Token),
            _closeWork).ConfigureAwait(false);
    }

    /// <inheritdoc />
    public ValueTask DisposeAsync() => new(EndAsync());
}

// Schema-native, source-generated serialization without any dependency on an HTTP server package.
internal sealed class AhpRequest
{
    public required string EndpointId { get; init; }
    public string? ConnectionId { get; init; }
    public string? Message { get; init; }
}

internal sealed class AhpSendRequest
{
    public required string EndpointId { get; init; }
    public required string ConnectionId { get; init; }
    public required string Message { get; init; }
}

internal sealed class AhpClosedNotification
{
    public required string EndpointId { get; init; }
    public required string ConnectionId { get; init; }
    public string? Error { get; init; }
}

internal sealed class AhpEmptyResult;

[JsonSourceGenerationOptions(PropertyNamingPolicy = JsonKnownNamingPolicy.CamelCase,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AhpRequest))]
[JsonSerializable(typeof(AhpSendRequest))]
[JsonSerializable(typeof(AhpClosedNotification))]
[JsonSerializable(typeof(AhpEmptyResult))]
internal partial class AhpJsonContext : JsonSerializerContext;
