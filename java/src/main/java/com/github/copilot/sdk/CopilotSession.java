/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.ExternalToolRequestedEvent;
import com.github.copilot.sdk.events.PermissionRequestedEvent;
import com.github.copilot.sdk.events.SessionErrorEvent;
import com.github.copilot.sdk.events.SessionEventParser;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.json.AgentInfo;
import com.github.copilot.sdk.json.GetMessagesResponse;
import com.github.copilot.sdk.json.HookInvocation;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionInvocation;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;
import com.github.copilot.sdk.json.PostToolUseHookInput;
import com.github.copilot.sdk.json.PreToolUseHookInput;
import com.github.copilot.sdk.json.SendMessageRequest;
import com.github.copilot.sdk.json.SendMessageResponse;
import com.github.copilot.sdk.json.SessionEndHookInput;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.SessionStartHookInput;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolResultObject;
import com.github.copilot.sdk.json.UserInputHandler;
import com.github.copilot.sdk.json.UserInputInvocation;
import com.github.copilot.sdk.json.UserInputRequest;
import com.github.copilot.sdk.json.UserInputResponse;
import com.github.copilot.sdk.json.UserPromptSubmittedHookInput;

/**
 * Represents a single conversation session with the Copilot CLI.
 * <p>
 * A session maintains conversation state, handles events, and manages tool
 * execution. Sessions are created via {@link CopilotClient#createSession} or
 * resumed via {@link CopilotClient#resumeSession}.
 * <p>
 * {@code CopilotSession} implements {@link AutoCloseable}. Use the
 * try-with-resources pattern for automatic cleanup, or call {@link #close()}
 * explicitly. Closing a session releases in-memory resources but preserves
 * session data on disk — the conversation can be resumed later via
 * {@link CopilotClient#resumeSession}. To permanently delete session data, use
 * {@link CopilotClient#deleteSession}.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Create a session with a permission handler (required)
 * var session = client
 * 		.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-5"))
 * 		.get();
 *
 * // Register type-safe event handlers
 * session.on(AssistantMessageEvent.class, msg -> {
 * 	System.out.println(msg.getData().content());
 * });
 * session.on(SessionIdleEvent.class, idle -> {
 * 	System.out.println("Session is idle");
 * });
 *
 * // Send messages
 * session.sendAndWait(new MessageOptions().setPrompt("Hello!")).get();
 *
 * // Clean up
 * session.close();
 * }</pre>
 *
 * @see CopilotClient#createSession(com.github.copilot.sdk.json.SessionConfig)
 * @see CopilotClient#resumeSession(String,
 *      com.github.copilot.sdk.json.ResumeSessionConfig)
 * @see AbstractSessionEvent
 * @since 1.0.0
 */
