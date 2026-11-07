/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.ExternalToolRequestedEvent;
import com.github.copilot.generated.rpc.SessionToolsHandlePendingToolCallResult;
import com.github.copilot.rpc.ToolDefinition;

/**
 * Verifies that a tool defined without a handler is declaration-only: the
 * session leaves its calls pending for the application, or another connected
 * client, to answer with {@code session.tools.handlePendingToolCall}.
 */
@AllowCopilotExperimental
class DeclarationOnlyToolTest {

    private JsonRpcClient rpc;
    private CopilotSession session;
    private final List<JsonNode> answers = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setup() {
        rpc = mock(JsonRpcClient.class);
        when(rpc.invoke(eq("session.tools.handlePendingToolCall"), any(),
                eq(SessionToolsHandlePendingToolCallResult.class))).thenAnswer(invocation -> {
                    answers.add(invocation.getArgument(1));
                    return CompletableFuture.completedFuture(null);
                });
        when(rpc.invoke(eq("session.tools.set"), any(), eq(Void.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(rpc.invoke(eq("session.detach"), any(), eq(CopilotSession.SessionDetachResponse.class)))
                .thenReturn(CompletableFuture.completedFuture(new CopilotSession.SessionDetachResponse(true, null)));
        session = new CopilotSession("session-1", rpc);
        // Run tool tasks on the dispatching thread so a missing answer is final.
        session.setExecutor(Runnable::run);
    }

    @AfterEach
    void tearDown() {
        session.close();
    }

    @Test
    void leavesCallsToADeclarationOnlyToolPending() {
        session.registerTools(List.of(declarationOnly("lookup_status"), served("calc")));

        session.dispatchEvent(externalToolRequested("req-declared", "lookup_status"));
        session.dispatchEvent(externalToolRequested("req-served", "calc"));

        assertEquals(List.of("req-served"), answeredRequestIds());
        assertEquals("4", answers.get(0).at("/result/textResultForLlm").asText());
    }

    @Test
    void leavesCallsToADeclarationOnlyReplacementToolPending() throws Exception {
        session.registerTools(List.of(served("lookup_status")));
        session.setTools(List.of(declarationOnly("lookup_status"), served("calc"))).get();

        session.dispatchEvent(externalToolRequested("req-declared", "lookup_status"));
        session.dispatchEvent(externalToolRequested("req-served", "calc"));

        assertEquals(List.of("req-served"), answeredRequestIds());
    }

    private List<String> answeredRequestIds() {
        return answers.stream().map(answer -> answer.path("requestId").asText()).toList();
    }

    private static ToolDefinition declarationOnly(String name) {
        return ToolDefinition.create(name, "Answered by the application", Map.of("type", "object"), null);
    }

    private static ToolDefinition served(String name) {
        return ToolDefinition.create(name, "Answered by this session", Map.of("type", "object"),
                invocation -> CompletableFuture.completedFuture("4"));
    }

    private static ExternalToolRequestedEvent externalToolRequested(String requestId, String toolName) {
        var event = new ExternalToolRequestedEvent();
        event.setData(ExternalToolRequestedEvent.ExternalToolRequestedEventData.builder().requestId(requestId)
                .sessionId("session-1").toolCallId("tool-call-" + requestId).toolName(toolName).arguments(Map.of())
                .build());
        return event;
    }
}
