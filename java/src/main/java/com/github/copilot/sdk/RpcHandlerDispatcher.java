/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.SessionEventParser;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.SessionLifecycleEvent;
import com.github.copilot.sdk.json.SessionLifecycleEventMetadata;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolInvocation;
import com.github.copilot.sdk.json.ToolResultObject;
import com.github.copilot.sdk.json.UserInputRequest;

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

    /**
     * Creates a dispatcher with session registry and lifecycle dispatcher.
     *
     * @param sessions
     *            the session registry to look up sessions by ID
     * @param lifecycleDispatcher
     *            callback for dispatching lifecycle events
     */
    RpcHandlerDispatcher(Map<String, CopilotSession> sessions, LifecycleEventDispatcher lifecycleDispatcher) {
        this.sessions = sessions;
        this.lifecycleDispatcher = lifecycleDispatcher;
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
        rpc.registerMethodHandler("hooks.invoke", (requestId, params) -> handleHooksInvoke(rpc, requestId, params));
    }

    private void handleSessionEvent(JsonNode params) {
        try {
            String sessionId = params.get("sessionId").asText();
            JsonNode eventNode = params.get("event");
            LOG.fine("Received session.event: " + eventNode);

            CopilotSession session = sessions.get(sessionId);
            if (session != null && eventNode != null) {
                AbstractSessionEvent event = SessionEventParser.parse(eventNode);
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
                if (tool == null || tool.handler() == null) {
                    var result = new ToolResultObject().setTextResultForLlm("Tool '" + toolName + "' is not supported.")
                            .setResultType("failure").setError("tool '" + toolName + "' not supported");
                    rpc.sendResponse(Long.parseLong(requestId), Map.of("result", result));
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
                            toolResult = new ToolResultObject().setResultType("success").setTextResultForLlm(
                                    result instanceof String s ? s : MAPPER.writeValueAsString(result));
                        }
                        rpc.sendResponse(Long.parseLong(requestId), Map.of("result", toolResult));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error sending tool result", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        var result = new ToolResultObject()
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
                    var result = new PermissionRequestResult()
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
                        var result = new PermissionRequestResult()
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

    private void handleUserInputRequest(JsonRpcClient rpc, String requestId, JsonNode params) {
        LOG.fine("Received userInput.request: " + params);
        CompletableFuture.runAsync(() -> {
            try {
                String sessionId = params.get("sessionId").asText();
                String question = params.get("question").asText();
                LOG.fine("Processing userInput for session " + sessionId + ", question: " + question);
                JsonNode choicesNode = params.get("choices");
                JsonNode allowFreeformNode = params.get("allowFreeform");

                CopilotSession session = sessions.get(sessionId);
                LOG.fine("Found session: " + (session != null));
                if (session == null) {
                    LOG.fine("Session not found, sending error");
                    rpc.sendErrorResponse(Long.parseLong(requestId), -32602, "Unknown session " + sessionId);
                    return;
                }

                var request = new UserInputRequest().setQuestion(question);
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
                        rpc.sendResponse(Long.parseLong(requestId),
                                Map.of("answer", answer, "wasFreeform", response.isWasFreeform()));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending user input response", e);
                    }
                }).exceptionally(ex -> {
                    LOG.log(Level.WARNING, "User input handler exception", ex);
                    try {
                        rpc.sendErrorResponse(Long.parseLong(requestId), -32603,
                                "User input handler error: " + ex.getMessage());
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

    private void handleHooksInvoke(JsonRpcClient rpc, String requestId, JsonNode params) {
        CompletableFuture.runAsync(() -> {
            try {
                String sessionId = params.get("sessionId").asText();
                String hookType = params.get("hookType").asText();
                JsonNode input = params.get("input");

                CopilotSession session = sessions.get(sessionId);
                if (session == null) {
                    rpc.sendErrorResponse(Long.parseLong(requestId), -32602, "Unknown session " + sessionId);
                    return;
                }

                session.handleHooksInvoke(hookType, input).thenAccept(output -> {
                    try {
                        rpc.sendResponse(Long.parseLong(requestId), Collections.singletonMap("output", output));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Error sending hooks response", e);
                    }
                }).exceptionally(ex -> {
                    try {
                        rpc.sendErrorResponse(Long.parseLong(requestId), -32603,
                                "Hooks handler error: " + ex.getMessage());
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
}
