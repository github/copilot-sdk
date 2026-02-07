/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using StreamJsonRpc;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;

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
/// </remarks>
/// <example>
/// <code>
/// await using var session = await client.CreateSessionAsync(new SessionConfig { Model = "gpt-4" });
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
public partial class CopilotSession : IAsyncDisposable
{
    private readonly HashSet<SessionEventHandler> _eventHandlers = new();
    private readonly Dictionary<string, AIFunction> _toolHandlers = new();
    private readonly JsonRpc _rpc;
    private PermissionHandler? _permissionHandler;
    private readonly SemaphoreSlim _permissionHandlerLock = new(1, 1);
    private UserInputHandler? _userInputHandler;
    private readonly SemaphoreSlim _userInputHandlerLock = new(1, 1);
    private SessionHooks? _hooks;
    private readonly SemaphoreSlim _hooksLock = new(1, 1);

    /// <summary>
    /// Gets the unique identifier for this session.
    /// </summary>
    /// <value>A string that uniquely identifies this session.</value>
    public string SessionId { get; }

    /// <summary>
    /// Gets the path to the session workspace directory when infinite sessions are enabled.
    /// </summary>
    /// <value>
    /// The path to the workspace containing checkpoints/, plan.md, and files/ subdirectories,
    /// or null if infinite sessions are disabled.
    /// </value>
    public string? WorkspacePath { get; }

    /// <summary>
    /// Initializes a new instance of the <see cref="CopilotSession"/> class.
    /// </summary>
    /// <param name="sessionId">The unique identifier for this session.</param>
    /// <param name="rpc">The JSON-RPC connection to the Copilot CLI.</param>
    /// <param name="workspacePath">The workspace path if infinite sessions are enabled.</param>
    /// <remarks>
    /// This constructor is internal. Use <see cref="CopilotClient.CreateSessionAsync"/> to create sessions.
    /// </remarks>
    internal CopilotSession(string sessionId, JsonRpc rpc, string? workspacePath = null)
    {
        SessionId = sessionId;
        _rpc = rpc;
        WorkspacePath = workspacePath;
    }

