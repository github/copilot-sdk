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
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

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
/// await using var session = await client.CreateSessionAsync(new SessionConfig { Model = "gpt-4" });
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
public partial class CopilotClient : IDisposable, IAsyncDisposable
{
    private readonly ConcurrentDictionary<string, CopilotSession> _sessions = new();
    private readonly CopilotClientOptions _options;
    private readonly ILogger _logger;
    private Task<Connection>? _connectionTask;
    private bool _disposed;
    private readonly int? _optionsPort;
    private readonly string? _optionsHost;
    private List<ModelInfo>? _modelsCache;
    private readonly SemaphoreSlim _modelsCacheLock = new(1, 1);

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
        if (!string.IsNullOrEmpty(_options.CliUrl) && (_options.UseStdio || _options.CliPath != null))
        {
            throw new ArgumentException("CliUrl is mutually exclusive with UseStdio and CliPath");
        }

        // Validate auth options with external server
        if (!string.IsNullOrEmpty(_options.CliUrl) && (!string.IsNullOrEmpty(_options.GithubToken) || _options.UseLoggedInUser != null))
        {
            throw new ArgumentException("GithubToken and UseLoggedInUser cannot be used with CliUrl (external server manages its own auth)");
        }

        _logger = _options.Logger ?? NullLogger.Instance;

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

            Task<Connection> result;

            if (_optionsHost is not null && _optionsPort is not null)
            {
                // External server (TCP)
                result = ConnectToServerAsync(null, _optionsHost, _optionsPort, ct);
            }
            else
            {
                // Child process (stdio or TCP)
                var (cliProcess, portOrNull) = await StartCliServerAsync(_options, _logger, ct);
                result = ConnectToServerAsync(cliProcess, portOrNull is null ? null : "localhost", portOrNull, ct);
            }

            var connection = await result;

            // Verify protocol version compatibility
            await VerifyProtocolVersionAsync(connection, ct);

