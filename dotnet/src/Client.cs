/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Newtonsoft.Json.Linq;
using StreamJsonRpc;
using System.Collections.Concurrent;
using System.Data;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Net.Sockets;
using System.Text.Json;
using System.Text.Json.Nodes;
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
///     if (evt.Type == "assistant.message")
///         Console.WriteLine(evt.Data?.Content);
/// });
///
/// // Send a message
/// await session.SendAsync(new MessageOptions { Prompt = "Hello!" });
/// </code>
/// </example>
public class CopilotClient : IDisposable, IAsyncDisposable
{
    private readonly ConcurrentDictionary<string, CopilotSession> _sessions = new();
    private readonly CopilotClientOptions _options;
    private readonly ILogger _logger;
    private Task<Connection>? _connectionTask;
    private bool _disposed;
    private readonly int? _optionsPort;
    private readonly string? _optionsHost;

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
    /// var client = new CopilotClient(new CopilotClientOptions { CliUrl = "localhost:3000" });
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

        var request = new CreateSessionRequest(
            config?.Model,
            config?.SessionId,
            config?.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
            config?.SystemMessage,
            config?.AvailableTools,
            config?.ExcludedTools,
            config?.Provider,
            config?.OnPermissionRequest != null ? true : null,
            config?.Streaming == true ? true : null,
            config?.McpServers,
            config?.CustomAgents);

        var response = await connection.Rpc.InvokeWithCancellationAsync<CreateSessionResponse>(
            "session.create", [request], cancellationToken);

        var session = new CopilotSession(response.SessionId, connection.Rpc);
        session.RegisterTools(config?.Tools ?? []);
        if (config?.OnPermissionRequest != null)
        {
            session.RegisterPermissionHandler(config.OnPermissionRequest);
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

        var request = new ResumeSessionRequest(
            sessionId,
            config?.Tools?.Select(ToolDefinition.FromAIFunction).ToList(),
            config?.Provider,
            config?.OnPermissionRequest != null ? true : null,
            config?.Streaming == true ? true : null,
            config?.McpServers,
            config?.CustomAgents);

        var response = await connection.Rpc.InvokeWithCancellationAsync<ResumeSessionResponse>(
            "session.resume", [request], cancellationToken);

        var session = new CopilotSession(response.SessionId, connection.Rpc);
        session.RegisterTools(config?.Tools ?? []);
        if (config?.OnPermissionRequest != null)
        {
            session.RegisterPermissionHandler(config.OnPermissionRequest);
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

        return await connection.Rpc.InvokeWithCancellationAsync<PingResponse>(
            "ping", [new { message }], cancellationToken);
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

        var response = await connection.Rpc.InvokeWithCancellationAsync<GetLastSessionIdResponse>(
            "session.getLastId", [], cancellationToken);

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

        var response = await connection.Rpc.InvokeWithCancellationAsync<DeleteSessionResponse>(
            "session.delete", [new { sessionId }], cancellationToken);

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

        var response = await connection.Rpc.InvokeWithCancellationAsync<ListSessionsResponse>(
            "session.list", [], cancellationToken);

        return response.Sessions;
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
        var pingResponse = await connection.Rpc.InvokeWithCancellationAsync<PingResponse>(
            "ping", [new { message = (string?)null }], cancellationToken);

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

        var rpc = new JsonRpc(new HeaderDelimitedMessageHandler(outputStream, inputStream, CreateFormatter()));
        rpc.AddLocalRpcTarget(new RpcHandler(this));
        rpc.StartListening();
        return new Connection(rpc, cliProcess, tcpClient, networkStream);
    }

    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Using the Json source generator.")]
    [UnconditionalSuppressMessage("AOT", "IL3050", Justification = "Using the Json source generator.")]
    static IJsonRpcMessageFormatter CreateFormatter()
    {
        var options = new JsonSerializerOptions(JsonSerializerDefaults.Web)
        {
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
        };
        foreach (var converter in SerializerOptions.Default.Converters)
        {
            options.Converters.Add(converter);
        }
        return new SystemTextJsonFormatter() { JsonSerializerOptions = options };
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
        [JsonRpcMethod("session.event")]
        public void OnSessionEvent(string sessionId,
            JsonElement? @event)
        {
            var session = client.GetSession(sessionId);
            if (session != null && @event != null)
            {
                var evt = SessionEvent.FromJson(@event.Value.GetRawText());
                session.DispatchEvent(evt);
            }
        }

        [JsonRpcMethod("tool.call")]
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
                        : JsonSerializer.Serialize(result, tool.JsonSerializerOptions),
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

        [JsonRpcMethod("permission.request")]
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
    private record CreateSessionRequest(
        string? Model,
        string? SessionId,
        List<ToolDefinition>? Tools,
        SystemMessageConfig? SystemMessage,
        List<string>? AvailableTools,
        List<string>? ExcludedTools,
        ProviderConfig? Provider,
        bool? RequestPermission,
        bool? Streaming,
        Dictionary<string, object>? McpServers,
        List<CustomAgentConfig>? CustomAgents);

    private record ToolDefinition(
        string Name,
        string? Description,
        JsonElement Parameters /* JSON schema */)
    {
        public static ToolDefinition FromAIFunction(AIFunction function)
            => new ToolDefinition(function.Name, function.Description, function.JsonSchema);
    }

    private record CreateSessionResponse(
        string SessionId);

    private record ResumeSessionRequest(
        string SessionId,
        List<ToolDefinition>? Tools,
        ProviderConfig? Provider,
        bool? RequestPermission,
        bool? Streaming,
        Dictionary<string, object>? McpServers,
        List<CustomAgentConfig>? CustomAgents);

    private record ResumeSessionResponse(
        string SessionId);

    private record GetLastSessionIdResponse(
        string? SessionId);

    private record DeleteSessionResponse(
        bool Success,
        string? Error);

    private record ListSessionsResponse(
        List<SessionMetadata> Sessions);

    private record ToolCallResponse(
        ToolResultObject? Result);

    private record PermissionRequestResponse(
        PermissionRequestResult Result);
}

// Must inherit from AIContent as a signal to MEAI to avoid JSON-serializing the
// value before passing it back to us
public class ToolResultAIContent(ToolResultObject toolResult) : AIContent
{
    public ToolResultObject Result => toolResult;
}
