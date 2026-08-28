/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.McpHeadersRefreshRequiredEvent;
import com.github.copilot.generated.McpHeadersRefreshRequiredReason;
import com.github.copilot.generated.rpc.SessionMcpHeadersHandlePendingHeadersRefreshRequestResult;
import com.github.copilot.rpc.McpHeadersRefreshHandler;
import com.github.copilot.rpc.McpHeadersRefreshResult;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class McpHeadersRefreshHandlerTest {

    private static final String METHOD = "session.mcp.headers.handlePendingHeadersRefreshRequest";

    @Test
    void createHandlerDispatchesHeadersWithoutTtl() {
        McpHeadersRefreshHandler handler = (request, invocation) -> {
            assertEquals("managed-server", request.serverName());
            assertEquals("https://mcp.example.com", request.serverUrl());
            assertEquals(McpHeadersRefreshRequiredReason.STARTUP, request.reason());
            assertEquals("session-1", invocation.getSessionId());
            return CompletableFuture
                    .completedFuture(McpHeadersRefreshResult.withHeaders(Map.of("Authorization", "Bearer dynamic")));
        };

        JsonNode params = dispatch(new SessionConfig().setOnMcpHeadersRefreshRequest(handler));

        assertEquals("headers", params.path("result").path("kind").asText());
        assertEquals("Bearer dynamic", params.path("result").path("headers").path("Authorization").asText());
        assertFalse(params.path("result").has("ttlMs"));
    }

    @Test
    void resumeHandlerDispatchesHeadersWithTtl() {
        McpHeadersRefreshHandler handler = (request, invocation) -> CompletableFuture
                .completedFuture(McpHeadersRefreshResult.withHeaders(Map.of("X-Dynamic", "value"), 60_000L));

        JsonNode params = dispatch(new ResumeSessionConfig().setOnMcpHeadersRefreshRequest(handler));

        assertEquals("headers", params.path("result").path("kind").asText());
        assertEquals("value", params.path("result").path("headers").path("X-Dynamic").asText());
        assertEquals(60_000L, params.path("result").path("ttlMs").asLong());
    }

    @Test
    void handlerCanReturnExplicitNone() {
        McpHeadersRefreshHandler handler = (request, invocation) -> CompletableFuture
                .completedFuture(McpHeadersRefreshResult.none());

        JsonNode params = dispatch(new SessionConfig().setOnMcpHeadersRefreshRequest(handler));

        assertEquals("none", params.path("result").path("kind").asText());
        assertEquals(1, params.path("result").size());
    }

    @Test
    void handlerFailureReturnsExplicitErrorMessage() {
        McpHeadersRefreshHandler handler = (request, invocation) -> CompletableFuture
                .failedFuture(new IllegalStateException("header provider unavailable"));

        JsonNode params = dispatch(new SessionConfig().setOnMcpHeadersRefreshRequest(handler));

        assertEquals("error", params.path("result").path("kind").asText());
        assertEquals("header provider unavailable", params.path("result").path("message").asText());
    }

    @Test
    void handlerExceptionReturnsExplicitErrorMessage() {
        McpHeadersRefreshHandler handler = (request, invocation) -> {
            throw new IllegalArgumentException("invalid header configuration");
        };

        JsonNode params = dispatch(new SessionConfig().setOnMcpHeadersRefreshRequest(handler));

        assertEquals("error", params.path("result").path("kind").asText());
        assertEquals("invalid header configuration", params.path("result").path("message").asText());
    }

    private JsonNode dispatch(SessionConfig config) {
        var rpc = rpc();
        var session = new CopilotSession("session-1", rpc);
        session.setExecutor(Runnable::run);
        SessionRequestBuilder.configureSession(session, config);
        return dispatchAndCapture(session, rpc);
    }

    private JsonNode dispatch(ResumeSessionConfig config) {
        var rpc = rpc();
        var session = new CopilotSession("session-1", rpc);
        session.setExecutor(Runnable::run);
        SessionRequestBuilder.configureSession(session, config);
        return dispatchAndCapture(session, rpc);
    }

    private JsonRpcClient rpc() {
        var rpc = mock(JsonRpcClient.class);
        when(rpc.invoke(eq(METHOD), any(), eq(SessionMcpHeadersHandlePendingHeadersRefreshRequestResult.class)))
                .thenReturn(CompletableFuture
                        .completedFuture(new SessionMcpHeadersHandlePendingHeadersRefreshRequestResult(true)));
        return rpc;
    }

    private JsonNode dispatchAndCapture(CopilotSession session, JsonRpcClient rpc) {
        var event = new McpHeadersRefreshRequiredEvent();
        event.setData(new McpHeadersRefreshRequiredEvent.McpHeadersRefreshRequiredEventData("headers-request",
                "managed-server", "https://mcp.example.com", McpHeadersRefreshRequiredReason.STARTUP));

        session.dispatchEvent(event);

        var paramsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rpc).invoke(eq(METHOD), paramsCaptor.capture(),
                eq(SessionMcpHeadersHandlePendingHeadersRefreshRequestResult.class));
        JsonNode params = JsonRpcClient.getObjectMapper().valueToTree(paramsCaptor.getValue());
        assertEquals("session-1", params.path("sessionId").asText());
        assertEquals("headers-request", params.path("requestId").asText());
        assertNull(params.get("unexpected"));
        return params;
    }
}
