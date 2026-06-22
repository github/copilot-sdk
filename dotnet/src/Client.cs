/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Globalization;
using System.Net.Sockets;
using System.Runtime.ExceptionServices;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace GitHub.Copilot;

/// <summary>
/// Provides a client for interacting with the Copilot CLI server.
/// </summary>
/// <remarks>
/// <para>
/// The <see cref="CopilotClient"/> manages the connection to the Copilot CLI server and provides
/// methods to create and manage conversation sessions. It can either spawn a CLI server process
/// or connect to an existing server.
/// </para>
/// <para>
/// The client supports both stdio (default) and TCP transport modes for communication with the CLI server.
/// </para>
/// </remarks>
/// <example>
/// <code>
/// // Create a client with default options (spawns CLI server)
/// await using var client = new CopilotClient();
///
/// // Create a session
/// await using var session = await client.CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll, Model = "gpt-4" });
///
/// // Handle events
/// using var subscription = session.On&lt;SessionEvent&gt;(evt =>
/// {
///     if (evt is AssistantMessageEvent assistantMessage)
///         Console.WriteLine(assistantMessage.Data?.Content);
/// });
///
/// // Send a message
/// await session.SendAsync(new MessageOptions { Prompt = "Hello!" });
/// </code>
/// </example>
public sealed partial class CopilotClient : IDisposable, IAsyncDisposable
{
    /// <summary>
    /// Minimum protocol version this SDK can communicate with.
    /// </summary>
    private const int MinProtocolVersion = 3;
    private static readonly TimeSpan s_stderrPumpShutdownTimeout = TimeSpan.FromSeconds(5);
    private static readonly TimeSpan s_runtimeShutdownTimeout = TimeSpan.FromSeconds(10);

    /// <summary>
    /// Provides a thread-safe collection of active Copilot sessions, indexed by session identifier.
    /// </summary>
    /// <remarks>
    /// This maintains a strong reference to every <see cref="CopilotSession"/> created on this
    /// <see cref="CopilotClient"/> that has not been explicitly disposed or removed.
    /// </remarks>
    internal readonly ConcurrentDictionary<string, CopilotSession> _sessions = new();

    private readonly CopilotClientOptions _options;
    private readonly RuntimeConnection _connection;
    private readonly ILogger _logger;
    private readonly int? _optionsPort;
    private readonly string? _optionsHost;
    private readonly Func<CancellationToken, Task<IList<ModelInfo>>>? _onListModels;
    private readonly List<LifecycleSubscription> _lifecycleHandlers = [];

    private Task<Connection>? _connectionTask;
    private bool _disposed;
    private int? _actualPort;
    private int? _negotiatedProtocolVersion;
    private SemaphoreSlim? _modelsCacheLock;
    private List<ModelInfo>? _modelsCache;
    private ServerRpc? _serverRpc;

    private sealed record LifecycleSubscription(Type EventType, Action<SessionLifecycleEvent> Handler);

    /// <summary>
    /// Gets the typed RPC client for server-scoped methods (no session required).
    /// </summary>
    /// <remarks>
    /// The client must be started before accessing this property. Call <see cref="StartAsync"/> before use.
    /// </remarks>
    /// <exception cref="ObjectDisposedException">Thrown if the client has been disposed.</exception>
    /// <exception cref="InvalidOperationException">Thrown if the client is not started.</exception>
    public ServerRpc Rpc => _disposed
        ? throw new ObjectDisposedException(nameof(CopilotClient))
        : _serverRpc ?? throw new InvalidOperationException("Client is not started. Call StartAsync first.");

    /// <summary>
    /// Gets the actual TCP port the runtime is listening on, if using TCP transport.
    /// </summary>
    public int? RuntimePort => _actualPort;

    /// <summary>
    /// Creates a new instance of <see cref="CopilotClient"/>.
    /// </summary>
    /// <param name="options">Options for creating the client. If null, default options are used.</param>
    /// <example>
    /// <code>
    /// // Default options - spawns the bundled runtime using stdio
    /// var client = new CopilotClient();
    ///
    /// // Connect to an existing runtime
    /// var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri("localhost:3000") });
    ///
    /// // Custom runtime path with specific log level
    /// var client = new CopilotClient(new CopilotClientOptions
    /// {
    ///     Connection = RuntimeConnection.ForStdio(path: "/usr/local/bin/copilot"),
    ///     LogLevel = CopilotLogLevel.Debug
    /// });
    /// </code>
    /// </example>
    public CopilotClient(CopilotClientOptions? options = null)
    {
        _options = options ?? new();
        _connection = _options.Connection ?? RuntimeConnection.ForStdio();

        switch (_connection)
        {
            case StdioRuntimeConnection:
                break;

            case TcpRuntimeConnection tcp:
                if (tcp.ConnectionToken is { Length: 0 })
                {
                    throw new ArgumentException("ConnectionToken must be a non-empty string or null.", nameof(options));
                }
                // Auto-generate a connection token when the SDK spawns the runtime over TCP
                // so the loopback listener is safe by default.
                tcp.ConnectionToken ??= Guid.NewGuid().ToString();
                break;

            case UriRuntimeConnection uri:
                if (string.IsNullOrEmpty(uri.Url))
                {
                    throw new ArgumentException("UriRuntimeConnection.Url must be a non-empty string.", nameof(options));
                }
                if (!string.IsNullOrEmpty(_options.GitHubToken) || _options.UseLoggedInUser != null)
                {
                    throw new ArgumentException("GitHubToken and UseLoggedInUser cannot be combined with RuntimeConnection.ForUri (the existing runtime manages its own auth).", nameof(options));
                }
                var parsed = ParseRuntimeUrl(uri.Url);
                _optionsHost = parsed.Host;
                _optionsPort = parsed.Port;
                break;

            default:
                throw new ArgumentException($"Unsupported RuntimeConnection type: {_connection.GetType().Name}", nameof(options));
        }

        _logger = _options.Logger ?? NullLogger.Instance;
        _onListModels = _options.OnListModels;

        // Empty mode: validate at construction time that the app supplied a
        // per-session persistence location. The runtime is mode-agnostic, so
        // without this check it would silently fall back to ~/.copilot, which
        // defeats the point of empty mode for multi-tenant scenarios.
        if (_options.Mode == CopilotClientMode.Empty)
        {
            var hasPersistence =
                !string.IsNullOrEmpty(_options.BaseDirectory) ||
                _options.SessionFs is not null ||
                // External runtimes manage their own persistence layer; the SDK
                // can't enforce it from here.
                _connection is UriRuntimeConnection;
            if (!hasPersistence)
            {
                throw new ArgumentException(
                    "CopilotClient was created with Mode = CopilotClientMode.Empty but neither " +
                    "BaseDirectory nor SessionFs was set. Empty mode requires an explicit " +
                    "per-session persistence location; pick one.",
                    nameof(options));
            }
        }
    }

