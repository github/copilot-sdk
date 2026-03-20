/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;
using StreamJsonRpc;
using System.Collections.Immutable;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using System.Threading.Channels;

namespace GitHub.Copilot.SDK;

/// <summary>
/// Represents a single conversation session with the Copilot CLI.
/// </summary>
/// <remarks>
/// <para>
/// A session maintains conversation state, handles events, and manages tool execution.
/// Sessions are created via <see cref="CopilotClient.CreateSessionAsync"/> or resumed via
/// <see cref="CopilotClient.ResumeSessionAsync"/>.
/// </para>
/// <para>
/// The session provides methods to send messages, subscribe to events, retrieve
/// conversation history, and manage the session lifecycle.
/// </para>
/// <para>
/// <see cref="CopilotSession"/> implements <see cref="IAsyncDisposable"/>. Use the
/// <c>await using</c> pattern for automatic cleanup, or call <see cref="DisposeAsync"/>
/// explicitly. Disposing a session releases in-memory resources but preserves session data
/// on disk — the conversation can be resumed later via
/// <see cref="CopilotClient.ResumeSessionAsync"/>. To permanently delete session data,
/// use <see cref="CopilotClient.DeleteSessionAsync"/>.
/// </para>
/// </remarks>
/// <example>
/// <code>
/// await using var session = await client.CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll, Model = "gpt-4" });
///
/// // Subscribe to events
/// using var subscription = session.On(evt =>
/// {
///     if (evt is AssistantMessageEvent assistantMessage)
///     {
///         Console.WriteLine($"Assistant: {assistantMessage.Data?.Content}");
///     }
/// });
///
/// // Send a message and wait for completion
/// await session.SendAndWaitAsync(new MessageOptions { Prompt = "Hello, world!" });
/// </code>
/// </example>
public sealed partial class CopilotSession : IAsyncDisposable
{
    private readonly Dictionary<string, AIFunction> _toolHandlers = [];
    private readonly JsonRpc _rpc;
    private readonly ILogger _logger;

    private volatile PermissionRequestHandler? _permissionHandler;
    private volatile UserInputHandler? _userInputHandler;
    private ImmutableArray<SessionEventHandler> _eventHandlers = ImmutableArray<SessionEventHandler>.Empty;

    private SessionHooks? _hooks;
    private readonly SemaphoreSlim _hooksLock = new(1, 1);
    private Dictionary<string, Func<string, Task<string>>>? _transformCallbacks;
    private readonly SemaphoreSlim _transformCallbacksLock = new(1, 1);
    private SessionRpc? _sessionRpc;
    private int _isDisposed;
    private event Action<ShellOutputNotification>? ShellOutputHandlers;
    private event Action<ShellExitNotification>? ShellExitHandlers;
    private readonly HashSet<string> _trackedProcessIds = [];
    private readonly object _trackedProcessIdsLock = new();
    private Action<string, CopilotSession>? _registerShellProcess;
    private Action<string>? _unregisterShellProcess;

    /// <summary>
    /// Channel that serializes event dispatch. <see cref="DispatchEvent"/> enqueues;
    /// a single background consumer (<see cref="ProcessEventsAsync"/>) dequeues and
    /// invokes handlers one at a time, preserving arrival order.
    /// </summary>
    private readonly Channel<SessionEvent> _eventChannel = Channel.CreateUnbounded<SessionEvent>(
        new() { SingleReader = true });

    /// <summary>
    /// Gets the unique identifier for this session.
    /// </summary>
    /// <value>A string that uniquely identifies this session.</value>
    public string SessionId { get; }

    /// <summary>
    /// Gets the typed RPC client for session-scoped methods.
    /// </summary>
    public SessionRpc Rpc => _sessionRpc ??= new SessionRpc(_rpc, SessionId, TrackShellProcess);

    /// <summary>
    /// Gets the path to the session workspace directory when infinite sessions are enabled.
    /// </summary>
    /// <value>
    /// The path to the workspace containing checkpoints/, plan.md, and files/ subdirectories,
    /// or null if infinite sessions are disabled.
    /// </value>
    public string? WorkspacePath { get; internal set; }

    /// <summary>
    /// Initializes a new instance of the <see cref="CopilotSession"/> class.
    /// </summary>
    /// <param name="sessionId">The unique identifier for this session.</param>
    /// <param name="rpc">The JSON-RPC connection to the Copilot CLI.</param>
    /// <param name="logger">Logger for diagnostics.</param>
    /// <param name="workspacePath">The workspace path if infinite sessions are enabled.</param>
    /// <remarks>
    /// This constructor is internal. Use <see cref="CopilotClient.CreateSessionAsync"/> to create sessions.
    /// </remarks>
    internal CopilotSession(string sessionId, JsonRpc rpc, ILogger logger, string? workspacePath = null)
    {
        SessionId = sessionId;
        _rpc = rpc;
        _logger = logger;
        WorkspacePath = workspacePath;

        // Start the asynchronous processing loop.
        _ = ProcessEventsAsync();
    }

