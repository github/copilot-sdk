/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.ExternalToolRequestedEvent;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolInvocation;

/**
 * Verifies that a tool handler learns which sub-agent and runtime request are
 * behind an external tool call.
 */
public class ToolInvocationIdentityTest {

    @Test
    void namesTheSubAgentAndRequestBehindTheCall() throws Exception {
        var rpc = mock(JsonRpcClient.class);
        when(rpc.invoke(eq("session.detach"), any(), eq(CopilotSession.SessionDetachResponse.class)))
                .thenReturn(CompletableFuture.completedFuture(new CopilotSession.SessionDetachResponse(true, null)));
        var session = new CopilotSession("session-1", rpc);
        var invocations = new LinkedBlockingQueue<ToolInvocation>();
        session.registerTools(List.of(ToolDefinition.create("calc", "Calculates", Map.of(), invocation -> {
            invocations.add(invocation);
            return new CompletableFuture<Object>();
        })));

        try {
            var subAgentCall = externalToolRequested("req-sub-agent", "tc-sub-agent");
            subAgentCall.setAgentId("task-agent-1");
            session.dispatchEvent(subAgentCall);
            session.dispatchEvent(externalToolRequested("req-root", "tc-root"));
            var blankAgentCall = externalToolRequested("req-blank-agent", "tc-blank-agent");
            blankAgentCall.setAgentId("");
            session.dispatchEvent(blankAgentCall);

            var byToolCallId = new HashMap<String, ToolInvocation>();
            for (int i = 0; i < 3; i++) {
                var invocation = invocations.poll(1, TimeUnit.SECONDS);
                assertNotNull(invocation, "tool handler did not start");
                byToolCallId.put(invocation.getToolCallId(), invocation);
            }

            var subAgent = byToolCallId.get("tc-sub-agent");
            assertEquals("task-agent-1", subAgent.getAgentId());
            assertEquals("req-sub-agent", subAgent.getRequestId());
            var root = byToolCallId.get("tc-root");
            assertNull(root.getAgentId());
            assertEquals("req-root", root.getRequestId());
            var blankAgent = byToolCallId.get("tc-blank-agent");
            assertNull(blankAgent.getAgentId());
            assertEquals("req-blank-agent", blankAgent.getRequestId());
        } finally {
            session.close();
        }
    }

    private static ExternalToolRequestedEvent externalToolRequested(String requestId, String toolCallId) {
        var event = new ExternalToolRequestedEvent();
        event.setData(new ExternalToolRequestedEvent.ExternalToolRequestedEventData(requestId, "session-1", toolCallId,
                "calc", null, Map.of(), null, null, null));
        return event;
    }
}