    /// <summary>
    /// Parses a runtime URL into a URI with host and port.
    /// </summary>
    /// <param name="url">The URL to parse. Supports formats: "port", "host:port", "http://host:port".</param>
    private static Uri ParseRuntimeUrl(string url)
    {
        // If it's just a port number, treat as localhost
        if (int.TryParse(url, out var port))
        {
            return new Uri($"http://localhost:{port}");
        }

        // Add scheme if missing
        if (!url.StartsWith("http://", StringComparison.OrdinalIgnoreCase) &&
            !url.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
        {
            url = "https://" + url;
        }

        return new Uri(url);
    }

    /// <summary>
    /// Starts the Copilot client and connects to the server.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
    /// <remarks>
    /// If the server is not already running and the client is configured to spawn one (default), it will be started.
    /// If connecting to an external runtime (via RuntimeConnection.ForUri), only establishes the connection.
    /// </remarks>
    /// <example>
    /// <code>
    /// var client = new CopilotClient();
    /// await client.StartAsync();
    /// // Now ready to create sessions
    /// </code>
    /// </example>
    public Task StartAsync(CancellationToken cancellationToken = default)
    {
        return _connectionTask ??= StartCoreAsync(cancellationToken);

        async Task<Connection> StartCoreAsync(CancellationToken ct)
        {
            _logger.LogDebug("Starting Copilot client");

            var startTimestamp = Stopwatch.GetTimestamp();
            Connection? connection = null;
            Process? cliProcess = null;
            ProcessStderrPump? stderrPump = null;

            try
            {
                if (_connection is UriRuntimeConnection)
                {
                    // External runtime
                    _actualPort = _optionsPort;
                    connection = await ConnectToServerAsync(null, _optionsHost, _optionsPort, null, ct);
                }
                else
                {
                    // Child process (stdio or TCP)
                    var (startedProcess, portOrNull, startedStderrPump) = await StartCliServerAsync(ct);
                    cliProcess = startedProcess;
                    stderrPump = startedStderrPump;
                    _actualPort = portOrNull;
                    connection = await ConnectToServerAsync(cliProcess, portOrNull is null ? null : "localhost", portOrNull, stderrPump, ct);
                }

                LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                    "CopilotClient.StartAsync transport setup complete. Elapsed={Elapsed}",
                    startTimestamp);

                // Verify protocol version compatibility
                await VerifyProtocolVersionAsync(connection, ct);
                LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                    "CopilotClient.StartAsync protocol verification complete. Elapsed={Elapsed}",
                    startTimestamp);

                var sessionFsTimestamp = Stopwatch.GetTimestamp();
                await ConfigureSessionFsAsync(ct);
                if (_options.SessionFs is not null)
                {
                    LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                        "CopilotClient.StartAsync session filesystem setup complete. Elapsed={Elapsed}",
                        sessionFsTimestamp);
                }

                LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                    "CopilotClient.StartAsync complete. Elapsed={Elapsed}",
                    startTimestamp);
                return connection;
            }
            catch (Exception ex)
            {
                if (ex is not OperationCanceledException)
                {
                    LoggingHelpers.LogTiming(_logger, LogLevel.Warning, ex,
                        "CopilotClient.StartAsync failed. Elapsed={Elapsed}",
                        startTimestamp);
                }

                if (connection is not null)
                {
                    await CleanupConnectionAsync(connection, errors: null, gracefulRuntimeShutdown: false);
                }
                else if (cliProcess is not null)
                {
                    await CleanupCliProcessAsync(cliProcess, stderrPump, errors: null, _logger);
                }

                throw;
            }
        }
    }

    /// <summary>
    /// Disconnects from the Copilot server and closes all active sessions.
    /// </summary>
    /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
    /// <remarks>
    /// <para>
    /// This method performs graceful cleanup:
    /// <list type="number">
    ///     <item>Closes all active sessions (releases in-memory resources)</item>
    ///     <item>Requests runtime shutdown for SDK-owned CLI processes</item>
    ///     <item>Closes the JSON-RPC connection</item>
    ///     <item>Terminates the CLI server process (if spawned by this client)</item>
    /// </list>
    /// </para>
    /// <para>
    /// Note: session data on disk is preserved, so sessions can be resumed later.
    /// To permanently remove session data before stopping, call
    /// <see cref="DeleteSessionAsync"/> for each session first.
    /// </para>
    /// </remarks>
    /// <exception cref="AggregateException">Thrown when multiple errors occur during cleanup.</exception>
    /// <example>
    /// <code>
    /// await client.StopAsync();
    /// </code>
    /// </example>
    public async Task StopAsync()
    {
        List<Exception> errors = [];

        foreach (var session in _sessions.Values.ToArray())
        {
            try
            {
                await session.DisposeAsync();
            }
            catch (Exception ex)
            {
                errors.Add(new IOException($"Failed to dispose session {session.SessionId}: {ex.Message}", ex));
            }
        }

        _sessions.Clear();

        await CleanupConnectionAsync(errors, gracefulRuntimeShutdown: true);

        ThrowErrors(errors);
    }

    /// <summary>
    /// Forces an immediate stop of the client without graceful cleanup.
    /// </summary>
    /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
    /// <remarks>
    /// Use this when <see cref="StopAsync"/> fails or takes too long. This method:
    /// <list type="bullet">
    ///     <item>Clears all sessions immediately without destroying them</item>
    ///     <item>Force closes the connection</item>
    ///     <item>Kills the CLI process (if spawned by this client)</item>
    /// </list>
    /// </remarks>
    /// <example>
    /// <code>
    /// // If normal stop hangs, force stop
    /// var stopTask = client.StopAsync();
    /// if (!stopTask.Wait(TimeSpan.FromSeconds(5)))
    /// {
    ///     await client.ForceStopAsync();
    /// }
    /// </code>
    /// </example>
    public async Task ForceStopAsync()
    {
        _sessions.Clear();

        var errors = new List<Exception>();
        await CleanupConnectionAsync(errors, gracefulRuntimeShutdown: false);
        ThrowErrors(errors);
    }

    private static void ThrowErrors(List<Exception>? errors)
    {
        if (errors is not null)
        {
            if (errors.Count == 1)
            {
                ExceptionDispatchInfo.Throw(errors[0]);
            }

            if (errors.Count > 0)
            {
                throw new AggregateException(errors);
            }
        }
    }

    private async Task CleanupConnectionAsync(List<Exception>? errors, bool gracefulRuntimeShutdown)
    {
        var connectionTask = _connectionTask;
        if (connectionTask is null)
        {
            return;
        }

        _connectionTask = null;

        Connection ctx;
        try
        {
            ctx = await connectionTask;
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Ignoring failed Copilot client startup during cleanup");
            return;
        }

        await CleanupConnectionAsync(ctx, errors, gracefulRuntimeShutdown);
    }

    private async Task CleanupConnectionAsync(Connection ctx, List<Exception>? errors, bool gracefulRuntimeShutdown)
    {
        var runtimeShutdownCompleted = false;
        if (gracefulRuntimeShutdown && ctx.CliProcess is not null)
        {
            var runtimeShutdownTimestamp = Stopwatch.GetTimestamp();
            try
            {
                using var cancellation = new CancellationTokenSource(s_runtimeShutdownTimeout);
                await ctx.Server.Runtime.ShutdownAsync(cancellation.Token);
                runtimeShutdownCompleted = true;
                LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                    "CopilotClient.StopAsync runtime shutdown complete. Elapsed={Elapsed}",
                    runtimeShutdownTimestamp);
            }
            catch (Exception ex) when (ex is OperationCanceledException
                or InvalidOperationException
                or ObjectDisposedException
                or IOException
                or SocketException)
            {
                LoggingHelpers.LogTiming(_logger, LogLevel.Debug, ex,
                    "CopilotClient.StopAsync runtime shutdown failed. Elapsed={Elapsed}",
                    runtimeShutdownTimestamp);
            }
        }

        try { ctx.Rpc.Dispose(); }
        catch (Exception ex) { AddCleanupError(errors, ex, _logger); }

        // Clear RPC and models cache
        _serverRpc = null;
        _modelsCache = null;

        if (ctx.NetworkStream is not null)
        {
            try { await ctx.NetworkStream.DisposeAsync(); }
            catch (Exception ex) { AddCleanupError(errors, ex, _logger); }
        }

        if (ctx.CliProcess is { } childProcess)
        {
            await CleanupCliProcessAsync(childProcess, ctx.StderrPump, errors, _logger, runtimeShutdownCompleted);
        }
    }

    private static async Task CleanupCliProcessAsync(Process childProcess, ProcessStderrPump? stderrPump, List<Exception>? errors, ILogger? logger, bool waitForGracefulExit = false)
    {
        stderrPump?.Cancel();

        try
        {
            if (!childProcess.HasExited)
            {
                if (waitForGracefulExit)
                {
                    var shutdownWaitTimestamp = Stopwatch.GetTimestamp();
                    try
                    {
                        await childProcess.WaitForExitAsync().WaitAsync(s_runtimeShutdownTimeout);
                    }
                    catch (TimeoutException ex)
                    {
                        if (logger is not null)
                        {
                            LoggingHelpers.LogTiming(logger, LogLevel.Debug, ex,
                                "Timed out waiting for runtime process to exit after graceful shutdown. Elapsed={Elapsed}, Timeout={Timeout}",
                                shutdownWaitTimestamp,
                                s_runtimeShutdownTimeout);
                        }
                    }
                }

                if (childProcess.HasExited)
                {
                    return;
                }

                childProcess.Kill(entireProcessTree: true);
                // Kill is asynchronous; wait for the root CLI process to exit so cleanup callers
                // do not observe StopAsync/DisposeAsync completion while it is still tearing down.
                var killWaitTimestamp = Stopwatch.GetTimestamp();
                try
                {
                    await childProcess.WaitForExitAsync().WaitAsync(s_runtimeShutdownTimeout);
                }
                catch (TimeoutException ex)
                {
                    if (logger is not null)
                    {
                        LoggingHelpers.LogTiming(logger, LogLevel.Debug, ex,
                            "Timed out waiting for runtime process to exit after kill. Elapsed={Elapsed}, Timeout={Timeout}",
                            killWaitTimestamp,
                            s_runtimeShutdownTimeout);
                    }

                    AddCleanupError(errors, ex, logger);
                }
            }
        }
        catch (Exception ex)
        {
            AddCleanupError(errors, ex, logger);
        }

        if (stderrPump is not null)
        {
            var stderrPumpWaitTimestamp = Stopwatch.GetTimestamp();
            try
            {
                await stderrPump.Completion.WaitAsync(s_stderrPumpShutdownTimeout);
            }
            catch (TimeoutException ex)
            {
                if (logger is not null)
                {
                    LoggingHelpers.LogTiming(logger, LogLevel.Debug, ex,
                        "Timed out waiting for runtime stderr pump to stop. Elapsed={Elapsed}, Timeout={Timeout}",
                        stderrPumpWaitTimestamp,
                        s_stderrPumpShutdownTimeout);
                }

                AddCleanupError(errors, ex, logger);
            }
            catch (Exception ex)
            {
                AddCleanupError(errors, ex, logger);
            }
        }

        try { childProcess.Dispose(); }
        catch (Exception ex) { AddCleanupError(errors, ex, logger); }
    }

    private static void AddCleanupError(List<Exception>? errors, Exception ex, ILogger? logger)
    {
        if (errors is not null)
        {
            errors.Add(ex);
        }
        else
        {
            logger?.LogDebug(ex, "Error while cleaning up Copilot CLI connection");
        }
    }

    private static (SystemMessageConfig? wireConfig, Dictionary<string, Func<string, Task<string>>>? callbacks) ExtractTransformCallbacks(SystemMessageConfig? systemMessage)
    {
        if (systemMessage?.Mode != SystemMessageMode.Customize || systemMessage.Sections == null)
        {
            return (systemMessage, null);
        }

        Dictionary<string, Func<string, Task<string>>>? callbacks = null;
        Dictionary<SystemMessageSection, SectionOverride>? wireSections = null;

        if (systemMessage.Sections is { Count: > 0 })
        {
            wireSections ??= [];

            foreach (var (sectionId, sectionOverride) in systemMessage.Sections)
            {
                if (sectionOverride.Transform != null)
                {
                    (callbacks ??= [])[sectionId.Value] = sectionOverride.Transform;
                    wireSections[sectionId] = new SectionOverride { Action = SectionOverrideAction.Transform };
                }
                else
                {
                    wireSections[sectionId] = sectionOverride;
                }
            }
        }

        if (callbacks is null)
        {
            return (systemMessage, null);
        }

        var wireConfig = new SystemMessageConfig
        {
            Mode = systemMessage.Mode,
            Content = systemMessage.Content,
            Sections = wireSections
        };

        return (wireConfig, callbacks);
    }

    /// <summary>
    /// Creates a <see cref="CopilotSession"/>, wires up handlers from the
    /// session config, registers it with the client, and starts its event
    /// processing loop. Used by both <see cref="CreateSessionAsync"/> (invoked
    /// from the JSON-RPC read loop the instant the response arrives, so that
    /// session events delivered between the response and the awaiter
    /// resuming are not dropped) and <see cref="ResumeSessionAsync"/>
    /// (invoked before the RPC is issued, since the session id is known up
    /// front).
    /// </summary>
    private CopilotSession InitializeSession(
        string sessionId,
        JsonRpc rpc,
        SessionConfigBase config,
        Dictionary<string, Func<string, Task<string>>>? transformCallbacks,
        bool hasHooks,
        string callerName)
    {
        var setupTimestamp = Stopwatch.GetTimestamp();
        var session = new CopilotSession(
            sessionId,
            rpc,
            _logger,
            this);
        session.RegisterTools(config.Tools ?? []);
        session.RegisterPermissionHandler(config.OnPermissionRequest);
        session.RegisterCommands(config.Commands);
        session.RegisterElicitationHandler(config.OnElicitationRequest);
        session.RegisterExitPlanModeHandler(config.OnExitPlanModeRequest);
        session.RegisterAutoModeSwitchHandler(config.OnAutoModeSwitchRequest);
        if (config.OnUserInputRequest != null)
        {
            session.RegisterUserInputHandler(config.OnUserInputRequest);
        }
        if (config.Hooks != null)
        {
            session.RegisterHooks(config.Hooks);
        }
        if (transformCallbacks != null)
        {
            session.RegisterTransformCallbacks(transformCallbacks);
        }
        if (config.OnEvent != null)
        {
            session.On<SessionEvent>(config.OnEvent);
        }
        ConfigureSessionFsHandlers(session, config.CreateSessionFsProvider);
        session.SetCanvasHandler(config.CanvasHandler);
        RegisterSession(session);
        session.StartProcessingEvents();
        LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
            callerName + " local setup complete. Elapsed={Elapsed}, SessionId={SessionId}, Tools={ToolsCount}, Commands={CommandsCount}, Hooks={HasHooks}",
            setupTimestamp,
            sessionId,
            config.Tools?.Count ?? 0,
            config.Commands?.Count ?? 0,
            hasHooks);
        return session;
    }

    /// <summary>
    /// Catches misuse of <see cref="SessionConfigBase.AvailableTools"/> /
    /// <see cref="SessionConfigBase.ExcludedTools"/> at the SDK boundary so
    /// callers get an actionable error rather than a silently-empty filter.
    /// The runtime treats a bare <c>"*"</c> as a literal name match for a tool
    /// whose name is the single character <c>*</c>, which the runtime's
    /// charset guard would reject at registration — so the filter effectively
    /// matches nothing.
    /// </summary>
    private static void ValidateToolFilterList(string field, IList<string>? list)
    {
        if (list is null) return;
        foreach (var entry in list)
        {
            if (entry == "*")
            {
                throw new ArgumentException(
                    $"Invalid {field} entry '*': there is no bare wildcard. " +
                    "Use `new ToolSet().AddBuiltIn(\"*\")`, `.AddMcp(\"*\")`, or " +
                    "`.AddCustom(\"*\")` to target a specific source.",
                    nameof(list));
            }
        }
    }

    /// <summary>
    /// Resolves <see cref="SessionConfigBase.AvailableTools"/> /
    /// <see cref="SessionConfigBase.ExcludedTools"/> for the wire payload,
    /// validating empty-mode requirements. <c>toolFilterPrecedence</c> is
    /// always <c>excluded</c> so SDK consumers get composable allowlist /
    /// denylist semantics.
    /// </summary>
    private (IList<string>? AvailableTools, IList<string>? ExcludedTools, OptionsUpdateToolFilterPrecedence ToolFilterPrecedence) ResolveToolFilterOptions(SessionConfigBase config)
    {
        ValidateToolFilterList(nameof(SessionConfigBase.AvailableTools), config.AvailableTools);
        ValidateToolFilterList(nameof(SessionConfigBase.ExcludedTools), config.ExcludedTools);

        if (_options.Mode == CopilotClientMode.Empty && config.AvailableTools is null)
        {
            throw new ArgumentException(
                "CopilotClient is in Mode = CopilotClientMode.Empty but the session config did " +
                "not specify AvailableTools. Empty mode requires every session to explicitly " +
                "opt into the tools it wants — e.g. " +
                "`AvailableTools = new ToolSet().AddBuiltIn(BuiltInTools.Isolated)`.",
                nameof(config));
        }

        return (config.AvailableTools, config.ExcludedTools, OptionsUpdateToolFilterPrecedence.Excluded);
    }

    /// <summary>
    /// Applies mode-specific defaults to a session config in place. Caller
    /// values win — only fields left unset by the caller are filled in.
    /// </summary>
    private void ApplyConfigDefaultsForMode(SessionConfigBase config)
    {
        if (_options.Mode == CopilotClientMode.Empty)
        {
            config.EnableSessionTelemetry ??= false;
            config.SkipEmbeddingRetrieval ??= true;
            config.EmbeddingCacheStorage ??= EmbeddingCacheStorageMode.InMemory;
            config.EnableOnDemandInstructionDiscovery ??= false;
            config.EnableFileHooks ??= false;
            config.EnableHostGitOperations ??= false;
            config.EnableSessionStore ??= false;
            config.EnableSkills ??= false;
            config.Memory ??= new MemoryConfiguration { Enabled = false };
            config.McpOAuthTokenStorage ??= McpOAuthTokenStorageMode.InMemory;
        }
    }

    /// <summary>
    /// Returns the <see cref="SystemMessageConfig"/> to send to the runtime,
    /// adjusted for the current mode. In empty mode the
    /// <c>environment_context</c> section is stripped unless the caller has
    /// already taken control of it; append-mode messages are promoted to
    /// customize so the env-context strip can apply alongside the caller's
    /// content (the runtime appends <see cref="SystemMessageConfig.Content"/>
    /// in both modes).
    /// </summary>
    private SystemMessageConfig? GetSystemMessageConfigForMode(SystemMessageConfig? supplied)
    {
        if (_options.Mode != CopilotClientMode.Empty)
        {
            return supplied;
        }

        if (supplied is null)
        {
            return new SystemMessageConfig
            {
                Mode = SystemMessageMode.Customize,
                Sections = new Dictionary<SystemMessageSection, SectionOverride>
                {
                    [SystemMessageSection.EnvironmentContext] = new() { Action = SectionOverrideAction.Remove },
                },
            };
        }

        switch (supplied.Mode)
        {
            case SystemMessageMode.Replace:
                return supplied;
            case SystemMessageMode.Customize:
                if (supplied.Sections is not null && supplied.Sections.ContainsKey(SystemMessageSection.EnvironmentContext))
                {
                    return supplied;
                }
                var mergedSections = supplied.Sections is null
                    ? []
                    : new Dictionary<SystemMessageSection, SectionOverride>(supplied.Sections);
                mergedSections[SystemMessageSection.EnvironmentContext] = new() { Action = SectionOverrideAction.Remove };
                return new SystemMessageConfig
                {
                    Mode = SystemMessageMode.Customize,
                    Content = supplied.Content,
                    Sections = mergedSections,
                };
            case SystemMessageMode.Append:
            case null:
                // Promote to customize so we can also strip environment_context.
                // The runtime appends Content to additional instructions in both
                // customize and append modes, so the caller's text is preserved.
                return new SystemMessageConfig
                {
                    Mode = SystemMessageMode.Customize,
                    Content = supplied.Content,
                    Sections = new Dictionary<SystemMessageSection, SectionOverride>
                    {
                        [SystemMessageSection.EnvironmentContext] = new() { Action = SectionOverrideAction.Remove },
                    },
                };
            default:
                return supplied;
        }
    }

    /// <summary>
    /// Applies the post-create / post-resume <c>session.options.update</c>
    /// patch for the current mode. In empty mode this defaults the four
    /// overridable feature flags to safe values (caller values from
    /// <paramref name="config"/> win); <c>installedPlugins=[]</c> is
    /// unconditional under empty mode so apps that need plugins must switch
    /// modes. In copilot-cli mode only explicitly-set fields are forwarded.
    /// </summary>
    private async Task UpdateSessionOptionsForModeAsync(CopilotSession session, SessionConfigBase config, CancellationToken cancellationToken)
    {
        var hasAnyPatch = false;
        bool? skipCustomInstructions = null;
        bool? customAgentsLocalOnly = null;
        bool? coauthorEnabled = null;
        bool? manageScheduleEnabled = null;
        IList<SessionInstalledPlugin>? installedPlugins = null;

        if (_options.Mode == CopilotClientMode.Empty)
        {
            skipCustomInstructions = config.SkipCustomInstructions ?? true;
            customAgentsLocalOnly = config.CustomAgentsLocalOnly ?? true;
            coauthorEnabled = config.CoauthorEnabled ?? false;
            manageScheduleEnabled = config.ManageScheduleEnabled ?? false;
            installedPlugins = [];
            hasAnyPatch = true;
        }
        else
        {
            if (config.SkipCustomInstructions is not null) { skipCustomInstructions = config.SkipCustomInstructions; hasAnyPatch = true; }
            if (config.CustomAgentsLocalOnly is not null) { customAgentsLocalOnly = config.CustomAgentsLocalOnly; hasAnyPatch = true; }
            if (config.CoauthorEnabled is not null) { coauthorEnabled = config.CoauthorEnabled; hasAnyPatch = true; }
            if (config.ManageScheduleEnabled is not null) { manageScheduleEnabled = config.ManageScheduleEnabled; hasAnyPatch = true; }
        }

        if (!hasAnyPatch) return;

        try
        {
#pragma warning disable GHCP001
            await session.Rpc.Options.UpdateAsync(
                skipCustomInstructions: skipCustomInstructions,
                customAgentsLocalOnly: customAgentsLocalOnly,
                coauthorEnabled: coauthorEnabled,
                manageScheduleEnabled: manageScheduleEnabled,
                installedPlugins: installedPlugins,
                cancellationToken: cancellationToken).ConfigureAwait(false);
#pragma warning restore GHCP001
        }
        catch
        {
            // The runtime session exists but the post-create options
            // patch failed — best-effort destroy so we don't leak it
            // (in empty mode it would otherwise stay alive with
            // permissive defaults).
            try
            {
                await session.DisposeAsync().ConfigureAwait(false);
            }
            catch
            {
                // Swallow: original error is what the caller needs.
            }
            throw;
        }
    }

    /// <summary>
    /// Creates a new Copilot session with the specified configuration.
    /// </summary>
    /// <param name="config">Configuration for the session.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves to provide the <see cref="CopilotSession"/>.</returns>
    /// <remarks>
    /// Sessions maintain conversation state, handle events, and manage tool execution.
    /// If the client is not connected,
    /// this will automatically start the connection.
    /// </remarks>
    /// <example>
    /// <code>
    /// // Basic session
    /// var session = await client.CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll });
    ///
    /// // Session with model and tools
    /// var session = await client.CreateSessionAsync(new()
    /// {
    ///     OnPermissionRequest = PermissionHandler.ApproveAll,
    ///     Model = "gpt-4",
    ///     Tools = [AIFunctionFactory.Create(MyToolMethod)]
    /// });
    /// </code>
    /// </example>
    public async Task<CopilotSession> CreateSessionAsync(SessionConfig config, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(config);

        var connection = await EnsureConnectedAsync(cancellationToken);
        var totalTimestamp = Stopwatch.GetTimestamp();

        ApplyConfigDefaultsForMode(config);
        config.SystemMessage = GetSystemMessageConfigForMode(config.SystemMessage);
        var toolFilter = ResolveToolFilterOptions(config);

        var hasHooks = config.Hooks != null && (
            config.Hooks.OnPreToolUse != null ||
            config.Hooks.OnPreMcpToolCall != null ||
            config.Hooks.OnPostToolUse != null ||
            config.Hooks.OnPostToolUseFailure != null ||
            config.Hooks.OnUserPromptSubmitted != null ||
            config.Hooks.OnSessionStart != null ||
            config.Hooks.OnSessionEnd != null ||
            config.Hooks.OnErrorOccurred != null);

        var (wireSystemMessage, transformCallbacks) = ExtractTransformCallbacks(config.SystemMessage);

        // For cloud sessions, let the CLI/server assign the session id and
        // register the session lazily once the response arrives. For non-cloud
        // sessions we generate the id client-side (when the caller didn't
        // supply one) so the session can be registered BEFORE the RPC — the
        // CLI may issue session-scoped requests (e.g. sessionFs.WriteFile
        // for workspace metadata) during session.create processing, before
        // it has sent the response.
        var useServerGeneratedId = config.Cloud != null && string.IsNullOrEmpty(config.SessionId);
        var localSessionId = useServerGeneratedId
            ? null
            : (string.IsNullOrEmpty(config.SessionId) ? Guid.NewGuid().ToString() : config.SessionId);

        CopilotSession? session = null;
        if (localSessionId != null)
        {
            session = InitializeSession(
                localSessionId,
                connection.Rpc,
                config,
                transformCallbacks,
                hasHooks,
                "CopilotClient.CreateSessionAsync");
        }
        try
        {
            var (traceparent, tracestate) = TelemetryHelpers.GetTraceContext();

            var request = new CreateSessionRequest(
                config.Model,
                localSessionId,
                config.ClientName,
                config.ReasoningEffort,
                config.ReasoningSummary,
                config.ContextTier,
                config.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
                wireSystemMessage,
                toolFilter.AvailableTools,
                toolFilter.ExcludedTools,
                config.Provider,
                config.EnableSessionTelemetry,
                config.OnPermissionRequest != null ? true : null,
                config.OnUserInputRequest != null ? true : null,
                config.OnExitPlanModeRequest != null ? true : null,
                config.OnAutoModeSwitchRequest != null ? true : null,
                hasHooks ? true : null,
                config.WorkingDirectory,
                config.Streaming is true ? true : null,
                config.IncludeSubAgentStreamingEvents,
                config.McpServers,
                config.McpOAuthTokenStorage,
                "direct",
                config.CustomAgents,
                config.DefaultAgent,
                config.Agent,
                config.ConfigDirectory,
                config.EnableConfigDiscovery,
                config.SkipEmbeddingRetrieval,
                config.EmbeddingCacheStorage,
                config.OrganizationCustomInstructions,
                config.EnableOnDemandInstructionDiscovery,
                config.EnableFileHooks,
                config.EnableHostGitOperations,
                config.EnableSessionStore,
                config.EnableSkills,
                config.SkillDirectories,
                config.DisabledSkills,
                config.InfiniteSessions,
                Commands: config.Commands?.Select(c => new CommandWireDefinition(c.Name, c.Description)).ToList(),
                RequestElicitation: config.OnElicitationRequest != null,
                RequestMcpApps: config.EnableMcpApps ? true : null,
                Traceparent: traceparent,
                Tracestate: tracestate,
                ModelCapabilities: config.ModelCapabilities,
                GitHubToken: config.GitHubToken,
                RemoteSession: config.RemoteSession,
                Cloud: config.Cloud,
                InstructionDirectories: config.InstructionDirectories,
                PluginDirectories: config.PluginDirectories,
                LargeOutput: config.LargeOutput,
                Memory: config.Memory,
                Canvases: config.Canvases,
                RequestCanvasRenderer: config.RequestCanvasRenderer,
                RequestExtensions: config.RequestExtensions,
                ExtensionSdkPath: config.ExtensionSdkPath,
                ExtensionInfo: config.ExtensionInfo,
                Providers: config.Providers,
                Models: config.Models,
                ToolFilterPrecedence: toolFilter.ToolFilterPrecedence,
                ExpAssignments: config.ExpAssignments);

            var rpcTimestamp = Stopwatch.GetTimestamp();

            // For the server-assigned (cloud) path, register the session
            // synchronously from the read loop the instant the response
            // arrives. This closes the small window where a session.event
            // notification could arrive after the response but before the
            // awaiter resumes — without this hook the dispatcher would
            // silently drop those events. Non-cloud sessions are already
            // registered above (before the RPC).
            Action<JsonElement>? onResponseInline = session != null ? null : raw =>
            {
                if (raw.ValueKind is JsonValueKind.Object
                    && raw.TryGetProperty("sessionId", out var sessionIdProp)
                    && sessionIdProp.ValueKind is JsonValueKind.String
                    && sessionIdProp.GetString() is string sessionId
                    && !string.IsNullOrEmpty(sessionId))
                {
                    session = InitializeSession(
                        sessionId,
                        connection.Rpc,
                        config,
                        transformCallbacks,
                        hasHooks,
                        "CopilotClient.CreateSessionAsync");
                }
            };

            var response = await InvokeRpcAsync<CreateSessionResponse>(
                connection.Rpc, "session.create", [request], null, cancellationToken, onResponseInline);
            LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                "CopilotClient.CreateSessionAsync session creation request completed successfully. Elapsed={Elapsed}, SessionId={SessionId}",
                rpcTimestamp,
                response.SessionId);

            if (session is null)
            {
                throw new InvalidOperationException("session.create response did not include a sessionId.");
            }

            if (localSessionId != null && !string.Equals(localSessionId, response.SessionId, StringComparison.Ordinal))
            {
                throw new InvalidOperationException(
                    $"session.create returned sessionId {response.SessionId} but the caller requested {localSessionId}.");
            }

            session.WorkspacePath = response.WorkspacePath;
            session.SetCapabilities(response.Capabilities);
            session.SetOpenCanvases(response.OpenCanvases);

            await UpdateSessionOptionsForModeAsync(session, config, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            session?.RemoveFromClient();

            if (ex is not OperationCanceledException)
            {
                LoggingHelpers.LogTiming(_logger, LogLevel.Warning, ex,
                    "CopilotClient.CreateSessionAsync failed. Elapsed={Elapsed}, SessionId={SessionId}",
                    totalTimestamp,
                    session?.SessionId);
            }

            throw;
        }

        LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
            "CopilotClient.CreateSessionAsync complete. Elapsed={Elapsed}, SessionId={SessionId}",
            totalTimestamp,
            session.SessionId);
        return session;
    }

    /// <summary>
    /// Resumes an existing Copilot session with the specified configuration.
    /// </summary>
    /// <param name="sessionId">The ID of the session to resume.</param>
    /// <param name="config">Configuration for the resumed session.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves to provide the <see cref="CopilotSession"/>.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the session does not exist or the client is not connected.</exception>
    /// <remarks>
    /// This allows you to continue a previous conversation, maintaining all conversation history.
    /// The session must have been previously created and not deleted.
    /// </remarks>
    /// <example>
    /// <code>
    /// // Resume a previous session
    /// var session = await client.ResumeSessionAsync("session-123", new() { OnPermissionRequest = PermissionHandler.ApproveAll });
    ///
    /// // Resume with new tools
    /// var session = await client.ResumeSessionAsync("session-123", new()
    /// {
    ///     OnPermissionRequest = PermissionHandler.ApproveAll,
    ///     Tools = [AIFunctionFactory.Create(MyNewToolMethod)]
    /// });
    /// </code>
    /// </example>
    public async Task<CopilotSession> ResumeSessionAsync(string sessionId, ResumeSessionConfig config, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);
        ArgumentNullException.ThrowIfNull(config);

        var connection = await EnsureConnectedAsync(cancellationToken);
        var totalTimestamp = Stopwatch.GetTimestamp();

        ApplyConfigDefaultsForMode(config);
        config.SystemMessage = GetSystemMessageConfigForMode(config.SystemMessage);
        var toolFilter = ResolveToolFilterOptions(config);

        var hasHooks = config.Hooks != null && (
            config.Hooks.OnPreToolUse != null ||
            config.Hooks.OnPreMcpToolCall != null ||
            config.Hooks.OnPostToolUse != null ||
            config.Hooks.OnPostToolUseFailure != null ||
            config.Hooks.OnUserPromptSubmitted != null ||
            config.Hooks.OnSessionStart != null ||
            config.Hooks.OnSessionEnd != null ||
            config.Hooks.OnErrorOccurred != null);

        var (wireSystemMessage, transformCallbacks) = ExtractTransformCallbacks(config.SystemMessage);

        // Create and register the session before issuing the RPC so that
        // events emitted by the CLI (e.g. session.start) are not dropped.
        var session = InitializeSession(
            sessionId,
            connection.Rpc,
            config,
            transformCallbacks,
            hasHooks,
            "CopilotClient.ResumeSessionAsync");

        try
        {
            var (traceparent, tracestate) = TelemetryHelpers.GetTraceContext();

            var request = new ResumeSessionRequest(
                sessionId,
                config.ClientName,
                config.Model,
                config.ReasoningEffort,
                config.ReasoningSummary,
                config.ContextTier,
                config.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
                wireSystemMessage,
                toolFilter.AvailableTools,
                toolFilter.ExcludedTools,
                config.Provider,
                config.EnableSessionTelemetry,
                config.OnPermissionRequest != null ? true : null,
                config.OnUserInputRequest != null ? true : null,
                config.OnExitPlanModeRequest != null ? true : null,
                config.OnAutoModeSwitchRequest != null ? true : null,
                hasHooks ? true : null,
                config.WorkingDirectory,
                config.ConfigDirectory,
                config.EnableConfigDiscovery,
                config.SkipEmbeddingRetrieval,
                config.EmbeddingCacheStorage,
                config.OrganizationCustomInstructions,
                config.EnableOnDemandInstructionDiscovery,
                config.EnableFileHooks,
                config.EnableHostGitOperations,
                config.EnableSessionStore,
                config.EnableSkills,
                config.SuppressResumeEvent is true ? true : null,
                config.Streaming is true ? true : null,
                config.IncludeSubAgentStreamingEvents,
                config.McpServers,
                config.McpOAuthTokenStorage,
                "direct",
                config.CustomAgents,
                config.DefaultAgent,
                config.Agent,
                config.SkillDirectories,
                config.DisabledSkills,
                config.InfiniteSessions,
                Commands: config.Commands?.Select(c => new CommandWireDefinition(c.Name, c.Description)).ToList(),
                RequestElicitation: config.OnElicitationRequest != null,
                RequestMcpApps: config.EnableMcpApps ? true : null,
                Traceparent: traceparent,
                Tracestate: tracestate,
                ModelCapabilities: config.ModelCapabilities,
                GitHubToken: config.GitHubToken,
                RemoteSession: config.RemoteSession,
                ContinuePendingWork: config.ContinuePendingWork,
                InstructionDirectories: config.InstructionDirectories,
                PluginDirectories: config.PluginDirectories,
                LargeOutput: config.LargeOutput,
                Memory: config.Memory,
                Canvases: config.Canvases,
                RequestCanvasRenderer: config.RequestCanvasRenderer,
                RequestExtensions: config.RequestExtensions,
                ExtensionSdkPath: config.ExtensionSdkPath,
                ExtensionInfo: config.ExtensionInfo,
                OpenCanvases: config.OpenCanvases,
                Providers: config.Providers,
                Models: config.Models,
                ToolFilterPrecedence: toolFilter.ToolFilterPrecedence,
                ExpAssignments: config.ExpAssignments);

            var rpcTimestamp = Stopwatch.GetTimestamp();
            var response = await InvokeRpcAsync<ResumeSessionResponse>(
                connection.Rpc, "session.resume", [request], cancellationToken);
            LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                "CopilotClient.ResumeSessionAsync session resume request completed successfully. Elapsed={Elapsed}, SessionId={SessionId}",
                rpcTimestamp,
                sessionId);

            session.WorkspacePath = response.WorkspacePath;
            session.SetCapabilities(response.Capabilities);
            session.SetOpenCanvases(response.OpenCanvases);

            await UpdateSessionOptionsForModeAsync(session, config, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            session.RemoveFromClient();
            if (ex is not OperationCanceledException)
            {
                LoggingHelpers.LogTiming(_logger, LogLevel.Warning, ex,
                    "CopilotClient.ResumeSessionAsync failed. Elapsed={Elapsed}, SessionId={SessionId}",
                    totalTimestamp,
                    sessionId);
            }
            throw;
        }

        LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
            "CopilotClient.ResumeSessionAsync complete. Elapsed={Elapsed}, SessionId={SessionId}",
            totalTimestamp,
            sessionId);
        return session;
    }

    /// <summary>
    /// Validates the health of the connection by sending a ping request.
    /// </summary>
    /// <param name="message">An optional message that will be reflected back in the response.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the <see cref="PingResponse"/> containing the message and server timestamp.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected.</exception>
    /// <example>
    /// <code>
    /// var response = await client.PingAsync("health check");
    /// Console.WriteLine($"Server responded at {response.Timestamp}");
    /// </code>
    /// </example>
    public async Task<PingResponse> PingAsync(string? message = null, CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        return await InvokeRpcAsync<PingResponse>(
            connection.Rpc, "ping", [new PingRequest { Message = message }], cancellationToken);
    }

    /// <summary>
    /// Gets CLI status including version and protocol information.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the status response containing version and protocol version.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected.</exception>
    public async Task<GetStatusResponse> GetStatusAsync(CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        return await InvokeRpcAsync<GetStatusResponse>(
            connection.Rpc, "status.get", [], cancellationToken);
    }

    /// <summary>
    /// Gets current authentication status.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the authentication status.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected.</exception>
    public async Task<GetAuthStatusResponse> GetAuthStatusAsync(CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        return await InvokeRpcAsync<GetAuthStatusResponse>(
            connection.Rpc, "auth.getStatus", [], cancellationToken);
    }

    /// <summary>
    /// Lists available models with their metadata.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with a list of available models.</returns>
    /// <remarks>
    /// Results are cached after the first successful call to avoid rate limiting.
    /// The cache is cleared when the client disconnects.
    /// </remarks>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected or not authenticated.</exception>
    public async Task<IList<ModelInfo>> ListModelsAsync(CancellationToken cancellationToken = default)
    {
        if (_modelsCacheLock is null)
        {
            Interlocked.CompareExchange(ref _modelsCacheLock, new(1, 1), null);
        }

        await _modelsCacheLock.WaitAsync(cancellationToken);
        try
        {
            // Check cache (already inside lock)
            if (_modelsCache is null)
            {
                IList<ModelInfo> models;
                if (_onListModels is not null)
                {
                    // Use custom handler instead of CLI RPC
                    models = await _onListModels(cancellationToken);
                }
                else
                {
                    var connection = await EnsureConnectedAsync(cancellationToken);

                    // Cache miss - fetch from backend while holding lock
                    var response = await InvokeRpcAsync<GetModelsResponse>(
                        connection.Rpc, "models.list", [], cancellationToken);
                    models = response.Models;
                }

                // Update cache before releasing lock (copy to prevent external mutation)
                _modelsCache = [.. models];
            }

            return [.. _modelsCache]; // Return a copy to prevent cache mutation
        }
        finally
        {
            _modelsCacheLock.Release();
        }
    }

    /// <summary>
    /// Gets the ID of the most recently used session.
    /// </summary>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the session ID, or null if no sessions exist.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected.</exception>
    /// <example>
    /// <code>
    /// var lastId = await client.GetLastSessionIdAsync();
    /// if (lastId != null)
    /// {
    ///     var session = await client.ResumeSessionAsync(lastId, new() { OnPermissionRequest = PermissionHandler.ApproveAll });
    /// }
    /// </code>
    /// </example>
    public async Task<string?> GetLastSessionIdAsync(CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<GetLastSessionIdResponse>(
            connection.Rpc, "session.getLastId", [], cancellationToken);

        return response.SessionId;
    }

    /// <summary>
    /// Permanently deletes a session and all its data from disk, including
    /// conversation history, planning state, and artifacts.
    /// </summary>
    /// <param name="sessionId">The ID of the session to delete.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that represents the asynchronous delete operation.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the session does not exist or deletion fails.</exception>
    /// <remarks>
    /// Unlike <see cref="CopilotSession.DisposeAsync"/>, which only releases in-memory
    /// resources and preserves session data for later resumption, this method is
    /// irreversible. The session cannot be resumed after deletion.
    /// </remarks>
    /// <example>
    /// <code>
    /// await client.DeleteSessionAsync("session-123");
    /// </code>
    /// </example>
    public async Task DeleteSessionAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<DeleteSessionResponse>(
            connection.Rpc, "session.delete", [new DeleteSessionRequest(sessionId)], cancellationToken);

        if (!response.Success)
        {
            throw new InvalidOperationException($"Failed to delete session {sessionId}: {response.Error}");
        }

        RemoveSession(sessionId);
    }

    /// <summary>
    /// Lists all sessions known to the Copilot server.
    /// </summary>
    /// <param name="filter">Optional filter to narrow down the session list by cwd, git root, repository, or branch.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with a list of <see cref="SessionMetadata"/> for all available sessions.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected.</exception>
    /// <example>
    /// <code>
    /// var sessions = await client.ListSessionsAsync();
    /// foreach (var session in sessions)
    /// {
    ///     Console.WriteLine($"{session.SessionId}: {session.Summary}");
    /// }
    /// </code>
    /// </example>
    public async Task<IList<SessionMetadata>> ListSessionsAsync(SessionListFilter? filter = null, CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<ListSessionsResponse>(
            connection.Rpc, "session.list", [new ListSessionsRequest(filter)], cancellationToken);

        return response.Sessions;
    }

    /// <summary>
    /// Gets metadata for a specific session by ID.
    /// </summary>
    /// <remarks>
    /// This provides an efficient O(1) lookup of a single session's metadata
    /// instead of listing all sessions.
    /// </remarks>
    /// <param name="sessionId">The ID of the session to look up.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves with the <see cref="SessionMetadata"/>, or null if the session was not found.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected.</exception>
    /// <example>
    /// <code>
    /// var metadata = await client.GetSessionMetadataAsync("session-123");
    /// if (metadata != null)
    /// {
    ///     Console.WriteLine($"Session started at: {metadata.StartTime}");
    /// }
    /// </code>
    /// </example>
    public async Task<SessionMetadata?> GetSessionMetadataAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<GetSessionMetadataResponse>(
            connection.Rpc, "session.getMetadata", [new GetSessionMetadataRequest(sessionId)], cancellationToken);

        return response.Session;
    }

    /// <summary>
    /// Gets the ID of the session currently displayed in the TUI.
    /// </summary>
    /// <remarks>
    /// This is only available when connecting to a server running in TUI+server mode
    /// (--ui-server).
    /// </remarks>
    /// <param name="cancellationToken">A token to cancel the operation.</param>
    /// <returns>The session ID, or null if no foreground session is set.</returns>
    /// <example>
    /// <code>
    /// var sessionId = await client.GetForegroundSessionIdAsync();
    /// if (sessionId != null)
    /// {
    ///     Console.WriteLine($"TUI is displaying session: {sessionId}");
    /// }
    /// </code>
    /// </example>
    public async Task<string?> GetForegroundSessionIdAsync(CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<GetForegroundSessionResponse>(
            connection.Rpc, "session.getForeground", [], cancellationToken);

        return response.SessionId;
    }

    /// <summary>
    /// Requests the TUI to switch to displaying the specified session.
    /// </summary>
    /// <remarks>
    /// This is only available when connecting to a server running in TUI+server mode
    /// (--ui-server).
    /// </remarks>
    /// <param name="sessionId">The ID of the session to display in the TUI.</param>
    /// <param name="cancellationToken">A token to cancel the operation.</param>
    /// <exception cref="InvalidOperationException">Thrown if the operation fails.</exception>
    /// <example>
    /// <code>
    /// await client.SetForegroundSessionIdAsync("session-123");
    /// </code>
    /// </example>
    public async Task SetForegroundSessionIdAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<SetForegroundSessionResponse>(
            connection.Rpc, "session.setForeground", [new SetForegroundSessionRequest(sessionId)], cancellationToken);

        if (!response.Success)
        {
            throw new InvalidOperationException(response.Error ?? "Failed to set foreground session");
        }
    }

    /// <summary>
    /// Subscribes to session lifecycle events of a specific kind.
    /// </summary>
    /// <typeparam name="T">
    /// The lifecycle event type to listen for. Pass a derived type such as
    /// <see cref="SessionCreatedEvent"/> to filter by kind, or
    /// <see cref="SessionLifecycleEvent"/> to receive every lifecycle event.
    /// </typeparam>
    /// <param name="handler">A callback invoked when a matching lifecycle event arrives.</param>
    /// <returns>An <see cref="IDisposable"/> that, when disposed, unsubscribes the handler.</returns>
    /// <example>
    /// <code>
    /// using var sub = client.OnLifecycle&lt;SessionForegroundEvent&gt;(evt =&gt;
    /// {
    ///     Console.WriteLine($"Session {evt.SessionId} is now in foreground");
    /// });
    /// </code>
    /// </example>
    public IDisposable OnLifecycle<T>(Action<T> handler) where T : SessionLifecycleEvent
    {
        ArgumentNullException.ThrowIfNull(handler);

        var subscription = new LifecycleSubscription(typeof(T), evt => handler((T)evt));

        lock (_lifecycleHandlers)
        {
            _lifecycleHandlers.Add(subscription);
        }

        return new ActionDisposable(() =>
        {
            lock (_lifecycleHandlers)
            {
                _lifecycleHandlers.Remove(subscription);
            }
        });
    }

    private void DispatchLifecycleEvent(SessionLifecycleEvent evt)
    {
        LifecycleSubscription[] snapshot;
        lock (_lifecycleHandlers)
        {
            snapshot = _lifecycleHandlers.ToArray();
        }

        var eventType = evt.GetType();
        foreach (var subscription in snapshot)
        {
            if (subscription.EventType.IsAssignableFrom(eventType))
            {
                try { subscription.Handler(evt); } catch { /* Ignore handler errors */ }
            }
        }
    }

    internal static Task<T> InvokeRpcAsync<T>(JsonRpc rpc, string method, object?[]? args, CancellationToken cancellationToken)
    {
        return InvokeRpcAsync<T>(rpc, method, args, null, cancellationToken);
    }

    internal static Task InvokeRpcAsync(JsonRpc rpc, string method, object?[]? args, CancellationToken cancellationToken)
    {
        return InvokeRpcAsync<object>(rpc, method, args, null, cancellationToken);
    }

    internal static Task<T> InvokeRpcAsync<T>(SessionRpc rpc, string method, object?[]? args, CancellationToken cancellationToken)
    {
        return InvokeRpcAsync<T>(rpc.Session.JsonRpc, method, args, cancellationToken);
    }

    internal static Task InvokeRpcAsync(SessionRpc rpc, string method, object?[]? args, CancellationToken cancellationToken)
    {
        return InvokeRpcAsync<object>(rpc, method, args, cancellationToken);
    }

    internal static async Task<T> InvokeRpcAsync<T>(JsonRpc rpc, string method, object?[]? args, StringBuilder? stderrBuffer, CancellationToken cancellationToken, Action<JsonElement>? onResponseInline = null)
    {
        try
        {
            return await rpc.InvokeAsync<T>(method, args, cancellationToken, onResponseInline);
        }
        catch (ConnectionLostException ex)
        {
            string? stderrOutput = null;
            if (stderrBuffer is not null)
            {
                lock (stderrBuffer)
                {
                    stderrOutput = stderrBuffer.ToString().Trim();
                }
            }

            if (!string.IsNullOrEmpty(stderrOutput))
            {
                throw new IOException(FormatCliExitedMessage("CLI process exited unexpectedly.", stderrOutput!), ex);
            }

            throw new IOException($"Communication error with Copilot CLI: {ex.Message}", ex);
        }
        catch (RemoteRpcException ex)
        {
            throw new IOException($"Communication error with Copilot CLI: {ex.Message}", ex);
        }
    }

    private static string FormatCliExitedMessage(string message, string stderrOutput)
    {
        return string.IsNullOrEmpty(stderrOutput)
            ? message
            : $"{message}\nstderr: {stderrOutput}";
    }

    [LoggerMessage(
        Level = LogLevel.Information,
        Message = "CopilotClient.StartCliServerAsync starting Copilot CLI. CliPath={CliPath}, Executable={Executable}, CliPathSource={CliPathSource}, UseStdio={UseStdio}, Port={Port}")]
    private static partial void LogStartingCopilotCli(ILogger logger, string cliPath, string executable, string cliPathSource, bool useStdio, int? port);

    [LoggerMessage(
        Level = LogLevel.Information,
        Message = "CopilotClient.ConnectToServerAsync connecting to CLI server. Host={Host}, Port={Port}")]
    private static partial void LogConnectingToCliServer(ILogger logger, string host, int port);

    private static IOException CreateCliExitedException(string message, StringBuilder stderrBuffer)
    {
        string stderrOutput;
        lock (stderrBuffer)
        {
            stderrOutput = stderrBuffer.ToString().Trim();
        }

        return new IOException(FormatCliExitedMessage(message, stderrOutput));
    }

    private Task<Connection> EnsureConnectedAsync(CancellationToken cancellationToken)
    {
        // If already started or starting, this will return the existing task
        return (Task<Connection>)StartAsync(cancellationToken);
    }

    private async Task ConfigureSessionFsAsync(CancellationToken cancellationToken)
    {
        if (_options.SessionFs is null)
        {
            return;
        }

        await Rpc.SessionFs.SetProviderAsync(
            _options.SessionFs.InitialWorkingDirectory,
            _options.SessionFs.SessionStatePath,
            _options.SessionFs.Conventions,
            _options.SessionFs.Capabilities,
            cancellationToken: cancellationToken);
    }

    private void ConfigureSessionFsHandlers(CopilotSession session, Func<CopilotSession, SessionFsProvider>? createSessionFsHandler)
    {
        if (_options.SessionFs is null)
        {
            return;
        }

        if (createSessionFsHandler is null)
        {
            throw new InvalidOperationException(
                "CreateSessionFsProvider is required in the session config when CopilotClientOptions.SessionFs is configured.");
        }

        var provider = createSessionFsHandler(session)
            ?? throw new InvalidOperationException("CreateSessionFsProvider returned null.");

        if (_options.SessionFs.Capabilities?.Sqlite == true && provider is not ISessionFsSqliteProvider)
        {
            throw new InvalidOperationException(
                "SessionFsConfig declares capabilities.sqlite but the provider does not implement ISessionFsSqliteProvider.");
        }

        session.ClientSessionApis.SessionFs = provider;
    }

    private async Task VerifyProtocolVersionAsync(Connection connection, CancellationToken cancellationToken)
    {
        var handshakeTimestamp = Stopwatch.GetTimestamp();
        var usedFallbackPing = false;
        var maxVersion = SdkProtocolVersion.GetVersion();
        int? serverVersion;
        try
        {
            var token = _connection switch
            {
                TcpRuntimeConnection tcp => tcp.ConnectionToken,
                UriRuntimeConnection uri => uri.ConnectionToken,
                _ => null,
            };
            var connectResponse = await InvokeRpcAsync<ConnectResult>(
                connection.Rpc, "connect", [new ConnectRequest { Token = token }], connection.StderrBuffer, cancellationToken);
            serverVersion = (int)connectResponse.ProtocolVersion;
        }
        catch (IOException ex) when (ex.InnerException is RemoteRpcException remoteEx && IsUnsupportedConnectMethod(remoteEx))
        {
            // Legacy server without `connect`; fall back to `ping`. A token, if any,
            // is silently dropped — the legacy server can't enforce one.
            usedFallbackPing = true;
            var pingResponse = await InvokeRpcAsync<PingResponse>(
                connection.Rpc, "ping", [new PingRequest()], connection.StderrBuffer, cancellationToken);
            serverVersion = pingResponse.ProtocolVersion;
        }

        if (!serverVersion.HasValue)
        {
            throw new InvalidOperationException(
                $"SDK protocol version mismatch: SDK supports versions {MinProtocolVersion}-{maxVersion}, " +
                $"but server does not report a protocol version. " +
                $"Please update your server to ensure compatibility.");
        }

        if (serverVersion.Value < MinProtocolVersion || serverVersion.Value > maxVersion)
        {
            throw new InvalidOperationException(
                $"SDK protocol version mismatch: SDK supports versions {MinProtocolVersion}-{maxVersion}, " +
                $"but server reports version {serverVersion.Value}. " +
                $"Please update your SDK or server to ensure compatibility.");
        }

        _negotiatedProtocolVersion = serverVersion.Value;
        LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
            "CopilotClient.VerifyProtocolVersionAsync protocol handshake complete. Elapsed={Elapsed}, ProtocolVersion={ProtocolVersion}, UsedFallbackPing={UsedFallbackPing}",
            handshakeTimestamp,
            serverVersion.Value,
            usedFallbackPing);
    }

    private static bool IsUnsupportedConnectMethod(RemoteRpcException ex)
    {
        return ex.ErrorCode == RemoteRpcException.MethodNotFoundErrorCode
            || string.Equals(ex.Message, "Unhandled method connect", StringComparison.Ordinal);
    }

    private async Task<(Process Process, int? DetectedLocalhostTcpPort, ProcessStderrPump StderrPump)> StartCliServerAsync(CancellationToken cancellationToken)
    {
        var options = _options;
        var logger = _logger;
        var childProcessConnection = (ChildProcessRuntimeConnection)_connection;
        var tcpConnection = _connection as TcpRuntimeConnection;
        var useStdio = _connection is StdioRuntimeConnection;

        // Use explicit path, COPILOT_CLI_PATH env var (from options.Environment or process env), or bundled runtime - no PATH fallback
        var envCliPath = options.Environment is not null && options.Environment.TryGetValue("COPILOT_CLI_PATH", out var envValue) ? envValue
            : System.Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
        var cliPath = childProcessConnection.Path
            ?? envCliPath
            ?? GetBundledCliPath(out var searchedPath)
            ?? throw new InvalidOperationException($"Copilot runtime not found at '{searchedPath}'. Ensure the SDK NuGet package was restored correctly or provide an explicit RuntimeConnection.ForStdio(path: ...) / RuntimeConnection.ForTcp(path: ...).");
        var cliPathSource = childProcessConnection.Path is not null ? "Options" : envCliPath is not null ? "Environment" : "Bundled";
        var args = new List<string>();

        if (childProcessConnection.Args != null)
        {
            args.AddRange(childProcessConnection.Args);
        }

        args.AddRange(["--headless", "--no-auto-update"]);
        if (options.LogLevel is { } logLevel && !string.IsNullOrEmpty(logLevel.Value))
        {
            args.AddRange(["--log-level", logLevel.Value]);
        }

        if (useStdio)
        {
            args.Add("--stdio");
        }
        else if (tcpConnection is { Port: > 0 } tcp)
        {
            args.AddRange(["--port", tcp.Port.ToString(CultureInfo.InvariantCulture)]);
        }

        // Add auth-related flags
        if (!string.IsNullOrEmpty(options.GitHubToken))
        {
            args.AddRange(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"]);
        }

        // Default UseLoggedInUser to false when GitHubToken is provided
        var useLoggedInUser = options.UseLoggedInUser ?? string.IsNullOrEmpty(options.GitHubToken);
        if (!useLoggedInUser)
        {
            args.Add("--no-auto-login");
        }

        if (options.SessionIdleTimeoutSeconds is > 0)
        {
            args.AddRange(["--session-idle-timeout", options.SessionIdleTimeoutSeconds.Value.ToString(CultureInfo.InvariantCulture)]);
        }

        if (options.EnableRemoteSessions)
        {
            args.Add("--remote");
        }

        var (fileName, processArgs) = ResolveCliCommand(cliPath, args);
        var configuredPort = useStdio ? (int?)null : tcpConnection?.Port;
        LogStartingCopilotCli(logger, cliPath, fileName, cliPathSource, useStdio, configuredPort);

        var startInfo = new ProcessStartInfo
        {
            FileName = fileName,
            Arguments = string.Join(" ", processArgs.Select(ProcessArgumentEscaper.Escape)),
            UseShellExecute = false,
            RedirectStandardInput = useStdio,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            WorkingDirectory = options.WorkingDirectory,
            CreateNoWindow = true
        };

        if (options.Environment != null)
        {
            startInfo.Environment.Clear();
            foreach (var (key, value) in options.Environment)
            {
                startInfo.Environment[key] = value;
            }
        }

        startInfo.Environment.Remove("NODE_DEBUG");

        // Set auth token in environment if provided
        if (!string.IsNullOrEmpty(options.GitHubToken))
        {
            startInfo.Environment["COPILOT_SDK_AUTH_TOKEN"] = options.GitHubToken;
        }

        if (tcpConnection?.ConnectionToken is { Length: > 0 } token)
        {
            startInfo.Environment["COPILOT_CONNECTION_TOKEN"] = token;
        }

        if (!string.IsNullOrEmpty(options.BaseDirectory))
        {
            startInfo.Environment["COPILOT_HOME"] = options.BaseDirectory;
        }

        // In empty mode, disable the system keychain. Keytar reads from a
        // process-wide store that's shared across sessions, which is unsafe
        // for multi-tenant hosts. The runtime falls back to file-based
        // credential storage scoped to COPILOT_HOME.
        if (options.Mode == CopilotClientMode.Empty)
        {
            startInfo.Environment["COPILOT_DISABLE_KEYTAR"] = "1";
        }

        // Set telemetry environment variables if configured
        if (options.Telemetry is { } telemetry)
        {
            startInfo.Environment["COPILOT_OTEL_ENABLED"] = "true";
            if (telemetry.OtlpEndpoint is not null) startInfo.Environment["OTEL_EXPORTER_OTLP_ENDPOINT"] = telemetry.OtlpEndpoint;
            if (telemetry.OtlpProtocol is not null) startInfo.Environment["OTEL_EXPORTER_OTLP_PROTOCOL"] = telemetry.OtlpProtocol;
            if (telemetry.FilePath is not null) startInfo.Environment["COPILOT_OTEL_FILE_EXPORTER_PATH"] = telemetry.FilePath;
            if (telemetry.ExporterType is not null) startInfo.Environment["COPILOT_OTEL_EXPORTER_TYPE"] = telemetry.ExporterType;
            if (telemetry.SourceName is not null) startInfo.Environment["COPILOT_OTEL_SOURCE_NAME"] = telemetry.SourceName;
            if (telemetry.CaptureContent is { } capture) startInfo.Environment["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = capture ? "true" : "false";
        }

        var cliProcess = new Process { StartInfo = startInfo };
        try
        {
            var spawnTimestamp = Stopwatch.GetTimestamp();
            cliProcess.Start();
            LoggingHelpers.LogTiming(logger, LogLevel.Debug, null,
                "CopilotClient.StartCliServerAsync subprocess spawned. Elapsed={Elapsed}",
                spawnTimestamp);
        }
        catch
        {
            cliProcess.Dispose();
            throw;
        }

        ProcessStderrPump? stderrPump = null;
        int? detectedLocalhostTcpPort = null;
        try
        {
            // Capture stderr for error messages and forward to logger.
            // The pump has its own lifetime token and is later cancelled/observed
            // by the owning Connection before the process is disposed.
            stderrPump = ProcessStderrPump.Start(cliProcess, logger);

            if (!useStdio)
            {
                // Wait for port announcement
                var portWaitTimestamp = Stopwatch.GetTimestamp();
                using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
                cts.CancelAfter(TimeSpan.FromSeconds(30));

                try
                {
                    while (await cliProcess.StandardOutput.ReadLineAsync(cts.Token) is string line)
                    {
                        if (logger.IsEnabled(LogLevel.Debug))
                        {
                            logger.LogDebug("[CLI] {Line}", line);
                        }

                        if (ListeningOnPortRegex().Match(line) is { Success: true } match)
                        {
                            detectedLocalhostTcpPort = int.Parse(match.Groups[1].Value, CultureInfo.InvariantCulture);
                            LoggingHelpers.LogTiming(logger, LogLevel.Debug, null,
                                "CopilotClient.StartCliServerAsync TCP port wait complete. Elapsed={Elapsed}, Port={Port}",
                                portWaitTimestamp,
                                detectedLocalhostTcpPort.Value);
                            break;
                        }
                    }

                    if (detectedLocalhostTcpPort is null)
                    {
                        // The CLI's stdout closed (process exited). Drain stderr
                        // before throwing so the surfaced exception includes the
                        // final diagnostic lines.
                        try { await stderrPump.Completion.WaitAsync(s_stderrPumpShutdownTimeout, CancellationToken.None); }
                        catch (TimeoutException) { /* best-effort: include whatever was captured */ }
                        catch (Exception ex) { logger.LogDebug(ex, "Runtime stderr pump faulted while draining"); }
                        throw CreateCliExitedException("Runtime process exited unexpectedly", stderrPump.Buffer);
                    }
                }
                catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested && cts.IsCancellationRequested)
                {
                    throw CreateCliExitedException("Timed out waiting for Copilot CLI to report its TCP listening port.", stderrPump.Buffer);
                }
            }

            return (cliProcess, detectedLocalhostTcpPort, stderrPump);
        }
        catch
        {
            await CleanupCliProcessAsync(cliProcess, stderrPump, errors: null, logger);

            throw;
        }
    }

    private static string? GetBundledCliPath(out string searchedPath)
    {
        var binaryName = OperatingSystem.IsWindows() ? "copilot.exe" : "copilot";
        // Always use portable RID (e.g., linux-x64) to match the build-time placement,
        // since distro-specific RIDs (e.g., ubuntu.24.04-x64) are normalized at build time.
        var rid = GetPortableRid()
            ?? Path.GetFileName(RuntimeInformation.RuntimeIdentifier);
        searchedPath = Path.Combine(AppContext.BaseDirectory, "runtimes", rid, "native", binaryName);
        return File.Exists(searchedPath) ? searchedPath : null;
    }

    private static string? GetPortableRid()
    {
        string os;
        if (OperatingSystem.IsWindows()) os = "win";
        else if (OperatingSystem.IsLinux()) os = "linux";
        else if (OperatingSystem.IsMacOS()) os = "osx";
        else return null;

        var arch = System.Runtime.InteropServices.RuntimeInformation.OSArchitecture switch
        {
            System.Runtime.InteropServices.Architecture.X64 => "x64",
            System.Runtime.InteropServices.Architecture.Arm64 => "arm64",
            _ => null,
        };

        return arch != null ? $"{os}-{arch}" : null;
    }

    private static (string FileName, IEnumerable<string> Args) ResolveCliCommand(string cliPath, IEnumerable<string> args)
    {
        var isJsFile = cliPath.EndsWith(".js", StringComparison.OrdinalIgnoreCase);

        if (isJsFile)
        {
            return ("node", new[] { cliPath }.Concat(args));
        }

        return (cliPath, args);
    }

    private async Task<Connection> ConnectToServerAsync(Process? cliProcess, string? tcpHost, int? tcpPort, ProcessStderrPump? stderrPump, CancellationToken cancellationToken)
    {
        var setupTimestamp = Stopwatch.GetTimestamp();
        NetworkStream? networkStream = null;
        JsonRpc? rpc = null;

        try
        {
            Stream inputStream, outputStream;

            if (_connection is StdioRuntimeConnection)
            {
                if (cliProcess == null)
                {
                    throw new InvalidOperationException("Runtime process not started");
                }

                inputStream = cliProcess.StandardOutput.BaseStream;
                outputStream = cliProcess.StandardInput.BaseStream;
            }
            else
            {
                if (tcpHost is null || tcpPort is null)
                {
                    throw new InvalidOperationException("Cannot connect because TCP host or port are not available");
                }

                var socket = new Socket(SocketType.Stream, ProtocolType.Tcp);
                try
                {
                    var tcpConnectTimestamp = Stopwatch.GetTimestamp();
                    LogConnectingToCliServer(_logger, tcpHost, tcpPort.Value);
                    await socket.ConnectAsync(tcpHost, tcpPort.Value, cancellationToken);
                    LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                        "CopilotClient.ConnectToServerAsync TCP connect complete. Elapsed={Elapsed}, Host={Host}, Port={Port}",
                        tcpConnectTimestamp,
                        tcpHost,
                        tcpPort.Value);
                }
                catch
                {
                    socket.Dispose();
                    throw;
                }

                inputStream = outputStream = networkStream = new NetworkStream(socket, ownsSocket: true);
            }

            rpc = new JsonRpc(
                outputStream,
                inputStream,
                SerializerOptionsForMessageFormatter,
                _logger);

            var handler = new RpcHandler(this);
            rpc.SetLocalRpcMethod("session.event", handler.OnSessionEvent);
            rpc.SetLocalRpcMethod("session.lifecycle", handler.OnSessionLifecycle);
            rpc.SetLocalRpcMethod("userInput.request", handler.OnUserInputRequest);
            rpc.SetLocalRpcMethod("exitPlanMode.request", handler.OnExitPlanModeRequest);
            rpc.SetLocalRpcMethod("autoModeSwitch.request", handler.OnAutoModeSwitchRequest);
            rpc.SetLocalRpcMethod("hooks.invoke", handler.OnHooksInvoke);
            rpc.SetLocalRpcMethod("systemMessage.transform", handler.OnSystemMessageTransform);
            ClientSessionApiRegistration.RegisterClientSessionApiHandlers(rpc, sessionId =>
            {
                var session = GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
                return session.ClientSessionApis;
            });
            rpc.StartListening();
            LoggingHelpers.LogTiming(_logger, LogLevel.Debug, null,
                "CopilotClient.ConnectToServerAsync transport setup complete. Elapsed={Elapsed}",
                setupTimestamp);

            var connection = new Connection(rpc, cliProcess, networkStream, stderrPump);
            _serverRpc = connection.Server;

            return connection;
        }
        catch
        {
            try { rpc?.Dispose(); }
            catch (Exception ex) { _logger.LogDebug(ex, "Failed to dispose JSON-RPC connection after startup failure"); }

            if (networkStream is not null)
            {
                try { await networkStream.DisposeAsync(); }
                catch (Exception ex) { _logger.LogDebug(ex, "Failed to dispose TCP stream after startup failure"); }
            }
            throw;
        }
    }

    private static JsonSerializerOptions SerializerOptionsForMessageFormatter { get; } = CreateSerializerOptions();

    /// <summary>
    /// Converts an arbitrary value into the <see cref="JsonElement"/> representation that wire
    /// DTOs use for opaque-JSON fields. Pass-through for <see cref="JsonElement"/>, otherwise
    /// serializes the runtime type using the shared JSON-RPC serializer options so that any
    /// type registered in the SDK's source-generated contexts (e.g. primitives,
    /// <c>Dictionary&lt;string, object&gt;</c>, generated DTOs) is supported.
    /// </summary>
    public static JsonElement? ToJsonElementForWire(object? value) => value switch
    {
        null => null,
        JsonElement je => je,
        _ => JsonSerializer.SerializeToElement(value, SerializerOptionsForMessageFormatter.GetTypeInfo(value.GetType()))
    };

    private static JsonSerializerOptions CreateSerializerOptions()
    {
        var options = new JsonSerializerOptions(JsonSerializerDefaults.Web)
        {
            AllowOutOfOrderMetadataProperties = true,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
        };

        options.TypeInfoResolverChain.Add(ClientJsonContext.Default);
        options.TypeInfoResolverChain.Add(TypesJsonContext.Default);
        options.TypeInfoResolverChain.Add(CopilotSession.SessionJsonContext.Default);
        options.TypeInfoResolverChain.Add(SessionEventsJsonContext.Default);
        options.TypeInfoResolverChain.Add(GitHub.Copilot.Rpc.RpcJsonContext.Default);

        options.MakeReadOnly();

        return options;
    }

    internal CopilotSession? GetSession(string sessionId)
    {
        _sessions.TryGetValue(sessionId, out var session);
        return session;
    }

    private void RegisterSession(CopilotSession session)
    {
        if (!_sessions.TryAdd(session.SessionId, session))
        {
            throw new InvalidOperationException($"Session '{session.SessionId}' is already tracked by this client.");
        }
    }

    private void RemoveSession(string sessionId)
    {
        _sessions.TryRemove(sessionId, out _);
    }

    /// <summary>
    /// Disposes the <see cref="CopilotClient"/> synchronously.
    /// </summary>
    /// <remarks>
    /// Prefer using <see cref="DisposeAsync"/> for better performance in async contexts.
    /// </remarks>
    public void Dispose()
    {
        DisposeAsync().AsTask().GetAwaiter().GetResult();
    }

    /// <summary>
    /// Disposes the <see cref="CopilotClient"/> asynchronously.
    /// </summary>
    /// <returns>A <see cref="ValueTask"/> representing the asynchronous dispose operation.</returns>
    /// <remarks>
    /// This method calls <see cref="ForceStopAsync"/> to immediately release all resources.
    /// </remarks>
    public async ValueTask DisposeAsync()
    {
        if (_disposed) return;
        _disposed = true;
        await ForceStopAsync();
    }

    private class RpcHandler(CopilotClient client)
    {
        public void OnSessionEvent(string sessionId, JsonElement? @event)
        {
            var session = client.GetSession(sessionId);
            if (session != null && @event != null)
            {
                var evt = SessionEvent.FromJson(@event.Value.GetRawText());
                if (evt != null)
                {
                    session.DispatchEvent(evt);
                }
            }
        }

        public void OnSessionLifecycle(string type, string sessionId, JsonElement? metadata)
        {
            SessionLifecycleEvent evt = type switch
            {
                "session.created" => new SessionCreatedEvent(),
                "session.deleted" => new SessionDeletedEvent(),
                "session.updated" => new SessionUpdatedEvent(),
                "session.foreground" => new SessionForegroundEvent(),
                "session.background" => new SessionBackgroundEvent(),
                _ => new SessionLifecycleEvent()
            };

            evt.Type = type;
            evt.SessionId = sessionId;
            if (metadata is not null)
            {
                evt.Metadata = JsonSerializer.Deserialize(
                    metadata.Value.GetRawText(),
                    TypesJsonContext.Default.SessionLifecycleEventMetadata);
            }

            client.DispatchLifecycleEvent(evt);
        }

        public async ValueTask<UserInputRequestResponse> OnUserInputRequest(string sessionId, string question, IList<string>? choices = null, bool? allowFreeform = null)
        {
            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            var request = new UserInputRequest
            {
                Question = question,
                Choices = choices,
                AllowFreeform = allowFreeform
            };

            var result = await session.HandleUserInputRequestAsync(request);
            return new UserInputRequestResponse(result.Answer, result.WasFreeform);
        }

        public async ValueTask<ExitPlanModeResult> OnExitPlanModeRequest(
            string sessionId,
            string summary,
            string? planContent = null,
            IList<string>? actions = null,
            string? recommendedAction = null)
        {
            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            var request = new ExitPlanModeRequest
            {
                Summary = summary,
                PlanContent = planContent,
                Actions = actions ?? [],
                RecommendedAction = recommendedAction ?? "autopilot"
            };

            return await session.HandleExitPlanModeRequestAsync(request);
        }

        public async ValueTask<AutoModeSwitchRequestResponse> OnAutoModeSwitchRequest(
            string sessionId,
            string? errorCode = null,
            double? retryAfterSeconds = null)
        {
            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            var response = await session.HandleAutoModeSwitchRequestAsync(new AutoModeSwitchRequest
            {
                ErrorCode = errorCode,
                RetryAfterSeconds = retryAfterSeconds
            });
            return new AutoModeSwitchRequestResponse(response);
        }

        public async ValueTask<HooksInvokeResponse> OnHooksInvoke(string sessionId, string hookType, JsonElement input)
        {
            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            var output = await session.HandleHooksInvokeAsync(hookType, input);
            return new HooksInvokeResponse(output);
        }

        public async ValueTask<SystemMessageTransformRpcResponse> OnSystemMessageTransform(string sessionId, JsonElement sections)
        {
            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            return await session.HandleSystemMessageTransformAsync(sections);
        }

    }

    private class Connection(
        JsonRpc rpc,
        Process? cliProcess, // Set if we created the child process
        NetworkStream? networkStream, // Set if using TCP
        ProcessStderrPump? stderrPump = null) // Captures stderr for error messages
    {
        public Process? CliProcess => cliProcess;
        public JsonRpc Rpc => rpc;
        public ServerRpc Server => field ?? Interlocked.CompareExchange(ref field, new(rpc), null) ?? field;
        public NetworkStream? NetworkStream => networkStream;
        public ProcessStderrPump? StderrPump => stderrPump;
        public StringBuilder? StderrBuffer => stderrPump?.Buffer;
    }

    private sealed class ProcessStderrPump
    {
        private readonly CancellationTokenSource _cancellationTokenSource = new();
        private readonly Task _completion;

        private ProcessStderrPump(Process process, ILogger logger)
        {
            _completion = Task.Run(() => PumpAsync(process, logger, _cancellationTokenSource.Token));
        }

        public StringBuilder Buffer { get; } = new();

        public Task Completion => _completion;

        public static ProcessStderrPump Start(Process process, ILogger logger)
        {
            return new ProcessStderrPump(process, logger);
        }

        public void Cancel() => _cancellationTokenSource.Cancel();

        private async Task PumpAsync(Process process, ILogger logger, CancellationToken cancellationToken)
        {
            try
            {
                while (await process.StandardError.ReadLineAsync(cancellationToken) is string line)
                {
                    lock (Buffer)
                    {
                        Buffer.AppendLine(line);
                    }

                    logger.LogWarning("[CLI] {Line}", line);
                }
            }
            catch (Exception e) when (cancellationToken.IsCancellationRequested
                && e is OperationCanceledException or InvalidOperationException or ObjectDisposedException or IOException)
            {
            }
            catch (Exception ex)
            {
                logger.LogDebug(ex, "Runtime stderr pump stopped unexpectedly");
            }
        }
    }

    private static class ProcessArgumentEscaper
    {
        public static string Escape(string arg)
        {
            if (string.IsNullOrEmpty(arg)) return "\"\"";
            if (!arg.Contains(' ') && !arg.Contains('"')) return arg;
            return "\"" + arg.Replace("\"", "\\\"") + "\"";
        }
    }

    // Request/Response types for RPC
    internal record CreateSessionRequest(
        string? Model,
        string? SessionId,
        string? ClientName,
        string? ReasoningEffort,
        ReasoningSummary? ReasoningSummary,
        ContextTier? ContextTier,
        IList<ToolDefinition>? Tools,
        SystemMessageConfig? SystemMessage,
        IList<string>? AvailableTools,
        IList<string>? ExcludedTools,
        ProviderConfig? Provider,
        bool? EnableSessionTelemetry,
        bool? RequestPermission,
        bool? RequestUserInput,
        bool? RequestExitPlanMode,
        bool? RequestAutoModeSwitch,
        bool? Hooks,
        string? WorkingDirectory,
        bool? Streaming,
        bool? IncludeSubAgentStreamingEvents,
        IDictionary<string, McpServerConfig>? McpServers,
        McpOAuthTokenStorageMode? McpOAuthTokenStorage,
        string? EnvValueMode,
        IList<CustomAgentConfig>? CustomAgents,
        DefaultAgentConfig? DefaultAgent,
        string? Agent,
        [property: JsonPropertyName("configDir")] string? ConfigDirectory,
        bool? EnableConfigDiscovery,
        bool? SkipEmbeddingRetrieval,
        EmbeddingCacheStorageMode? EmbeddingCacheStorage,
        string? OrganizationCustomInstructions,
        bool? EnableOnDemandInstructionDiscovery,
        bool? EnableFileHooks,
        bool? EnableHostGitOperations,
        bool? EnableSessionStore,
        bool? EnableSkills,
        IList<string>? SkillDirectories,
        IList<string>? DisabledSkills,
        InfiniteSessionConfig? InfiniteSessions,
        IList<CommandWireDefinition>? Commands = null,
        bool? RequestElicitation = null,
        bool? RequestMcpApps = null,
        string? Traceparent = null,
        string? Tracestate = null,
        ModelCapabilitiesOverride? ModelCapabilities = null,
        string? GitHubToken = null,
        RemoteSessionMode? RemoteSession = null,
        CloudSessionOptions? Cloud = null,
        IList<string>? InstructionDirectories = null,
        IList<string>? PluginDirectories = null,
        LargeToolOutputConfig? LargeOutput = null,
        MemoryConfiguration? Memory = null,
#pragma warning disable GHCP001
        IList<CanvasDeclaration>? Canvases = null,
        bool? RequestCanvasRenderer = null,
        bool? RequestExtensions = null,
        string? ExtensionSdkPath = null,
        ExtensionInfo? ExtensionInfo = null,
        IList<NamedProviderConfig>? Providers = null,
        IList<ProviderModelConfig>? Models = null,
        OptionsUpdateToolFilterPrecedence? ToolFilterPrecedence = null,
        [property: JsonPropertyName("expAssignments")] JsonElement? ExpAssignments = null);
