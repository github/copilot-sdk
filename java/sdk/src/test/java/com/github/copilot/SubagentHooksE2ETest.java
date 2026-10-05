/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PostToolUseHookOutput;
import com.github.copilot.rpc.PreToolUseHookOutput;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

public class SubagentHooksE2ETest {

    private static final String SNAPSHOT_NAME = "should_invoke_pretooluse_and_posttooluse_hooks_for_sub_agent_tool_calls";

    @Test
    void shouldInvokePreToolUseAndPostToolUseHooksForSubAgentToolCalls() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            ctx.configureForTest("subagent_hooks", SNAPSHOT_NAME);

            ConcurrentLinkedQueue<HookEntry> hookLog = new ConcurrentLinkedQueue<>();
            RecordingForwardingRequestHandler requestHandler = new RecordingForwardingRequestHandler();
            String waitingText = "I've launched an explore agent to read subagent-test.txt. Waiting for it to complete...";
            String finalText = "The explore agent successfully read the file. The contents of **subagent-test.txt** are:\n\n```\nHello from subagent test!\n```";
            String parentSessionId = UUID.randomUUID().toString();
            CompletableFuture<Void> parentWaiting = new CompletableFuture<>();
            HashMap<String, String> env = new HashMap<>(ctx.getEnvironment());
            env.put("COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS", "true");

            try (CopilotClient client = ctx
                    .createClient(new CopilotClientOptions().setEnvironment(env).setRequestHandler(requestHandler))) {
                CopilotSession session = client.createSession(new SessionConfig().setSessionId(parentSessionId)
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                        .setHooks(new SessionHooks().setOnPreToolUse((input, invocation) -> {
                            hookLog.add(new HookEntry("pre", input.getToolName(), input.getSessionId()));
                            return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
                        }).setOnPostToolUse((input, invocation) -> {
                            hookLog.add(new HookEntry("post", input.getToolName(), input.getSessionId()));
                            // A fast child can inject its result before the fixture's waiting reply is
                            // requested.
                            if ("view".equals(input.getToolName()) && !parentSessionId.equals(input.getSessionId())) {
                                return parentWaiting.thenApply(ignored -> null);
                            }
                            return CompletableFuture.completedFuture((PostToolUseHookOutput) null);
                        }))).get();
                try (var subscription = session.on(AssistantMessageEvent.class, message -> {
                    if ((message.getAgentId() == null || message.getAgentId().isEmpty())
                            && waitingText.equals(message.getData().content())) {
                        parentWaiting.complete(null);
                    }
                })) {
                    Files.writeString(ctx.getWorkDir().resolve("subagent-test.txt"), "Hello from subagent test!");
                    var response = session.sendAndWait(new MessageOptions()
                            .setPrompt("Use the task tool to spawn an explore agent that reads the file "
                                    + "subagent-test.txt in the current directory and reports its contents. "
                                    + "You must use the task tool."))
                            .get(120, TimeUnit.SECONDS);
                    assertNotNull(response);
                    assertTrue(response.getAgentId() == null || response.getAgentId().isEmpty());
                    assertEquals(finalText, response.getData().content());
                    var replies = session.getMessages().get().stream()
                            .filter(event -> event instanceof AssistantMessageEvent
                                    && (event.getAgentId() == null || event.getAgentId().isEmpty()))
                            .map(event -> ((AssistantMessageEvent) event).getData().content())
                            .filter(content -> waitingText.equals(content) || finalText.equals(content)).toList();
                    assertEquals(List.of(waitingText, finalText), replies,
                            "Durable history must contain the waiting reply before the final reply");

                    HookEntry taskPre = hookLog.stream()
                            .filter(h -> h.kind().equals("pre") && h.toolName().equals("task")).findFirst()
                            .orElse(null);
                    assertNotNull(taskPre, "preToolUse should fire for the parent's 'task' tool call");

                    List<HookEntry> viewPre = hookLog.stream()
                            .filter(h -> h.kind().equals("pre") && h.toolName().equals("view")).toList();
                    List<HookEntry> viewPost = hookLog.stream()
                            .filter(h -> h.kind().equals("post") && h.toolName().equals("view")).toList();
                    assertFalse(viewPre.isEmpty(), "preToolUse should fire for the sub-agent's 'view' tool call");
                    assertFalse(viewPost.isEmpty(), "postToolUse should fire for the sub-agent's 'view' tool call");
                    assertNotEquals(taskPre.sessionId(), viewPre.get(0).sessionId(),
                            "Sub-agent tool hooks should have a different sessionId than parent tool hooks");
                    assertSubagentRequestMetadata(requestHandler.inferenceRequests());
                } finally {
                    parentWaiting.complete(null);
                    session.close();
                }
            }
        }
    }

    private static void assertSubagentRequestMetadata(List<RequestRecord> records) {
        assertFalse(records.isEmpty(), "request handler should observe inference requests");
        RequestRecord subagentRequest = records.stream()
                .filter(r -> r.parentAgentId() != null && !r.parentAgentId().isEmpty()).findFirst().orElse(null);
        assertNotNull(subagentRequest, "sub-agent inference request should carry a parentAgentId");
        assertFalse(subagentRequest.agentId() == null || subagentRequest.agentId().isEmpty(),
                "sub-agent inference request should carry an agentId");
        assertFalse(subagentRequest.interactionType() == null || subagentRequest.interactionType().isEmpty(),
                "sub-agent inference request should carry an interactionType");
        assertNotEquals(subagentRequest.parentAgentId(), subagentRequest.agentId());
    }

    private static boolean isInferenceUrl(String url) {
        String u = url.toLowerCase();
        return u.endsWith("/chat/completions") || u.endsWith("/responses") || u.endsWith("/v1/messages")
                || u.endsWith("/messages");
    }

    private record HookEntry(String kind, String toolName, String sessionId) {
    }

    private record RequestRecord(String url, String agentId, String parentAgentId, String interactionType) {
    }

    private static final class RecordingForwardingRequestHandler extends CopilotRequestHandler {
        private final ConcurrentLinkedQueue<RequestRecord> records = new ConcurrentLinkedQueue<>();

        List<RequestRecord> inferenceRequests() {
            return records.stream().filter(r -> isInferenceUrl(r.url())).toList();
        }

        @Override
        protected HttpResponse<InputStream> sendRequest(HttpRequest request, CopilotRequestContext ctx)
                throws Exception {
            records.add(new RequestRecord(request.uri().toString(), ctx.agentId(), ctx.parentAgentId(),
                    ctx.interactionType()));
            return super.sendRequest(request, ctx);
        }
    }
}