    private Task<T> InvokeRpcAsync<T>(string method, object?[]? args, CancellationToken cancellationToken)
    {
        return CopilotClient.InvokeRpcAsync<T>(_rpc, method, args, cancellationToken);
    }

    /// <summary>
    /// Sends a message to the Copilot session and waits for the response.
    /// </summary>
    /// <param name="options">Options for the message to be sent, including the prompt and optional attachments.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the ID of the response message, which can be used to correlate events.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the session has been disposed.</exception>
    /// <remarks>
    /// <para>
    /// This method returns immediately after the message is queued. Use <see cref="SendAndWaitAsync"/>
    /// if you need to wait for the assistant to finish processing.
    /// </para>
    /// <para>
    /// Subscribe to events via <see cref="On"/> to receive streaming responses and other session events.
    /// </para>
    /// </remarks>
    /// <example>
    /// <code>
    /// var messageId = await session.SendAsync(new MessageOptions
    /// {
    ///     Prompt = "Explain this code",
    ///     Attachments = new List&lt;Attachment&gt;
    ///     {
    ///         new() { Type = "file", Path = "./Program.cs" }
    ///     }
    /// });
    /// </code>
    /// </example>
    public async Task<string> SendAsync(MessageOptions options, CancellationToken cancellationToken = default)
    {
        var (traceparent, tracestate) = TelemetryHelpers.GetTraceContext();

        var request = new SendMessageRequest
        {
            SessionId = SessionId,
            Prompt = options.Prompt,
            Attachments = options.Attachments,
            Mode = options.Mode,
            Traceparent = traceparent,
            Tracestate = tracestate
        };

        var response = await InvokeRpcAsync<SendMessageResponse>(
            "session.send", [request], cancellationToken);

        return response.MessageId;
    }

    /// <summary>
    /// Sends a message to the Copilot session and waits until the session becomes idle.
    /// </summary>
    /// <param name="options">Options for the message to be sent, including the prompt and optional attachments.</param>
    /// <param name="timeout">Timeout duration (default: 60 seconds). Controls how long to wait; does not abort in-flight agent work.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the final assistant message event, or null if none was received.</returns>
    /// <exception cref="TimeoutException">Thrown if the timeout is reached before the session becomes idle.</exception>
    /// <exception cref="OperationCanceledException">Thrown if the <paramref name="cancellationToken"/> is cancelled.</exception>
    /// <exception cref="InvalidOperationException">Thrown if the session has been disposed.</exception>
    /// <remarks>
    /// <para>
    /// This is a convenience method that combines <see cref="SendAsync"/> with waiting for
    /// the <c>session.idle</c> event. Use this when you want to block until the assistant
    /// has finished processing the message.
    /// </para>
    /// <para>
    /// Events are still delivered to handlers registered via <see cref="On"/> while waiting.
    /// </para>
    /// </remarks>
    /// <example>
    /// <code>
    /// // Send and wait for completion with default 60s timeout
    /// var response = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
    /// Console.WriteLine(response?.Data?.Content); // "4"
    /// </code>
    /// </example>
    public async Task<AssistantMessageEvent?> SendAndWaitAsync(
        MessageOptions options,
        TimeSpan? timeout = null,
        CancellationToken cancellationToken = default)
    {
        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(60);
        var tcs = new TaskCompletionSource<AssistantMessageEvent?>(TaskCreationOptions.RunContinuationsAsynchronously);
        AssistantMessageEvent? lastAssistantMessage = null;

        void Handler(SessionEvent evt)
        {
            switch (evt)
            {
                case AssistantMessageEvent assistantMessage:
                    lastAssistantMessage = assistantMessage;
                    break;

                case SessionIdleEvent:
                    tcs.TrySetResult(lastAssistantMessage);
                    break;

                case SessionErrorEvent errorEvent:
                    var message = errorEvent.Data?.Message ?? "session error";
                    tcs.TrySetException(new InvalidOperationException($"Session error: {message}"));
                    break;
            }
        }

        using var subscription = On(Handler);

        await SendAsync(options, cancellationToken);

        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(effectiveTimeout);

        using var registration = cts.Token.Register(() =>
        {
            if (cancellationToken.IsCancellationRequested)
                tcs.TrySetCanceled(cancellationToken);
            else
                tcs.TrySetException(new TimeoutException($"SendAndWaitAsync timed out after {effectiveTimeout}"));
        });
        return await tcs.Task;
    }