            _logger.LogInformation("Copilot client connected");
            return connection;
        }
    }

    /// <summary>
    /// Disconnects from the Copilot server and stops all active sessions.
    /// </summary>
    /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
    /// <remarks>
    /// <para>
    /// This method performs graceful cleanup:
    /// <list type="number">
    ///     <item>Destroys all active sessions</item>
    ///     <item>Closes the JSON-RPC connection</item>
    ///     <item>Terminates the CLI server process (if spawned by this client)</item>
    /// </list>
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
                errors.Add(new Exception($"Failed to destroy session {session.SessionId}: {ex.Message}", ex));
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

        // Clear models cache
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
    /// <param name="config">Configuration for the session. If null, default settings are used.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that resolves to provide the <see cref="CopilotSession"/>.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the client is not connected and AutoStart is disabled, or when a session with the same ID already exists.</exception>
    /// <remarks>
    /// Sessions maintain conversation state, handle events, and manage tool execution.
    /// If the client is not connected and <see cref="CopilotClientOptions.AutoStart"/> is enabled (default),
    /// this will automatically start the connection.
    /// </remarks>
    /// <example>
    /// <code>
    /// // Basic session
    /// var session = await client.CreateSessionAsync();
    ///
    /// // Session with model and tools
    /// var session = await client.CreateSessionAsync(new SessionConfig
    /// {
    ///     Model = "gpt-4",
    ///     Tools = [AIFunctionFactory.Create(MyToolMethod)]
    /// });
    /// </code>
    /// </example>
    public async Task<CopilotSession> CreateSessionAsync(SessionConfig? config = null, CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var hasHooks = config?.Hooks != null && (
            config.Hooks.OnPreToolUse != null ||
            config.Hooks.OnPostToolUse != null ||
            config.Hooks.OnUserPromptSubmitted != null ||
            config.Hooks.OnSessionStart != null ||
            config.Hooks.OnSessionEnd != null ||
            config.Hooks.OnErrorOccurred != null);

        var request = new CreateSessionRequest(
            config?.Model,
            config?.SessionId,
            config?.ReasoningEffort,
            config?.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
            config?.SystemMessage,
            config?.AvailableTools,
            config?.ExcludedTools,
            config?.Provider,
            config?.OnPermissionRequest != null ? true : null,
            config?.OnUserInputRequest != null ? true : null,
            hasHooks ? true : null,
            config?.WorkingDirectory,
            config?.Streaming == true ? true : null,
            config?.McpServers,
            config?.CustomAgents,
            config?.ConfigDir,
            config?.SkillDirectories,
            config?.DisabledSkills,
            config?.InfiniteSessions);

        var response = await InvokeRpcAsync<CreateSessionResponse>(
            connection.Rpc, "session.create", [request], cancellationToken);

        var session = new CopilotSession(response.SessionId, connection.Rpc, response.WorkspacePath);
        session.RegisterTools(config?.Tools ?? []);
        if (config?.OnPermissionRequest != null)
        {
            session.RegisterPermissionHandler(config.OnPermissionRequest);
        }
        if (config?.OnUserInputRequest != null)
        {
            session.RegisterUserInputHandler(config.OnUserInputRequest);
        }
        if (config?.Hooks != null)
        {
            session.RegisterHooks(config.Hooks);
        }

        if (!_sessions.TryAdd(response.SessionId, session))
        {
            throw new InvalidOperationException($"Session {response.SessionId} already exists");
        }

        return session;
    }

    /// <summary>
    /// Resumes an existing Copilot session with the specified configuration.
    /// </summary>
    /// <param name="sessionId">The ID of the session to resume.</param>
    /// <param name="config">Configuration for the resumed session. If null, default settings are used.</param>
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
    /// var session = await client.ResumeSessionAsync("session-123");
    ///
    /// // Resume with new tools
    /// var session = await client.ResumeSessionAsync("session-123", new ResumeSessionConfig
    /// {
    ///     Tools = [AIFunctionFactory.Create(MyNewToolMethod)]
    /// });
    /// </code>
    /// </example>
    public async Task<CopilotSession> ResumeSessionAsync(string sessionId, ResumeSessionConfig? config = null, CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var hasHooks = config?.Hooks != null && (
            config.Hooks.OnPreToolUse != null ||
            config.Hooks.OnPostToolUse != null ||
            config.Hooks.OnUserPromptSubmitted != null ||
            config.Hooks.OnSessionStart != null ||
            config.Hooks.OnSessionEnd != null ||
            config.Hooks.OnErrorOccurred != null);

        var request = new ResumeSessionRequest(
            sessionId,
            config?.ReasoningEffort,
            config?.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
            config?.Provider,
            config?.OnPermissionRequest != null ? true : null,
            config?.OnUserInputRequest != null ? true : null,
            hasHooks ? true : null,
            config?.WorkingDirectory,
            config?.DisableResume == true ? true : null,
            config?.Streaming == true ? true : null,
            config?.McpServers,
            config?.CustomAgents,
            config?.SkillDirectories,
            config?.DisabledSkills);

        var response = await InvokeRpcAsync<ResumeSessionResponse>(
            connection.Rpc, "session.resume", [request], cancellationToken);

        var session = new CopilotSession(response.SessionId, connection.Rpc, response.WorkspacePath);
        session.RegisterTools(config?.Tools ?? []);
        if (config?.OnPermissionRequest != null)
        {
            session.RegisterPermissionHandler(config.OnPermissionRequest);
        }
        if (config?.OnUserInputRequest != null)
        {
            session.RegisterUserInputHandler(config.OnUserInputRequest);
        }
        if (config?.Hooks != null)
        {
            session.RegisterHooks(config.Hooks);
        }

        // Replace any existing session entry to ensure new config (like permission handler) is used
        _sessions[response.SessionId] = session;
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
    ///     var session = await client.CreateSessionAsync();
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
        var connection = await EnsureConnectedAsync(cancellationToken);

        // Use semaphore for async locking to prevent race condition with concurrent calls
        await _modelsCacheLock.WaitAsync(cancellationToken);
        try
        {
            // Check cache (already inside lock)
            if (_modelsCache is not null)
            {
                return new List<ModelInfo>(_modelsCache); // Return a copy to prevent cache mutation
            }

            // Cache miss - fetch from backend while holding lock
            var response = await InvokeRpcAsync<GetModelsResponse>(
                connection.Rpc, "models.list", [], cancellationToken);

            // Update cache before releasing lock
            _modelsCache = response.Models;

            return new List<ModelInfo>(response.Models); // Return a copy to prevent cache mutation
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
    ///     var session = await client.ResumeSessionAsync(lastId);
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
    /// Deletes a Copilot session by its ID.
    /// </summary>
    /// <param name="sessionId">The ID of the session to delete.</param>
    /// <param name="cancellationToken">A <see cref="CancellationToken"/> that can be used to cancel the operation.</param>
    /// <returns>A task that represents the asynchronous delete operation.</returns>
    /// <exception cref="InvalidOperationException">Thrown when the session does not exist or deletion fails.</exception>
    /// <remarks>
    /// This permanently removes the session and all its conversation history.
    /// The session cannot be resumed after deletion.
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
    public async Task<List<SessionMetadata>> ListSessionsAsync(CancellationToken cancellationToken = default)
    {
        var connection = await EnsureConnectedAsync(cancellationToken);

        var response = await InvokeRpcAsync<ListSessionsResponse>(
            connection.Rpc, "session.list", [], cancellationToken);

        return response.Sessions;
    }

    internal static async Task<T> InvokeRpcAsync<T>(JsonRpc rpc, string method, object?[]? args, CancellationToken cancellationToken)
    {
        try
        {
            return await rpc.InvokeWithCancellationAsync<T>(method, args, cancellationToken);
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
        var expectedVersion = SdkProtocolVersion.GetVersion();
        var pingResponse = await InvokeRpcAsync<PingResponse>(
            connection.Rpc, "ping", [new PingRequest()], cancellationToken);

        if (!pingResponse.ProtocolVersion.HasValue)
        {
            throw new InvalidOperationException(
                $"SDK protocol version mismatch: SDK expects version {expectedVersion}, " +
                $"but server does not report a protocol version. " +
                $"Please update your server to ensure compatibility.");
        }

        if (pingResponse.ProtocolVersion.Value != expectedVersion)
        {
            throw new InvalidOperationException(
                $"SDK protocol version mismatch: SDK expects version {expectedVersion}, " +
                $"but server reports version {pingResponse.ProtocolVersion.Value}. " +
                $"Please update your SDK or server to ensure compatibility.");
        }
    }

    private static async Task<(Process Process, int? DetectedLocalhostTcpPort)> StartCliServerAsync(CopilotClientOptions options, ILogger logger, CancellationToken cancellationToken)
    {
        var cliPath = options.CliPath ?? "copilot";
        var args = new List<string>();

        if (options.CliArgs != null)
        {
            args.AddRange(options.CliArgs);
        }

        args.AddRange(["--server", "--log-level", options.LogLevel]);

        if (options.UseStdio)
        {
            args.Add("--stdio");
        }
        else if (options.Port > 0)
        {
            args.AddRange(["--port", options.Port.ToString()]);
        }

        // Add auth-related flags
        if (!string.IsNullOrEmpty(options.GithubToken))
        {
            args.AddRange(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"]);
        }

        // Default UseLoggedInUser to false when GithubToken is provided
        var useLoggedInUser = options.UseLoggedInUser ?? string.IsNullOrEmpty(options.GithubToken);
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
        if (!string.IsNullOrEmpty(options.GithubToken))
        {
            startInfo.Environment["COPILOT_SDK_AUTH_TOKEN"] = options.GithubToken;
        }

        var cliProcess = new Process { StartInfo = startInfo };
        cliProcess.Start();

        // Forward stderr to logger
        _ = Task.Run(async () =>
        {
            while (cliProcess != null && !cliProcess.HasExited)
            {
                var line = await cliProcess.StandardError.ReadLineAsync(cancellationToken);
                if (line != null)
                {
                    logger.LogDebug("[CLI] {Line}", line);
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
                var line = await cliProcess.StandardOutput.ReadLineAsync(cts.Token);
                if (line == null) throw new Exception("CLI process exited unexpectedly");

                var match = Regex.Match(line, @"listening on port (\d+)", RegexOptions.IgnoreCase);
                if (match.Success)
                {
                    detectedLocalhostTcpPort = int.Parse(match.Groups[1].Value);
                    break;
                }
            }
        }

        return (cliProcess, detectedLocalhostTcpPort);
    }

    private static (string FileName, IEnumerable<string> Args) ResolveCliCommand(string cliPath, IEnumerable<string> args)
    {
        var isJsFile = cliPath.EndsWith(".js", StringComparison.OrdinalIgnoreCase);

        if (isJsFile)
        {
            return ("node", new[] { cliPath }.Concat(args));
        }

        // On Windows with UseShellExecute=false, Process.Start doesn't search PATHEXT,
        // so use cmd /c to let the shell resolve the executable
        if (OperatingSystem.IsWindows() && !Path.IsPathRooted(cliPath))
        {
            return ("cmd", new[] { "/c", cliPath }.Concat(args));
        }

        return (cliPath, args);
    }

    private async Task<Connection> ConnectToServerAsync(Process? cliProcess, string? tcpHost, int? tcpPort, CancellationToken cancellationToken)
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
        rpc.AddLocalRpcMethod("tool.call", handler.OnToolCall);
        rpc.AddLocalRpcMethod("permission.request", handler.OnPermissionRequest);
        rpc.AddLocalRpcMethod("userInput.request", handler.OnUserInputRequest);
        rpc.AddLocalRpcMethod("hooks.invoke", handler.OnHooksInvoke);
        rpc.StartListening();
        return new Connection(rpc, cliProcess, tcpClient, networkStream);
    }

    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Using happy path from https://microsoft.github.io/vs-streamjsonrpc/docs/nativeAOT.html")]
    [UnconditionalSuppressMessage("AOT", "IL3050", Justification = "Using happy path from https://microsoft.github.io/vs-streamjsonrpc/docs/nativeAOT.html")]
    private static SystemTextJsonFormatter CreateSystemTextJsonFormatter() =>
        new SystemTextJsonFormatter() { JsonSerializerOptions = SerializerOptionsForMessageFormatter };

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

        options.MakeReadOnly();

        return options;
    }

    internal CopilotSession? GetSession(string sessionId) =>
        _sessions.TryGetValue(sessionId, out var session) ? session : null;

    /// <summary>
    /// Disposes the <see cref="CopilotClient"/> synchronously.
    /// </summary>
    /// <remarks>
    /// Prefer using <see cref="DisposeAsync"/> for better performance in async contexts.
    /// </remarks>
    public void Dispose()
    {
        DisposeAsync().GetAwaiter().GetResult();
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

        public async Task<ToolCallResponse> OnToolCall(string sessionId,
            string toolCallId,
            string toolName,
            object? arguments)
        {
            var session = client.GetSession(sessionId);
            if (session == null)
            {
                throw new ArgumentException($"Unknown session {sessionId}");
            }

            if (session.GetTool(toolName) is not { } tool)
            {
                return new ToolCallResponse(new ToolResultObject
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

                // Map args from JSON into AIFunction format
                var aiFunctionArgs = new AIFunctionArguments
                {
                    Context = new Dictionary<object, object?>
                    {
                        // Allow recipient to access the raw ToolInvocation if they want, e.g., to get SessionId
                        // This is an alternative to using MEAI's ConfigureParameterBinding, which we can't use
                        // because we're not the ones producing the AIFunction.
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
                        // MEAI will deserialize the JsonElement value respecting the delegate's parameter types
                        aiFunctionArgs[prop.Name] = prop.Value;
                    }
                }

                var result = await tool.InvokeAsync(aiFunctionArgs);

                // If the function returns a ToolResultObject, use it directly; otherwise, wrap the result
                // This lets the developer provide BinaryResult, SessionLog, etc. if they deal with that themselves
                var toolResultObject = result is ToolResultAIContent trac ? trac.Result : new ToolResultObject
                {
                    ResultType = "success",

                    // In most cases, result will already have been converted to JsonElement by the AIFunction.
                    // We special-case string for consistency with our Node/Python/Go clients.
                    // TODO: I don't think it's right to special-case string here, and all the clients should
                    // always serialize the result to JSON (otherwise what stringification is going to happen?
                    // something we don't control? an error?)
                    TextResultForLlm = result is JsonElement { ValueKind: JsonValueKind.String } je
                        ? je.GetString()!
                        : JsonSerializer.Serialize(result, tool.JsonSerializerOptions.GetTypeInfo(typeof(object))),
                };
                return new ToolCallResponse(toolResultObject);
            }
            catch (Exception ex)
            {
                return new ToolCallResponse(new()
                {
                    // TODO: We should offer some way to control whether or not to expose detailed exception information to the LLM.
                    //       For security, the default must be false, but developers can opt into allowing it.
                    TextResultForLlm = $"Invoking this tool produced an error. Detailed information is not available.",
                    ResultType = "failure",
                    Error = ex.Message
                });
            }
        }

        public async Task<PermissionRequestResponse> OnPermissionRequest(string sessionId, JsonElement permissionRequest)
        {
            var session = client.GetSession(sessionId);
            if (session == null)
            {
                return new PermissionRequestResponse(new PermissionRequestResult
                {
                    Kind = "denied-no-approval-rule-and-could-not-request-from-user"
                });
            }

            try
            {
                var result = await session.HandlePermissionRequestAsync(permissionRequest);
                return new PermissionRequestResponse(result);
            }
            catch
            {
                // If permission handler fails, deny the permission
                return new PermissionRequestResponse(new PermissionRequestResult
                {
                    Kind = "denied-no-approval-rule-and-could-not-request-from-user"
                });
            }
        }

        public async Task<UserInputRequestResponse> OnUserInputRequest(string sessionId, string question, List<string>? choices = null, bool? allowFreeform = null)
        {
            var session = client.GetSession(sessionId);
            if (session == null)
            {
                throw new ArgumentException($"Unknown session {sessionId}");
            }

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
            var session = client.GetSession(sessionId);
            if (session == null)
            {
                throw new ArgumentException($"Unknown session {sessionId}");
            }

            var output = await session.HandleHooksInvokeAsync(hookType, input);
            return new HooksInvokeResponse(output);
        }
    }

    private class Connection(
        JsonRpc rpc,
        Process? cliProcess, // Set if we created the child process
        TcpClient? tcpClient, // Set if using TCP
        NetworkStream? networkStream) // Set if using TCP
    {
        public Process? CliProcess => cliProcess;
        public TcpClient? TcpClient => tcpClient;
        public JsonRpc Rpc => rpc;
        public NetworkStream? NetworkStream => networkStream;
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
        List<CustomAgentConfig>? CustomAgents,
        string? ConfigDir,
        List<string>? SkillDirectories,
        List<string>? DisabledSkills,
        InfiniteSessionConfig? InfiniteSessions);

    internal record ToolDefinition(
        string Name,
        string? Description,
        JsonElement Parameters /* JSON schema */)
    {
        public static ToolDefinition FromAIFunction(AIFunction function)
            => new ToolDefinition(function.Name, function.Description, function.JsonSchema);
    }

    internal record CreateSessionResponse(
        string SessionId,
        string? WorkspacePath);

    internal record ResumeSessionRequest(
        string SessionId,
        string? ReasoningEffort,
        List<ToolDefinition>? Tools,
        ProviderConfig? Provider,
        bool? RequestPermission,
        bool? RequestUserInput,
        bool? Hooks,
        string? WorkingDirectory,
        bool? DisableResume,
        bool? Streaming,
        Dictionary<string, object>? McpServers,
        List<CustomAgentConfig>? CustomAgents,
        List<string>? SkillDirectories,
        List<string>? DisabledSkills);

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

    internal record ListSessionsResponse(
        List<SessionMetadata> Sessions);

    internal record ToolCallResponse(
        ToolResultObject? Result);

    internal record PermissionRequestResponse(
        PermissionRequestResult Result);

    internal record UserInputRequestResponse(
        string Answer,
        bool WasFreeform);

    internal record HooksInvokeResponse(
        object? Output);

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
            public override void TraceEvent(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, string? message) =>
                logger.Log(MapLevel(eventType), "[{Source}] {Message}", source, message);

            public override void TraceEvent(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, string? format, params object?[]? args) =>
                logger.Log(MapLevel(eventType), "[{Source}] {Message}", source, args is null || args.Length == 0 ? format : string.Format(format ?? "", args));

            public override void TraceData(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, object? data) =>
                logger.Log(MapLevel(eventType), "[{Source}] {Data}", source, data);

            public override void TraceData(TraceEventCache? eventCache, string source, TraceEventType eventType, int id, params object?[]? data) =>
                logger.Log(MapLevel(eventType), "[{Source}] {Data}", source, data is null ? null : string.Join(", ", data));

            public override void Write(string? message) =>
                logger.LogTrace("{Message}", message);

            public override void WriteLine(string? message) =>
                logger.LogTrace("{Message}", message);

            private static LogLevel MapLevel(TraceEventType eventType) => eventType switch
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
    [JsonSerializable(typeof(ListSessionsResponse))]
    [JsonSerializable(typeof(PermissionRequestResponse))]
    [JsonSerializable(typeof(PermissionRequestResult))]
    [JsonSerializable(typeof(ProviderConfig))]
    [JsonSerializable(typeof(ResumeSessionRequest))]
    [JsonSerializable(typeof(ResumeSessionResponse))]
    [JsonSerializable(typeof(SessionMetadata))]
    [JsonSerializable(typeof(SystemMessageConfig))]
    [JsonSerializable(typeof(ToolCallResponse))]
    [JsonSerializable(typeof(ToolDefinition))]
    [JsonSerializable(typeof(ToolResultAIContent))]
    [JsonSerializable(typeof(ToolResultObject))]
    [JsonSerializable(typeof(UserInputRequestResponse))]
    [JsonSerializable(typeof(UserInputRequest))]
    [JsonSerializable(typeof(UserInputResponse))]
    internal partial class ClientJsonContext : JsonSerializerContext;
}

// Must inherit from AIContent as a signal to MEAI to avoid JSON-serializing the
// value before passing it back to us
public class ToolResultAIContent(ToolResultObject toolResult) : AIContent
{
    public ToolResultObject Result => toolResult;
}
