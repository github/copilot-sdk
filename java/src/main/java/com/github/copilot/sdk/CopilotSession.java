/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionErrorEvent;
import com.github.copilot.sdk.events.SessionEventParser;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.json.GetMessagesResponse;
import com.github.copilot.sdk.json.HookInvocation;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionInvocation;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PostToolUseHookInput;
import com.github.copilot.sdk.json.PreToolUseHookInput;
import com.github.copilot.sdk.json.SendMessageRequest;
import com.github.copilot.sdk.json.SendMessageResponse;
import com.github.copilot.sdk.json.SessionEndHookInput;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.SessionStartHookInput;
import com.github.copilot.sdk.json.ToolDefinition;
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
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Create a session
 * var session = client.createSession(new SessionConfig().setModel("gpt-5")).get();
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

    private final String sessionId;
    private final String workspacePath;
    private final JsonRpcClient rpc;
    private final Set<Consumer<AbstractSessionEvent>> eventHandlers = ConcurrentHashMap.newKeySet();
    private final Map<String, ToolDefinition> toolHandlers = new ConcurrentHashMap<>();
    private final AtomicReference<PermissionHandler> permissionHandler = new AtomicReference<>();
    private final AtomicReference<UserInputHandler> userInputHandler = new AtomicReference<>();
    private final AtomicReference<SessionHooks> hooksHandler = new AtomicReference<>();
    private volatile EventErrorHandler eventErrorHandler;
    private volatile EventErrorPolicy eventErrorPolicy = EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS;

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

        return rpc.invoke("session.send", request, SendMessageResponse.class)
                .thenApply(SendMessageResponse::getMessageId);
    }

    /**
     * Sends a message and waits until the session becomes idle.
     * <p>
     * This method blocks until the assistant finishes processing the message or
     * until the timeout expires. It's suitable for simple request/response
     * interactions where you don't need to process streaming events.
     *
     * @param options
     *            the message options containing the prompt and attachments
     * @param timeoutMs
     *            timeout in milliseconds (0 or negative for no timeout)
     * @return a future that resolves with the final assistant message event, or
     *         {@code null} if no assistant message was received. The future
     *         completes exceptionally with a TimeoutException if the timeout
     *         expires.
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

        return future.whenComplete((result, ex) -> {
            try {
                subscription.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error closing subscription", e);
            }
            scheduler.shutdown();
        });
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
                toolHandlers.put(tool.getName(), tool);
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
                    if (response.getEvents() != null) {
                        for (JsonNode eventNode : response.getEvents()) {
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

}