    /// <summary>
    /// Registers a callback for session events.
    /// </summary>
    /// <param name="handler">A callback to be invoked when a session event occurs.</param>
    /// <returns>An <see cref="IDisposable"/> that, when disposed, unsubscribes the handler.</returns>
    /// <remarks>
    /// <para>
    /// Events include assistant messages, tool executions, errors, and session state changes.
    /// Multiple handlers can be registered and will all receive events.
    /// </para>
    /// <para>
    /// Handlers are invoked serially in event-arrival order on a background thread.
    /// A handler will never be called concurrently with itself or with other handlers
    /// on the same session.
    /// </para>
    /// </remarks>
    /// <example>
    /// <code>
    /// using var subscription = session.On(evt =>
    /// {
    ///     switch (evt)
    ///     {
    ///         case AssistantMessageEvent:
    ///             Console.WriteLine($"Assistant: {evt.Data?.Content}");
    ///             break;
    ///         case SessionErrorEvent:
    ///             Console.WriteLine($"Error: {evt.Data?.Message}");
    ///             break;
    ///     }
    /// });
    ///
    /// // The handler is automatically unsubscribed when the subscription is disposed.
    /// </code>
    /// </example>
    public IDisposable On(SessionEventHandler handler)
    {
        ImmutableInterlocked.Update(ref _eventHandlers, array => array.Add(handler));
        return new ActionDisposable(() => ImmutableInterlocked.Update(ref _eventHandlers, array => array.Remove(handler)));
    }

    /// <summary>
    /// Subscribes to shell output notifications for this session.
    /// </summary>
    /// <param name="handler">A callback that receives shell output notifications.</param>
    /// <returns>An <see cref="IDisposable"/> that unsubscribes the handler when disposed.</returns>
    /// <remarks>
    /// Shell output notifications are streamed in chunks when commands started
    /// via <c>session.Rpc.Shell.ExecAsync()</c> produce stdout or stderr output.
    /// </remarks>
    /// <example>
    /// <code>
    /// using var sub = session.OnShellOutput(n =>
    /// {
    ///     Console.WriteLine($"[{n.ProcessId}:{n.Stream}] {n.Data}");
    /// });
    /// </code>
    /// </example>
    public IDisposable OnShellOutput(Action<ShellOutputNotification> handler)
    {
        ShellOutputHandlers += handler;
        return new ActionDisposable(() => ShellOutputHandlers -= handler);
    }

    /// <summary>
    /// Subscribes to shell exit notifications for this session.
    /// </summary>
    /// <param name="handler">A callback that receives shell exit notifications.</param>
    /// <returns>An <see cref="IDisposable"/> that unsubscribes the handler when disposed.</returns>
    /// <remarks>
    /// Shell exit notifications are sent when commands started via
    /// <c>session.Rpc.Shell.ExecAsync()</c> complete (after all output has been streamed).
    /// </remarks>
    /// <example>
    /// <code>
    /// using var sub = session.OnShellExit(n =>
    /// {
    ///     Console.WriteLine($"Process {n.ProcessId} exited with code {n.ExitCode}");
    /// });
    /// </code>
    /// </example>
    public IDisposable OnShellExit(Action<ShellExitNotification> handler)
    {
        ShellExitHandlers += handler;
        return new ActionDisposable(() => ShellExitHandlers -= handler);
    }

    /// <summary>
    /// Enqueues an event for serial dispatch to all registered handlers.
    /// </summary>
    /// <param name="sessionEvent">The session event to dispatch.</param>
    /// <remarks>
    /// This method is non-blocking. Broadcast request events (external_tool.requested,
    /// permission.requested) are fired concurrently so that a stalled handler does not
    /// block event delivery. The event is then placed into an in-memory channel and
    /// processed by a single background consumer (<see cref="ProcessEventsAsync"/>),
    /// which guarantees user handlers see events one at a time, in order.
    /// </remarks>
    internal void DispatchEvent(SessionEvent sessionEvent)
    {
        // Fire broadcast work concurrently (fire-and-forget with error logging).
        // This is done outside the channel so broadcast handlers don't block the
        // consumer loop — important when a secondary client's handler intentionally
        // never completes (multi-client permission scenario).
        _ = HandleBroadcastEventAsync(sessionEvent);

        // Queue the event for serial processing by user handlers.
        _eventChannel.Writer.TryWrite(sessionEvent);
    }

    /// <summary>
    /// Single-reader consumer loop that processes events from the channel.
    /// Ensures user event handlers are invoked serially and in FIFO order.
    /// </summary>
    private async Task ProcessEventsAsync()
    {
        await foreach (var sessionEvent in _eventChannel.Reader.ReadAllAsync())
        {
            foreach (var handler in _eventHandlers)
            {
                try
                {
                    handler(sessionEvent);
                }
                catch (Exception ex)
                {
                    LogEventHandlerError(ex);
                }
            }
        }
    }

