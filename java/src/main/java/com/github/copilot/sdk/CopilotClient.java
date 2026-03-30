/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.CreateSessionResponse;
import com.github.copilot.sdk.json.DeleteSessionResponse;
import com.github.copilot.sdk.json.GetAuthStatusResponse;
import com.github.copilot.sdk.json.GetLastSessionIdResponse;
import com.github.copilot.sdk.json.GetModelsResponse;
import com.github.copilot.sdk.json.GetStatusResponse;
import com.github.copilot.sdk.json.ListSessionsResponse;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.PingResponse;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.ResumeSessionResponse;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionLifecycleHandler;
import com.github.copilot.sdk.json.SessionListFilter;
import com.github.copilot.sdk.json.SessionMetadata;

/**
 * Provides a client for interacting with the Copilot CLI server.
 * <p>
 * The CopilotClient manages the connection to the Copilot CLI server and
 * provides methods to create and manage conversation sessions. It can either
 * spawn a CLI server process or connect to an existing server.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * try (var client = new CopilotClient()) {
 * 	client.start().get();
 *
 * 	var session = client
 * 			.createSession(
 * 					new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-5"))
 * 			.get();
 *
 * 	session.on(AssistantMessageEvent.class, msg -> {
 * 		System.out.println(msg.getData().content());
 * 	});
 *
 * 	session.send(new MessageOptions().setPrompt("Hello!")).get();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class CopilotClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(CopilotClient.class.getName());

    /**
     * Timeout, in seconds, used by {@link #close()} when waiting for graceful
     * shutdown via {@link #stop()}.
     */
    public static final int AUTOCLOSEABLE_TIMEOUT_SECONDS = 10;
    private final CopilotClientOptions options;
    private final CliServerManager serverManager;
    private final LifecycleEventManager lifecycleManager = new LifecycleEventManager();
    private final Map<String, CopilotSession> sessions = new ConcurrentHashMap<>();
    private volatile CompletableFuture<Connection> connectionFuture;
    private volatile boolean disposed = false;
    private final String optionsHost;
    private final Integer optionsPort;
    private volatile List<ModelInfo> modelsCache;
    private final Object modelsCacheLock = new Object();

    /**
     * Creates a new CopilotClient with default options.
     */
    public CopilotClient() {
        this(new CopilotClientOptions());
    }

    /**
     * Creates a new CopilotClient with the specified options.
     *
     * @param options
     *            Options for creating the client
     * @throws IllegalArgumentException
     *             if mutually exclusive options are provided
     */
    public CopilotClient(CopilotClientOptions options) {
        this.options = options != null ? options : new CopilotClientOptions();

        // When cliUrl is set, auto-correct useStdio since we're connecting via TCP
        if (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty()) {
            this.options.setUseStdio(false);
        }

        // Validate mutually exclusive options: cliUrl and cliPath cannot both be set
        if (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty()
                && this.options.getCliPath() != null) {
            throw new IllegalArgumentException("CliUrl is mutually exclusive with CliPath");
        }

        // Validate auth options with external server
        if (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty()
                && (this.options.getGitHubToken() != null || this.options.getUseLoggedInUser() != null)) {
            throw new IllegalArgumentException(
                    "GitHubToken and UseLoggedInUser cannot be used with CliUrl (external server manages its own auth)");
        }

        // Parse CliUrl if provided
        if (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty()) {
            URI uri = CliServerManager.parseCliUrl(this.options.getCliUrl());
            this.optionsHost = uri.getHost();
            this.optionsPort = uri.getPort();
        } else {
            this.optionsHost = null;
            this.optionsPort = null;
        }

        this.serverManager = new CliServerManager(this.options);
    }

    /**
     * Starts the Copilot client and connects to the server.
     *
     * @return A future that completes when the connection is established
     */
    public CompletableFuture<Void> start() {
        if (connectionFuture == null) {
            synchronized (this) {
                if (connectionFuture == null) {
                    connectionFuture = startCore();
                }
            }
        }
        return connectionFuture.thenApply(c -> null);
    }

    private CompletableFuture<Connection> startCore() {
        LOG.fine("Starting Copilot client");

        Executor exec = options.getExecutor();
        return exec != null
                ? CompletableFuture.supplyAsync(this::startCoreBody, exec)
                : CompletableFuture.supplyAsync(this::startCoreBody);
    }

    private Connection startCoreBody() {
        try {
            JsonRpcClient rpc;
            Process process = null;

            if (optionsHost != null && optionsPort != null) {
                // External server (TCP)
                rpc = serverManager.connectToServer(null, optionsHost, optionsPort);
            } else {
                // Child process (stdio or TCP)
                CliServerManager.ProcessInfo processInfo = serverManager.startCliServer();
                process = processInfo.process();
                rpc = serverManager.connectToServer(process, processInfo.port() != null ? "localhost" : null,
                        processInfo.port());
            }

            Connection connection = new Connection(rpc, process);

            // Register handlers for server-to-client calls
            RpcHandlerDispatcher dispatcher = new RpcHandlerDispatcher(sessions, lifecycleManager::dispatch,
                    options.getExecutor());
            dispatcher.registerHandlers(rpc);

            // Verify protocol version
            verifyProtocolVersion(connection);

            LOG.info("Copilot client connected");
            return connection;
        } catch (Exception e) {
            String stderr = serverManager.getStderrOutput();
            if (!stderr.isEmpty()) {
                throw new CompletionException(new IOException("CLI process exited unexpectedly. stderr: " + stderr, e));
            }
            throw new CompletionException(e);
        }
    }

    private static final int MIN_PROTOCOL_VERSION = 2;

    private void verifyProtocolVersion(Connection connection) throws Exception {
        int expectedVersion = SdkProtocolVersion.get();
        var params = new HashMap<String, Object>();
        params.put("message", null);
        PingResponse pingResponse = connection.rpc.invoke("ping", params, PingResponse.class).get(30, TimeUnit.SECONDS);

        if (pingResponse.protocolVersion() == null) {
            throw new RuntimeException("SDK protocol version mismatch: SDK expects version " + expectedVersion
                    + ", but server does not report a protocol version. "
                    + "Please update your server to ensure compatibility.");
        }

        int serverVersion = pingResponse.protocolVersion();
        if (serverVersion < MIN_PROTOCOL_VERSION || serverVersion > expectedVersion) {
            throw new RuntimeException("SDK protocol version mismatch: SDK expects version " + expectedVersion
                    + " (minimum " + MIN_PROTOCOL_VERSION + "), but server reports version " + serverVersion + ". "
                    + "Please update your SDK or server to ensure compatibility.");
        }
    }

    /**
     * Disconnects from the Copilot server and closes all active sessions.
     * <p>
     * This method performs graceful cleanup:
     * <ol>
     * <li>Closes all active sessions (releases in-memory resources)</li>
     * <li>Closes the JSON-RPC connection</li>
     * <li>Terminates the CLI server process (if spawned by this client)</li>
     * </ol>
     * <p>
     * Note: session data on disk is preserved, so sessions can be resumed later. To
     * permanently remove session data before stopping, call
     * {@link #deleteSession(String)} for each session first.
     *
     * @return A future that completes when the client is stopped
     */
    public CompletableFuture<Void> stop() {
        var closeFutures = new ArrayList<CompletableFuture<Void>>();
        Executor exec = options.getExecutor();

        for (CopilotSession session : new ArrayList<>(sessions.values())) {
            Runnable closeTask = () -> {
                try {
                    session.close();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error closing session " + session.getSessionId(), e);
                }
            };
            closeFutures.add(
                    exec != null ? CompletableFuture.runAsync(closeTask, exec) : CompletableFuture.runAsync(closeTask));
        }
        sessions.clear();

        return CompletableFuture.allOf(closeFutures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> cleanupConnection());
    }

    /**
     * Forces an immediate stop of the client without graceful cleanup.
     *
     * @return A future that completes when the client is stopped
     */
    public CompletableFuture<Void> forceStop() {
        disposed = true;
        sessions.clear();
        return cleanupConnection();
    }

    private CompletableFuture<Void> cleanupConnection() {
        CompletableFuture<Connection> future = connectionFuture;
        connectionFuture = null;

        // Clear models cache
        modelsCache = null;

        if (future == null) {
            return CompletableFuture.completedFuture(null);
        }

        return future.thenAccept(connection -> {
            try {
                connection.rpc.close();
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error closing RPC", e);
            }

            if (connection.process != null) {
                try {
                    if (connection.process.isAlive()) {
                        connection.process.destroyForcibly();
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Error killing process", e);
                }
            }
        }).exceptionally(ex -> null);
    }

    /**
     * Creates a new Copilot session with the specified configuration.
     * <p>
     * The session maintains conversation state and can be used to send messages and
     * receive responses. Remember to close the session when done.
     * <p>
     * A permission handler is required when creating a session. Use
     * {@link com.github.copilot.sdk.json.PermissionHandler#APPROVE_ALL} to approve
     * all permission requests, or provide a custom handler to control permissions
     * selectively.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * var session = client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
     * }</pre>
     *
     * @param config
     *            configuration for the session, including the required
     *            {@link SessionConfig#setOnPermissionRequest(com.github.copilot.sdk.json.PermissionHandler)}
     *            handler
     * @return a future that resolves with the created CopilotSession
     * @throws IllegalArgumentException
     *             if {@code config} is {@code null} or does not have a permission
     *             handler set
     * @see SessionConfig
     * @see com.github.copilot.sdk.json.PermissionHandler#APPROVE_ALL
     */
    public CompletableFuture<CopilotSession> createSession(SessionConfig config) {
        if (config == null || config.getOnPermissionRequest() == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("An onPermissionRequest handler is required when creating a session. "
                            + "For example, to allow all permissions, use: "
                            + "new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)"));
        }
        return ensureConnected().thenCompose(connection -> {
            // Pre-generate session ID so the session can be registered before the RPC call,
            // ensuring no events emitted by the CLI during creation are lost.
            String sessionId = config.getSessionId() != null
                    ? config.getSessionId()
                    : java.util.UUID.randomUUID().toString();

            var session = new CopilotSession(sessionId, connection.rpc);
            if (options.getExecutor() != null) {
                session.setExecutor(options.getExecutor());
            }
            SessionRequestBuilder.configureSession(session, config);
            sessions.put(sessionId, session);

            // Extract transform callbacks from the system message config.
            // Callbacks are registered with the session; a wire-safe copy of the
            // system message (with transform sections replaced by action="transform")
            // is used in the RPC request.
            var extracted = SessionRequestBuilder.extractTransformCallbacks(config.getSystemMessage());
            if (extracted.transformCallbacks() != null) {
                session.registerTransformCallbacks(extracted.transformCallbacks());
            }

            var request = SessionRequestBuilder.buildCreateRequest(config, sessionId);
            if (extracted.wireSystemMessage() != config.getSystemMessage()) {
                request.setSystemMessage(extracted.wireSystemMessage());
            }

            return connection.rpc.invoke("session.create", request, CreateSessionResponse.class).thenApply(response -> {
                session.setWorkspacePath(response.workspacePath());
                // If the server returned a different sessionId (e.g. a v2 CLI that ignores
                // the client-supplied ID), re-key the sessions map.
                String returnedId = response.sessionId();
                if (returnedId != null && !returnedId.equals(sessionId)) {
                    sessions.remove(sessionId);
                    session.setActiveSessionId(returnedId);
                    sessions.put(returnedId, session);
                }
                return session;
            }).exceptionally(ex -> {
                sessions.remove(sessionId);
                throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
            });
        });
    }

    /**
     * Resumes an existing Copilot session.
     * <p>
     * This restores a previously saved session, allowing you to continue a
     * conversation. The session's history is preserved.
     * <p>
     * A permission handler is required when resuming a session. Use
     * {@link com.github.copilot.sdk.json.PermissionHandler#APPROVE_ALL} to approve
     * all permission requests, or provide a custom handler to control permissions
     * selectively.
     *
     * @param sessionId
     *            the ID of the session to resume
     * @param config
     *            configuration for the resumed session, including the required
     *            {@link ResumeSessionConfig#setOnPermissionRequest(com.github.copilot.sdk.json.PermissionHandler)}
     *            handler
     * @return a future that resolves with the resumed CopilotSession
     * @throws IllegalArgumentException
     *             if {@code config} is {@code null} or does not have a permission
     *             handler set
     * @see #listSessions()
     * @see #getLastSessionId()
     * @see com.github.copilot.sdk.json.PermissionHandler#APPROVE_ALL
     */
    public CompletableFuture<CopilotSession> resumeSession(String sessionId, ResumeSessionConfig config) {
        if (config == null || config.getOnPermissionRequest() == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("An onPermissionRequest handler is required when resuming a session. "
                            + "For example, to allow all permissions, use: "
                            + "new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)"));
        }
        return ensureConnected().thenCompose(connection -> {
            // Register the session before the RPC call to avoid missing early events.
            var session = new CopilotSession(sessionId, connection.rpc);
            if (options.getExecutor() != null) {
                session.setExecutor(options.getExecutor());
            }
            SessionRequestBuilder.configureSession(session, config);
            sessions.put(sessionId, session);

            // Extract transform callbacks from the system message config.
            var extracted = SessionRequestBuilder.extractTransformCallbacks(config.getSystemMessage());
            if (extracted.transformCallbacks() != null) {
                session.registerTransformCallbacks(extracted.transformCallbacks());
            }

            var request = SessionRequestBuilder.buildResumeRequest(sessionId, config);
            if (extracted.wireSystemMessage() != config.getSystemMessage()) {
                request.setSystemMessage(extracted.wireSystemMessage());
            }

            return connection.rpc.invoke("session.resume", request, ResumeSessionResponse.class).thenApply(response -> {
                session.setWorkspacePath(response.workspacePath());
                // If the server returned a different sessionId than what was requested, re-key.
                String returnedId = response.sessionId();
                if (returnedId != null && !returnedId.equals(sessionId)) {
                    sessions.remove(sessionId);
                    session.setActiveSessionId(returnedId);
                    sessions.put(returnedId, session);
                }
                return session;
            }).exceptionally(ex -> {
                sessions.remove(sessionId);
                throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
            });
        });
    }

    /**
     * Gets the current connection state.
     *
     * @return the current connection state
     * @see ConnectionState
     */
    public ConnectionState getState() {
        if (connectionFuture == null)
            return ConnectionState.DISCONNECTED;
        if (connectionFuture.isCompletedExceptionally())
            return ConnectionState.ERROR;
        if (!connectionFuture.isDone())
            return ConnectionState.CONNECTING;
        return ConnectionState.CONNECTED;
    }

    /**
     * Pings the server to check connectivity.
     * <p>
     * This can be used to verify that the server is responsive and to check the
     * protocol version.
     *
     * @param message
     *            an optional message to echo back
     * @return a future that resolves with the ping response
     * @see PingResponse
     */
    public CompletableFuture<PingResponse> ping(String message) {
        return ensureConnected().thenCompose(connection -> connection.rpc.invoke("ping",
                Map.of("message", message != null ? message : ""), PingResponse.class));
    }

    /**
     * Gets CLI status including version and protocol information.
     *
     * @return a future that resolves with the status response containing version
     *         and protocol version
     * @see GetStatusResponse
     */
    public CompletableFuture<GetStatusResponse> getStatus() {
        return ensureConnected()
                .thenCompose(connection -> connection.rpc.invoke("status.get", Map.of(), GetStatusResponse.class));
    }

    /**
     * Gets current authentication status.
     *
     * @return a future that resolves with the authentication status
     * @see GetAuthStatusResponse
     */
    public CompletableFuture<GetAuthStatusResponse> getAuthStatus() {
        return ensureConnected().thenCompose(
                connection -> connection.rpc.invoke("auth.getStatus", Map.of(), GetAuthStatusResponse.class));
    }

    /**
     * Lists available models with their metadata.
     * <p>
     * Results are cached after the first successful call to avoid rate limiting.
     * The cache is cleared when the client disconnects.
     * <p>
     * If an {@code onListModels} handler was provided in
     * {@link com.github.copilot.sdk.json.CopilotClientOptions}, it is called
     * instead of querying the CLI server. This is useful in BYOK mode.
     *
     * @return a future that resolves with a list of available models
     * @see ModelInfo
     */
    public CompletableFuture<List<ModelInfo>> listModels() {
        // Check cache first
        List<ModelInfo> cached = modelsCache;
        if (cached != null) {
            return CompletableFuture.completedFuture(new ArrayList<>(cached));
        }

        // If a custom handler is configured, use it instead of querying the CLI server
        var onListModels = options.getOnListModels();
        if (onListModels != null) {
            synchronized (modelsCacheLock) {
                if (modelsCache != null) {
                    return CompletableFuture.completedFuture(new ArrayList<>(modelsCache));
                }
            }
            return onListModels.get().thenApply(models -> {
                synchronized (modelsCacheLock) {
                    modelsCache = models;
                }
                return new ArrayList<>(models);
            });
        }

        return ensureConnected().thenCompose(connection -> {
            // Double-check cache inside lock
            synchronized (modelsCacheLock) {
                if (modelsCache != null) {
                    return CompletableFuture.completedFuture(new ArrayList<>(modelsCache));
                }
            }

            return connection.rpc.invoke("models.list", Map.of(), GetModelsResponse.class).thenApply(response -> {
                List<ModelInfo> models = response.getModels();
                synchronized (modelsCacheLock) {
                    modelsCache = models;
                }
                return new ArrayList<>(models); // Return a copy to prevent cache mutation
            });
        });
    }

    /**
     * Gets the ID of the most recently used session.
     * <p>
     * This is useful for resuming the last conversation without needing to list all
     * sessions.
     *
     * @return a future that resolves with the last session ID, or {@code null} if
     *         no sessions exist
     * @see #resumeSession(String, com.github.copilot.sdk.json.ResumeSessionConfig)
     */
    public CompletableFuture<String> getLastSessionId() {
        return ensureConnected().thenCompose(
                connection -> connection.rpc.invoke("session.getLastId", Map.of(), GetLastSessionIdResponse.class)
                        .thenApply(GetLastSessionIdResponse::sessionId));
    }

    /**
     * Permanently deletes a session and all its data from disk, including
     * conversation history, planning state, and artifacts.
     * <p>
     * Unlike {@link CopilotSession#close()}, which only releases in-memory
     * resources and preserves session data for later resumption, this method is
     * irreversible. The session cannot be resumed after deletion.
     *
     * @param sessionId
     *            the ID of the session to delete
     * @return a future that completes when the session is deleted
     * @throws RuntimeException
     *             if the deletion fails
     */
    public CompletableFuture<Void> deleteSession(String sessionId) {
        return ensureConnected().thenCompose(connection -> connection.rpc
                .invoke("session.delete", Map.of("sessionId", sessionId), DeleteSessionResponse.class)
                .thenAccept(response -> {
                    if (!response.success()) {
                        throw new RuntimeException("Failed to delete session " + sessionId + ": " + response.error());
                    }
                    sessions.remove(sessionId);
                }));
    }

    /**
     * Lists all available sessions.
     * <p>
     * Returns metadata about all sessions that can be resumed, including their IDs,
     * start times, and summaries.
     *
     * @return a future that resolves with a list of session metadata
     * @see SessionMetadata
     * @see #resumeSession(String, com.github.copilot.sdk.json.ResumeSessionConfig)
     */
    public CompletableFuture<List<SessionMetadata>> listSessions() {
        return listSessions(null);
    }

    /**
     * Lists all available sessions with optional filtering.
     * <p>
     * Returns metadata about all sessions that can be resumed, including their IDs,
     * start times, summaries, and context information. Use the filter parameter to
     * narrow down sessions by working directory, git repository, or branch.
     *
     * <h2>Example Usage</h2>
     *
     * <pre>{@code
     * // List all sessions
     * var allSessions = client.listSessions().get();
     *
     * // Filter by repository
     * var filter = new SessionListFilter().setRepository("owner/repo");
     * var repoSessions = client.listSessions(filter).get();
     * }</pre>
     *
     * @param filter
     *            optional filter to narrow down sessions by context fields, or
     *            {@code null} to list all sessions
     * @return a future that resolves with a list of session metadata
     * @see SessionMetadata
     * @see SessionListFilter
     * @see #resumeSession(String, com.github.copilot.sdk.json.ResumeSessionConfig)
     */
    public CompletableFuture<List<SessionMetadata>> listSessions(SessionListFilter filter) {
        return ensureConnected().thenCompose(connection -> {
            Map<String, Object> params = filter != null ? Map.of("filter", filter) : Map.of();
            return connection.rpc.invoke("session.list", params, ListSessionsResponse.class)
                    .thenApply(ListSessionsResponse::sessions);
        });
    }

    /**
     * Gets the ID of the session currently displayed in the TUI.
     * <p>
     * This is only available when connecting to a server running in TUI+server mode
     * (--ui-server).
     *
     * @return a future that resolves with the session ID, or null if no foreground
     *         session is set
     */
    public CompletableFuture<String> getForegroundSessionId() {
        return ensureConnected().thenCompose(connection -> connection.rpc
                .invoke("session.getForeground", Map.of(),
                        com.github.copilot.sdk.json.GetForegroundSessionResponse.class)
                .thenApply(com.github.copilot.sdk.json.GetForegroundSessionResponse::sessionId));
    }

    /**
     * Requests the TUI to switch to displaying the specified session.
     * <p>
     * This is only available when connecting to a server running in TUI+server mode
     * (--ui-server).
     *
     * @param sessionId
     *            the ID of the session to display in the TUI
     * @return a future that completes when the operation is done
     * @throws RuntimeException
     *             if the operation fails
     */
    public CompletableFuture<Void> setForegroundSessionId(String sessionId) {
        return ensureConnected()
                .thenCompose(
                        connection -> connection.rpc
                                .invoke("session.setForeground", Map.of("sessionId", sessionId),
                                        com.github.copilot.sdk.json.SetForegroundSessionResponse.class)
                                .thenAccept(response -> {
                                    if (!response.success()) {
                                        throw new RuntimeException(response.error() != null
                                                ? response.error()
                                                : "Failed to set foreground session");
                                    }
                                }));
    }

    /**
     * Subscribes to all session lifecycle events.
     * <p>
     * Lifecycle events are emitted when sessions are created, deleted, updated, or
     * change foreground/background state (in TUI+server mode).
     *
     * @param handler
     *            a callback that receives lifecycle events
     * @return an AutoCloseable that, when closed, unsubscribes the handler
     */
    public AutoCloseable onLifecycle(SessionLifecycleHandler handler) {
        return lifecycleManager.subscribe(handler);
    }

    /**
     * Subscribes to a specific session lifecycle event type.
     *
     * @param eventType
     *            the event type to listen for (use
     *            {@link com.github.copilot.sdk.json.SessionLifecycleEventTypes}
     *            constants)
     * @param handler
     *            a callback that receives events of the specified type
     * @return an AutoCloseable that, when closed, unsubscribes the handler
     */
    public AutoCloseable onLifecycle(String eventType, SessionLifecycleHandler handler) {
        return lifecycleManager.subscribe(eventType, handler);
    }

    private CompletableFuture<Connection> ensureConnected() {
        if (connectionFuture == null && !options.isAutoStart()) {
            throw new IllegalStateException("Client not connected. Call start() first.");
        }

        start();
        return connectionFuture;
    }

    /**
     * Closes this client using graceful shutdown semantics.
     * <p>
     * This method is intended for {@code try-with-resources} usage and blocks while
     * waiting for {@link #stop()} to complete, up to
     * {@link #AUTOCLOSEABLE_TIMEOUT_SECONDS} seconds. If shutdown fails or times
     * out, the error is logged at {@link Level#FINE} and the method returns.
     * <p>
     * This method is idempotent.
     *
     * @see #stop()
     * @see #forceStop()
     * @see #AUTOCLOSEABLE_TIMEOUT_SECONDS
     */
    @Override
    public void close() {
        if (disposed)
            return;
        disposed = true;
        try {
            stop().get(AUTOCLOSEABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error during close", e);
        }
    }

    private static record Connection(JsonRpcClient rpc, Process process) {
    };

}