#pragma warning restore GHCP001

    internal record ToolDefinition(
        string Name,
        string? Description,
        JsonElement Parameters, /* JSON schema */
        bool? OverridesBuiltInTool = null,
        bool? SkipPermission = null,
        CopilotToolDefer? Defer = null)
    {
        public static ToolDefinition FromAIFunction(AIFunctionDeclaration function)
        {
            var overrides = function.AdditionalProperties.TryGetValue(CopilotTool.OverridesBuiltInToolKey, out var val) && val is true;
            var skipPerm = function.AdditionalProperties.TryGetValue(CopilotTool.SkipPermissionKey, out var skipVal) && skipVal is true;
            var defer = function.AdditionalProperties.TryGetValue(CopilotTool.DeferKey, out var deferVal) && deferVal is CopilotToolDefer d ? d : (CopilotToolDefer?)null;
            return new ToolDefinition(function.Name, function.Description, function.JsonSchema,
                overrides ? true : null,
                skipPerm ? true : null,
                defer);
        }
    }

    internal record CreateSessionResponse(
        string SessionId,
        string? WorkspacePath,
        SessionCapabilities? Capabilities = null,
#pragma warning disable GHCP001
        IList<OpenCanvasInstance>? OpenCanvases = null);
#pragma warning restore GHCP001

    internal record ResumeSessionRequest(
        string SessionId,
        string? ClientName,
        string? Model,
        string? ReasoningEffort,
        ReasoningSummary? ReasoningSummary,
        ContextTier? ContextTier,
        IList<ToolDefinition>? Tools,
        SystemMessageConfig? SystemMessage,
        IList<string>? AvailableTools,
        IList<string>? ExcludedTools,
        ProviderConfig? Provider,
        bool? EnableSessionTelemetry,
        bool? RequestPermission,
        bool? RequestUserInput,
        bool? RequestExitPlanMode,
        bool? RequestAutoModeSwitch,
        bool? Hooks,
        string? WorkingDirectory,
        [property: JsonPropertyName("configDir")] string? ConfigDirectory,
        bool? EnableConfigDiscovery,
        bool? SkipEmbeddingRetrieval,
        EmbeddingCacheStorageMode? EmbeddingCacheStorage,
        string? OrganizationCustomInstructions,
        bool? EnableOnDemandInstructionDiscovery,
        bool? EnableFileHooks,
        bool? EnableHostGitOperations,
        bool? EnableSessionStore,
        bool? EnableSkills,
        [property: JsonPropertyName("disableResume")] bool? SuppressResumeEvent,
        bool? Streaming,
        bool? IncludeSubAgentStreamingEvents,
        IDictionary<string, McpServerConfig>? McpServers,
        McpOAuthTokenStorageMode? McpOAuthTokenStorage,
        string? EnvValueMode,
        IList<CustomAgentConfig>? CustomAgents,
        DefaultAgentConfig? DefaultAgent,
        string? Agent,
        IList<string>? SkillDirectories,
        IList<string>? DisabledSkills,
        InfiniteSessionConfig? InfiniteSessions,
        IList<CommandWireDefinition>? Commands = null,
        bool? RequestElicitation = null,
        bool? RequestMcpApps = null,
        string? Traceparent = null,
        string? Tracestate = null,
        ModelCapabilitiesOverride? ModelCapabilities = null,
        string? GitHubToken = null,
        RemoteSessionMode? RemoteSession = null,
        bool? ContinuePendingWork = null,
        IList<string>? InstructionDirectories = null,
        IList<string>? PluginDirectories = null,
        LargeToolOutputConfig? LargeOutput = null,
        MemoryConfiguration? Memory = null,
#pragma warning disable GHCP001
        IList<CanvasDeclaration>? Canvases = null,
        bool? RequestCanvasRenderer = null,
        bool? RequestExtensions = null,
        string? ExtensionSdkPath = null,
        ExtensionInfo? ExtensionInfo = null,
        IList<OpenCanvasInstance>? OpenCanvases = null,
        IList<NamedProviderConfig>? Providers = null,
        IList<ProviderModelConfig>? Models = null,
        OptionsUpdateToolFilterPrecedence? ToolFilterPrecedence = null,
        [property: JsonPropertyName("expAssignments")] JsonElement? ExpAssignments = null);