    /// <summary>
    /// Dispatches a shell output notification to all registered handlers.
    /// </summary>
    internal void DispatchShellOutput(ShellOutputNotification notification)
    {
        ShellOutputHandlers?.Invoke(notification);
    }

    /// <summary>
    /// Dispatches a shell exit notification to all registered handlers.
    /// </summary>
    internal void DispatchShellExit(ShellExitNotification notification)
    {
        ShellExitHandlers?.Invoke(notification);
    }

    /// <summary>
    /// Track a shell process ID so notifications are routed to this session.
    /// </summary>
    internal void TrackShellProcess(string processId)
    {
        lock (_trackedProcessIdsLock)
        {
            _trackedProcessIds.Add(processId);
        }
        _registerShellProcess?.Invoke(processId, this);
    }

    /// <summary>
    /// Stop tracking a shell process ID.
    /// </summary>
    internal void UntrackShellProcess(string processId)
    {
        lock (_trackedProcessIdsLock)
        {
            _trackedProcessIds.Remove(processId);
        }
        _unregisterShellProcess?.Invoke(processId);
    }

    /// <summary>
    /// Set the registration callbacks for shell process tracking.
    /// </summary>
    internal void SetShellProcessCallbacks(
        Action<string, CopilotSession> register,
        Action<string> unregister)
    {
        _registerShellProcess = register;
        _unregisterShellProcess = unregister;
    }

    /// <summary>
    /// Registers custom tool handlers for this session.
    /// </summary>
    /// <param name="tools">A collection of AI functions that can be invoked by the assistant.</param>
    /// <remarks>
    /// Tools allow the assistant to execute custom functions. When the assistant invokes a tool,
    /// the corresponding handler is called with the tool arguments.
    /// </remarks>
    internal void RegisterTools(ICollection<AIFunction> tools)
    {
        _toolHandlers.Clear();
        foreach (var tool in tools)
        {
            _toolHandlers.Add(tool.Name, tool);
        }
    }

    /// <summary>
    /// Retrieves a registered tool by name.
    /// </summary>
    /// <param name="name">The name of the tool to retrieve.</param>
    /// <returns>The tool if found; otherwise, <c>null</c>.</returns>
    internal AIFunction? GetTool(string name)
    {
        return _toolHandlers.TryGetValue(name, out var tool) ? tool : null;
    }

    /// <summary>
    /// Registers a handler for permission requests.
    /// </summary>
    /// <param name="handler">The permission handler function.</param>
    /// <remarks>
    /// When the assistant needs permission to perform certain actions (e.g., file operations),
    /// this handler is called to approve or deny the request.
    /// </remarks>
    internal void RegisterPermissionHandler(PermissionRequestHandler handler)
    {
        _permissionHandler = handler;
    }

    /// <summary>
    /// Handles a permission request from the Copilot CLI.
    /// </summary>
    /// <param name="permissionRequestData">The permission request data from the CLI.</param>
    /// <returns>A task that resolves with the permission decision.</returns>
    internal async Task<PermissionRequestResult> HandlePermissionRequestAsync(JsonElement permissionRequestData)
    {
        var handler = _permissionHandler;

        if (handler == null)
        {
            return new PermissionRequestResult
            {
                Kind = PermissionRequestResultKind.DeniedCouldNotRequestFromUser
            };
        }

        var request = JsonSerializer.Deserialize(permissionRequestData.GetRawText(), SessionEventsJsonContext.Default.PermissionRequest)
            ?? throw new InvalidOperationException("Failed to deserialize permission request");

        var invocation = new PermissionInvocation
        {
            SessionId = SessionId
        };

        return await handler(request, invocation);
    }

    /// <summary>
    /// Handles broadcast request events by executing local handlers and responding via RPC.
    /// Implements the protocol v3 broadcast model where tool calls and permission requests
    /// are broadcast as session events to all clients.
    /// </summary>
    private async Task HandleBroadcastEventAsync(SessionEvent sessionEvent)
    {
        try
        {
            switch (sessionEvent)
            {
                case ExternalToolRequestedEvent toolEvent:
                    {
                        var data = toolEvent.Data;
                        if (string.IsNullOrEmpty(data.RequestId) || string.IsNullOrEmpty(data.ToolName))
                            return;

                        var tool = GetTool(data.ToolName);
                        if (tool is null)
                            return; // This client doesn't handle this tool; another client will.

                        using (TelemetryHelpers.RestoreTraceContext(data.Traceparent, data.Tracestate))
                            await ExecuteToolAndRespondAsync(data.RequestId, data.ToolName, data.ToolCallId, data.Arguments, tool);
                        break;
                    }

                case PermissionRequestedEvent permEvent:
                    {
                        var data = permEvent.Data;
                        if (string.IsNullOrEmpty(data.RequestId) || data.PermissionRequest is null)
                            return;

                        var handler = _permissionHandler;
                        if (handler is null)
                            return; // This client doesn't handle permissions; another client will.

                        await ExecutePermissionAndRespondAsync(data.RequestId, data.PermissionRequest, handler);
                        break;
                    }
            }
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            LogBroadcastHandlerError(ex);
        }
    }

