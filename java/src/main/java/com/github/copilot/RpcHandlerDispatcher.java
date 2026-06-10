/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.rpc.AutoModeSwitchRequest;
import com.github.copilot.rpc.ExitPlanModeRequest;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.PermissionRequestResultKind;
import com.github.copilot.rpc.SessionLifecycleEvent;
import com.github.copilot.rpc.SessionLifecycleEventMetadata;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolInvocation;
import com.github.copilot.rpc.ToolResultObject;
import com.github.copilot.rpc.UserInputRequest;

/**
 * Dispatches incoming JSON-RPC method calls to the appropriate handlers.
 * <p>
 * This class handles all server-to-client RPC calls including:
 * <ul>
 * <li>Session events</li>
 * <li>Tool calls</li>
 * <li>Permission requests</li>
 * <li>User input requests</li>
 * <li>Hooks invocations</li>
 * <li>Lifecycle events</li>
 * </ul>
 */
final class RpcHandlerDispatcher {

    private static final Logger LOG = Logger.getLogger(RpcHandlerDispatcher.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private final Map<String, CopilotSession> sessions;
    private final LifecycleEventDispatcher lifecycleDispatcher;
    private final Executor executor;

    /**
     * Creates a dispatcher with session registry and lifecycle dispatcher.
     *
     * @param sessions
     *            the session registry to look up sessions by ID
     * @param lifecycleDispatcher
     *            callback for dispatching lifecycle events
     * @param executor
     *            the executor for async dispatch, or {@code null} for default
     */
    RpcHandlerDispatcher(Map<String, CopilotSession> sessions, LifecycleEventDispatcher lifecycleDispatcher,
            Executor executor) {
        this.sessions = sessions;
        this.lifecycleDispatcher = lifecycleDispatcher;
        this.executor = executor;
    }

    /**
     * Registers all RPC method handlers with the given JSON-RPC client.
     *
     * @param rpc
     *            the JSON-RPC client to register handlers with
     */
    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("session.event", (requestId, params) -> handleSessionEvent(params));
        rpc.registerMethodHandler("session.lifecycle", (requestId, params) -> handleLifecycleEvent(params));
        rpc.registerMethodHandler("tool.call", (requestId, params) -> handleToolCall(rpc, requestId, params));
        rpc.registerMethodHandler("permission.request",
                (requestId, params) -> handlePermissionRequest(rpc, requestId, params));
        rpc.registerMethodHandler("userInput.request",
                (requestId, params) -> handleUserInputRequest(rpc, requestId, params));
        rpc.registerMethodHandler("exitPlanMode.request",
                (requestId, params) -> handleExitPlanModeRequest(rpc, requestId, params));
        rpc.registerMethodHandler("autoModeSwitch.request",
                (requestId, params) -> handleAutoModeSwitchRequest(rpc, requestId, params));
        rpc.registerMethodHandler("hooks.invoke", (requestId, params) -> handleHooksInvoke(rpc, requestId, params));
        rpc.registerMethodHandler("systemMessage.transform",
                (requestId, params) -> handleSystemMessageTransform(rpc, requestId, params));
    }

