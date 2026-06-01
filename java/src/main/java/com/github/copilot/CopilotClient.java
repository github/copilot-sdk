/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.CreateSessionResponse;
import com.github.copilot.generated.rpc.SessionOptionsUpdateParams;
import com.github.copilot.generated.rpc.SessionInstalledPlugin;
import com.github.copilot.generated.rpc.ConnectParams;
import com.github.copilot.generated.rpc.ServerRpc;
import com.github.copilot.rpc.DeleteSessionResponse;
import com.github.copilot.rpc.GetAuthStatusResponse;
import com.github.copilot.rpc.GetLastSessionIdResponse;
import com.github.copilot.rpc.GetSessionMetadataResponse;
import com.github.copilot.rpc.GetModelsResponse;
import com.github.copilot.rpc.GetStatusResponse;
import com.github.copilot.rpc.ListSessionsResponse;
import com.github.copilot.rpc.ModelInfo;
import com.github.copilot.rpc.PingResponse;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.ResumeSessionResponse;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionLifecycleHandler;
import com.github.copilot.rpc.SessionListFilter;
import com.github.copilot.rpc.SessionMetadata;

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
    private static final int RUNTIME_SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final int FORCE_KILL_TIMEOUT_SECONDS = 10;

    /**
     * One-shot dispatcher used to run the owned-executor shutdown off any caller
     * thread that might itself belong to that executor (e.g. the
     * {@link #forceStop()} continuation, which is chained off async work scheduled
     * on the internal executor). Spawning a fresh daemon thread guarantees
     * {@link java.util.concurrent.ExecutorService#awaitTermination(long, TimeUnit)}
     * is never called from inside the very executor it is waiting on.
     */
    private static final Executor SHUTDOWN_DISPATCHER = runnable -> {
        Thread t = new Thread(runnable, "copilot-client-shutdown");
        t.setDaemon(true);
        t.start();
    };

    private final CopilotClientOptions options;
    private final Executor executor;
    private final boolean executorCanBeShutdown;
    private final CliServerManager serverManager;
    private final LifecycleEventManager lifecycleManager = new LifecycleEventManager();
    private final Map<String, CopilotSession> sessions = new ConcurrentHashMap<>();
    private volatile CompletableFuture<Connection> connectionFuture;
    private volatile boolean disposed = false;
    private final String optionsHost;
    private final Integer optionsPort;
    private final String effectiveConnectionToken;
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
                && (this.options.getGitHubToken() != null || this.options.getUseLoggedInUser().isPresent())) {
            throw new IllegalArgumentException(
                    "GitHubToken and UseLoggedInUser cannot be used with CliUrl (external server manages its own auth)");
        }

        // Validate tcpConnectionToken
        if (this.options.getTcpConnectionToken() != null) {
            if (this.options.getTcpConnectionToken().isEmpty()) {
                throw new IllegalArgumentException("TcpConnectionToken must be a non-empty string");
            }
            if (this.options.isUseStdio()) {
                throw new IllegalArgumentException("TcpConnectionToken cannot be used with UseStdio = true");
            }
        }

        // Compute effective connection token: use provided, or auto-generate for
        // SDK-spawned TCP mode, or null for stdio/external server
        boolean sdkSpawnsCli = !this.options.isUseStdio()
                && (this.options.getCliUrl() == null || this.options.getCliUrl().isEmpty());
        this.effectiveConnectionToken = this.options.getTcpConnectionToken() != null
                ? this.options.getTcpConnectionToken()
                : (sdkSpawnsCli ? java.util.UUID.randomUUID().toString() : null);

        // Empty mode: validate at construction time that the app supplied a
        // per-session persistence location.
        if (this.options.getMode() == CopilotClientMode.EMPTY) {
            boolean hasPersistence = (this.options.getCopilotHome() != null && !this.options.getCopilotHome().isEmpty())
                    || (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty());
            if (!hasPersistence) {
                throw new IllegalArgumentException(
                        "CopilotClient was created with Mode = EMPTY but neither CopilotHome nor CliUrl was set. "
                                + "Empty mode requires an explicit per-session persistence location.");
            }
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

        InternalExecutorProvider executorProvider = new InternalExecutorProvider(this.options.getExecutor());
        this.executor = executorProvider.get();
        this.executorCanBeShutdown = executorProvider.canBeShutdown();

        this.serverManager = new CliServerManager(this.options);
        this.serverManager.setConnectionToken(this.effectiveConnectionToken);
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

        try {
            return CompletableFuture.supplyAsync(this::startCoreBody, executor);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private Connection startCoreBody() {
        Process process = null;
        long startNanos = System.nanoTime();
        try {
            JsonRpcClient rpc;

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

            LoggingHelpers.logTiming(LOG, Level.FINE, "CopilotClient.start transport setup complete. Elapsed={Elapsed}",
                    startNanos);

            Connection connection = new Connection(rpc, process, new ServerRpc(rpc::invoke));

            // Register handlers for server-to-client calls
            RpcHandlerDispatcher dispatcher = new RpcHandlerDispatcher(sessions, lifecycleManager::dispatch, executor);
            dispatcher.registerHandlers(rpc);

            // Verify protocol version
            verifyProtocolVersion(connection);
            LoggingHelpers.logTiming(LOG, Level.FINE,
                    "CopilotClient.start protocol verification complete. Elapsed={Elapsed}", startNanos);

            LoggingHelpers.logTiming(LOG, Level.FINE, "CopilotClient.start complete. Elapsed={Elapsed}", startNanos);
            return connection;
        } catch (Exception e) {
            if (!(e instanceof java.util.concurrent.CancellationException)) {
                LoggingHelpers.logTiming(LOG, Level.WARNING, e, "CopilotClient.start failed. Elapsed={Elapsed}",
                        startNanos);
            }
            // Clean up the spawned process if connection setup failed
            if (process != null) {
                cleanupCliProcess(process, true);
            }
            String stderr = serverManager.getStderrOutput();
            if (!stderr.isEmpty()) {
                throw new CompletionException(new IOException(
                        CliServerManager.formatCliExitedMessage("CLI process exited unexpectedly.", stderr), e));
            }
            throw new CompletionException(e);
        }
    }

    private static final int MIN_PROTOCOL_VERSION = 2;
    private static final int METHOD_NOT_FOUND_ERROR_CODE = -32601;

    private void verifyProtocolVersion(Connection connection) throws Exception {
        int expectedVersion = SdkProtocolVersion.get();
        Integer serverVersion;

        try {
            // Try the new 'connect' RPC which supports connection tokens
            var connectParams = new ConnectParams(effectiveConnectionToken);
            var connectResponse = connection.rpc
                    .invoke("connect", connectParams, com.github.copilot.generated.rpc.ConnectResult.class)
                    .get(30, TimeUnit.SECONDS);
            serverVersion = connectResponse.protocolVersion() != null
                    ? connectResponse.protocolVersion().intValue()
                    : null;
        } catch (Exception e) {
            // Unwrap CompletionException/ExecutionException to check inner cause
            Throwable cause = e;
            while (cause instanceof java.util.concurrent.ExecutionException || cause instanceof CompletionException) {
                cause = cause.getCause();
            }
            if (cause instanceof JsonRpcException rpcEx && isUnsupportedConnectMethod(rpcEx)) {
                // Legacy server without 'connect'; fall back to 'ping'.
                // A token, if any, is silently dropped — the legacy server can't enforce one.
                var params = new HashMap<String, Object>();
                params.put("message", null);
                PingResponse pingResponse = connection.rpc.invoke("ping", params, PingResponse.class).get(30,
                        TimeUnit.SECONDS);
                serverVersion = pingResponse.protocolVersion();
            } else {
                throw e;
            }
        }

        if (serverVersion == null) {
            throw new RuntimeException("SDK protocol version mismatch: SDK supports versions " + MIN_PROTOCOL_VERSION
                    + "-" + expectedVersion + ", but server does not report a protocol version. "
                    + "Please update your server to ensure compatibility.");
        }

        if (serverVersion < MIN_PROTOCOL_VERSION || serverVersion > expectedVersion) {
            throw new RuntimeException("SDK protocol version mismatch: SDK supports versions " + MIN_PROTOCOL_VERSION
                    + "-" + expectedVersion + ", but server reports version " + serverVersion + ". "
                    + "Please update your SDK or server to ensure compatibility.");
        }
    }

    private static boolean isUnsupportedConnectMethod(JsonRpcException ex) {
        return ex.getCode() == METHOD_NOT_FOUND_ERROR_CODE || "Unhandled method connect".equals(ex.getMessage());
    }

    /**
     * Disconnects from the Copilot server and closes all active sessions.
     * <p>
     * This method performs graceful cleanup:
     * <ol>
     * <li>Closes all active sessions (releases in-memory resources)</li>
     * <li>Requests runtime shutdown for SDK-owned CLI processes</li>
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

        for (CopilotSession session : new ArrayList<>(sessions.values())) {
            Runnable closeTask = () -> {
                try {
                    session.close();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error closing session " + session.getSessionId(), e);
                }
            };
            CompletableFuture<Void> future;
            try {
                future = CompletableFuture.runAsync(closeTask, executor);
            } catch (RejectedExecutionException e) {
                LOG.log(Level.WARNING, "Executor rejected session close task; closing inline", e);
                closeTask.run();
                future = CompletableFuture.completedFuture(null);
            }
            closeFutures.add(future);
        }
        sessions.clear();

        return CompletableFuture.allOf(closeFutures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> cleanupConnection(true));
    }

    /**
     * Forces an immediate stop of the client without graceful cleanup.
     *
     * @return A future that completes when the client is stopped
     */
    public CompletableFuture<Void> forceStop() {
        disposed = true;
        sessions.clear();
        // Dispatch the blocking shutdownOwnedExecutor() on a dedicated thread:
        // cleanupConnection() is chained off async work running on the owned
        // executor, so a plain whenComplete(...) here could land the awaitTermination
        // call on one of the very threads it is waiting to drain, forcing the full
        // AUTOCLOSEABLE_TIMEOUT_SECONDS timeout followed by shutdownNow().
        return cleanupConnection(false).whenCompleteAsync((ignored, error) -> shutdownOwnedExecutor(), SHUTDOWN_DISPATCHER);
    }

    private CompletableFuture<Void> cleanupConnection(boolean gracefulRuntimeShutdown) {
        CompletableFuture<Connection> future = connectionFuture;
        connectionFuture = null;

        // Clear models cache
        modelsCache = null;

        if (future == null) {
            return CompletableFuture.completedFuture(null);
        }

        return future.handle((connection, startupError) -> {
            if (startupError != null) {
                LOG.log(Level.FINE, "Ignoring failed Copilot client startup during cleanup", startupError);
                return CompletableFuture.<Void>completedFuture(null);
            }

            CompletableFuture<Void> shutdownFuture = CompletableFuture.completedFuture(null);
            if (gracefulRuntimeShutdown && connection.process != null) {
                long runtimeShutdownStartNanos = System.nanoTime();
                shutdownFuture = connection.rpc.invoke("runtime.shutdown", Map.of(), Void.class)
                        .orTimeout(RUNTIME_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .whenComplete((ignored, error) -> {
                            if (error == null) {
                                LoggingHelpers.logTiming(LOG, Level.FINE,
                                        "CopilotClient.stop runtime shutdown complete. Elapsed={Elapsed}",
                                        runtimeShutdownStartNanos);
                            } else {
                                LoggingHelpers.logTiming(LOG, Level.FINE, error,
                                        "CopilotClient.stop runtime shutdown failed. Elapsed={Elapsed}",
                                        runtimeShutdownStartNanos);
                            }
                        });
            }

            return shutdownFuture.handle((ignored, error) -> {
                try {
                    connection.rpc.close();
                } catch (Exception e) {
                    LOG.log(Level.FINE, "Error closing RPC", e);
                }

                if (connection.process != null) {
                    cleanupCliProcess(connection.process, !gracefulRuntimeShutdown || error != null);
                }
                return (Void) null;
            });
        }).thenCompose(result -> result);
    }

    private static void cleanupCliProcess(Process process, boolean forceImmediately) {
        try {
            if (process.isAlive()) {
                if (!forceImmediately && process.waitFor(FORCE_KILL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    return;
                }

                if (forceImmediately) {
                    process.destroyForcibly();
                    if (!process.waitFor(FORCE_KILL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        LOG.fine("Process did not terminate within force kill timeout");
                    }
                    return;
                } else {
                    process.destroy();
                }
                if (!forceImmediately && process.waitFor(FORCE_KILL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    return;
                }

                process.destroyForcibly();
                if (!process.waitFor(FORCE_KILL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOG.fine("Process did not terminate within force kill timeout");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.FINE, "Interrupted while killing process", e);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error killing process", e);
        }
    }

    /**
     * Creates a new Copilot session with the specified configuration.
     * <p>
     * The session maintains conversation state and can be used to send messages and
     * receive responses. Remember to close the session when done.
     * <p>
     * A permission handler is required when creating a session. Use
     * {@link com.github.copilot.rpc.PermissionHandler#APPROVE_ALL} to approve all
     * permission requests, or provide a custom handler to control permissions
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
     *            {@link SessionConfig#setOnPermissionRequest(com.github.copilot.rpc.PermissionHandler)}
     *            handler
     * @return a future that resolves with the created CopilotSession
     * @throws IllegalArgumentException
     *             if {@code config} is {@code null} or does not have a permission
     *             handler set
     * @see SessionConfig
     * @see com.github.copilot.rpc.PermissionHandler#APPROVE_ALL
     */
    public CompletableFuture<CopilotSession> createSession(SessionConfig config) {
        if (config == null || config.getOnPermissionRequest() == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("An onPermissionRequest handler is required when creating a session. "
                            + "For example, to allow all permissions, use: "
                            + "new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)"));
        }
        return ensureConnected().thenCompose(connection -> {
            long totalNanos = System.nanoTime();
            // For cloud sessions, let the CLI/server assign the session id
            // and register the session lazily once the response arrives. For
            // non-cloud sessions we generate the id client-side (when the
            // caller didn't supply one) so the session can be registered
            // BEFORE the RPC — the CLI may issue session-scoped requests
            // (e.g. sessionFs.writeFile for workspace metadata) during
            // session.create processing, before it has sent the response.
            String callerSessionId = config.getSessionId();
            boolean useServerGeneratedId = config.getCloud() != null
                    && (callerSessionId == null || callerSessionId.isEmpty());
            String localSessionId = useServerGeneratedId
                    ? null
                    : (callerSessionId != null && !callerSessionId.isEmpty()
                            ? callerSessionId
                            : java.util.UUID.randomUUID().toString());

            // Extract transform callbacks from the system message config. Callbacks
            // are registered with the session; a wire-safe copy of the system
            // message (with transform sections replaced by action="transform") is
            // used in the RPC request.
            var extracted = SessionRequestBuilder.extractTransformCallbacks(config.getSystemMessage());

            // Creates the session, wires up handlers, and registers it in the
            // sessions map.
            java.util.function.Function<String, CopilotSession> initializeSession = sid -> {
                long setupNanos = System.nanoTime();
                var s = new CopilotSession(sid, connection.rpc);
                s.setExecutor(executor);
                SessionRequestBuilder.configureSession(s, config);
                if (extracted.transformCallbacks() != null) {
                    s.registerTransformCallbacks(extracted.transformCallbacks());
                }
                sessions.put(sid, s);
                LoggingHelpers.logTiming(LOG, Level.FINE,
                        "CopilotClient.createSession local setup complete. Elapsed={Elapsed}, SessionId=" + sid,
                        setupNanos);
                return s;
            };

            String[] registeredIdHolder = new String[1];
            CopilotSession[] preRegisteredSessionHolder = new CopilotSession[1];

            // Pre-register non-cloud sessions BEFORE issuing the RPC so any
            // session-scoped requests the CLI emits during session.create
            // processing can be routed to the correct handlers.
            if (localSessionId != null) {
                preRegisteredSessionHolder[0] = initializeSession.apply(localSessionId);
                registeredIdHolder[0] = localSessionId;
            }

            var request = SessionRequestBuilder.buildCreateRequest(config, localSessionId);
            if (extracted.wireSystemMessage() != config.getSystemMessage()) {
                request.setSystemMessage(extracted.wireSystemMessage());
            }

            // Empty mode: validate availableTools and set toolFilterPrecedence
            if (options.getMode() == CopilotClientMode.EMPTY) {
                if (config.getAvailableTools() == null) {
                    if (registeredIdHolder[0] != null) {
                        sessions.remove(registeredIdHolder[0]);
                    }
                    throw new IllegalArgumentException(
                            "CopilotClient is in Mode = EMPTY but the session config did not specify "
                                    + "availableTools. Empty mode requires every session to explicitly opt into "
                                    + "the tools it wants — e.g. setAvailableTools(new ToolSet().addBuiltIn(BuiltInTools.ISOLATED)).");
                }
                request.setToolFilterPrecedence("excluded");
                if (request.getSkipEmbeddingRetrieval() == null) {
                    request.setSkipEmbeddingRetrieval(true);
                }
                if (request.getEmbeddingCacheStorage() == null) {
                    request.setEmbeddingCacheStorage("in-memory");
                }
                if (request.getEnableOnDemandInstructionDiscovery() == null) {
                    request.setEnableOnDemandInstructionDiscovery(false);
                }
                if (request.getEnableFileHooks() == null) {
                    request.setEnableFileHooks(false);
                }
                if (request.getEnableHostGitOperations() == null) {
                    request.setEnableHostGitOperations(false);
                }
                if (request.getEnableSessionStore() == null) {
                    request.setEnableSessionStore(false);
                }
                if (request.getEnableSkills() == null) {
                    request.setEnableSkills(false);
                }
                if (request.getMcpOAuthTokenStorage() == null) {
                    request.setMcpOAuthTokenStorage("in-memory");
                }
            }

            long rpcNanos = System.nanoTime();
            return connection.rpc.invoke("session.create", request, CreateSessionResponse.class)
                    .thenCompose(response -> {
                        String returnedId = response.sessionId();
                        LoggingHelpers.logTiming(LOG, Level.FINE,
                                "CopilotClient.createSession session creation request completed. Elapsed={Elapsed}, SessionId="
                                        + (returnedId != null ? returnedId : localSessionId),
                                rpcNanos);
                        if (returnedId == null || returnedId.isEmpty()) {
                            throw new RuntimeException("session.create response did not include a sessionId");
                        }
                        if (localSessionId != null && !localSessionId.equals(returnedId)) {
                            throw new RuntimeException("session.create returned sessionId " + returnedId
                                    + " but the caller requested " + localSessionId);
                        }
                        CopilotSession session = preRegisteredSessionHolder[0] != null
                                ? preRegisteredSessionHolder[0]
                                : initializeSession.apply(returnedId);
                        registeredIdHolder[0] = returnedId;
                        session.setWorkspacePath(response.workspacePath());
                        session.setCapabilities(response.capabilities());

                        return updateSessionOptionsForMode(session, config.getSkipCustomInstructions().orElse(null),
                                config.getCustomAgentsLocalOnly().orElse(null),
                                config.getCoauthorEnabled().orElse(null),
                                config.getManageScheduleEnabled().orElse(null)).thenApply(v -> {
                                    LoggingHelpers.logTiming(LOG, Level.FINE,
                                            "CopilotClient.createSession complete. Elapsed={Elapsed}, SessionId="
                                                    + session.getSessionId(),
                                            totalNanos);
                                    return session;
                                });
                    }).exceptionally(ex -> {
                        if (registeredIdHolder[0] != null) {
                            sessions.remove(registeredIdHolder[0]);
                        }
                        LoggingHelpers.logTiming(LOG, Level.WARNING, ex,
                                "CopilotClient.createSession failed. Elapsed={Elapsed}, SessionId="
                                        + (registeredIdHolder[0] != null ? registeredIdHolder[0] : "<unassigned>"),
                                totalNanos);
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
     * {@link com.github.copilot.rpc.PermissionHandler#APPROVE_ALL} to approve all
     * permission requests, or provide a custom handler to control permissions
     * selectively.
     *
     * @param sessionId
     *            the ID of the session to resume
     * @param config
     *            configuration for the resumed session, including the required
     *            {@link ResumeSessionConfig#setOnPermissionRequest(com.github.copilot.rpc.PermissionHandler)}
     *            handler
     * @return a future that resolves with the resumed CopilotSession
     * @throws IllegalArgumentException
     *             if {@code config} is {@code null} or does not have a permission
     *             handler set
     * @see #listSessions()
     * @see #getLastSessionId()
     * @see com.github.copilot.rpc.PermissionHandler#APPROVE_ALL
     */
    public CompletableFuture<CopilotSession> resumeSession(String sessionId, ResumeSessionConfig config) {
        if (config == null || config.getOnPermissionRequest() == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("An onPermissionRequest handler is required when resuming a session. "
                            + "For example, to allow all permissions, use: "
                            + "new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)"));
        }
        return ensureConnected().thenCompose(connection -> {
            long totalNanos = System.nanoTime();
            // Register the session before the RPC call to avoid missing early events.
            long setupNanos = System.nanoTime();
            var session = new CopilotSession(sessionId, connection.rpc);
            session.setExecutor(executor);
            SessionRequestBuilder.configureSession(session, config);
            sessions.put(sessionId, session);
            LoggingHelpers.logTiming(LOG, Level.FINE,
                    "CopilotClient.resumeSession local setup complete. Elapsed={Elapsed}, SessionId=" + sessionId,
                    setupNanos);

            // Extract transform callbacks from the system message config.
            var extracted = SessionRequestBuilder.extractTransformCallbacks(config.getSystemMessage());
            if (extracted.transformCallbacks() != null) {
                session.registerTransformCallbacks(extracted.transformCallbacks());
            }

            var request = SessionRequestBuilder.buildResumeRequest(sessionId, config);
            if (extracted.wireSystemMessage() != config.getSystemMessage()) {
                request.setSystemMessage(extracted.wireSystemMessage());
            }

            // Empty mode: validate availableTools and set toolFilterPrecedence for resume
            // path
            if (options.getMode() == CopilotClientMode.EMPTY) {
                if (config.getAvailableTools() == null) {
                    throw new IllegalArgumentException(
                            "CopilotClient is in Mode = EMPTY but the resume session config did not specify "
                                    + "availableTools. Empty mode requires every session to explicitly opt into "
                                    + "the tools it wants — e.g. setAvailableTools(new ToolSet().addBuiltIn(BuiltInTools.ISOLATED)).");
                }
                request.setToolFilterPrecedence("excluded");
                if (request.getSkipEmbeddingRetrieval() == null) {
                    request.setSkipEmbeddingRetrieval(true);
                }
                if (request.getEmbeddingCacheStorage() == null) {
                    request.setEmbeddingCacheStorage("in-memory");
                }
                if (request.getEnableOnDemandInstructionDiscovery() == null) {
                    request.setEnableOnDemandInstructionDiscovery(false);
                }
                if (request.getEnableFileHooks() == null) {
                    request.setEnableFileHooks(false);
                }
                if (request.getEnableHostGitOperations() == null) {
                    request.setEnableHostGitOperations(false);
                }
                if (request.getEnableSessionStore() == null) {
                    request.setEnableSessionStore(false);
                }
                if (request.getEnableSkills() == null) {
                    request.setEnableSkills(false);
                }
                if (request.getMcpOAuthTokenStorage() == null) {
                    request.setMcpOAuthTokenStorage("in-memory");
                }
            }

            long rpcNanos = System.nanoTime();
            return connection.rpc.invoke("session.resume", request, ResumeSessionResponse.class)
                    .thenCompose(response -> {
                        LoggingHelpers.logTiming(LOG, Level.FINE,
                                "CopilotClient.resumeSession session resume request completed. Elapsed={Elapsed}, SessionId="
                                        + sessionId,
                                rpcNanos);
                        session.setWorkspacePath(response.workspacePath());
                        session.setCapabilities(response.capabilities());
                        // If the server returned a different sessionId than what was requested,
                        // re-key.
                        String returnedId = response.sessionId();
                        if (returnedId != null && !returnedId.equals(sessionId)) {
                            sessions.remove(sessionId);
                            session.setActiveSessionId(returnedId);
                            sessions.put(returnedId, session);
                        }

                        return updateSessionOptionsForMode(session, config.getSkipCustomInstructions().orElse(null),
                                config.getCustomAgentsLocalOnly().orElse(null),
                                config.getCoauthorEnabled().orElse(null),
                                config.getManageScheduleEnabled().orElse(null)).thenApply(v -> {
                                    LoggingHelpers.logTiming(LOG, Level.FINE,
                                            "CopilotClient.resumeSession complete. Elapsed={Elapsed}, SessionId="
                                                    + sessionId,
                                            totalNanos);
                                    return session;
                                });
                    }).exceptionally(ex -> {
                        sessions.remove(sessionId);
                        // Also remove the re-keyed entry if the server returned a different ID
                        String activeId = session.getSessionId();
                        if (!sessionId.equals(activeId)) {
                            sessions.remove(activeId);
                        }
                        LoggingHelpers.logTiming(LOG, Level.WARNING, ex,
                                "CopilotClient.resumeSession failed. Elapsed={Elapsed}, SessionId=" + sessionId,
                                totalNanos);
                        throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
                    });
        });
    }

    /**
     * Applies the post-create / post-resume {@code session.options.update} patch.
     * <p>
     * In {@link CopilotClientMode#EMPTY EMPTY} mode this defaults the four
     * overridable feature flags to safe values (caller values from the config win);
     * {@code installedPlugins=[]} is unconditional under empty mode so apps that
     * need plugins must switch modes. In {@link CopilotClientMode#COPILOT_CLI
     * COPILOT_CLI} mode only explicitly-set fields are forwarded.
     *
     * @param session
     *            the session to patch
     * @param skipCustomInstructions
     *            caller-supplied value, or {@code null} if not set
     * @param customAgentsLocalOnly
     *            caller-supplied value, or {@code null} if not set
     * @param coauthorEnabled
     *            caller-supplied value, or {@code null} if not set
     * @param manageScheduleEnabled
     *            caller-supplied value, or {@code null} if not set
     * @return a future that completes when the patch has been applied
     */
    CompletableFuture<Void> updateSessionOptionsForMode(CopilotSession session, Boolean skipCustomInstructions,
            Boolean customAgentsLocalOnly, Boolean coauthorEnabled, Boolean manageScheduleEnabled) {

        Boolean patchSkip = null;
        Boolean patchAgents = null;
        Boolean patchCoauthor = null;
        Boolean patchSchedule = null;
        List<SessionInstalledPlugin> patchPlugins = null;
        boolean hasAnyPatch = false;

        if (options.getMode() == CopilotClientMode.EMPTY) {
            patchSkip = skipCustomInstructions != null ? skipCustomInstructions : true;
            patchAgents = customAgentsLocalOnly != null ? customAgentsLocalOnly : true;
            patchCoauthor = coauthorEnabled != null ? coauthorEnabled : false;
            patchSchedule = manageScheduleEnabled != null ? manageScheduleEnabled : false;
            patchPlugins = List.of();
            hasAnyPatch = true;
        } else {
            if (skipCustomInstructions != null) {
                patchSkip = skipCustomInstructions;
                hasAnyPatch = true;
            }
            if (customAgentsLocalOnly != null) {
                patchAgents = customAgentsLocalOnly;
                hasAnyPatch = true;
            }
            if (coauthorEnabled != null) {
                patchCoauthor = coauthorEnabled;
                hasAnyPatch = true;
            }
            if (manageScheduleEnabled != null) {
                patchSchedule = manageScheduleEnabled;
                hasAnyPatch = true;
            }
        }

        if (!hasAnyPatch) {
            return CompletableFuture.completedFuture(null);
        }

        var params = new SessionOptionsUpdateParams(null, // sessionId — set by SessionOptionsApi
                null, // model
                null, // reasoningEffort
                null, // clientName
                null, // lspClientName
                null, // integrationId
                null, // featureFlags
                null, // isExperimentalMode
                null, // provider
                null, // workingDirectory
                null, // availableTools
                null, // excludedTools
                null, // toolFilterPrecedence
                null, // enableScriptSafety
                null, // shellInitProfile
                null, // shellProcessFlags
                null, // sandboxConfig
                null, // logInteractiveShells
                null, // envValueMode
                null, // skillDirectories
                null, // disabledSkills
                null, // enableOnDemandInstructionDiscovery
                patchPlugins, // installedPlugins
                patchAgents, // customAgentsLocalOnly
                patchSkip, // skipCustomInstructions
                null, // disabledInstructionSources
                patchCoauthor, // coauthorEnabled
                null, // trajectoryFile
                null, // enableStreaming
                null, // copilotUrl
                null, // askUserDisabled
                null, // continueOnAutoMode
                null, // runningInInteractiveMode
                null, // enableReasoningSummaries
                null, // agentContext
                null, // eventsLogDirectory
                null, // additionalContentExclusionPolicies
                patchSchedule // manageScheduleEnabled
        );

        return session.getRpc().options.update(params).<Void>thenCompose(result -> {
            LOG.fine("session.options.update applied for session " + session.getSessionId());
            return CompletableFuture.completedFuture(null);
        }).exceptionally(ex -> {
            // The runtime session exists but the post-create options patch failed.
            // Best-effort disconnect so we don't leak it (in empty mode it would
            // otherwise stay alive with permissive defaults).
            LOG.log(Level.WARNING, "session.options.update failed for session " + session.getSessionId(), ex);
            sessions.remove(session.getSessionId());
            try {
                session.close();
            } catch (Exception closeEx) {
                // Swallow: original error is the one the caller needs.
            }
            throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
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
     * Returns the typed RPC client for server-level methods.
     * <p>
     * Provides strongly-typed access to all server-level API namespaces such as
     * {@code models}, {@code tools}, {@code account}, and {@code mcp}.
     * <p>
     * Example usage:
     *
     * <pre>{@code
     * client.start().get();
     * var models = client.getRpc().models.list().get();
     * }</pre>
     *
     * @return the server-level typed RPC client
     * @throws IllegalStateException
     *             if the client is not connected; call {@link #start()} first
     * @since 1.0.0
     */
    public ServerRpc getRpc() {
        CompletableFuture<Connection> future = connectionFuture;
        if (future == null || !future.isDone() || future.isCompletedExceptionally()) {
            throw new IllegalStateException("Client not connected; call start() first");
        }
        return future.join().serverRpc();
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
     * {@link com.github.copilot.rpc.CopilotClientOptions}, it is called instead of
     * querying the CLI server. This is useful in BYOK mode.
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
     * @see #resumeSession(String, com.github.copilot.rpc.ResumeSessionConfig)
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
     * @see #resumeSession(String, com.github.copilot.rpc.ResumeSessionConfig)
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
     * @see #resumeSession(String, com.github.copilot.rpc.ResumeSessionConfig)
     */
    public CompletableFuture<List<SessionMetadata>> listSessions(SessionListFilter filter) {
        return ensureConnected().thenCompose(connection -> {
            Map<String, Object> params = filter != null ? Map.of("filter", filter) : Map.of();
            return connection.rpc.invoke("session.list", params, ListSessionsResponse.class)
                    .thenApply(ListSessionsResponse::sessions);
        });
    }

    /**
     * Gets metadata for a specific session by ID.
     * <p>
     * This provides an efficient O(1) lookup of a single session's metadata instead
     * of listing all sessions.
     *
     * <h2>Example Usage</h2>
     *
     * <pre>{@code
     * var metadata = client.getSessionMetadata("session-123").get();
     * if (metadata != null) {
     * 	System.out.println("Session started at: " + metadata.getStartTime());
     * }
     * }</pre>
     *
     * @param sessionId
     *            the ID of the session to look up
     * @return a future that resolves with the {@link SessionMetadata}, or
     *         {@code null} if the session was not found
     * @see SessionMetadata
     * @since 1.0.0
     */
    public CompletableFuture<SessionMetadata> getSessionMetadata(String sessionId) {
        return ensureConnected().thenCompose(connection -> connection.rpc
                .invoke("session.getMetadata", Map.of("sessionId", sessionId), GetSessionMetadataResponse.class)
                .thenApply(GetSessionMetadataResponse::session));
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
                .invoke("session.getForeground", Map.of(), com.github.copilot.rpc.GetForegroundSessionResponse.class)
                .thenApply(com.github.copilot.rpc.GetForegroundSessionResponse::sessionId));
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
        return ensureConnected().thenCompose(connection -> connection.rpc
                .invoke("session.setForeground", new com.github.copilot.rpc.SetForegroundSessionRequest(sessionId),
                        com.github.copilot.rpc.SetForegroundSessionResponse.class)
                .thenAccept(response -> {
                    if (!response.success()) {
                        throw new RuntimeException(
                                response.error() != null ? response.error() : "Failed to set foreground session");
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
     *            {@link com.github.copilot.rpc.SessionLifecycleEventTypes}
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
        } finally {
            shutdownOwnedExecutor();
        }
    }

    private void shutdownOwnedExecutor() {
        if (!executorCanBeShutdown) {
            return;
        }

        ExecutorService serviceToShutdown = executor instanceof ExecutorService es ? es : null;
        if (serviceToShutdown == null) {
            LOG.log(Level.FINE, "Executor is not an ExecutorService; skipping shutdown");
            return;
        }

        // Short-circuit when the owned executor is already shut down. close() and
        // forceStop() can each call this method (e.g. forceStop() invoked before a
        // subsequent close() in user code), and re-entering shutdown() +
        // awaitTermination()
        // is redundant. Logging at FINE aids diagnostics without spamming normal
        // output.
        if (serviceToShutdown.isShutdown()) {
            LOG.log(Level.FINE, "Owned executor was already shut down; skipping redundant shutdown call.");
            return;
        }

        serviceToShutdown.shutdown();
        try {
            if (!serviceToShutdown.awaitTermination(AUTOCLOSEABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOG.log(Level.FINE, "Owned executor did not terminate within {0} seconds; forcing shutdown.",
                        AUTOCLOSEABLE_TIMEOUT_SECONDS);
                serviceToShutdown.shutdownNow();
            }
        } catch (InterruptedException e) {
            serviceToShutdown.shutdownNow();
            Thread.currentThread().interrupt();
            LOG.log(Level.FINE, "Interrupted while waiting for owned executor to terminate", e);
        }
    }

    private static record Connection(JsonRpcClient rpc, Process process, ServerRpc serverRpc) {
    };

}