    /// <summary>
    /// Executes a tool handler and sends the result back via the HandlePendingToolCall RPC.
    /// </summary>
    private async Task ExecuteToolAndRespondAsync(string requestId, string toolName, string toolCallId, object? arguments, AIFunction tool)
    {
        try
        {
            var invocation = new ToolInvocation
            {
                SessionId = SessionId,
                ToolCallId = toolCallId,
                ToolName = toolName,
                Arguments = arguments
            };

            var aiFunctionArgs = new AIFunctionArguments
            {
                Context = new Dictionary<object, object?>
                {
                    [typeof(ToolInvocation)] = invocation
                }
            };

            if (arguments is not null)
            {
                if (arguments is not JsonElement incomingJsonArgs)
                {
                    throw new InvalidOperationException($"Incoming arguments must be a {nameof(JsonElement)}; received {arguments.GetType().Name}");
                }

                foreach (var prop in incomingJsonArgs.EnumerateObject())
                {
                    aiFunctionArgs[prop.Name] = prop.Value;
                }
            }

            var result = await tool.InvokeAsync(aiFunctionArgs);

            var toolResultObject = result is ToolResultAIContent trac ? trac.Result : new ToolResultObject
            {
                ResultType = "success",
                TextResultForLlm = result is JsonElement { ValueKind: JsonValueKind.String } je
                    ? je.GetString()!
                    : JsonSerializer.Serialize(result, tool.JsonSerializerOptions.GetTypeInfo(typeof(object))),
            };

            await Rpc.Tools.HandlePendingToolCallAsync(requestId, toolResultObject, error: null);
        }
        catch (Exception ex)
        {
            try
            {
                await Rpc.Tools.HandlePendingToolCallAsync(requestId, result: null, error: ex.Message);
            }
            catch (IOException)
            {
                // Connection lost or RPC error — nothing we can do
            }
            catch (ObjectDisposedException)
            {
                // Connection already disposed — nothing we can do
            }
        }
    }

    /// <summary>
    /// Executes a permission handler and sends the result back via the HandlePendingPermissionRequest RPC.
    /// </summary>
    private async Task ExecutePermissionAndRespondAsync(string requestId, PermissionRequest permissionRequest, PermissionRequestHandler handler)
    {
        try
        {
            var invocation = new PermissionInvocation
            {
                SessionId = SessionId
            };

            var result = await handler(permissionRequest, invocation);
            if (result.Kind == new PermissionRequestResultKind("no-result"))
            {
                return;
            }
            await Rpc.Permissions.HandlePendingPermissionRequestAsync(requestId, result);
        }
        catch (Exception)
        {
            try
            {
                await Rpc.Permissions.HandlePendingPermissionRequestAsync(requestId, new PermissionRequestResult
                {
                    Kind = PermissionRequestResultKind.DeniedCouldNotRequestFromUser
                });
            }
            catch (IOException)
            {
                // Connection lost or RPC error — nothing we can do
            }
            catch (ObjectDisposedException)
            {
                // Connection already disposed — nothing we can do
            }
        }
    }

    /// <summary>
    /// Registers a handler for user input requests from the agent.
    /// </summary>
    /// <param name="handler">The handler to invoke when user input is requested.</param>
    internal void RegisterUserInputHandler(UserInputHandler handler)
    {
        _userInputHandler = handler;
    }

    /// <summary>
    /// Handles a user input request from the Copilot CLI.
    /// </summary>
    /// <param name="request">The user input request from the CLI.</param>
    /// <returns>A task that resolves with the user's response.</returns>
    internal async Task<UserInputResponse> HandleUserInputRequestAsync(UserInputRequest request)
    {
        var handler = _userInputHandler ?? throw new InvalidOperationException("No user input handler registered");
        var invocation = new UserInputInvocation
        {
            SessionId = SessionId
        };

        return await handler(request, invocation);
    }

