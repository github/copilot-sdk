/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PostToolUseHookOutput;
import com.github.copilot.rpc.PreToolUseHookOutput;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.SubagentStartHookInput;
import com.github.copilot.rpc.SubagentStartHookOutput;
import com.github.copilot.rpc.SubagentStopHookInput;
import com.github.copilot.rpc.SubagentStopHookOutput;

public class SubagentHooksE2ETest {

    private static final String SNAPSHOT_NAME = "should_apply_subagent_lifecycle_hook_outputs";
    private static final String CHILD_CONTEXT = "Subagent start hook verified: read the requested file.";
    private static final String STOP_RESPONSE_PREFIX = "Subagent stop hook verified: ";

    @Test
    void shouldApplySubagentLifecycleHookOutputs() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            ctx.configureForTest("subagent_hooks", SNAPSHOT_NAME);

            ConcurrentLinkedQueue<HookEntry> hookLog = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<SubagentStartHookInput> subagentStarts = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<SubagentStopHookInput> subagentStops = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<String> startInvocationIds = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<String> stopInvocationIds = new ConcurrentLinkedQueue<>();
            CompletableFuture<SubagentStopHookInput> subagentStopped = new CompletableFuture<>();
            RecordingForwardingRequestHandler requestHandler = new RecordingForwardingRequestHandler();
            String waitingText = "I've launched an explore agent to read subagent-test.txt. "
                    + "Waiting for it to complete...";
            String finalText = "The explore agent successfully read the file. "
                    + "The contents of **subagent-test.txt** are:\n\n```\nHello from subagent test!\n```";
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
                        }).setOnSubagentStart((input, invocation) -> {
                            subagentStarts.add(input);
                            startInvocationIds.add(invocation.getSessionId());
                            return CompletableFuture.completedFuture(new SubagentStartHookOutput(CHILD_CONTEXT));
                        }).setOnSubagentStop((input, invocation) -> {
                            subagentStops.add(input);
                            stopInvocationIds.add(invocation.getSessionId());
                            subagentStopped.complete(input);
                            return CompletableFuture.completedFuture(
                                    new SubagentStopHookOutput(null, null, STOP_RESPONSE_PREFIX + input.response()));
                        }))).get();
                try (var subscription = session.on(AssistantMessageEvent.class, message -> {
                    if ((message.getAgentId() == null || message.getAgentId().isEmpty())
                            && waitingText.equals(message.getData().content())) {
                        parentWaiting.complete(null);
                    }
                })) {
                    try {
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
                        subagentStopped.get(120, TimeUnit.SECONDS);

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
                        ObjectMapper mapper = new ObjectMapper();
                        boolean childContextObserved = false;
                        for (RequestRecord request : requestHandler.inferenceRequests()) {
                            if (request.parentAgentId() == null || request.parentAgentId().isEmpty()) {
                                continue;
                            }
                            for (JsonNode message : mapper.readTree(request.body()).path("messages")) {
                                if (hasChildContext(message)) {
                                    childContextObserved = true;
                                }
                            }
                        }
                        assertTrue(childContextObserved,
                                "start hook context should be prepended to the child inference prompt");
                        assertTrue(
                                requestHandler.inferenceRequests().stream()
                                        .anyMatch(r -> (r.parentAgentId() == null || r.parentAgentId().isEmpty())
                                                && r.body().contains(STOP_RESPONSE_PREFIX)),
                                "rewritten stop response should reach a parent inference request");

                        assertEquals(1, subagentStarts.size(), "one start hook per launched subagent");
                        assertEquals(List.of(session.getSessionId()), List.copyOf(startInvocationIds));
                        SubagentStartHookInput start = subagentStarts.element();
                        assertEquals(session.getSessionId(), start.sessionId());
                        assertTrue(start.timestamp() > 0);
                        assertEquals(ctx.getWorkDir().toAbsolutePath().normalize(),
                                Path.of(start.cwd()).toAbsolutePath().normalize());
                        assertNotNull(start.transcriptPath());
                        assertEquals("explore", start.agentName());
                        assertNull(start.agentDisplayName());
                        assertNull(start.agentDescription());

                        assertEquals(1, subagentStops.size(), "one stop hook per completed subagent");
                        assertEquals(List.of(session.getSessionId()), List.copyOf(stopInvocationIds));
                        SubagentStopHookInput stop = subagentStops.element();
                        assertEquals(start.sessionId(), stop.sessionId());
                        assertTrue(stop.timestamp() >= start.timestamp());
                        assertEquals(start.cwd(), stop.cwd());
                        assertEquals(start.transcriptPath(), stop.transcriptPath());
                        assertEquals(start.agentName(), stop.agentName());
                        assertEquals(start.agentDisplayName(), stop.agentDisplayName());
                        assertEquals(start.agentDescription(), stop.agentDescription());
                        assertEquals("explore", stop.agentType());
                        assertNotNull(stop.agentId());
                        assertFalse(stop.agentId().isBlank());
                        assertEquals("end_turn", stop.stopReason());
                        assertTrue(stop.response().contains("Hello from subagent test!"));
                    } finally {
                        parentWaiting.complete(null);
                        session.close();
                    }
                }
            }
        }
    }

    private static boolean hasChildContext(JsonNode message) {
        if (!"user".equals(message.path("role").asText())) {
            return false;
        }
        String expected = CHILD_CONTEXT + "\n\nRead the file \"subagent-test.txt\"";
        JsonNode content = message.path("content");
        if (content.isTextual()) {
            return content.asText().contains(expected);
        }
        if (content.isArray()) {
            for (JsonNode part : content) {
                if ("text".equals(part.path("type").asText()) && part.path("text").asText().contains(expected)) {
                    return true;
                }
            }
        }
        return false;
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

    private record RequestRecord(String url, String agentId, String parentAgentId, String interactionType,
            String body) {
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
                    ctx.interactionType(),
                    isInferenceUrl(request.uri().toString())
                            ? CopilotRequestTestSupport.requestBodyText(request)
                            : ""));
            return super.sendRequest(request, ctx);
        }
    }
}