public final class CopilotSession implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(CopilotSession.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    /**
     * The current active session ID. Initialized to the pre-generated value and may
     * be updated after session.create / session.resume if the server returns a
     * different ID (e.g. when working against a v2 CLI that ignores the
     * client-supplied sessionId).
     */
    private volatile String sessionId;
    private volatile String workspacePath;
    private final JsonRpcClient rpc;
    private final Set<Consumer<AbstractSessionEvent>> eventHandlers = ConcurrentHashMap.newKeySet();
    private final Map<String, ToolDefinition> toolHandlers = new ConcurrentHashMap<>();
    private final AtomicReference<PermissionHandler> permissionHandler = new AtomicReference<>();
    private final AtomicReference<UserInputHandler> userInputHandler = new AtomicReference<>();
    private final AtomicReference<SessionHooks> hooksHandler = new AtomicReference<>();
    private volatile EventErrorHandler eventErrorHandler;
    private volatile EventErrorPolicy eventErrorPolicy = EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS;
    private volatile Map<String, java.util.function.Function<String, CompletableFuture<String>>> transformCallbacks;

    /** Tracks whether this session instance has been terminated via close(). */
    private volatile boolean isTerminated = false;

    /**
     * Creates a new session with the given ID and RPC client.
     * <p>
     * This constructor is package-private. Sessions should be created via
     * {@link CopilotClient#createSession} or {@link CopilotClient#resumeSession}.
     *
     * @param sessionId
     *            the unique session identifier
     * @param rpc
     *            the JSON-RPC client for communication
     */
    CopilotSession(String sessionId, JsonRpcClient rpc) {
        this(sessionId, rpc, null);
    }

    /**
     * Creates a new session with the given ID, RPC client, and workspace path.
     * <p>
     * This constructor is package-private. Sessions should be created via
     * {@link CopilotClient#createSession} or {@link CopilotClient#resumeSession}.
     *
     * @param sessionId
     *            the unique session identifier
     * @param rpc
     *            the JSON-RPC client for communication
     * @param workspacePath
     *            the workspace path if infinite sessions are enabled
     */
    CopilotSession(String sessionId, JsonRpcClient rpc, String workspacePath) {
        this.sessionId = sessionId;
        this.rpc = rpc;
        this.workspacePath = workspacePath;
    }

    /**
     * Gets the unique identifier for this session.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Updates the active session ID. Package-private; called by CopilotClient if
     * the server returns a different session ID than the pre-generated one (e.g.
     * when a v2 CLI ignores the client-supplied sessionId).
     *
     * @param sessionId
     *            the server-confirmed session ID
     */
    void setActiveSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the path to the session workspace directory when infinite sessions are
     * enabled.
     * <p>
     * The workspace directory contains checkpoints/, plan.md, and files/
     * subdirectories.
     *
     * @return the workspace path, or {@code null} if infinite sessions are disabled
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    /**
     * Sets the workspace path. Package-private; called by CopilotClient after
     * session.create or session.resume RPC response.
     *
     * @param workspacePath
     *            the workspace path
     */
    void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    /**
     * Sets a custom error handler for exceptions thrown by event handlers.
     * <p>
     * When an event handler registered via {@link #on(Consumer)} or
     * {@link #on(Class, Consumer)} throws an exception during event dispatch, the
     * error handler is invoked with the event and exception. The error is always
     * logged at {@link Level#WARNING} regardless of whether a custom handler is
     * set.
     *
     * <p>
     * Whether dispatch continues or stops after an error is controlled by the
     * {@link EventErrorPolicy} set via {@link #setEventErrorPolicy}. The error
     * handler is always invoked regardless of the policy.
     *
     * <p>
     * If the error handler itself throws an exception, that exception is caught and
     * logged at {@link Level#SEVERE}, and dispatch is stopped regardless of the
     * configured policy.
     *
     * <p>
     * <b>Example:</b>
     *
     * <pre>{@code
     * session.setEventErrorHandler((event, exception) -> {
     * 	metrics.increment("handler.errors");
     * 	logger.error("Handler failed on {}: {}", event.getType(), exception.getMessage());
     * });
     * }</pre>
     *
     * @param handler
     *            the error handler, or {@code null} to use only the default logging
     *            behavior
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see EventErrorHandler
     * @see #setEventErrorPolicy(EventErrorPolicy)
     * @since 1.0.8
     */
    public void setEventErrorHandler(EventErrorHandler handler) {
        ensureNotTerminated();
        this.eventErrorHandler = handler;
    }

    /**
     * Sets the error propagation policy for event dispatch.
     * <p>
     * Controls whether remaining event listeners continue to execute when a
     * preceding listener throws an exception. Errors are always logged at
     * {@link Level#WARNING} regardless of the policy.
     *
     * <ul>
     * <li>{@link EventErrorPolicy#PROPAGATE_AND_LOG_ERRORS} (default) — log the
     * error and stop dispatch after the first error</li>
     * <li>{@link EventErrorPolicy#SUPPRESS_AND_LOG_ERRORS} — log the error and
     * continue dispatching to all remaining listeners</li>
     * </ul>
     *
     * <p>
     * The configured {@link EventErrorHandler} (if any) is always invoked
     * regardless of the policy.
     *
     * <p>
     * <b>Example:</b>
     *
     * <pre>{@code
     * // Opt-in to suppress errors (continue dispatching despite errors)
     * session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
     * session.setEventErrorHandler((event, ex) -> logger.error("Handler failed, continuing: {}", ex.getMessage(), ex));
     * }</pre>
     *
     * @param policy
     *            the error policy (default is
     *            {@link EventErrorPolicy#PROPAGATE_AND_LOG_ERRORS})
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see EventErrorPolicy
     * @see #setEventErrorHandler(EventErrorHandler)
     * @since 1.0.8
     */
    public void setEventErrorPolicy(EventErrorPolicy policy) {
        ensureNotTerminated();
        if (policy == null) {
            throw new NullPointerException("policy must not be null");
        }
        this.eventErrorPolicy = policy;
    }

    /**
     * Sends a simple text message to the Copilot session.
     * <p>
     * This is a convenience method equivalent to
     * {@code send(new MessageOptions().setPrompt(prompt))}.
     *
     * @param prompt
     *            the message text to send
     * @return a future that resolves with the message ID assigned by the server
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #send(MessageOptions)
     */
    public CompletableFuture<String> send(String prompt) {
        ensureNotTerminated();
        return send(new MessageOptions().setPrompt(prompt));
    }

    /**
     * Sends a simple text message and waits until the session becomes idle.
     * <p>
     * This is a convenience method equivalent to
     * {@code sendAndWait(new MessageOptions().setPrompt(prompt))}.
     *
     * @param prompt
     *            the message text to send
     * @return a future that resolves with the final assistant message event, or
     *         {@code null} if no assistant message was received
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #sendAndWait(MessageOptions)
     */
    public CompletableFuture<AssistantMessageEvent> sendAndWait(String prompt) {
        ensureNotTerminated();
        return sendAndWait(new MessageOptions().setPrompt(prompt));
    }

    /**
     * Sends a message to the Copilot session.
     * <p>
     * This method sends a message asynchronously and returns immediately. Use
     * {@link #sendAndWait(MessageOptions)} to wait for the response.
     *
     * @param options
     *            the message options containing the prompt and attachments
     * @return a future that resolves with the message ID assigned by the server
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #sendAndWait(MessageOptions)
     * @see #send(String)
     */
    public CompletableFuture<String> send(MessageOptions options) {
        ensureNotTerminated();
        var request = new SendMessageRequest();
        request.setSessionId(sessionId);
        request.setPrompt(options.getPrompt());
        request.setAttachments(options.getAttachments());
        request.setMode(options.getMode());

        return rpc.invoke("session.send", request, SendMessageResponse.class).thenApply(SendMessageResponse::messageId);
    }

    /**
     * Sends a message and waits until the session becomes idle.
     * <p>
     * This method blocks until the assistant finishes processing the message or
     * until the timeout expires. It's suitable for simple request/response
     * interactions where you don't need to process streaming events.
     * <p>
     * The returned future can be cancelled via
     * {@link java.util.concurrent.Future#cancel(boolean)}. If cancelled externally,
     * the future completes with {@link java.util.concurrent.CancellationException}.
     * If the timeout expires first, the future completes exceptionally with a
     * {@link TimeoutException}.
     *
     * @param options
     *            the message options containing the prompt and attachments
     * @param timeoutMs
     *            timeout in milliseconds (0 or negative for no timeout)
     * @return a future that resolves with the final assistant message event, or
     *         {@code null} if no assistant message was received. The future
     *         completes exceptionally with a TimeoutException if the timeout
     *         expires, or with CancellationException if cancelled externally.
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #sendAndWait(MessageOptions)
     * @see #send(MessageOptions)
     */
    public CompletableFuture<AssistantMessageEvent> sendAndWait(MessageOptions options, long timeoutMs) {
        ensureNotTerminated();
        var future = new CompletableFuture<AssistantMessageEvent>();
        var lastAssistantMessage = new AtomicReference<AssistantMessageEvent>();

        Consumer<AbstractSessionEvent> handler = evt -> {
            if (evt instanceof AssistantMessageEvent msg) {
                lastAssistantMessage.set(msg);
            } else if (evt instanceof SessionIdleEvent) {
                future.complete(lastAssistantMessage.get());
            } else if (evt instanceof SessionErrorEvent errorEvent) {
                String message = errorEvent.getData() != null ? errorEvent.getData().message() : "session error";
                future.completeExceptionally(new RuntimeException("Session error: " + message));
            }
        };

        Closeable subscription = on(handler);

        send(options).exceptionally(ex -> {
            try {
                subscription.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error closing subscription", e);
            }
            future.completeExceptionally(ex);
            return null;
        });

        // Set up timeout with daemon thread so it doesn't prevent JVM exit
        var scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "sendAndWait-timeout");
            t.setDaemon(true);
            return t;
        });
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new TimeoutException("sendAndWait timed out after " + timeoutMs + "ms"));
            }
            scheduler.shutdown();
        }, timeoutMs, TimeUnit.MILLISECONDS);

        var result = new CompletableFuture<AssistantMessageEvent>();

        // When inner future completes, run cleanup and propagate to result
        future.whenComplete((r, ex) -> {
            try {
                subscription.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error closing subscription", e);
            }
            scheduler.shutdown();
            if (!result.isDone()) {
                if (ex != null) {
                    result.completeExceptionally(ex);
                } else {
                    result.complete(r);
                }
            }
        });

        // When result is cancelled externally, cancel inner future to trigger cleanup
        result.whenComplete((v, ex) -> {
            if (result.isCancelled() && !future.isDone()) {
                future.cancel(true);
            }
        });

        return result;
    }

    /**
     * Sends a message and waits until the session becomes idle with default 60
     * second timeout.
     *
     * @param options
     *            the message options containing the prompt and attachments
     * @return a future that resolves with the final assistant message event, or
     *         {@code null} if no assistant message was received
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #sendAndWait(MessageOptions, long)
     */
    public CompletableFuture<AssistantMessageEvent> sendAndWait(MessageOptions options) {
        ensureNotTerminated();
        return sendAndWait(options, 60000);
    }

    /**
     * Registers a callback for all session events.
     * <p>
     * The handler will be invoked for every event in this session, including
     * assistant messages, tool calls, and session state changes. For type-safe
     * handling of specific event types, prefer {@link #on(Class, Consumer)}
     * instead.
     *
     * <p>
     * <b>Exception handling:</b> If a handler throws an exception, the error is
     * routed to the configured {@link EventErrorHandler} (if set). Whether
     * remaining handlers execute depends on the configured
     * {@link EventErrorPolicy}.
     *
     * <p>
     * <b>Example:</b>
     *
     * <pre>{@code
     * // Collect all events
     * var events = new ArrayList<AbstractSessionEvent>();
     * session.on(events::add);
     * }</pre>
     *
     * @param handler
     *            a callback to be invoked when a session event occurs
     * @return a Closeable that, when closed, unsubscribes the handler
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #on(Class, Consumer)
     * @see AbstractSessionEvent
     * @see #setEventErrorPolicy(EventErrorPolicy)
     */
    public Closeable on(Consumer<AbstractSessionEvent> handler) {
        ensureNotTerminated();
        eventHandlers.add(handler);
        return () -> eventHandlers.remove(handler);
    }

    /**
     * Registers an event handler for a specific event type.
     * <p>
     * This provides a type-safe way to handle specific events without needing
     * {@code instanceof} checks. The handler will only be called for events
     * matching the specified type.
     *
     * <p>
     * <b>Exception handling:</b> If a handler throws an exception, the error is
     * routed to the configured {@link EventErrorHandler} (if set). Whether
     * remaining handlers execute depends on the configured
     * {@link EventErrorPolicy}.
     *
     * <p>
     * <b>Example Usage</b>
     * </p>
     *
     * <pre>{@code
     * // Handle assistant messages
     * session.on(AssistantMessageEvent.class, msg -> {
     * 	System.out.println(msg.getData().content());
     * });
     *
     * // Handle session idle
     * session.on(SessionIdleEvent.class, idle -> {
     * 	done.complete(null);
     * });
     *
     * // Handle streaming deltas
     * session.on(AssistantMessageDeltaEvent.class, delta -> {
     * 	System.out.print(delta.getData().deltaContent());
     * });
     * }</pre>
     *
     * @param <T>
     *            the event type
     * @param eventType
     *            the class of the event to listen for
     * @param handler
     *            a callback invoked when events of this type occur
     * @return a Closeable that unsubscribes the handler when closed
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see #on(Consumer)
     * @see AbstractSessionEvent
     */
    public <T extends AbstractSessionEvent> Closeable on(Class<T> eventType, Consumer<T> handler) {
        ensureNotTerminated();
        Consumer<AbstractSessionEvent> wrapper = event -> {
            if (eventType.isInstance(event)) {
                handler.accept(eventType.cast(event));
            }
        };
        eventHandlers.add(wrapper);
        return () -> eventHandlers.remove(wrapper);
    }

    /**
     * Dispatches an event to all registered handlers.
     * <p>
     * This is called internally when events are received from the server. Each
     * handler is invoked in its own try/catch block. Errors are always logged at
     * {@link Level#WARNING}. Whether dispatch continues after a handler error
     * depends on the configured {@link EventErrorPolicy}:
     * <ul>
     * <li>{@link EventErrorPolicy#PROPAGATE_AND_LOG_ERRORS} (default) — dispatch
     * stops after the first error</li>
     * <li>{@link EventErrorPolicy#SUPPRESS_AND_LOG_ERRORS} — remaining handlers
     * still execute</li>
     * </ul>
     * <p>
     * The configured {@link EventErrorHandler} is always invoked (if set),
     * regardless of the policy. If the error handler itself throws, dispatch stops
     * regardless of policy and the error is logged at {@link Level#SEVERE}.
     *
     * @param event
     *            the event to dispatch
     * @see #setEventErrorHandler(EventErrorHandler)
     * @see #setEventErrorPolicy(EventErrorPolicy)
     */
    void dispatchEvent(AbstractSessionEvent event) {
        // Handle broadcast request events (protocol v3) before dispatching to user
        // handlers. These are fire-and-forget: the response is sent asynchronously.
        handleBroadcastEventAsync(event);

        for (Consumer<AbstractSessionEvent> handler : eventHandlers) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error in event handler", e);
                EventErrorHandler errorHandler = this.eventErrorHandler;
                if (errorHandler != null) {
                    try {
                        errorHandler.handleError(event, e);
                    } catch (Exception errorHandlerException) {
                        LOG.log(Level.SEVERE, "Error in event error handler", errorHandlerException);
                        break; // error handler itself failed — stop regardless of policy
                    }
                }
                if (eventErrorPolicy == EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS) {
                    break;
                }
            }
        }
    }

    /**
     * Handles broadcast request events by executing local handlers and responding
     * via RPC (protocol v3).
     * <p>
     * Fire-and-forget: the response is sent asynchronously.
     *
     * @param event
     *            the event to handle
     */
    private void handleBroadcastEventAsync(AbstractSessionEvent event) {
        if (event instanceof ExternalToolRequestedEvent toolEvent) {
            var data = toolEvent.getData();
            if (data == null || data.requestId() == null || data.toolName() == null) {
                return;
            }
            ToolDefinition tool = getTool(data.toolName());
            if (tool == null) {
                return; // This client doesn't handle this tool; another client will
            }
            executeToolAndRespondAsync(data.requestId(), data.toolName(), data.toolCallId(), data.arguments(), tool);

        } else if (event instanceof PermissionRequestedEvent permEvent) {
            var data = permEvent.getData();
            if (data == null || data.requestId() == null || data.permissionRequest() == null) {
                return;
            }
            PermissionHandler handler = permissionHandler.get();
            if (handler == null) {
                return; // This client doesn't handle permissions; another client will
            }
            executePermissionAndRespondAsync(data.requestId(), data.permissionRequest(), handler);
        }
    }

    /**
     * Executes a tool handler and sends the result back via
     * {@code session.tools.handlePendingToolCall}.
     */
    private void executeToolAndRespondAsync(String requestId, String toolName, String toolCallId, Object arguments,
            ToolDefinition tool) {
        CompletableFuture.runAsync(() -> {
            try {
                JsonNode argumentsNode = arguments instanceof JsonNode jn
                        ? jn
                        : (arguments != null ? MAPPER.valueToTree(arguments) : null);
                var invocation = new com.github.copilot.sdk.json.ToolInvocation().setSessionId(sessionId)
                        .setToolCallId(toolCallId).setToolName(toolName).setArguments(argumentsNode);

                tool.handler().invoke(invocation).thenAccept(result -> {
                    try {
                        ToolResultObject toolResult;
                        if (result instanceof ToolResultObject tr) {
                            toolResult = tr;
                        } else {
                            toolResult = ToolResultObject
                                    .success(result instanceof String s ? s : MAPPER.writeValueAsString(result));
                        }
                        rpc.invoke("session.tools.handlePendingToolCall",
                                Map.of("sessionId", sessionId, "requestId", requestId, "result", toolResult),
                                Object.class);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Error sending tool result for requestId=" + requestId, e);
                    }
                }).exceptionally(ex -> {
                    try {
                        rpc.invoke(
                                "session.tools.handlePendingToolCall", Map.of("sessionId", sessionId, "requestId",
                                        requestId, "error", ex.getMessage() != null ? ex.getMessage() : ex.toString()),
                                Object.class);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Error sending tool error for requestId=" + requestId, e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error executing tool for requestId=" + requestId, e);
                try {
                    rpc.invoke(
                            "session.tools.handlePendingToolCall", Map.of("sessionId", sessionId, "requestId",
                                    requestId, "error", e.getMessage() != null ? e.getMessage() : e.toString()),
                            Object.class);
                } catch (Exception sendEx) {
                    LOG.log(Level.WARNING, "Error sending tool error for requestId=" + requestId, sendEx);
                }
            }
        });
    }

    /**
     * Executes a permission handler and sends the result back via
     * {@code session.permissions.handlePendingPermissionRequest}.
     */
    private void executePermissionAndRespondAsync(String requestId, PermissionRequest permissionRequest,
            PermissionHandler handler) {
        CompletableFuture.runAsync(() -> {
            try {
                var invocation = new PermissionInvocation();
                invocation.setSessionId(sessionId);
                handler.handle(permissionRequest, invocation).thenAccept(result -> {
                    try {
                        if (PermissionRequestResultKind.NO_RESULT.equals(result.getKind())) {
                            // Handler explicitly abstains — leave the request unanswered
                            // so another client can handle it.
                            return;
                        }
                        rpc.invoke("session.permissions.handlePendingPermissionRequest",
                                Map.of("sessionId", sessionId, "requestId", requestId, "result", result), Object.class);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Error sending permission result for requestId=" + requestId, e);
                    }
                }).exceptionally(ex -> {
                    try {
                        PermissionRequestResult denied = new PermissionRequestResult();
                        denied.setKind(PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER);
                        rpc.invoke("session.permissions.handlePendingPermissionRequest",
                                Map.of("sessionId", sessionId, "requestId", requestId, "result", denied), Object.class);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Error sending permission denied for requestId=" + requestId, e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error executing permission handler for requestId=" + requestId, e);
                try {
                    PermissionRequestResult denied = new PermissionRequestResult();
                    denied.setKind(PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER);
                    rpc.invoke("session.permissions.handlePendingPermissionRequest",
                            Map.of("sessionId", sessionId, "requestId", requestId, "result", denied), Object.class);
                } catch (Exception sendEx) {
                    LOG.log(Level.WARNING, "Error sending permission denied for requestId=" + requestId, sendEx);
                }
            }
        });
    }

    /**
     * Registers custom tool handlers for this session.
     * <p>
     * Called internally when creating or resuming a session with tools.
     *
     * @param tools
     *            the list of tool definitions with handlers
     */
    void registerTools(List<ToolDefinition> tools) {
        toolHandlers.clear();
        if (tools != null) {
            for (ToolDefinition tool : tools) {
                toolHandlers.put(tool.name(), tool);
            }
        }
    }

    /**
     * Retrieves a registered tool by name.
     *
     * @param name
     *            the tool name
     * @return the tool definition, or {@code null} if not found
     */
    ToolDefinition getTool(String name) {
        return toolHandlers.get(name);
    }

    /**
     * Registers a handler for permission requests.
     * <p>
     * Called internally when creating or resuming a session with permission
     * handling.
     *
     * @param handler
     *            the permission handler
     */
    void registerPermissionHandler(PermissionHandler handler) {
        permissionHandler.set(handler);
    }

    /**
     * Handles a permission request from the Copilot CLI.
     * <p>
     * Called internally when the server requests permission for an operation.
     *
     * @param permissionRequestData
     *            the JSON data for the permission request
     * @return a future that resolves with the permission result
     */
    CompletableFuture<PermissionRequestResult> handlePermissionRequest(JsonNode permissionRequestData) {
        PermissionHandler handler = permissionHandler.get();
        if (handler == null) {
            PermissionRequestResult result = new PermissionRequestResult();
            result.setKind("denied-no-approval-rule-and-could-not-request-from-user");
            return CompletableFuture.completedFuture(result);
        }

        try {
            PermissionRequest request = MAPPER.treeToValue(permissionRequestData, PermissionRequest.class);
            var invocation = new PermissionInvocation();
            invocation.setSessionId(sessionId);
            return handler.handle(request, invocation).exceptionally(ex -> {
                LOG.log(Level.SEVERE, "Permission handler threw an exception", ex);
                PermissionRequestResult result = new PermissionRequestResult();
                result.setKind("denied-no-approval-rule-and-could-not-request-from-user");
                return result;
            });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process permission request", e);
            PermissionRequestResult result = new PermissionRequestResult();
            result.setKind("denied-no-approval-rule-and-could-not-request-from-user");
            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * Registers a handler for user input requests.
     * <p>
     * Called internally when creating or resuming a session with user input
     * handling.
     *
     * @param handler
     *            the user input handler
     */
    void registerUserInputHandler(UserInputHandler handler) {
        userInputHandler.set(handler);
    }

    /**
     * Handles a user input request from the Copilot CLI.
     * <p>
     * Called internally when the server requests user input.
     *
     * @param request
     *            the user input request
     * @return a future that resolves with the user input response
     */
    CompletableFuture<UserInputResponse> handleUserInputRequest(UserInputRequest request) {
        UserInputHandler handler = userInputHandler.get();
        if (handler == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No user input handler registered"));
        }

        try {
            var invocation = new UserInputInvocation().setSessionId(sessionId);
            return handler.handle(request, invocation).exceptionally(ex -> {
                LOG.log(Level.SEVERE, "User input handler threw an exception", ex);
                throw new RuntimeException("User input handler error", ex);
            });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process user input request", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Registers hook handlers for this session.
     * <p>
     * Called internally when creating or resuming a session with hooks.
     *
     * @param hooks
     *            the hooks configuration
     */
    void registerHooks(SessionHooks hooks) {
        hooksHandler.set(hooks);
    }

    /**
     * Registers transform callbacks for system message sections.
     * <p>
     * Called internally when creating or resuming a session with
     * {@link com.github.copilot.sdk.SystemMessageMode#CUSTOMIZE} and transform
     * callbacks.
     *
     * @param callbacks
     *            the transform callbacks keyed by section identifier; {@code null}
     *            clears any previously registered callbacks
     */
    void registerTransformCallbacks(
            Map<String, java.util.function.Function<String, CompletableFuture<String>>> callbacks) {
        this.transformCallbacks = callbacks;
    }

    /**
     * Handles a {@code systemMessage.transform} RPC call from the Copilot CLI.
     * <p>
     * The CLI sends section content; the SDK invokes the registered transform
     * callbacks and returns the transformed sections.
     *
     * @param sections
     *            JSON node containing sections keyed by section identifier
     * @return a future resolving with a map of transformed sections
     */
    CompletableFuture<Map<String, Object>> handleSystemMessageTransform(JsonNode sections) {
        var callbacks = this.transformCallbacks;
        var result = new java.util.LinkedHashMap<String, Object>();
        var futures = new ArrayList<CompletableFuture<Void>>();

        if (sections != null && sections.isObject()) {
            sections.fields().forEachRemaining(entry -> {
                String sectionId = entry.getKey();
                String content = entry.getValue().has("content") ? entry.getValue().get("content").asText("") : "";

                java.util.function.Function<String, CompletableFuture<String>> cb = callbacks != null
                        ? callbacks.get(sectionId)
                        : null;

                if (cb != null) {
                    CompletableFuture<Void> f = cb.apply(content).exceptionally(ex -> content)
                            .thenAccept(transformed -> {
                                synchronized (result) {
                                    result.put(sectionId, Map.of("content", transformed != null ? transformed : ""));
                                }
                            });
                    futures.add(f);
                } else {
                    result.put(sectionId, Map.of("content", content));
                }
            });
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> {
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("sections", result);
            return response;
        });
    }

    /**
     * Handles a hook invocation from the Copilot CLI.
     * <p>
     * Called internally when the server invokes a hook.
     *
     * @param hookType
     *            the type of hook to invoke
     * @param input
     *            the hook input data
     * @return a future that resolves with the hook output
     */
    CompletableFuture<Object> handleHooksInvoke(String hookType, JsonNode input) {
        SessionHooks hooks = hooksHandler.get();
        if (hooks == null) {
            return CompletableFuture.completedFuture(null);
        }

        var invocation = new HookInvocation().setSessionId(sessionId);

        try {
            switch (hookType) {
                case "preToolUse" :
                    if (hooks.getOnPreToolUse() != null) {
                        PreToolUseHookInput preInput = MAPPER.treeToValue(input, PreToolUseHookInput.class);
                        return hooks.getOnPreToolUse().handle(preInput, invocation)
                                .thenApply(output -> (Object) output);
                    }
                    break;
                case "postToolUse" :
                    if (hooks.getOnPostToolUse() != null) {
                        PostToolUseHookInput postInput = MAPPER.treeToValue(input, PostToolUseHookInput.class);
                        return hooks.getOnPostToolUse().handle(postInput, invocation)
                                .thenApply(output -> (Object) output);
                    }
                    break;
                case "userPromptSubmitted" :
                    if (hooks.getOnUserPromptSubmitted() != null) {
                        UserPromptSubmittedHookInput promptInput = MAPPER.treeToValue(input,
                                UserPromptSubmittedHookInput.class);
                        return hooks.getOnUserPromptSubmitted().handle(promptInput, invocation)
                                .thenApply(output -> (Object) output);
                    }
                    break;
                case "sessionStart" :
                    if (hooks.getOnSessionStart() != null) {
                        SessionStartHookInput startInput = MAPPER.treeToValue(input, SessionStartHookInput.class);
                        return hooks.getOnSessionStart().handle(startInput, invocation)
                                .thenApply(output -> (Object) output);
                    }
                    break;
                case "sessionEnd" :
                    if (hooks.getOnSessionEnd() != null) {
                        SessionEndHookInput endInput = MAPPER.treeToValue(input, SessionEndHookInput.class);
                        return hooks.getOnSessionEnd().handle(endInput, invocation)
                                .thenApply(output -> (Object) output);
                    }
                    break;
                default :
                    LOG.fine("Unhandled hook type: " + hookType);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to process hook invocation", e);
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Gets the complete list of messages and events in the session.
     * <p>
     * This retrieves the full conversation history, including all user messages,
     * assistant responses, tool invocations, and other session events.
     *
     * @return a future that resolves with a list of all session events
     * @throws IllegalStateException
     *             if this session has been terminated
     * @see AbstractSessionEvent
     */
    public CompletableFuture<List<AbstractSessionEvent>> getMessages() {
        ensureNotTerminated();
        return rpc.invoke("session.getMessages", Map.of("sessionId", sessionId), GetMessagesResponse.class)
                .thenApply(response -> {
                    var events = new ArrayList<AbstractSessionEvent>();
                    if (response.events() != null) {
                        for (JsonNode eventNode : response.events()) {
                            try {
                                AbstractSessionEvent event = SessionEventParser.parse(eventNode);
                                if (event != null) {
                                    events.add(event);
                                }
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Failed to parse event", e);
                            }
                        }
                    }
                    return events;
                });
    }

    /**
     * Aborts the currently processing message in this session.
     * <p>
     * Use this to cancel a long-running operation or stop the assistant from
     * continuing to generate a response.
     *
     * @return a future that completes when the abort is acknowledged
     * @throws IllegalStateException
     *             if this session has been terminated
     */
    public CompletableFuture<Void> abort() {
        ensureNotTerminated();
        return rpc.invoke("session.abort", Map.of("sessionId", sessionId), Void.class);
    }

    /**
     * Changes the model for this session with an optional reasoning effort level.
     * <p>
     * The new model takes effect for the next message. Conversation history is
     * preserved.
     *
     * <pre>{@code
     * session.setModel("gpt-4.1").get();
     * session.setModel("claude-sonnet-4.6", "high").get();
     * }</pre>
     *
     * @param model
     *            the model ID to switch to (e.g., {@code "gpt-4.1"})
     * @param reasoningEffort
     *            reasoning effort level (e.g., {@code "low"}, {@code "medium"},
     *            {@code "high"}, {@code "xhigh"}); {@code null} to use default
     * @return a future that completes when the model switch is acknowledged
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.2.0
     */
    public CompletableFuture<Void> setModel(String model, String reasoningEffort) {
        ensureNotTerminated();
        var params = new java.util.HashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("modelId", model);
        if (reasoningEffort != null) {
            params.put("reasoningEffort", reasoningEffort);
        }
        return rpc.invoke("session.model.switchTo", params, Void.class);
    }

    /**
     * Changes the model for this session.
     * <p>
     * The new model takes effect for the next message. Conversation history is
     * preserved.
     *
     * <pre>{@code
     * session.setModel("gpt-4.1").get();
     * }</pre>
     *
     * @param model
     *            the model ID to switch to (e.g., {@code "gpt-4.1"})
     * @return a future that completes when the model switch is acknowledged
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.0.11
     */
    public CompletableFuture<Void> setModel(String model) {
        return setModel(model, null);
    }

    /**
     * Logs a message to the session timeline.
     * <p>
     * The message appears in the session event stream and is visible to SDK
     * consumers. Non-ephemeral messages are also persisted to the session event log
     * on disk.
     *
     * <h2>Example Usage</h2>
     *
     * <pre>{@code
     * session.log("Build completed successfully").get();
     * session.log("Disk space low", "warning", null).get();
     * session.log("Temporary status", null, true).get();
     * session.log("Details at link", "info", null, "https://example.com").get();
     * }</pre>
     *
     * @param message
     *            the message to log
     * @param level
     *            the log severity level ({@code "info"}, {@code "warning"},
     *            {@code "error"}), or {@code null} to use the default
     *            ({@code "info"})
     * @param ephemeral
     *            when {@code true}, the message is transient and not persisted to
     *            disk; {@code null} uses default behavior
     * @param url
     *            optional URL to associate with the log entry; {@code null} to omit
     * @return a future that completes when the message is logged
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.2.0
     */
    public CompletableFuture<Void> log(String message, String level, Boolean ephemeral, String url) {
        ensureNotTerminated();
        var params = new java.util.HashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("message", message);
        if (level != null) {
            params.put("level", level);
        }
        if (ephemeral != null) {
            params.put("ephemeral", ephemeral);
        }
        if (url != null) {
            params.put("url", url);
        }
        return rpc.invoke("session.log", params, Void.class);
    }

    /**
     * Logs a message to the session timeline.
     * <p>
     * The message appears in the session event stream and is visible to SDK
     * consumers. Non-ephemeral messages are also persisted to the session event log
     * on disk.
     *
     * <h2>Example Usage</h2>
     *
     * <pre>{@code
     * session.log("Build completed successfully").get();
     * session.log("Disk space low", "warning", null).get();
     * session.log("Temporary status", null, true).get();
     * }</pre>
     *
     * @param message
     *            the message to log
     * @param level
     *            the log severity level ({@code "info"}, {@code "warning"},
     *            {@code "error"}), or {@code null} to use the default
     *            ({@code "info"})
     * @param ephemeral
     *            when {@code true}, the message is transient and not persisted to
     *            disk; {@code null} uses default behavior
     * @return a future that completes when the message is logged
     * @throws IllegalStateException
     *             if this session has been terminated
     */
    public CompletableFuture<Void> log(String message, String level, Boolean ephemeral) {
        return log(message, level, ephemeral, null);
    }

    /**
     * Logs an informational message to the session timeline.
     *
     * @param message
     *            the message to log
     * @return a future that completes when the message is logged
     * @throws IllegalStateException
     *             if this session has been terminated
     */
    public CompletableFuture<Void> log(String message) {
        return log(message, null, null);
    }

    /**
     * Lists the custom agents available for selection in this session.
     *
     * @return a future that resolves with the list of available agents
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.0.11
     */
    public CompletableFuture<List<AgentInfo>> listAgents() {
        ensureNotTerminated();
        return rpc.invoke("session.agent.list", Map.of("sessionId", sessionId), AgentListResponse.class)
                .thenApply(response -> response.agents() != null
                        ? Collections.unmodifiableList(response.agents())
                        : Collections.emptyList());
    }

    /**
     * Gets the currently selected custom agent for this session, or {@code null} if
     * no custom agent is selected.
     *
     * @return a future that resolves with the current agent, or {@code null} if
     *         using the default agent
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.0.11
     */
    public CompletableFuture<AgentInfo> getCurrentAgent() {
        ensureNotTerminated();
        return rpc.invoke("session.agent.getCurrent", Map.of("sessionId", sessionId), AgentGetCurrentResponse.class)
                .thenApply(AgentGetCurrentResponse::agent);
    }

    /**
     * Selects a custom agent for this session.
     *
     * @param agentName
     *            the name/identifier of the agent to select
     * @return a future that resolves with the selected agent information
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.0.11
     */
    public CompletableFuture<AgentInfo> selectAgent(String agentName) {
        ensureNotTerminated();
        return rpc.invoke("session.agent.select", Map.of("sessionId", sessionId, "name", agentName),
                AgentSelectResponse.class).thenApply(AgentSelectResponse::agent);
    }

    /**
     * Deselects the currently selected custom agent, returning to the default
     * agent.
     *
     * @return a future that completes when the agent is deselected
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.0.11
     */
    public CompletableFuture<Void> deselectAgent() {
        ensureNotTerminated();
        return rpc.invoke("session.agent.deselect", Map.of("sessionId", sessionId), Void.class);
    }

    /**
     * Compacts the session context to reduce token usage.
     * <p>
     * This triggers an immediate session compaction, summarizing the conversation
     * history to free up context window space.
     *
     * @return a future that completes when compaction finishes
     * @throws IllegalStateException
     *             if this session has been terminated
     * @since 1.0.11
     */
    public CompletableFuture<Void> compact() {
        ensureNotTerminated();
        return rpc.invoke("session.compaction.compact", Map.of("sessionId", sessionId), Void.class);
    }

    /**
     * Verifies that this session has not yet been terminated.
     *
     * @throws IllegalStateException
     *             if close() has already been invoked
     */
    private void ensureNotTerminated() {
        if (isTerminated) {
            throw new IllegalStateException("Session is closed");
        }
    }

    /**
     * Disposes the session and releases all associated resources.
     * <p>
     * This destroys the session on the server, clears all event handlers, and
     * releases tool and permission handlers. After calling this method, the session
     * cannot be used again. Subsequent calls to this method have no effect.
     */
    @Override
    public void close() {
        synchronized (this) {
            if (isTerminated) {
                return; // Already terminated - no-op
            }
            isTerminated = true;
        }

        try {
            rpc.invoke("session.destroy", Map.of("sessionId", sessionId), Void.class).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error destroying session", e);
        }

        eventHandlers.clear();
        toolHandlers.clear();
        permissionHandler.set(null);
        userInputHandler.set(null);
        hooksHandler.set(null);
    }

    // ===== Internal response types for agent API =====

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentListResponse(@JsonProperty("agents") List<AgentInfo> agents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentGetCurrentResponse(@JsonProperty("agent") AgentInfo agent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgentSelectResponse(@JsonProperty("agent") AgentInfo agent) {
    }

}