    /// <summary>
    /// Registers hook handlers for this session.
    /// </summary>
    /// <param name="hooks">The hooks configuration.</param>
    internal void RegisterHooks(SessionHooks hooks)
    {
        _hooksLock.Wait();
        try
        {
            _hooks = hooks;
        }
        finally
        {
            _hooksLock.Release();
        }
    }

    /// <summary>
    /// Handles a hook invocation from the Copilot CLI.
    /// </summary>
    /// <param name="hookType">The type of hook to invoke.</param>
    /// <param name="input">The hook input data.</param>
    /// <returns>A task that resolves with the hook output.</returns>
    internal async Task<object?> HandleHooksInvokeAsync(string hookType, JsonElement input)
    {
        await _hooksLock.WaitAsync();
        SessionHooks? hooks;
        try
        {
            hooks = _hooks;
        }
        finally
        {
            _hooksLock.Release();
        }

        if (hooks == null)
        {
            return null;
        }

        var invocation = new HookInvocation
        {
            SessionId = SessionId
        };

        return hookType switch
        {
            "preToolUse" => hooks.OnPreToolUse != null
                ? await hooks.OnPreToolUse(
                    JsonSerializer.Deserialize(input.GetRawText(), SessionJsonContext.Default.PreToolUseHookInput)!,
                    invocation)
                : null,
            "postToolUse" => hooks.OnPostToolUse != null
                ? await hooks.OnPostToolUse(
                    JsonSerializer.Deserialize(input.GetRawText(), SessionJsonContext.Default.PostToolUseHookInput)!,
                    invocation)
                : null,
            "userPromptSubmitted" => hooks.OnUserPromptSubmitted != null
                ? await hooks.OnUserPromptSubmitted(
                    JsonSerializer.Deserialize(input.GetRawText(), SessionJsonContext.Default.UserPromptSubmittedHookInput)!,
                    invocation)
                : null,
            "sessionStart" => hooks.OnSessionStart != null
                ? await hooks.OnSessionStart(
                    JsonSerializer.Deserialize(input.GetRawText(), SessionJsonContext.Default.SessionStartHookInput)!,
                    invocation)
                : null,
            "sessionEnd" => hooks.OnSessionEnd != null
                ? await hooks.OnSessionEnd(
                    JsonSerializer.Deserialize(input.GetRawText(), SessionJsonContext.Default.SessionEndHookInput)!,
                    invocation)
                : null,
            "errorOccurred" => hooks.OnErrorOccurred != null
                ? await hooks.OnErrorOccurred(
                    JsonSerializer.Deserialize(input.GetRawText(), SessionJsonContext.Default.ErrorOccurredHookInput)!,
                    invocation)
                : null,
            _ => throw new ArgumentException($"Unknown hook type: {hookType}")
        };
    }

    /// <summary>
    /// Registers transform callbacks for system message sections.
    /// </summary>
    /// <param name="callbacks">The transform callbacks keyed by section identifier.</param>
    internal void RegisterTransformCallbacks(Dictionary<string, Func<string, Task<string>>>? callbacks)
    {
        _transformCallbacksLock.Wait();
        try
        {
            _transformCallbacks = callbacks;
        }
        finally
        {
            _transformCallbacksLock.Release();
        }
    }

    /// <summary>
    /// Handles a systemMessage.transform RPC call from the Copilot CLI.
    /// </summary>
    /// <param name="sections">The raw JSON element containing sections to transform.</param>
    /// <returns>A task that resolves with the transformed sections.</returns>
    internal async Task<SystemMessageTransformRpcResponse> HandleSystemMessageTransformAsync(JsonElement sections)
    {
        Dictionary<string, Func<string, Task<string>>>? callbacks;
        await _transformCallbacksLock.WaitAsync();
        try
        {
            callbacks = _transformCallbacks;
        }
        finally
        {
            _transformCallbacksLock.Release();
        }

        var parsed = JsonSerializer.Deserialize(
            sections.GetRawText(),
            SessionJsonContext.Default.DictionaryStringSystemMessageTransformSection) ?? new();

        var result = new Dictionary<string, SystemMessageTransformSection>();
        foreach (var (sectionId, data) in parsed)
        {
            Func<string, Task<string>>? callback = null;
            callbacks?.TryGetValue(sectionId, out callback);

            if (callback != null)
            {
                try
                {
                    var transformed = await callback(data.Content ?? "");
                    result[sectionId] = new SystemMessageTransformSection { Content = transformed };
                }
                catch
                {
                    result[sectionId] = new SystemMessageTransformSection { Content = data.Content ?? "" };
                }
            }
            else
            {
                result[sectionId] = new SystemMessageTransformSection { Content = data.Content ?? "" };
            }
        }

        return new SystemMessageTransformRpcResponse { Sections = result };
    }

