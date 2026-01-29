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
import java.util.concurrent.ScheduledExecutorService;
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
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionInvocation;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.SendMessageRequest;
import com.github.copilot.sdk.json.SendMessageResponse;
import com.github.copilot.sdk.json.ToolDefinition;

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
 * CopilotSession session = client.createSession(new SessionConfig().setModel("gpt-5")).get();
 *
 * // Register event handlers
 * session.on(evt -> {
 * 	if (evt instanceof AssistantMessageEvent msg) {
 * 		System.out.println(msg.getData().getContent());
 * 	}
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
     * Sends a simple text message to the Copilot session.
     * <p>
     * This is a convenience method equivalent to
     * {@code send(new MessageOptions().setPrompt(prompt))}.
     *
     * @param prompt
     *            the message text to send
     * @return a future that resolves with the message ID assigned by the server
     * @see #send(MessageOptions)
     */
    public CompletableFuture<String> send(String prompt) {
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
     * @see #sendAndWait(MessageOptions)
     */
    public CompletableFuture<AssistantMessageEvent> sendAndWait(String prompt) {
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
     * @see #sendAndWait(MessageOptions)
     * @see #send(String)
     */
    public CompletableFuture<String> send(MessageOptions options) {
        SendMessageRequest request = new SendMessageRequest();
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
     * @see #sendAndWait(MessageOptions)
     * @see #send(MessageOptions)
     */
    public CompletableFuture<AssistantMessageEvent> sendAndWait(MessageOptions options, long timeoutMs) {
        CompletableFuture<AssistantMessageEvent> future = new CompletableFuture<>();
        AtomicReference<AssistantMessageEvent> lastAssistantMessage = new AtomicReference<>();

        Consumer<AbstractSessionEvent> handler = evt -> {
            if (evt instanceof AssistantMessageEvent msg) {
                lastAssistantMessage.set(msg);
            } else if (evt instanceof SessionIdleEvent) {
                future.complete(lastAssistantMessage.get());
            } else if (evt instanceof SessionErrorEvent errorEvent) {
                String message = errorEvent.getData() != null ? errorEvent.getData().getMessage() : "session error";
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
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sendAndWait-timeout");
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
     * @see #sendAndWait(MessageOptions, long)
     */
    public CompletableFuture<AssistantMessageEvent> sendAndWait(MessageOptions options) {
        return sendAndWait(options, 60000);
    }

    /**
     * Registers a callback for session events.
     * <p>
     * The handler will be invoked for all events in this session, including
     * assistant messages, tool calls, and session state changes.
     *
     * <p>
     * <b>Example:</b>
     *
     * <pre>{@code
     * Closeable subscription = session.on(evt -> {
     * 	if (evt instanceof AssistantMessageEvent msg) {
     * 		System.out.println(msg.getData().getContent());
     * 	}
     * });
     *
     * // Later, to unsubscribe:
     * subscription.close();
     * }</pre>
     *
     * @param handler
     *            a callback to be invoked when a session event occurs
     * @return a Closeable that, when closed, unsubscribes the handler
     * @see AbstractSessionEvent
     */
    public Closeable on(Consumer<AbstractSessionEvent> handler) {
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
     * <b>Example Usage</b>
     * </p>
     *
     * <pre>{@code
     * // Handle assistant messages
     * session.on(AssistantMessageEvent.class, msg -> {
     * 	System.out.println(msg.getData().getContent());
     * });
     *
     * // Handle session idle
     * session.on(SessionIdleEvent.class, idle -> {
     * 	done.complete(null);
     * });
     *
     * // Handle streaming deltas
     * session.on(AssistantMessageDeltaEvent.class, delta -> {
     * 	System.out.print(delta.getData().getDeltaContent());
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
     * @see #on(Consumer)
     * @see AbstractSessionEvent
     */
    public <T extends AbstractSessionEvent> Closeable on(Class<T> eventType, Consumer<T> handler) {
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
     * This is called internally when events are received from the server.
     *
     * @param event
     *            the event to dispatch
     */
    void dispatchEvent(AbstractSessionEvent event) {
        for (Consumer<AbstractSessionEvent> handler : eventHandlers) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error in event handler", e);
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
            PermissionInvocation invocation = new PermissionInvocation();
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
     * Gets the complete list of messages and events in the session.
     * <p>
     * This retrieves the full conversation history, including all user messages,
     * assistant responses, tool invocations, and other session events.
     *
     * @return a future that resolves with a list of all session events
     * @see AbstractSessionEvent
     */
    public CompletableFuture<List<AbstractSessionEvent>> getMessages() {
        return rpc.invoke("session.getMessages", Map.of("sessionId", sessionId), GetMessagesResponse.class)
                .thenApply(response -> {
                    List<AbstractSessionEvent> events = new ArrayList<>();
                    if (response.getEvents() != null) {
                        for (JsonNode eventNode : response.getEvents()) {
                            try {
                                AbstractSessionEvent event = SessionEventParser.parse(eventNode.toString());
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
     */
    public CompletableFuture<Void> abort() {
        return rpc.invoke("session.abort", Map.of("sessionId", sessionId), Void.class);
    }

    /**
     * Disposes the session and releases all associated resources.
     * <p>
     * This destroys the session on the server, clears all event handlers, and
     * releases tool and permission handlers. After calling this method, the session
     * cannot be used again.
     */
    @Override
    public void close() {
        try {
            rpc.invoke("session.destroy", Map.of("sessionId", sessionId), Void.class).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error destroying session", e);
        }

        eventHandlers.clear();
        toolHandlers.clear();
        permissionHandler.set(null);
    }

}
