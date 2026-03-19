/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using StreamJsonRpc;
using System.Collections.Concurrent;
using System.Data;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using System.Text.RegularExpressions;
using GitHub.Copilot.SDK.Rpc;
using System.Globalization;

namespace GitHub.Copilot.SDK;

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
/// using var subscription = session.On(evt =>
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
    internal const string NoResultPermissionV2ErrorMessage =
        "Permission handlers cannot return 'no-result' when connected to a protocol v2 server.";

    /// <summary>
    /// Minimum protocol version this SDK can communicate with.
    /// </summary>
    private const int MinProtocolVersion = 2;

    private readonly ConcurrentDictionary<string, CopilotSession> _sessions = new();
    private readonly CopilotClientOptions _options;
    private readonly ILogger _logger;
    private Task<Connection>? _connectionTask;
    private volatile bool _disconnected;
    private bool _disposed;
    private readonly int? _optionsPort;
    private readonly string? _optionsHost;
    private int? _actualPort;
    private int? _negotiatedProtocolVersion;
    private List<ModelInfo>? _modelsCache;
    private readonly SemaphoreSlim _modelsCacheLock = new(1, 1);
    private readonly Func<CancellationToken, Task<List<ModelInfo>>>? _onListModels;
    private readonly List<Action<SessionLifecycleEvent>> _lifecycleHandlers = [];
    private readonly Dictionary<string, List<Action<SessionLifecycleEvent>>> _typedLifecycleHandlers = [];
    private readonly object _lifecycleHandlersLock = new();
    private ServerRpc? _rpc;

    /// <summary>
    /// Gets the typed RPC client for server-scoped methods (no session required).
    /// </summary>
    /// <remarks>
    /// The client must be started before accessing this property. Use <see cref="StartAsync"/> or set <see cref="CopilotClientOptions.AutoStart"/> to true.
    /// </remarks>
    /// <exception cref="ObjectDisposedException">Thrown if the client has been disposed.</exception>
    /// <exception cref="InvalidOperationException">Thrown if the client is not started.</exception>
    public ServerRpc Rpc => _disposed
        ? throw new ObjectDisposedException(nameof(CopilotClient))
        : _rpc ?? throw new InvalidOperationException("Client is not started. Call StartAsync first.");

    /// <summary>
    /// Gets the actual TCP port the CLI server is listening on, if using TCP transport.
    /// </summary>
    public int? ActualPort => _actualPort;

    /// <summary>
    /// Creates a new instance of <see cref="CopilotClient"/>.
    /// </summary>
    /// <param name="options">Options for creating the client. If null, default options are used.</param>
    /// <exception cref="ArgumentException">Thrown when mutually exclusive options are provided (e.g., CliUrl with UseStdio or CliPath).</exception>
    /// <example>
    /// <code>
    /// // Default options - spawns CLI server using stdio
    /// var client = new CopilotClient();
    ///
    /// // Connect to an existing server
    /// var client = new CopilotClient(new CopilotClientOptions { CliUrl = "localhost:3000", UseStdio = false });
    ///
    /// // Custom CLI path with specific log level
    /// var client = new CopilotClient(new CopilotClientOptions
    /// {
    ///     CliPath = "/usr/local/bin/copilot",
    ///     LogLevel = "debug"
    /// });
    /// </code>
    /// </example>
    public CopilotClient(CopilotClientOptions? options = null)
    {
        _options = options ?? new();

        // Validate mutually exclusive options
        if (!string.IsNullOrEmpty(_options.CliUrl) && _options.CliPath != null)
        {
            throw new ArgumentException("CliUrl is mutually exclusive with CliPath");
        }

        // When CliUrl is provided, disable UseStdio (we connect to an external server, not spawn one)
        if (!string.IsNullOrEmpty(_options.CliUrl))
        {
            _options.UseStdio = false;
        }

        // Validate auth options with external server
        if (!string.IsNullOrEmpty(_options.CliUrl) && (!string.IsNullOrEmpty(_options.GitHubToken) || _options.UseLoggedInUser != null))
        {
            throw new ArgumentException("GitHubToken and UseLoggedInUser cannot be used with CliUrl (external server manages its own auth)");
        }

        _logger = _options.Logger ?? NullLogger.Instance;
        _onListModels = _options.OnListModels;

        // Parse CliUrl if provided
        if (!string.IsNullOrEmpty(_options.CliUrl))
        {
            var uri = ParseCliUrl(_options.CliUrl!);
            _optionsHost = uri.Host;
            _optionsPort = uri.Port;
        }
    }

    /// <summary>
    /// Parses a CLI URL into a URI with host and port.
    /// </summary>
    /// <param name="url">The URL to parse. Supports formats: "port", "host:port", "http://host:port".</param>
    /// <returns>A <see cref="Uri"/> containing the parsed host and port.</returns>
    private static Uri ParseCliUrl(string url)
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
    /// <para>
    /// If the server is not already running and the client is configured to spawn one (default), it will be started.
    /// If connecting to an external server (via CliUrl), only establishes the connection.
    /// </para>
    /// <para>
    /// This method is called automatically when creating a session if <see cref="CopilotClientOptions.AutoStart"/> is true (default).
    /// </para>
    /// </remarks>
    /// <example>
    /// <code>
    /// var client = new CopilotClient(new CopilotClientOptions { AutoStart = false });
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
            _disconnected = false;

            Task<Connection> result;

            if (_optionsHost is not null && _optionsPort is not null)
            {
                // External server (TCP)
                _actualPort = _optionsPort;
                result = ConnectToServerAsync(null, _optionsHost, _optionsPort, null, ct);
            }
            else
            {
                // Child process (stdio or TCP)
                var (cliProcess, portOrNull, stderrBuffer) = await StartCliServerAsync(_options, _logger, ct);
                _actualPort = portOrNull;
                result = ConnectToServerAsync(cliProcess, portOrNull is null ? null : "localhost", portOrNull, stderrBuffer, ct);
            }

            var connection = await result;

            // Verify protocol version compatibility
            await VerifyProtocolVersionAsync(connection, ct);

            _logger.LogInformation("Copilot client connected");
            return connection;
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
        var errors = new List<Exception>();

        foreach (var session in _sessions.Values.ToArray())
        {
            try
            {
                await session.DisposeAsync();
            }
            catch (Exception ex)
            {
                errors.Add(new Exception($"Failed to dispose session {session.SessionId}: {ex.Message}", ex));
            }
        }

        _sessions.Clear();
        await CleanupConnectionAsync(errors);
        _connectionTask = null;

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
        var errors = new List<Exception>();

        _sessions.Clear();
        await CleanupConnectionAsync(errors);
        _connectionTask = null;

        ThrowErrors(errors);
    }

    private static void ThrowErrors(List<Exception> errors)
    {
        if (errors.Count == 1)
        {
            throw errors[0];
        }
        else if (errors.Count > 0)
        {
            throw new AggregateException(errors);
        }
    }

    private async Task CleanupConnectionAsync(List<Exception>? errors)
    {
        if (_connectionTask is null)
        {
            return;
        }

        var ctx = await _connectionTask;
        _connectionTask = null;

        try { ctx.Rpc.Dispose(); }
        catch (Exception ex) { errors?.Add(ex); }

        // Clear RPC and models cache
        _rpc = null;
        _modelsCache = null;

        if (ctx.NetworkStream is not null)
        {
            try { await ctx.NetworkStream.DisposeAsync(); }
            catch (Exception ex) { errors?.Add(ex); }
        }

        if (ctx.TcpClient is not null)
        {
            try { ctx.TcpClient.Dispose(); }
            catch (Exception ex) { errors?.Add(ex); }
        }

        if (ctx.CliProcess is { } childProcess)
        {
            try
            {
                if (!childProcess.HasExited) childProcess.Kill();
                childProcess.Dispose();
            }
            catch (Exception ex) { errors?.Add(ex); }
        }
    }

    /// <summary>
    /// Creates a new Copilot session with the specified configuration.
    /// </summary>
    /// <param name="config">Configuration for the session, including the required <see cref="SessionConfig.OnPermissionRequest"/> handler.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves to provide the <see cref="CopilotSession"/>.</returns>
    /// <remarks>
    /// Sessions maintain conversation state, handle events, and manage tool execution.
    /// If the client is not connected and <see cref="CopilotClientOptions.AutoStart"/> is enabled (default),
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
        if (config.OnPermissionRequest == null)
        {
            throw new ArgumentException(
                "An OnPermissionRequest handler is required when creating a session. " +
                "For example, to allow all permissions, use CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll });");
        }

        var connection = await EnsureConnectedAsync(cancellationToken);

        var hasHooks = config.Hooks != null && (
            config.Hooks.OnPreToolUse != null ||
            config.Hooks.OnPostToolUse != null ||
            config.Hooks.OnUserPromptSubmitted != null ||
            config.Hooks.OnSessionStart != null ||
            config.Hooks.OnSessionEnd != null ||
            config.Hooks.OnErrorOccurred != null);

        var sessionId = config.SessionId ?? Guid.NewGuid().ToString();

        // Create and register the session before issuing the RPC so that
        // events emitted by the CLI (e.g. session.start) are not dropped.
        var session = new CopilotSession(sessionId, connection.Rpc, _logger);
        session.RegisterTools(config.Tools ?? []);
        session.RegisterPermissionHandler(config.OnPermissionRequest);
        if (config.OnUserInputRequest != null)
        {
            session.RegisterUserInputHandler(config.OnUserInputRequest);
        }
        if (config.Hooks != null)
        {
            session.RegisterHooks(config.Hooks);
        }
        if (config.OnEvent != null)
        {
            session.On(config.OnEvent);
        }
        _sessions[sessionId] = session;

        try
        {
            var (traceparent, tracestate) = TelemetryHelpers.GetTraceContext();

            var request = new CreateSessionRequest(
                config.Model,
                sessionId,
                config.ClientName,
                config.ReasoningEffort,
                config.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
                config.SystemMessage,
                config.AvailableTools,
                config.ExcludedTools,
                config.Provider,
                (bool?)true,
                config.OnUserInputRequest != null ? true : null,
                hasHooks ? true : null,
                config.WorkingDirectory,
                config.Streaming is true ? true : null,
                config.McpServers,
                "direct",
                config.CustomAgents,
                config.Agent,
                config.ConfigDir,
                config.SkillDirectories,
                config.DisabledSkills,
                config.InfiniteSessions,
                traceparent,
                tracestate);

            var response = await InvokeRpcAsync<CreateSessionResponse>(
                connection.Rpc, "session.create", [request], cancellationToken);

            session.WorkspacePath = response.WorkspacePath;
        }
        catch
        {
            _sessions.TryRemove(sessionId, out _);
            throw;
        }

        return session;
    }

    /// <summary>
    /// Resumes an existing Copilot session with the specified configuration.
    /// </summary>
    /// <param name="sessionId">The ID of the session to resume.</param>
    /// <param name="config">Configuration for the resumed session, including the required <see cref="ResumeSessionConfig.OnPermissionRequest"/> handler.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves to provide the <see cref="CopilotSession"/>.</returns>
    /// <exception cref="ArgumentException">Thrown when <see cref="ResumeSessionConfig.OnPermissionRequest"/> is not set.</exception>
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
        if (config.OnPermissionRequest == null)
        {
            throw new ArgumentException(
                "An OnPermissionRequest handler is required when resuming a session. " +
                "For example, to allow all permissions, use new() { OnPermissionRequest = PermissionHandler.ApproveAll }.");
        }

        var connection = await EnsureConnectedAsync(cancellationToken);

        var hasHooks = config.Hooks != null && (
            config.Hooks.OnPreToolUse != null ||
            config.Hooks.OnPostToolUse != null ||
            config.Hooks.OnUserPromptSubmitted != null ||
            config.Hooks.OnSessionStart != null ||
            config.Hooks.OnSessionEnd != null ||
            config.Hooks.OnErrorOccurred != null);

        // Create and register the session before issuing the RPC so that
        // events emitted by the CLI (e.g. session.start) are not dropped.
        var session = new CopilotSession(sessionId, connection.Rpc, _logger);
        session.RegisterTools(config.Tools ?? []);
        session.RegisterPermissionHandler(config.OnPermissionRequest);
        if (config.OnUserInputRequest != null)
        {
            session.RegisterUserInputHandler(config.OnUserInputRequest);
        }
        if (config.Hooks != null)
        {
            session.RegisterHooks(config.Hooks);
        }
        if (config.OnEvent != null)
        {
            session.On(config.OnEvent);
        }
        _sessions[sessionId] = session;

        try
        {
            var (traceparent, tracestate) = TelemetryHelpers.GetTraceContext();

            var request = new ResumeSessionRequest(
                sessionId,
                config.ClientName,
                config.Model,
                config.ReasoningEffort,
                config.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
                config.SystemMessage,
                config.AvailableTools,
                config.ExcludedTools,
                config.Provider,
                (bool?)true,
                config.OnUserInputRequest != null ? true : null,
                hasHooks ? true : null,
                config.WorkingDirectory,
                config.ConfigDir,
                config.DisableResume is true ? true : null,
                config.Streaming is true ? true : null,
                config.McpServers,
                "direct",
                config.CustomAgents,
                config.Agent,
                config.SkillDirectories,
                config.DisabledSkills,
                config.InfiniteSessions,
                traceparent,
                tracestate);

            var response = await InvokeRpcAsync<ResumeSessionResponse>(
                connection.Rpc, "session.resume", [request], cancellationToken);

            session.WorkspacePath = response.WorkspacePath;
        }
        catch
        {
            _sessions.TryRemove(sessionId, out _);
            throw;
        }

        return session;
    }

    /// <summary>
    /// Gets the current connection state of the client.
    /// </summary>
    /// <value>
    /// The current <see cref="ConnectionState"/>: Disconnected, Connecting, Connected, or Error.
    /// </value>
    /// <example>
    /// <code>
    /// if (client.State == ConnectionState.Connected)
    /// {
    ///     var session = await client.CreateSessionAsync(new() { OnPermissionRequest = PermissionHandler.ApproveAll });
    /// }
    /// </code>
    /// </example>
    public ConnectionState State
    {
        get
        {
            if (_connectionTask == null) return ConnectionState.Disconnected;
            if (_connectionTask.IsFaulted) return ConnectionState.Error;
            if (!_connectionTask.IsCompleted) return ConnectionState.Connecting;
            if (_disconnected) return ConnectionState.Disconnected;
            return ConnectionState.Connected;
        }
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
    public async Task<List<ModelInfo>> ListModelsAsync(CancellationToken cancellationToken = default)
    {
        await _modelsCacheLock.WaitAsync(cancellationToken);
        try
        {
            // Check cache (already inside lock)
            if (_modelsCache is not null)
            {
                return [.. _modelsCache]; // Return a copy to prevent cache mutation
            }

            List<ModelInfo> models;
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

            return [.. models]; // Return a copy to prevent cache mutation
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
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<DeleteSessionResponse>(
            connection.Rpc, "session.delete", [new DeleteSessionRequest(sessionId)], cancellationToken);

        if (!response.Success)
        {
            throw new InvalidOperationException($"Failed to delete session {sessionId}: {response.Error}");
        }

        _sessions.TryRemove(sessionId, out _);
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
    public async Task<List<SessionMetadata>> ListSessionsAsync(SessionListFilter? filter = null, CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<ListSessionsResponse>(
            connection.Rpc, "session.list", [new ListSessionsRequest(filter)], cancellationToken);

        return response.Sessions;
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
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<SetForegroundSessionResponse>(
            connection.Rpc, "session.setForeground", [new { sessionId }], cancellationToken);

        if (!response.Success)
        {
            throw new InvalidOperationException(response.Error ?? "Failed to set foreground session");
        }
    }

    /// <summary>
    /// Subscribes to all session lifecycle events.
    /// </summary>
    /// <remarks>
    /// Lifecycle events are emitted when sessions are created, deleted, updated,
    /// or change foreground/background state (in TUI+server mode).
    /// </remarks>
    /// <param name="handler">A callback function that receives lifecycle events.</param>
    /// <returns>An IDisposable that, when disposed, unsubscribes the handler.</returns>
    /// <example>
    /// <code>
    /// using var subscription = client.On(evt =>
    /// {
    ///     Console.WriteLine($"Session {evt.SessionId}: {evt.Type}");
    /// });
    /// </code>
    /// </example>
    public IDisposable On(Action<SessionLifecycleEvent> handler)
    {
        lock (_lifecycleHandlersLock)
        {
            _lifecycleHandlers.Add(handler);
        }

        return new ActionDisposable(() =>
        {
            lock (_lifecycleHandlersLock)
            {
                _lifecycleHandlers.Remove(handler);
            }
        });
    }

    /// <summary>
    /// Subscribes to a specific session lifecycle event type.
    /// </summary>
    /// <param name="eventType">The event type to listen for (use SessionLifecycleEventTypes constants).</param>
    /// <param name="handler">A callback function that receives events of the specified type.</param>
    /// <returns>An IDisposable that, when disposed, unsubscribes the handler.</returns>
    /// <example>
    /// <code>
    /// using var subscription = client.On(SessionLifecycleEventTypes.Foreground, evt =>
    /// {
    ///     Console.WriteLine($"Session {evt.SessionId} is now in foreground");
    /// });
    /// </code>
    /// </example>
    public IDisposable On(string eventType, Action<SessionLifecycleEvent> handler)
    {
        lock (_lifecycleHandlersLock)
        {
            if (!_typedLifecycleHandlers.TryGetValue(eventType, out var handlers))
            {
                handlers = [];
                _typedLifecycleHandlers[eventType] = handlers;
            }
            handlers.Add(handler);
        }

        return new ActionDisposable(() =>
        {
            lock (_lifecycleHandlersLock)
            {
                if (_typedLifecycleHandlers.TryGetValue(eventType, out var handlers))
                {
                    handlers.Remove(handler);
                }
            }
        });
    }

    private void DispatchLifecycleEvent(SessionLifecycleEvent evt)
    {
        List<Action<SessionLifecycleEvent>> typedHandlers;
        List<Action<SessionLifecycleEvent>> wildcardHandlers;

        lock (_lifecycleHandlersLock)
        {
            typedHandlers = _typedLifecycleHandlers.TryGetValue(evt.Type, out var handlers)
                ? [.. handlers]
                : [];
            wildcardHandlers = [.. _lifecycleHandlers];
        }

        foreach (var handler in typedHandlers)
        {
            try { handler(evt); } catch { /* Ignore handler errors */ }
        }

        foreach (var handler in wildcardHandlers)
        {
            try { handler(evt); } catch { /* Ignore handler errors */ }
        }
    }

    internal static async Task<T> InvokeRpcAsync<T>(JsonRpc rpc, string method, object?[]? args, CancellationToken cancellationToken)
    {
        return await InvokeRpcAsync<T>(rpc, method, args, null, cancellationToken);
    }

    internal static async Task<T> InvokeRpcAsync<T>(JsonRpc rpc, string method, object?[]? args, StringBuilder? stderrBuffer, CancellationToken cancellationToken)
    {
        try
        {
            return await rpc.InvokeWithCancellationAsync<T>(method, args, cancellationToken);
        }
        catch (StreamJsonRpc.ConnectionLostException ex)
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
                throw new IOException($"CLI process exited unexpectedly.\nstderr: {stderrOutput}", ex);
            }
            throw new IOException($"Communication error with Copilot CLI: {ex.Message}", ex);
        }
        catch (StreamJsonRpc.RemoteRpcException ex)
        {
            throw new IOException($"Communication error with Copilot CLI: {ex.Message}", ex);
        }
    }

    private Task<Connection> EnsureConnectedAsync(CancellationToken cancellationToken)
    {
        if (_connectionTask is null && !_options.AutoStart)
        {
            throw new InvalidOperationException($"Client not connected. Call {nameof(StartAsync)}() first.");
        }

        // If already started or starting, this will return the existing task
        return (Task<Connection>)StartAsync(cancellationToken);
    }

    private async Task VerifyProtocolVersionAsync(Connection connection, CancellationToken cancellationToken)
    {
        var maxVersion = SdkProtocolVersion.GetVersion();
        var pingResponse = await InvokeRpcAsync<PingResponse>(
            connection.Rpc, "ping", [new PingRequest()], connection.StderrBuffer, cancellationToken);

        if (!pingResponse.ProtocolVersion.HasValue)
        {
            throw new InvalidOperationException(
                $"SDK protocol version mismatch: SDK supports versions {MinProtocolVersion}-{maxVersion}, " +
                $"but server does not report a protocol version. " +
                $"Please update your server to ensure compatibility.");
        }

        var serverVersion = pingResponse.ProtocolVersion.Value;
        if (serverVersion < MinProtocolVersion || serverVersion > maxVersion)
        {
            throw new InvalidOperationException(
                $"SDK protocol version mismatch: SDK supports versions {MinProtocolVersion}-{maxVersion}, " +
                $"but server reports version {serverVersion}. " +
                $"Please update your SDK or server to ensure compatibility.");
        }

        _negotiatedProtocolVersion = serverVersion;
    }

    private static async Task<(Process Process, int? DetectedLocalhostTcpPort, StringBuilder StderrBuffer)> StartCliServerAsync(CopilotClientOptions options, ILogger logger, CancellationToken cancellationToken)
    {
        // Use explicit path or bundled CLI - no PATH fallback
        var cliPath = options.CliPath ?? GetBundledCliPath(out var searchedPath)
            ?? throw new InvalidOperationException($"Copilot CLI not found at '{searchedPath}'. Ensure the SDK NuGet package was restored correctly or provide an explicit CliPath.");
        var args = new List<string>();

        if (options.CliArgs != null)
        {
            args.AddRange(options.CliArgs);
        }

        args.AddRange(["--headless", "--no-auto-update", "--log-level", options.LogLevel]);

        if (options.UseStdio)
        {
            args.Add("--stdio");
        }
        else if (options.Port > 0)
        {
            args.AddRange(["--port", options.Port.ToString(CultureInfo.InvariantCulture)]);
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

        var (fileName, processArgs) = ResolveCliCommand(cliPath, args);

        var startInfo = new ProcessStartInfo
        {
            FileName = fileName,
            Arguments = string.Join(" ", processArgs.Select(ProcessArgumentEscaper.Escape)),
            UseShellExecute = false,
            RedirectStandardInput = options.UseStdio,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            WorkingDirectory = options.Cwd,
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

        // Set telemetry environment variables if configured
        if (options.Telemetry is { } telemetry)
        {
            startInfo.Environment["COPILOT_OTEL_ENABLED"] = "true";
            if (telemetry.OtlpEndpoint is not null) startInfo.Environment["OTEL_EXPORTER_OTLP_ENDPOINT"] = telemetry.OtlpEndpoint;
            if (telemetry.FilePath is not null) startInfo.Environment["COPILOT_OTEL_FILE_EXPORTER_PATH"] = telemetry.FilePath;
            if (telemetry.ExporterType is not null) startInfo.Environment["COPILOT_OTEL_EXPORTER_TYPE"] = telemetry.ExporterType;
            if (telemetry.SourceName is not null) startInfo.Environment["COPILOT_OTEL_SOURCE_NAME"] = telemetry.SourceName;
            if (telemetry.CaptureContent is { } capture) startInfo.Environment["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = capture ? "true" : "false";
        }

        var cliProcess = new Process { StartInfo = startInfo };
        cliProcess.Start();

        // Capture stderr for error messages and forward to logger
        var stderrBuffer = new StringBuilder();
        _ = Task.Run(async () =>
        {
            while (cliProcess != null && !cliProcess.HasExited)
            {
                var line = await cliProcess.StandardError.ReadLineAsync(cancellationToken);
                if (line != null)
                {
                    lock (stderrBuffer)
                    {
                        stderrBuffer.AppendLine(line);
                    }

                    if (logger.IsEnabled(LogLevel.Debug))
                    {
                        logger.LogDebug("[CLI] {Line}", line);
                    }
                }
            }
        }, cancellationToken);

        var detectedLocalhostTcpPort = (int?)null;
        if (!options.UseStdio)
        {
            // Wait for port announcement
            using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            cts.CancelAfter(TimeSpan.FromSeconds(30));

            while (!cts.Token.IsCancellationRequested)
            {
                var line = await cliProcess.StandardOutput.ReadLineAsync(cts.Token) ?? throw new IOException("CLI process exited unexpectedly");
                if (ListeningOnPortRegex().Match(line) is { Success: true } match)
                {
                    detectedLocalhostTcpPort = int.Parse(match.Groups[1].Value, CultureInfo.InvariantCulture);
                    break;
                }
            }
        }

        return (cliProcess, detectedLocalhostTcpPort, stderrBuffer);
    }

    private static string? GetBundledCliPath(out string searchedPath)
    {
        var binaryName = OperatingSystem.IsWindows() ? "copilot.exe" : "copilot";
        // Always use portable RID (e.g., linux-x64) to match the build-time placement,
        // since distro-specific RIDs (e.g., ubuntu.24.04-x64) are normalized at build time.
        var rid = GetPortableRid()
            ?? Path.GetFileName(System.Runtime.InteropServices.RuntimeInformation.RuntimeIdentifier);
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

    private async Task<Connection> ConnectToServerAsync(Process? cliProcess, string? tcpHost, int? tcpPort, StringBuilder? stderrBuffer, CancellationToken cancellationToken)
    {
        Stream inputStream, outputStream;
        TcpClient? tcpClient = null;
        NetworkStream? networkStream = null;

        if (_options.UseStdio)
        {
            if (cliProcess == null) throw new InvalidOperationException("CLI process not started");
            inputStream = cliProcess.StandardOutput.BaseStream;
            outputStream = cliProcess.StandardInput.BaseStream;
        }
        else
        {
            if (tcpHost is null || tcpPort is null)
            {
                throw new InvalidOperationException("Cannot connect because TCP host or port are not available");
            }

            tcpClient = new();
            await tcpClient.ConnectAsync(tcpHost, tcpPort.Value, cancellationToken);
            networkStream = tcpClient.GetStream();
            inputStream = networkStream;
            outputStream = networkStream;
        }

        var rpc = new JsonRpc(new HeaderDelimitedMessageHandler(
            outputStream,
            inputStream,
            CreateSystemTextJsonFormatter()))
        {
            TraceSource = new LoggerTraceSource(_logger),
        };

        var handler = new RpcHandler(this);
        rpc.AddLocalRpcMethod("session.event", handler.OnSessionEvent);
        rpc.AddLocalRpcMethod("session.lifecycle", handler.OnSessionLifecycle);
        // Protocol v3 servers send tool calls / permission requests as broadcast events.
        // Protocol v2 servers use the older tool.call / permission.request RPC model.
        // We always register v2 adapters because handlers are set up before version
        // negotiation; a v3 server will simply never send these requests.
        rpc.AddLocalRpcMethod("tool.call", handler.OnToolCallV2);
        rpc.AddLocalRpcMethod("permission.request", handler.OnPermissionRequestV2);
        rpc.AddLocalRpcMethod("userInput.request", handler.OnUserInputRequest);
        rpc.AddLocalRpcMethod("hooks.invoke", handler.OnHooksInvoke);
        rpc.StartListening();

        // Transition state to Disconnected if the JSON-RPC connection drops
        _ = rpc.Completion.ContinueWith(_ => _disconnected = true, TaskScheduler.Default);

        _rpc = new ServerRpc(rpc);

        return new Connection(rpc, cliProcess, tcpClient, networkStream, stderrBuffer);
    }

    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Using happy path from https://microsoft.github.io/vs-streamjsonrpc/docs/nativeAOT.html")]
    [UnconditionalSuppressMessage("AOT", "IL3050", Justification = "Using happy path from https://microsoft.github.io/vs-streamjsonrpc/docs/nativeAOT.html")]
    private static SystemTextJsonFormatter CreateSystemTextJsonFormatter()
    {
        return new() { JsonSerializerOptions = SerializerOptionsForMessageFormatter };
    }

    private static JsonSerializerOptions SerializerOptionsForMessageFormatter { get; } = CreateSerializerOptions();

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
        options.TypeInfoResolverChain.Add(SDK.Rpc.RpcJsonContext.Default);

        // StreamJsonRpc's RequestId needs serialization when CancellationToken fires during
        // JSON-RPC operations. Its built-in converter (RequestIdSTJsonConverter) is internal,
        // and [JsonSerializable] can't source-gen for it (SYSLIB1220), so we provide our own
        // AOT-safe resolver + converter.
        options.TypeInfoResolverChain.Add(new RequestIdTypeInfoResolver());

        options.MakeReadOnly();

        return options;
    }

    internal CopilotSession? GetSession(string sessionId)
    {
        return _sessions.TryGetValue(sessionId, out var session) ? session : null;
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
            var evt = new SessionLifecycleEvent
            {
                Type = type,
                SessionId = sessionId
            };

            if (metadata != null)
            {
                evt.Metadata = JsonSerializer.Deserialize(
                    metadata.Value.GetRawText(),
                    TypesJsonContext.Default.SessionLifecycleEventMetadata);
            }

            client.DispatchLifecycleEvent(evt);
        }

        public async Task<UserInputRequestResponse> OnUserInputRequest(string sessionId, string question, List<string>? choices = null, bool? allowFreeform = null)
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

        public async Task<HooksInvokeResponse> OnHooksInvoke(string sessionId, string hookType, JsonElement input)
        {
            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            var output = await session.HandleHooksInvokeAsync(hookType, input);
            return new HooksInvokeResponse(output);
        }

        // Protocol v2 backward-compatibility adapters

        public async Task<ToolCallResponseV2> OnToolCallV2(string sessionId,
            string toolCallId,
            string toolName,
            object? arguments,
            string? traceparent = null,
            string? tracestate = null)
        {
            using var _ = TelemetryHelpers.RestoreTraceContext(traceparent, tracestate);

            var session = client.GetSession(sessionId) ?? throw new ArgumentException($"Unknown session {sessionId}");
            if (session.GetTool(toolName) is not { } tool)
            {
                return new ToolCallResponseV2(new ToolResultObject
                {
                    TextResultForLlm = $"Tool '{toolName}' is not supported.",
                    ResultType = "failure",
                    Error = $"tool '{toolName}' not supported"
                });
            }

            try
            {
                var invocation = new ToolInvocation
                {
                    SessionId = sessionId,
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
                return new ToolCallResponseV2(toolResultObject);
            }
            catch (Exception ex)
            {
                return new ToolCallResponseV2(new ToolResultObject
                {
                    TextResultForLlm = "Invoking this tool produced an error. Detailed information is not available.",
                    ResultType = "failure",
                    Error = ex.Message
                });
            }
        }

        public async Task<PermissionRequestResponseV2> OnPermissionRequestV2(string sessionId, JsonElement permissionRequest)
        {
            var session = client.GetSession(sessionId)
                ?? throw new ArgumentException($"Unknown session {sessionId}");

            try
            {
                var result = await session.HandlePermissionRequestAsync(permissionRequest);
                if (result.Kind == new PermissionRequestResultKind("no-result"))
                {
                    throw new InvalidOperationException(NoResultPermissionV2ErrorMessage);
                }
                return new PermissionRequestResponseV2(result);
            }
            catch (InvalidOperationException ex) when (ex.Message == NoResultPermissionV2ErrorMessage)
            {
                throw;
            }
            catch (Exception)
            {
                return new PermissionRequestResponseV2(new PermissionRequestResult
                {
                    Kind = PermissionRequestResultKind.DeniedCouldNotRequestFromUser
                });
            }
        }
    }

    private class Connection(
        JsonRpc rpc,
        Process? cliProcess, // Set if we created the child process
        TcpClient? tcpClient, // Set if using TCP
        NetworkStream? networkStream, // Set if using TCP
        StringBuilder? stderrBuffer = null) // Captures stderr for error messages
    {
        public Process? CliProcess => cliProcess;
        public TcpClient? TcpClient => tcpClient;
        public JsonRpc Rpc => rpc;
        public NetworkStream? NetworkStream => networkStream;
        public StringBuilder? StderrBuffer => stderrBuffer;
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
        List<ToolDefinition>? Tools,
        SystemMessageConfig? SystemMessage,
        List<string>? AvailableTools,
        List<string>? ExcludedTools,
        ProviderConfig? Provider,
        bool? RequestPermission,
        bool? RequestUserInput,
        bool? Hooks,
        string? WorkingDirectory,
        bool? Streaming,
        Dictionary<string, object>? McpServers,
        string? EnvValueMode,
        List<CustomAgentConfig>? CustomAgents,
        string? Agent,
        string? ConfigDir,
        List<string>? SkillDirectories,
        List<string>? DisabledSkills,
        InfiniteSessionConfig? InfiniteSessions,
        string? Traceparent = null,
        string? Tracestate = null);

    internal record ToolDefinition(
        string Name,
        string? Description,
        JsonElement Parameters, /* JSON schema */
        bool? OverridesBuiltInTool = null,
        bool? SkipPermission = null)
    {
        public static ToolDefinition FromAIFunction(AIFunction function)
        {
            var overrides = function.AdditionalProperties.TryGetValue("is_override", out var val) && val is true;
            var skipPerm = function.AdditionalProperties.TryGetValue("skip_permission", out var skipVal) && skipVal is true;
            return new ToolDefinition(function.Name, function.Description, function.JsonSchema,
                overrides ? true : null,
                skipPerm ? true : null);
        }
    }

    internal record CreateSessionResponse(
        string SessionId,
        string? WorkspacePath);

    internal record ResumeSessionRequest(
        string SessionId,
        string? ClientName,
        string? Model,
        string? ReasoningEffort,
        List<ToolDefinition>? Tools,
        SystemMessageConfig? SystemMessage,
        List<string>? AvailableTools,
        List<string>? ExcludedTools,
        ProviderConfig? Provider,
        bool? RequestPermission,
        bool? RequestUserInput,
        bool? Hooks,
        string? WorkingDirectory,
        string? ConfigDir,
        bool? DisableResume,
        bool? Streaming,
        Dictionary<string, object>? McpServers,
        string? EnvValueMode,
        List<CustomAgentConfig>? CustomAgents,
        string? Agent,
        List<string>? SkillDirectories,
        List<string>? DisabledSkills,
        InfiniteSessionConfig? InfiniteSessions,
        string? Traceparent = null,
        string? Tracestate = null);

    internal record ResumeSessionResponse(
        string SessionId,
        string? WorkspacePath);

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

    internal record UserInputRequestResponse(
        string Answer,
        bool WasFreeform);

    internal record HooksInvokeResponse(
        object? Output);

    // Protocol v2 backward-compatibility response types
    internal record ToolCallResponseV2(
        ToolResultObject Result);

    internal record PermissionRequestResponseV2(
        PermissionRequestResult Result);

    /// <summary>Trace source that forwards all logs to the ILogger.</summary>
    internal sealed class LoggerTraceSource : TraceSource
    {
        public LoggerTraceSource(ILogger logger) : base(nameof(LoggerTraceSource), SourceLevels.All)
        {
            Listeners.Clear();
            Listeners.Add(new LoggerTraceListener(logger));
        }

        private sealed class LoggerTraceListener(ILogger logger) : TraceListener
        {
            public override void TraceEvent(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, string? message)
            {
                LogLevel level = MapLevel(eventType);
                if (logger.IsEnabled(level))
                {
                    logger.Log(level, "[{Source}] {Message}", source, message);
                }
            }

            public override void TraceEvent(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, string? format, params object?[]? args)
            {
                LogLevel level = MapLevel(eventType);
                if (logger.IsEnabled(level))
                {
                    logger.Log(level, "[{Source}] {Message}", source, args is null || args.Length == 0 ? format : string.Format(CultureInfo.InvariantCulture, format ?? "", args));
                }
            }

            public override void TraceData(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, object? data)
            {
                LogLevel level = MapLevel(eventType);
                if (logger.IsEnabled(level))
                {
                    logger.Log(level, "[{Source}] {Data}", source, data);
                }
            }

            public override void TraceData(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, params object?[]? data)
            {
                LogLevel level = MapLevel(eventType);
                if (logger.IsEnabled(level))
                {
                    logger.Log(level, "[{Source}] {Data}", source, data is null ? null : string.Join(", ", data));
                }
            }

            public override void Write(string? message)
            {
                if (logger.IsEnabled(LogLevel.Trace))
                {
                    logger.LogTrace("{Message}", message);
                }
            }

            public override void WriteLine(string? message)
            {
                if (logger.IsEnabled(LogLevel.Trace))
                {
                    logger.LogTrace("{Message}", message);
                }
            }

            private static LogLevel MapLevel(TraceEventType eventType)
            {
                return eventType switch
                {
                    TraceEventType.Critical => LogLevel.Critical,
                    TraceEventType.Error => LogLevel.Error,
                    TraceEventType.Warning => LogLevel.Warning,
                    TraceEventType.Information => LogLevel.Information,
                    TraceEventType.Verbose => LogLevel.Debug,
                    _ => LogLevel.Trace
                };
            }
        }
    }

    [JsonSourceGenerationOptions(
        JsonSerializerDefaults.Web,
        AllowOutOfOrderMetadataProperties = true,
        NumberHandling = JsonNumberHandling.AllowReadingFromString,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonSerializable(typeof(CreateSessionRequest))]
    [JsonSerializable(typeof(CreateSessionResponse))]
    [JsonSerializable(typeof(CustomAgentConfig))]
    [JsonSerializable(typeof(DeleteSessionRequest))]
    [JsonSerializable(typeof(DeleteSessionResponse))]
    [JsonSerializable(typeof(GetLastSessionIdResponse))]
    [JsonSerializable(typeof(HooksInvokeResponse))]
    [JsonSerializable(typeof(ListSessionsRequest))]
    [JsonSerializable(typeof(ListSessionsResponse))]
    [JsonSerializable(typeof(PermissionRequestResult))]
    [JsonSerializable(typeof(PermissionRequestResponseV2))]
    [JsonSerializable(typeof(ProviderConfig))]
    [JsonSerializable(typeof(ResumeSessionRequest))]
    [JsonSerializable(typeof(ResumeSessionResponse))]
    [JsonSerializable(typeof(SessionMetadata))]
    [JsonSerializable(typeof(SystemMessageConfig))]
    [JsonSerializable(typeof(ToolCallResponseV2))]
    [JsonSerializable(typeof(ToolDefinition))]
    [JsonSerializable(typeof(ToolResultAIContent))]
    [JsonSerializable(typeof(ToolResultObject))]
    [JsonSerializable(typeof(UserInputRequestResponse))]
    [JsonSerializable(typeof(UserInputRequest))]
    [JsonSerializable(typeof(UserInputResponse))]
    internal partial class ClientJsonContext : JsonSerializerContext;

    /// <summary>
    /// AOT-safe type info resolver for <see cref="RequestId"/>.
    /// StreamJsonRpc's own RequestIdSTJsonConverter is internal (SYSLIB1220/CS0122),
    /// so we provide our own converter and wire it through <see cref="JsonMetadataServices.CreateValueInfo{T}"/>
    /// to stay fully AOT/trimming-compatible.
    /// </summary>
    private sealed class RequestIdTypeInfoResolver : IJsonTypeInfoResolver
    {
        public JsonTypeInfo? GetTypeInfo(Type type, JsonSerializerOptions options)
        {
            if (type == typeof(RequestId))
                return JsonMetadataServices.CreateValueInfo<RequestId>(options, new RequestIdJsonConverter());
            return null;
        }
    }

    private sealed class RequestIdJsonConverter : JsonConverter<RequestId>
    {
        public override RequestId Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return reader.TokenType switch
            {
                JsonTokenType.Number => reader.TryGetInt64(out long val)
                    ? new RequestId(val)
                    : new RequestId(reader.HasValueSequence
                        ? Encoding.UTF8.GetString(reader.ValueSequence)
                        : Encoding.UTF8.GetString(reader.ValueSpan)),
                JsonTokenType.String => new RequestId(reader.GetString()!),
                JsonTokenType.Null => RequestId.Null,
                _ => throw new JsonException($"Unexpected token type for RequestId: {reader.TokenType}"),
            };
        }

        public override void Write(Utf8JsonWriter writer, RequestId value, JsonSerializerOptions options)
        {
            if (value.Number.HasValue)
                writer.WriteNumberValue(value.Number.Value);
            else if (value.String is not null)
                writer.WriteStringValue(value.String);
            else
                writer.WriteNullValue();
        }
    }

    [GeneratedRegex(@"listening on port ([0-9]+)", RegexOptions.IgnoreCase)]
    private static partial Regex ListeningOnPortRegex();
}

/// <summary>
/// Wraps a <see cref="ToolResultObject"/> as <see cref="AIContent"/> to pass structured tool results
/// back through Microsoft.Extensions.AI without JSON serialization.
/// </summary>
/// <param name="toolResult">The tool result to wrap.</param>
public class ToolResultAIContent(ToolResultObject toolResult) : AIContent
{
    /// <summary>
    /// Gets the underlying <see cref="ToolResultObject"/>.
    /// </summary>
    public ToolResultObject Result => toolResult;
}