    /// <summary>
    /// Gets the complete list of messages and events in the session.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that, when resolved, gives the list of all session events in chronological order.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the session has been disposed.</exception>
    /// <remarks>
    /// This returns the complete conversation history including user messages, assistant responses,
    /// tool executions, and other session events.
    /// </remarks>
    /// <example>
    /// <code>
    /// var events = await session.GetMessagesAsync();
    /// foreach (var evt in events)
    /// {
    ///     if (evt is AssistantMessageEvent)
    ///     {
    ///         Console.WriteLine($"Assistant: {evt.Data?.Content}");
    ///     }
    /// }
    /// </code>
    /// </example>
    public async Task<IReadOnlyList<SessionEvent>> GetMessagesAsync(CancellationToken cancellationToken = default)
    {
        var response = await InvokeRpcAsync<GetMessagesResponse>(
            "session.getMessages", [new GetMessagesRequest { SessionId = SessionId }], cancellationToken);

        return response.Events
            .Select(e => SessionEvent.FromJson(e.ToJsonString()))
            .OfType<SessionEvent>()
            .ToList();
    }

    /// <summary>
    /// Aborts the currently processing message in this session.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task representing the abort operation.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the session has been disposed.</exception>
    /// <remarks>
    /// Use this to cancel a long-running request. The session remains valid and can continue
    /// to be used for new messages.
    /// </remarks>
    /// <example>
    /// <code>
    /// // Start a long-running request
    /// var messageTask = session.SendAsync(new MessageOptions
    /// {
    ///     Prompt = "Write a very long story..."
    /// });
    ///
    /// // Abort after 5 seconds
    /// await Task.Delay(TimeSpan.FromSeconds(5));
    /// await session.AbortAsync();
    /// </code>
    /// </example>
    public async Task AbortAsync(CancellationToken cancellationToken = default)
    {
        await InvokeRpcAsync<object>(
            "session.abort", [new SessionAbortRequest { SessionId = SessionId }], cancellationToken);
    }

    /// <summary>
    /// Changes the model for this session.
    /// The new model takes effect for the next message. Conversation history is preserved.
    /// </summary>
    /// <param name="model">Model ID to switch to (e.g., "gpt-4.1").</param>
    /// <param name="reasoningEffort">Reasoning effort level (e.g., "low", "medium", "high", "xhigh").</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <example>
    /// <code>
    /// await session.SetModelAsync("gpt-4.1");
    /// await session.SetModelAsync("claude-sonnet-4.6", "high");
    /// </code>
    /// </example>
    public async Task SetModelAsync(string model, string? reasoningEffort, CancellationToken cancellationToken = default)
    {
        await Rpc.Model.SwitchToAsync(model, reasoningEffort, cancellationToken);
    }

    /// <summary>
    /// Changes the model for this session.
    /// </summary>
    public Task SetModelAsync(string model, CancellationToken cancellationToken = default)
    {
        return SetModelAsync(model, reasoningEffort: null, cancellationToken);
    }

    /// <summary>
    /// Log a message to the session timeline.
    /// The message appears in the session event stream and is visible to SDK consumers
    /// and (for non-ephemeral messages) persisted to the session event log on disk.
    /// </summary>
    /// <param name="message">The message to log.</param>
    /// <param name="level">Log level (default: info).</param>
    /// <param name="ephemeral">When <c>true</c>, the message is not persisted to disk.</param>
    /// <param name="url">Optional URL to associate with the log entry.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <example>
    /// <code>
    /// await session.LogAsync("Build completed successfully");
    /// await session.LogAsync("Disk space low", level: SessionLogRequestLevel.Warning);
    /// await session.LogAsync("Connection failed", level: SessionLogRequestLevel.Error);
    /// await session.LogAsync("Temporary status", ephemeral: true);
    /// </code>
    /// </example>
    public async Task LogAsync(string message, SessionLogRequestLevel? level = null, bool? ephemeral = null, string? url = null, CancellationToken cancellationToken = default)
    {
        await Rpc.LogAsync(message, level, ephemeral, url, cancellationToken);
    }