#pragma warning restore GHCP001

    internal record ResumeSessionResponse(
        string SessionId,
        string? WorkspacePath,
        SessionCapabilities? Capabilities = null,
#pragma warning disable GHCP001
        IList<OpenCanvasInstance>? OpenCanvases = null);
#pragma warning restore GHCP001

    internal record CommandWireDefinition(
        string Name,
        string? Description);

    internal record GetLastSessionIdResponse(
        string? SessionId);

    internal record DeleteSessionRequest(
        string SessionId);

    internal record DeleteSessionResponse(
        bool Success,
        string? Error);

    internal record ListSessionsRequest(
        SessionListFilter? Filter);

    internal record ListSessionsResponse(
        List<SessionMetadata> Sessions);

    internal record GetSessionMetadataRequest(
        string SessionId);

    internal record GetSessionMetadataResponse(
        SessionMetadata? Session);

    internal record SetForegroundSessionRequest(
        string SessionId);

    internal record UserInputRequestResponse(
        string Answer,
        bool WasFreeform);

    internal record AutoModeSwitchRequestResponse(
        AutoModeSwitchResponse Response);

    internal record HooksInvokeResponse(
        object? Output);

    [JsonSourceGenerationOptions(
        JsonSerializerDefaults.Web,
        AllowOutOfOrderMetadataProperties = true,
        NumberHandling = JsonNumberHandling.AllowReadingFromString,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonSerializable(typeof(CreateSessionRequest))]
    [JsonSerializable(typeof(CreateSessionResponse))]
    [JsonSerializable(typeof(AutoModeSwitchRequest))]
    [JsonSerializable(typeof(AutoModeSwitchRequestResponse))]
    [JsonSerializable(typeof(AutoModeSwitchResponse))]
    [JsonSerializable(typeof(CustomAgentConfig))]
    [JsonSerializable(typeof(DeleteSessionRequest))]
    [JsonSerializable(typeof(DeleteSessionResponse))]
    [JsonSerializable(typeof(ExitPlanModeRequest))]
    [JsonSerializable(typeof(ExitPlanModeResult))]
    [JsonSerializable(typeof(GetLastSessionIdResponse))]
    [JsonSerializable(typeof(HooksInvokeResponse))]
    [JsonSerializable(typeof(ListSessionsRequest))]
    [JsonSerializable(typeof(ListSessionsResponse))]
    [JsonSerializable(typeof(GetSessionMetadataRequest))]
    [JsonSerializable(typeof(GetSessionMetadataResponse))]
    [JsonSerializable(typeof(McpOAuthTokenStorageMode))]
    [JsonSerializable(typeof(EmbeddingCacheStorageMode))]
    [JsonSerializable(typeof(ModelCapabilitiesOverride))]
    [JsonSerializable(typeof(ProviderConfig))]
    [JsonSerializable(typeof(NamedProviderConfig))]
    [JsonSerializable(typeof(ProviderModelConfig))]
    [JsonSerializable(typeof(ResumeSessionRequest))]
    [JsonSerializable(typeof(ResumeSessionResponse))]
    [JsonSerializable(typeof(SessionCapabilities))]
    [JsonSerializable(typeof(SessionUiCapabilities))]
    [JsonSerializable(typeof(SessionMetadata))]
    [JsonSerializable(typeof(SetForegroundSessionRequest))]
    [JsonSerializable(typeof(SystemMessageConfig))]
    [JsonSerializable(typeof(SystemMessageTransformRpcResponse))]
    [JsonSerializable(typeof(CommandWireDefinition))]
    [JsonSerializable(typeof(ToolDefinition))]
    [JsonSerializable(typeof(CopilotToolDefer))]
    [JsonSerializable(typeof(ToolResultAIContent))]
    [JsonSerializable(typeof(ToolResultObject))]
    [JsonSerializable(typeof(UserInputRequestResponse))]
    [JsonSerializable(typeof(UserInputRequest))]
    [JsonSerializable(typeof(UserInputResponse))]
    internal partial class ClientJsonContext : JsonSerializerContext;

#if NET8_0_OR_GREATER
    [GeneratedRegex(@"listening on port ([0-9]+)", RegexOptions.IgnoreCase)]
    private static partial Regex ListeningOnPortRegex();
#else
    private static readonly Regex s_listeningOnPortRegex = new(@"listening on port ([0-9]+)", RegexOptions.IgnoreCase);

    private static Regex ListeningOnPortRegex() => s_listeningOnPortRegex;
#endif
}

/// <summary>
/// Wraps a <see cref="ToolResultObject"/> as <see cref="AIContent"/> to pass structured tool results
/// back through Microsoft.Extensions.AI without JSON serialization.
/// </summary>
/// <param name="toolResult">The tool result to wrap.</param>
public sealed class ToolResultAIContent(ToolResultObject toolResult) : AIContent
{
    /// <summary>
    /// Gets the underlying <see cref="ToolResultObject"/>.
    /// </summary>
    public ToolResultObject Result => toolResult;
}
