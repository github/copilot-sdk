/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.SessionEventParser;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.CreateSessionRequest;
import com.github.copilot.sdk.json.CreateSessionResponse;
import com.github.copilot.sdk.json.DeleteSessionResponse;
import com.github.copilot.sdk.json.GetAuthStatusResponse;
import com.github.copilot.sdk.json.GetLastSessionIdResponse;
import com.github.copilot.sdk.json.GetModelsResponse;
import com.github.copilot.sdk.json.GetStatusResponse;
import com.github.copilot.sdk.json.ListSessionsResponse;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PingResponse;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.ResumeSessionRequest;
import com.github.copilot.sdk.json.ResumeSessionResponse;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionMetadata;
import com.github.copilot.sdk.json.ToolDef;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolInvocation;
import com.github.copilot.sdk.json.ToolResultObject;

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
 * try (CopilotClient client = new CopilotClient()) {
 * 	client.start().get();
 *
 * 	CopilotSession session = client.createSession(new SessionConfig().setModel("gpt-5")).get();
 *
 * 	session.on(evt -> {
 * 		if (evt instanceof AssistantMessageEvent msg) {
 * 			System.out.println(msg.getData().getContent());
 * 		}
 * 	});
 *
 * 	session.send(new MessageOptions().setPrompt("Hello!")).get();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class CopilotClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(CopilotClient.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private final CopilotClientOptions options;
    private final Map<String, CopilotSession> sessions = new ConcurrentHashMap<>();
    private volatile CompletableFuture<Connection> connectionFuture;
    private volatile boolean disposed = false;
    private final String optionsHost;
    private final Integer optionsPort;

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

        // Validate mutually exclusive options
        if (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty()
                && (this.options.isUseStdio() || this.options.getCliPath() != null)) {
            throw new IllegalArgumentException("CliUrl is mutually exclusive with UseStdio and CliPath");
        }

        // Parse CliUrl if provided
        if (this.options.getCliUrl() != null && !this.options.getCliUrl().isEmpty()) {
            URI uri = parseCliUrl(this.options.getCliUrl());
            this.optionsHost = uri.getHost();
            this.optionsPort = uri.getPort();
        } else {
            this.optionsHost = null;
            this.optionsPort = null;
        }
    }

    private static URI parseCliUrl(String url) {
        // If it's just a port number, treat as localhost
        try {
            int port = Integer.parseInt(url);
            return URI.create("http://localhost:" + port);
        } catch (NumberFormatException e) {
            // Not a port number, continue
        }

        // Add scheme if missing
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            url = "https://" + url;
        }

        return URI.create(url);
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
                Connection connection;

                if (optionsHost != null && optionsPort != null) {
                    // External server (TCP)
                    connection = connectToServer(null, optionsHost, optionsPort);
                } else {
                    // Child process (stdio or TCP)
                    ProcessInfo processInfo = startCliServer();
                    connection = connectToServer(processInfo.process, processInfo.port != null ? "localhost" : null,
                            processInfo.port);
                }

                // Register handlers for server-to-client calls
                registerRpcHandlers(connection.rpc);

                // Verify protocol version
                verifyProtocolVersion(connection);

                LOG.info("Copilot client connected");
                return connection;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private void registerRpcHandlers(JsonRpcClient rpc) {
        // Handle session events
        rpc.registerMethodHandler("session.event", (requestId, params) -> {
            try {
                String sessionId = params.get("sessionId").asText();
                JsonNode eventNode = params.get("event");

                CopilotSession session = sessions.get(sessionId);
                if (session != null && eventNode != null) {
                    AbstractSessionEvent event = SessionEventParser.parse(eventNode.toString());
                    if (event != null) {
                        session.dispatchEvent(event);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling session event", e);
            }
        });

        // Handle tool calls
        rpc.registerMethodHandler("tool.call", (requestId, params) -> {
            handleToolCall(rpc, requestId, params);
        });

        // Handle permission requests
        rpc.registerMethodHandler("permission.request", (requestId, params) -> {
            handlePermissionRequest(rpc, requestId, params);
        });
    }

    private void handleToolCall(JsonRpcClient rpc, String requestId, JsonNode params) {
        CompletableFuture.runAsync(() -> {
            try {
                String sessionId = params.get("sessionId").asText();
                String toolCallId = params.get("toolCallId").asText();
                String toolName = params.get("toolName").asText();
                JsonNode arguments = params.get("arguments");

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    rpc.sendErrorResponse(Long.parseLong(requestId), -32602, "Unknown session " + sessionId);
                    return;
                }

                ToolDefinition tool = session.getTool(toolName);
                if (tool == null || tool.getHandler() == null) {
                    ToolResultObject result = new ToolResultObject()
                            .setTextResultForLlm("Tool '" + toolName + "' is not supported.").setResultType("failure")
                            .setError("tool '" + toolName + "' not supported");
                    rpc.sendResponse(Long.parseLong(requestId), Map.of("result", result));
                    return;
                }

                ToolInvocation invocation = new ToolInvocation().setSessionId(sessionId).setToolCallId(toolCallId)
                        .setToolName(toolName).setArguments(arguments);

                tool.getHandler().invoke(invocation).thenAccept(result -> {
                    try {
                        ToolResultObject toolResult;
                        if (result instanceof ToolResultObject tr) {
                            toolResult = tr;
                        } else {
                            toolResult = new ToolResultObject().setResultType("success").setTextResultForLlm(
                                    result instanceof String s ? s : MAPPER.writeValueAsString(result));
                        }
                        rpc.sendResponse(Long.parseLong(requestId), Map.of("result", toolResult));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error sending tool result", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        ToolResultObject result = new ToolResultObject()
                                .setTextResultForLlm(
                                        "Invoking this tool produced an error. Detailed information is not available.")
                                .setResultType("failure").setError(ex.getMessage());
                        rpc.sendResponse(Long.parseLong(requestId), Map.of("result", result));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error sending tool error", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling tool call", e);
                try {
                    rpc.sendErrorResponse(Long.parseLong(requestId), -32603, e.getMessage());
                } catch (IOException ioe) {
                    LOG.log(Level.SEVERE, "Failed to send error response", ioe);
                }
            }
        });
    }

    private void handlePermissionRequest(JsonRpcClient rpc, String requestId, JsonNode params) {
        CompletableFuture.runAsync(() -> {
            try {
                String sessionId = params.get("sessionId").asText();
                JsonNode permissionRequest = params.get("permissionRequest");

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    PermissionRequestResult result = new PermissionRequestResult()
                            .setKind("denied-no-approval-rule-and-could-not-request-from-user");
                    rpc.sendResponse(Long.parseLong(requestId), Map.of("result", result));
                    return;
                }

                session.handlePermissionRequest(permissionRequest).thenAccept(result -> {
                    try {
                        rpc.sendResponse(Long.parseLong(requestId), Map.of("result", result));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending permission result", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        PermissionRequestResult result = new PermissionRequestResult()
                                .setKind("denied-no-approval-rule-and-could-not-request-from-user");
                        rpc.sendResponse(Long.parseLong(requestId), Map.of("result", result));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending permission denied", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling permission request", e);
            }
        });
    }

    private void verifyProtocolVersion(Connection connection) throws Exception {
        int expectedVersion = SdkProtocolVersion.get();
        Map<String, Object> params = new HashMap<>();
        params.put("message", null);
        PingResponse pingResponse = connection.rpc.invoke("ping", params, PingResponse.class).get(30, TimeUnit.SECONDS);

        if (pingResponse.getProtocolVersion() == null) {
            throw new RuntimeException("SDK protocol version mismatch: SDK expects version " + expectedVersion
                    + ", but server does not report a protocol version. "
                    + "Please update your server to ensure compatibility.");
        }

        if (pingResponse.getProtocolVersion() != expectedVersion) {
            throw new RuntimeException("SDK protocol version mismatch: SDK expects version " + expectedVersion
                    + ", but server reports version " + pingResponse.getProtocolVersion() + ". "
                    + "Please update your SDK or server to ensure compatibility.");
        }
    }

    /**
     * Stops the client and closes all sessions.
     *
     * @return A future that completes when the client is stopped
     */
    public CompletableFuture<Void> stop() {
        List<CompletableFuture<Void>> closeFutures = new ArrayList<>();

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
        sessions.clear();
        return cleanupConnection();
    }

    private CompletableFuture<Void> cleanupConnection() {
        CompletableFuture<Connection> future = connectionFuture;
        connectionFuture = null;

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
            CreateSessionRequest request = new CreateSessionRequest();
            if (config != null) {
                request.setModel(config.getModel());
                request.setSessionId(config.getSessionId());
                request.setTools(config.getTools() != null
                        ? config.getTools().stream()
                                .map(t -> new ToolDef(t.getName(), t.getDescription(), t.getParameters()))
                                .collect(Collectors.toList())
                        : null);
                request.setSystemMessage(config.getSystemMessage());
                request.setAvailableTools(config.getAvailableTools());
                request.setExcludedTools(config.getExcludedTools());
                request.setProvider(config.getProvider());
                request.setRequestPermission(config.getOnPermissionRequest() != null ? true : null);
                request.setStreaming(config.isStreaming() ? true : null);
                request.setMcpServers(config.getMcpServers());
                request.setCustomAgents(config.getCustomAgents());
                request.setInfiniteSessions(config.getInfiniteSessions());
                request.setSkillDirectories(config.getSkillDirectories());
                request.setDisabledSkills(config.getDisabledSkills());
                request.setConfigDir(config.getConfigDir());
            }

            return connection.rpc.invoke("session.create", request, CreateSessionResponse.class).thenApply(response -> {
                CopilotSession session = new CopilotSession(response.getSessionId(), connection.rpc,
                        response.getWorkspacePath());
                if (config != null && config.getTools() != null) {
                    session.registerTools(config.getTools());
                }
                if (config != null && config.getOnPermissionRequest() != null) {
                    session.registerPermissionHandler(config.getOnPermissionRequest());
                }
                sessions.put(response.getSessionId(), session);
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
            ResumeSessionRequest request = new ResumeSessionRequest();
            request.setSessionId(sessionId);
            if (config != null) {
                request.setTools(config.getTools() != null
                        ? config.getTools().stream()
                                .map(t -> new ToolDef(t.getName(), t.getDescription(), t.getParameters()))
                                .collect(Collectors.toList())
                        : null);
                request.setProvider(config.getProvider());
                request.setRequestPermission(config.getOnPermissionRequest() != null ? true : null);
                request.setStreaming(config.isStreaming() ? true : null);
                request.setMcpServers(config.getMcpServers());
                request.setCustomAgents(config.getCustomAgents());
                request.setSkillDirectories(config.getSkillDirectories());
                request.setDisabledSkills(config.getDisabledSkills());
            }

            return connection.rpc.invoke("session.resume", request, ResumeSessionResponse.class).thenApply(response -> {
                CopilotSession session = new CopilotSession(response.getSessionId(), connection.rpc,
                        response.getWorkspacePath());
                if (config != null && config.getTools() != null) {
                    session.registerTools(config.getTools());
                }
                if (config != null && config.getOnPermissionRequest() != null) {
                    session.registerPermissionHandler(config.getOnPermissionRequest());
                }
                sessions.put(response.getSessionId(), session);
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
     *
     * @return a future that resolves with a list of available models
     * @see ModelInfo
     */
    public CompletableFuture<List<ModelInfo>> listModels() {
        return ensureConnected().thenCompose(connection -> connection.rpc
                .invoke("models.list", Map.of(), GetModelsResponse.class).thenApply(GetModelsResponse::getModels));
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
                        .thenApply(GetLastSessionIdResponse::getSessionId));
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
                    if (!response.isSuccess()) {
                        throw new RuntimeException(
                                "Failed to delete session " + sessionId + ": " + response.getError());
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
        return ensureConnected()
                .thenCompose(connection -> connection.rpc.invoke("session.list", Map.of(), ListSessionsResponse.class)
                        .thenApply(ListSessionsResponse::getSessions));
    }

    private CompletableFuture<Connection> ensureConnected() {
        if (connectionFuture == null && !options.isAutoStart()) {
            throw new IllegalStateException("Client not connected. Call start() first.");
        }

        start();
        return connectionFuture;
    }

    private ProcessInfo startCliServer() throws IOException, InterruptedException {
        String cliPath = options.getCliPath() != null ? options.getCliPath() : "copilot";
        List<String> args = new ArrayList<>();

        if (options.getCliArgs() != null) {
            args.addAll(Arrays.asList(options.getCliArgs()));
        }

        args.add("--server");
        args.add("--log-level");
        args.add(options.getLogLevel());

        if (options.isUseStdio()) {
            args.add("--stdio");
        } else if (options.getPort() > 0) {
            args.add("--port");
            args.add(String.valueOf(options.getPort()));
        }

        List<String> command = resolveCliCommand(cliPath, args);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        if (options.getCwd() != null) {
            pb.directory(new File(options.getCwd()));
        }

        if (options.getEnvironment() != null) {
            pb.environment().clear();
            pb.environment().putAll(options.getEnvironment());
        }
        pb.environment().remove("NODE_DEBUG");

        Process process = pb.start();

        // Forward stderr to logger in background
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.fine("[CLI] " + line);
                }
            } catch (IOException e) {
                LOG.log(Level.FINE, "Error reading stderr", e);
            }
        }, "cli-stderr-reader");
        stderrThread.setDaemon(true);
        stderrThread.start();

        Integer detectedPort = null;
        if (!options.isUseStdio()) {
            // Wait for port announcement
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Pattern portPattern = Pattern.compile("listening on port (\\d+)", Pattern.CASE_INSENSITIVE);
            long deadline = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < deadline) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("CLI process exited unexpectedly");
                }

                Matcher matcher = portPattern.matcher(line);
                if (matcher.find()) {
                    detectedPort = Integer.parseInt(matcher.group(1));
                    break;
                }
            }

            if (detectedPort == null) {
                process.destroyForcibly();
                throw new IOException("Timeout waiting for CLI to announce port");
            }
        }

        return new ProcessInfo(process, detectedPort);
    }

    private List<String> resolveCliCommand(String cliPath, List<String> args) {
        boolean isJsFile = cliPath.toLowerCase().endsWith(".js");

        if (isJsFile) {
            List<String> result = new ArrayList<>();
            result.add("node");
            result.add(cliPath);
            result.addAll(args);
            return result;
        }

        // On Windows, use cmd /c to resolve the executable
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") && !new File(cliPath).isAbsolute()) {
            List<String> result = new ArrayList<>();
            result.add("cmd");
            result.add("/c");
            result.add(cliPath);
            result.addAll(args);
            return result;
        }

        List<String> result = new ArrayList<>();
        result.add(cliPath);
        result.addAll(args);
        return result;
    }

    private Connection connectToServer(Process process, String tcpHost, Integer tcpPort) throws IOException {
        JsonRpcClient rpc;

        if (options.isUseStdio()) {
            if (process == null) {
                throw new IllegalStateException("CLI process not started");
            }
            rpc = JsonRpcClient.fromProcess(process);
        } else {
            if (tcpHost == null || tcpPort == null) {
                throw new IllegalStateException("Cannot connect because TCP host or port are not available");
            }
            Socket socket = new Socket(tcpHost, tcpPort);
            rpc = JsonRpcClient.fromSocket(socket);
        }

        return new Connection(rpc, process);
    }

    @Override
    public void close() {
        if (disposed)
            return;
        disposed = true;
        try {
            forceStop().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error during close", e);
        }
    }

    private static class ProcessInfo {
        final Process process;
        final Integer port;

        ProcessInfo(Process process, Integer port) {
            this.process = process;
            this.port = port;
        }
    }

    private static class Connection {
        final JsonRpcClient rpc;

        final Process process;

        Connection(JsonRpcClient rpc, Process process) {
            this.rpc = rpc;
            this.process = process;
        }
    }
}