    /// <summary>
    /// Closes this session and releases all in-memory resources (event handlers,
    /// tool handlers, permission handlers).
    /// </summary>
    /// <returns>A task representing the dispose operation.</returns>
    /// <remarks>
    /// <para>
    /// The caller should ensure the session is idle (e.g., <see cref="SendAndWaitAsync"/>
    /// has returned) before disposing. If the session is not idle, in-flight event handlers
    /// or tool handlers may observe failures.
    /// </para>
    /// <para>
    /// Session state on disk (conversation history, planning state, artifacts) is
    /// preserved, so the conversation can be resumed later by calling
    /// <see cref="CopilotClient.ResumeSessionAsync"/> with the session ID. To
    /// permanently remove all session data including files on disk, use
    /// <see cref="CopilotClient.DeleteSessionAsync"/> instead.
    /// </para>
    /// <para>
    /// After calling this method, the session object can no longer be used.
    /// </para>
    /// </remarks>
    /// <example>
    /// <code>
    /// // Using 'await using' for automatic disposal — session can still be resumed later
    /// await using var session = await client.CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll });
    ///
    /// // Or manually dispose
    /// var session2 = await client.CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll });
    /// // ... use the session ...
    /// await session2.DisposeAsync();
    /// </code>
    /// </example>
    public async ValueTask DisposeAsync()
    {
        if (Interlocked.Exchange(ref _isDisposed, 1) == 1)
        {
            return;
        }

        _eventChannel.Writer.TryComplete();

        try
        {
            await InvokeRpcAsync<object>(
                "session.destroy", [new SessionDestroyRequest() { SessionId = SessionId }], CancellationToken.None);
        }
        catch (ObjectDisposedException)
        {
            // Connection was already disposed (e.g., client.StopAsync() was called first)
        }
        catch (IOException)
        {
            // Connection is broken or closed
        }

        _eventHandlers = ImmutableInterlocked.InterlockedExchange(ref _eventHandlers, ImmutableArray<SessionEventHandler>.Empty);
        ShellOutputHandlers = null;
        ShellExitHandlers = null;

        lock (_trackedProcessIdsLock)
        {
            foreach (var processId in _trackedProcessIds)
            {
                _unregisterShellProcess?.Invoke(processId);
            }
            _trackedProcessIds.Clear();
        }

        _toolHandlers.Clear();

        _permissionHandler = null;
    }

    [LoggerMessage(Level = LogLevel.Error, Message = "Unhandled exception in broadcast event handler")]
    private partial void LogBroadcastHandlerError(Exception exception);

    [LoggerMessage(Level = LogLevel.Error, Message = "Unhandled exception in session event handler")]
    private partial void LogEventHandlerError(Exception exception);

    internal record SendMessageRequest
    {
        public string SessionId { get; init; } = string.Empty;
        public string Prompt { get; init; } = string.Empty;
        public List<UserMessageDataAttachmentsItem>? Attachments { get; init; }
        public string? Mode { get; init; }
        public string? Traceparent { get; init; }
        public string? Tracestate { get; init; }
    }

    internal record SendMessageResponse
    {
        public string MessageId { get; init; } = string.Empty;
    }

    internal record GetMessagesRequest
    {
        public string SessionId { get; init; } = string.Empty;
    }

    internal record GetMessagesResponse
    {
        public List<JsonObject> Events { get; init; } = [];
    }

    internal record SessionAbortRequest
    {
        public string SessionId { get; init; } = string.Empty;
    }

    internal record SessionDestroyRequest
    {
        public string SessionId { get; init; } = string.Empty;
    }

    [JsonSourceGenerationOptions(
        JsonSerializerDefaults.Web,
        AllowOutOfOrderMetadataProperties = true,
        NumberHandling = JsonNumberHandling.AllowReadingFromString,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonSerializable(typeof(GetMessagesRequest))]
    [JsonSerializable(typeof(GetMessagesResponse))]
    [JsonSerializable(typeof(SendMessageRequest))]
    [JsonSerializable(typeof(SendMessageResponse))]
    [JsonSerializable(typeof(SessionAbortRequest))]
    [JsonSerializable(typeof(SessionDestroyRequest))]
    [JsonSerializable(typeof(UserMessageDataAttachmentsItem))]
    [JsonSerializable(typeof(PreToolUseHookInput))]
    [JsonSerializable(typeof(PreToolUseHookOutput))]
    [JsonSerializable(typeof(PostToolUseHookInput))]
    [JsonSerializable(typeof(PostToolUseHookOutput))]
    [JsonSerializable(typeof(UserPromptSubmittedHookInput))]
    [JsonSerializable(typeof(UserPromptSubmittedHookOutput))]
    [JsonSerializable(typeof(SessionStartHookInput))]
    [JsonSerializable(typeof(SessionStartHookOutput))]
    [JsonSerializable(typeof(SessionEndHookInput))]
    [JsonSerializable(typeof(SessionEndHookOutput))]
    [JsonSerializable(typeof(ErrorOccurredHookInput))]
    [JsonSerializable(typeof(ErrorOccurredHookOutput))]
    [JsonSerializable(typeof(SystemMessageTransformSection))]
    [JsonSerializable(typeof(SystemMessageTransformRpcResponse))]
    [JsonSerializable(typeof(Dictionary<string, SystemMessageTransformSection>))]
    internal partial class SessionJsonContext : JsonSerializerContext;
}