    private Task<T> InvokeRpcAsync<T>(string method, object?[]? args, CancellationToken cancellationToken) =>
        CopilotClient.InvokeRpcAsync<T>(_rpc, method, args, cancellationToken);

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
        var request = new SendMessageRequest
        {
            SessionId = SessionId,
            Prompt = options.Prompt,
            Attachments = options.Attachments,
            Mode = options.Mode
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
        var tcs = new TaskCompletionSource<AssistantMessageEvent?>();
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
            tcs.TrySetException(new TimeoutException($"SendAndWaitAsync timed out after {effectiveTimeout}")));
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
    /// Handler exceptions are allowed to propagate so they are not lost.
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
        _eventHandlers.Add(handler);
        return new OnDisposeCall(() => _eventHandlers.Remove(handler));
    }

    /// <summary>
    /// Dispatches an event to all registered handlers.
    /// </summary>
    /// <param name="sessionEvent">The session event to dispatch.</param>
    /// <remarks>
    /// This method is internal. Handler exceptions are allowed to propagate so they are not lost.
    /// </remarks>
    internal void DispatchEvent(SessionEvent sessionEvent)
    {
        foreach (var handler in _eventHandlers.ToArray())
        {
            // We allow handler exceptions to propagate so they are not lost
            handler(sessionEvent);
        }
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
    internal AIFunction? GetTool(string name) =>
        _toolHandlers.TryGetValue(name, out var tool) ? tool : null;

    /// <summary>
    /// Registers a handler for permission requests.
    /// </summary>
    /// <param name="handler">The permission handler function.</param>
    /// <remarks>
    /// When the assistant needs permission to perform certain actions (e.g., file operations),
    /// this handler is called to approve or deny the request.
    /// </remarks>
    internal void RegisterPermissionHandler(PermissionHandler handler)
    {
        _permissionHandlerLock.Wait();
        try
        {
            _permissionHandler = handler;
        }
        finally
        {
            _permissionHandlerLock.Release();
        }
    }

    /// <summary>
    /// Handles a permission request from the Copilot CLI.
    /// </summary>
    /// <param name="permissionRequestData">The permission request data from the CLI.</param>
    /// <returns>A task that resolves with the permission decision.</returns>
    internal async Task<PermissionRequestResult> HandlePermissionRequestAsync(JsonElement permissionRequestData)
    {
        await _permissionHandlerLock.WaitAsync();
        PermissionHandler? handler;
        try
        {
            handler = _permissionHandler;
        }
        finally
        {
            _permissionHandlerLock.Release();
        }

        if (handler == null)
        {
            return new PermissionRequestResult
            {
                Kind = "denied-no-approval-rule-and-could-not-request-from-user"
            };
        }

        var request = JsonSerializer.Deserialize(permissionRequestData.GetRawText(), SessionJsonContext.Default.PermissionRequest)
            ?? throw new InvalidOperationException("Failed to deserialize permission request");

        var invocation = new PermissionInvocation
        {
            SessionId = SessionId
        };

        return await handler(request, invocation);
    }

    /// <summary>
    /// Registers a handler for user input requests from the agent.
    /// </summary>
    /// <param name="handler">The handler to invoke when user input is requested.</param>
    internal void RegisterUserInputHandler(UserInputHandler handler)
    {
        _userInputHandlerLock.Wait();
        try
        {
            _userInputHandler = handler;
        }
        finally
        {
            _userInputHandlerLock.Release();
        }
    }

    /// <summary>
    /// Handles a user input request from the Copilot CLI.
    /// </summary>
    /// <param name="request">The user input request from the CLI.</param>
    /// <returns>A task that resolves with the user's response.</returns>
    internal async Task<UserInputResponse> HandleUserInputRequestAsync(UserInputRequest request)
    {
        await _userInputHandlerLock.WaitAsync();
        UserInputHandler? handler;
        try
        {
            handler = _userInputHandler;
        }
        finally
        {
            _userInputHandlerLock.Release();
        }

        if (handler == null)
        {
            throw new InvalidOperationException("No user input handler registered");
        }

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
    /// Disposes the <see cref="CopilotSession"/> and releases all associated resources.
    /// </summary>
    /// <returns>A task representing the dispose operation.</returns>
    /// <remarks>
    /// <para>
    /// After calling this method, the session can no longer be used. All event handlers
    /// and tool handlers are cleared.
    /// </para>
    /// <para>
    /// To continue the conversation, use <see cref="CopilotClient.ResumeSessionAsync"/>
    /// with the session ID.
    /// </para>
    /// </remarks>
    /// <example>
    /// <code>
    /// // Using 'await using' for automatic disposal
    /// await using var session = await client.CreateSessionAsync();
    ///
    /// // Or manually dispose
    /// var session2 = await client.CreateSessionAsync();
    /// // ... use the session ...
    /// await session2.DisposeAsync();
    /// </code>
    /// </example>
    public async ValueTask DisposeAsync()
    {
        await InvokeRpcAsync<object>(
            "session.destroy", [new SessionDestroyRequest() { SessionId = SessionId }], CancellationToken.None);

        _eventHandlers.Clear();
        _toolHandlers.Clear();

        await _permissionHandlerLock.WaitAsync();
        try
        {
            _permissionHandler = null;
        }
        finally
        {
            _permissionHandlerLock.Release();
        }
    }

    private class OnDisposeCall(Action callback) : IDisposable
    {
        public void Dispose() => callback();
    }

    internal record SendMessageRequest
    {
        public string SessionId { get; init; } = string.Empty;
        public string Prompt { get; init; } = string.Empty;
        public List<UserMessageDataAttachmentsItem>? Attachments { get; init; }
        public string? Mode { get; init; }
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
        public List<JsonObject> Events { get; init; } = new();
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
    [JsonSerializable(typeof(PermissionRequest))]
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
    internal partial class SessionJsonContext : JsonSerializerContext;
}