    private void handleSessionEvent(JsonNode params) {
        try {
            String sessionId = params.get("sessionId").asText();
            JsonNode eventNode = params.get("event");
            LOG.fine("Received session.event: " + eventNode);

            CopilotSession session = sessions.get(sessionId);
            if (session != null && eventNode != null) {
                SessionEvent event = MAPPER.treeToValue(eventNode, SessionEvent.class);
                if (event != null) {
                    session.dispatchEvent(event);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error handling session event", e);
        }
    }

    private void handleLifecycleEvent(JsonNode params) {
        try {
            String type = params.has("type") ? params.get("type").asText() : "";
            String sessionId = params.has("sessionId") ? params.get("sessionId").asText() : "";

            SessionLifecycleEvent event = new SessionLifecycleEvent();
            event.setType(type);
            event.setSessionId(sessionId);

            if (params.has("metadata") && !params.get("metadata").isNull()) {
                SessionLifecycleEventMetadata metadata = MAPPER.treeToValue(params.get("metadata"),
                        SessionLifecycleEventMetadata.class);
                event.setMetadata(metadata);
            }

            lifecycleDispatcher.dispatch(event);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error handling session lifecycle event", e);
        }
    }

    private void handleToolCall(JsonRpcClient rpc, String requestId, JsonNode params) {
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "tool.call");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.get("sessionId").asText();
                String toolCallId = params.get("toolCallId").asText();
                String toolName = params.get("toolName").asText();
                JsonNode arguments = params.get("arguments");

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    rpc.sendErrorResponse(requestIdLong, -32602, "Unknown session " + sessionId);
                    return;
                }

                ToolDefinition tool = session.getTool(toolName);
                if (tool == null || tool.handler() == null) {
                    var result = ToolResultObject.failure("Tool '" + toolName + "' is not supported.",
                            "tool '" + toolName + "' not supported");
                    rpc.sendResponse(requestIdLong, Map.of("result", result));
                    return;
                }

                var invocation = new ToolInvocation().setSessionId(sessionId).setToolCallId(toolCallId)
                        .setToolName(toolName).setArguments(arguments);

                tool.handler().invoke(invocation).thenAccept(result -> {
                    try {
                        ToolResultObject toolResult;
                        if (result instanceof ToolResultObject tr) {
                            toolResult = tr;
                        } else {
                            toolResult = ToolResultObject
                                    .success(result instanceof String s ? s : MAPPER.writeValueAsString(result));
                        }
                        rpc.sendResponse(requestIdLong, Map.of("result", toolResult));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error sending tool result", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        var result = ToolResultObject.failure(
                                "Invoking this tool produced an error. Detailed information is not available.",
                                ex.getMessage());
                        rpc.sendResponse(requestIdLong, Map.of("result", result));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error sending tool error", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling tool call", e);
                try {
                    rpc.sendErrorResponse(requestIdLong, -32603, e.getMessage());
                } catch (IOException ioe) {
                    LOG.log(Level.SEVERE, "Failed to send error response", ioe);
                }
            }
        });
    }

    private void handlePermissionRequest(JsonRpcClient rpc, String requestId, JsonNode params) {
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "permission.request");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.get("sessionId").asText();
                JsonNode permissionRequest = params.get("permissionRequest");

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    var result = new PermissionRequestResult()
                            .setKind(PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER);
                    rpc.sendResponse(requestIdLong, Map.of("result", result));
                    return;
                }

                session.handlePermissionRequest(permissionRequest).thenAccept(result -> {
                    try {
                        if (PermissionRequestResultKind.NO_RESULT.getValue().equalsIgnoreCase(result.getKind())) {
                            // Protocol v2 does not support NO_RESULT — the server
                            // expects exactly one response per request, so abstaining
                            // would leave it hanging.
                            throw new IllegalStateException(
                                    "Permission handlers cannot return 'no-result' when connected to a protocol v2 server.");
                        }
                        rpc.sendResponse(requestIdLong, Map.of("result", result));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending permission result", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        var result = new PermissionRequestResult()
                                .setKind(PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER);
                        rpc.sendResponse(requestIdLong, Map.of("result", result));
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

    private void handleUserInputRequest(JsonRpcClient rpc, String requestId, JsonNode params) {
        LOG.fine("Received userInput.request: " + params);
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "userInput.request");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.get("sessionId").asText();
                String question = params.get("question").asText();
                LOG.fine("Processing userInput for session " + sessionId + ", question: " + question);
                JsonNode headerNode = params.get("header");
                JsonNode choicesNode = params.get("choices");
                JsonNode allowFreeformNode = params.get("allowFreeform");

                CopilotSession session = sessions.get(sessionId);
                LOG.fine("Found session: " + (session != null));
                if (session == null) {
                    LOG.fine("Session not found, sending error");
                    rpc.sendErrorResponse(requestIdLong, -32602, "Unknown session " + sessionId);
                    return;
                }

                var request = new UserInputRequest().setQuestion(question);
                if (headerNode != null && !headerNode.isNull()) {
                    request.setHeader(headerNode.asText());
                }
                if (choicesNode != null && choicesNode.isArray()) {
                    var choices = new ArrayList<String>();
                    for (JsonNode choice : choicesNode) {
                        choices.add(choice.asText());
                    }
                    request.setChoices(choices);
                }
                if (allowFreeformNode != null) {
                    request.setAllowFreeform(allowFreeformNode.asBoolean());
                }

                session.handleUserInputRequest(request).thenAccept(response -> {
                    try {
                        // Ensure answer is never null - CLI requires a non-null string
                        String answer = response.getAnswer() != null ? response.getAnswer() : "";
                        LOG.fine("Sending userInput response: answer=" + answer + ", wasFreeform="
                                + response.isWasFreeform());
                        rpc.sendResponse(requestIdLong,
                                Map.of("answer", answer, "wasFreeform", response.isWasFreeform()));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending user input response", e);
                    }
                }).exceptionally(ex -> {
                    LOG.log(Level.WARNING, "User input handler exception", ex);
                    try {
                        rpc.sendErrorResponse(requestIdLong, -32603, "User input handler error: " + ex.getMessage());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending user input error", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling user input request", e);
            }
        });
    }

    private void handleExitPlanModeRequest(JsonRpcClient rpc, String requestId, JsonNode params) {
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "exitPlanMode.request");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.get("sessionId").asText();

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    rpc.sendErrorResponse(requestIdLong, -32602, "Unknown session " + sessionId);
                    return;
                }

                var request = new ExitPlanModeRequest();
                if (params.has("summary")) {
                    request.setSummary(params.get("summary").asText());
                }
                if (params.has("planContent") && !params.get("planContent").isNull()) {
                    request.setPlanContent(params.get("planContent").asText());
                }
                if (params.has("actions") && params.get("actions").isArray()) {
                    var actions = new ArrayList<String>();
                    for (JsonNode action : params.get("actions")) {
                        actions.add(action.asText());
                    }
                    request.setActions(actions);
                }
                if (params.has("recommendedAction") && !params.get("recommendedAction").isNull()) {
                    request.setRecommendedAction(params.get("recommendedAction").asText());
                }

                session.handleExitPlanModeRequest(request).thenAccept(result -> {
                    try {
                        rpc.sendResponse(requestIdLong, MAPPER.valueToTree(result));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending exit plan mode response", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        rpc.sendErrorResponse(requestIdLong, -32603,
                                "Exit plan mode handler error: " + ex.getMessage());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending exit plan mode error", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling exit plan mode request", e);
            }
        });
    }

    private void handleAutoModeSwitchRequest(JsonRpcClient rpc, String requestId, JsonNode params) {
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "autoModeSwitch.request");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.get("sessionId").asText();

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    rpc.sendErrorResponse(requestIdLong, -32602, "Unknown session " + sessionId);
                    return;
                }

                var request = new AutoModeSwitchRequest();
                if (params.has("errorCode") && !params.get("errorCode").isNull()) {
                    request.setErrorCode(params.get("errorCode").asText());
                }
                if (params.has("retryAfterSeconds") && !params.get("retryAfterSeconds").isNull()) {
                    request.setRetryAfterSeconds(params.get("retryAfterSeconds").asDouble());
                }

                session.handleAutoModeSwitchRequest(request).thenAccept(response -> {
                    try {
                        rpc.sendResponse(requestIdLong, Map.of("response", response));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending auto mode switch response", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        rpc.sendErrorResponse(requestIdLong, -32603,
                                "Auto mode switch handler error: " + ex.getMessage());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending auto mode switch error", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling auto mode switch request", e);
            }
        });
    }

    private void handleHooksInvoke(JsonRpcClient rpc, String requestId, JsonNode params) {
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "hooks.invoke");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.get("sessionId").asText();
                String hookType = params.get("hookType").asText();
                JsonNode input = params.get("input");

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    rpc.sendErrorResponse(requestIdLong, -32602, "Unknown session " + sessionId);
                    return;
                }

                session.handleHooksInvoke(hookType, input).thenAccept(output -> {
                    try {
                        rpc.sendResponse(requestIdLong, Collections.singletonMap("output", output));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending hooks response", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        rpc.sendErrorResponse(requestIdLong, -32603, "Hooks handler error: " + ex.getMessage());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending hooks error", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling hooks invoke", e);
            }
        });
    }

    /**
     * Functional interface for dispatching lifecycle events.
     */
    @FunctionalInterface
    interface LifecycleEventDispatcher {

        void dispatch(SessionLifecycleEvent event);
    }

    private void handleSystemMessageTransform(JsonRpcClient rpc, String requestId, JsonNode params) {
        runAsync(() -> {
            final long requestIdLong = parseRequestId(requestId, "systemMessage.transform");
            if (requestIdLong == -1) {
                return;
            }
            try {
                String sessionId = params.has("sessionId") ? params.get("sessionId").asText() : null;
                JsonNode sections = params.get("sections");

                CopilotSession session = sessionId != null ? sessions.get(sessionId) : null;
                if (session == null) {
                    rpc.sendErrorResponse(requestIdLong, -32602, "Unknown session " + sessionId);
                    return;
                }

                session.handleSystemMessageTransform(sections).thenAccept(result -> {
                    try {
                        rpc.sendResponse(requestIdLong, result);
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending systemMessage.transform response", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        rpc.sendErrorResponse(requestIdLong, -32603, "Transform error: " + ex.getMessage());
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending transform error response", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error handling systemMessage.transform", e);
            }
        });
    }

    /**
     * Parses a JSON-RPC request ID string into a {@code long}.
     *
     * @param requestId
     *            the request ID string received from the JSON-RPC layer
     * @param methodName
     *            the RPC method name, used in the log message on failure
     * @return the parsed request ID, or {@code -1} if the string is not a valid
     *         long
     */
    private static long parseRequestId(String requestId, String methodName) {
        try {
            return Long.parseLong(requestId);
        } catch (NumberFormatException nfe) {
            LOG.log(Level.SEVERE, "Invalid requestId for " + methodName + ": " + requestId, nfe);
            return -1;
        }
    }

    private void runAsync(Runnable task) {
        try {
            if (executor != null) {
                CompletableFuture.runAsync(task, executor);
            } else {
                CompletableFuture.runAsync(task);
            }
        } catch (RejectedExecutionException e) {
            LOG.log(Level.WARNING, "Executor rejected handler task; running inline", e);
            task.run();
        }
    }
}
