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
 * 	var session = client.createSession(new SessionConfig().setModel("gpt-5")).get();
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
                && (this.options.getGithubToken() != null || this.options.getUseLoggedInUser() != null)) {
            throw new IllegalArgumentException(
                    "GithubToken and UseLoggedInUser cannot be used with CliUrl (external server manages its own auth)");
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

        return CompletableFuture.supplyAsync(() -> {
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
                RpcHandlerDispatcher dispatcher = new RpcHandlerDispatcher(sessions, lifecycleManager::dispatch);
                dispatcher.registerHandlers(rpc);

                // Verify protocol version
                verifyProtocolVersion(connection);

                LOG.info("Copilot client connected");
                return connection;
            } catch (Exception e) {
                String stderr = serverManager.getStderrOutput();
                if (!stderr.isEmpty()) {
                    throw new CompletionException(
                            new IOException("CLI process exited unexpectedly. stderr: " + stderr, e));
                }
                throw new CompletionException(e);
            }
        });
    }

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

        if (pingResponse.protocolVersion() != expectedVersion) {
            throw new RuntimeException("SDK protocol version mismatch: SDK expects version " + expectedVersion
                    + ", but server reports version " + pingResponse.protocolVersion() + ". "
                    + "Please update your SDK or server to ensure compatibility.");
        }
    }

    /**
     * Stops the client and closes all sessions.
     *
     * @return A future that completes when the client is stopped
     */
    public CompletableFuture<Void> stop() {
        var closeFutures = new ArrayList<CompletableFuture<Void>>();

        for (CopilotSession session : new ArrayList<>(sessions.values())) {
            closeFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    session.close();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error closing session " + session.getSessionId(), e);
                }
            }));
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
     *
     * @param config
     *            configuration for the session (model, tools, etc.)
     * @return a future that resolves with the created CopilotSession
     * @see #createSession()
     * @see SessionConfig
     */
    public CompletableFuture<CopilotSession> createSession(SessionConfig config) {
        return ensureConnected().thenCompose(connection -> {
            var request = SessionRequestBuilder.buildCreateRequest(config);

            return connection.rpc.invoke("session.create", request, CreateSessionResponse.class).thenApply(response -> {
                var session = new CopilotSession(response.sessionId(), connection.rpc, response.workspacePath());
                SessionRequestBuilder.configureSession(session, config);
                sessions.put(response.sessionId(), session);
                return session;
            });
        });
    }

    /**
     * Creates a new Copilot session with default configuration.
     *
     * @return a future that resolves with the created CopilotSession
     * @see #createSession(SessionConfig)
     */
    public CompletableFuture<CopilotSession> createSession() {
        return createSession(null);
    }

    /**
     * Resumes an existing Copilot session.
     * <p>
     * This restores a previously saved session, allowing you to continue a
     * conversation. The session's history is preserved.
     *
     * @param sessionId
     *            the ID of the session to resume
     * @param config
     *            configuration for the resumed session
     * @return a future that resolves with the resumed CopilotSession
     * @see #resumeSession(String)
     * @see #listSessions()
     * @see #getLastSessionId()
     */
    public CompletableFuture<CopilotSession> resumeSession(String sessionId, ResumeSessionConfig config) {
        return ensureConnected().thenCompose(connection -> {
            var request = SessionRequestBuilder.buildResumeRequest(sessionId, config);

            return connection.rpc.invoke("session.resume", request, ResumeSessionResponse.class).thenApply(response -> {
                var session = new CopilotSession(response.sessionId(), connection.rpc, response.workspacePath());
                SessionRequestBuilder.configureSession(session, config);
                sessions.put(response.sessionId(), session);
                return session;
            });
        });
    }

    /**
     * Resumes an existing session with default configuration.
     *
     * @param sessionId
     *            the ID of the session to resume
     * @return a future that resolves with the resumed CopilotSession
     * @see #resumeSession(String, ResumeSessionConfig)
     */
    public CompletableFuture<CopilotSession> resumeSession(String sessionId) {
        return resumeSession(sessionId, null);
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
     * @see #resumeSession(String)
     */
    public CompletableFuture<String> getLastSessionId() {
        return ensureConnected().thenCompose(
                connection -> connection.rpc.invoke("session.getLastId", Map.of(), GetLastSessionIdResponse.class)
                        .thenApply(GetLastSessionIdResponse::sessionId));
    }

    /**
     * Deletes a session by ID.
     * <p>
     * This permanently removes the session and its conversation history.
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
     * @see #resumeSession(String)
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
     * @see #resumeSession(String)
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
