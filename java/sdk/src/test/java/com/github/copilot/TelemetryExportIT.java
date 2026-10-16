/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.TelemetryConfig;

class TelemetryExportIT {
    private static final String PROMPT = "Use the task tool in sync mode to ask a task agent to read "
            + "subagent-otel.txt with the view tool. Then reply with SUBAGENT_OTEL_DONE.";

    @Test
    void shouldExportPerRequestSubagentChatSpans() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            ctx.configureForTest("telemetry", "should_export_per_request_subagent_chat_spans");
            Path telemetryPath = ctx.getWorkDir().resolve("java-subagent-telemetry.jsonl");
            String sourceName = "java-sdk-subagent-telemetry-e2e";
            Files.writeString(ctx.getWorkDir().resolve("subagent-otel.txt"), "SUBAGENT_OTEL_FILE_CONTENT");

            var options = new CopilotClientOptions().setConnection(RuntimeConnection.forStdio(ctx.getCliPath()))
                    .setTelemetry(new TelemetryConfig().setFilePath(telemetryPath.toString()).setExporterType("file")
                            .setSourceName(sourceName).setCaptureContent(true));
            String sessionId;
            try (CopilotClient client = ctx.createClient(options)) {
                CopilotSession session = client
                        .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS);
                try {
                    sessionId = session.getSessionId();
                    var response = session.sendAndWait(new MessageOptions().setPrompt(PROMPT), 90_000).get(120,
                            TimeUnit.SECONDS);
                    assertNotNull(response);
                    assertTrue(response.getData().content().contains("SUBAGENT_OTEL_DONE"));
                } finally {
                    session.close();
                }
                client.stop().get(30, TimeUnit.SECONDS);
            }

            ObjectMapper mapper = new ObjectMapper();
            List<JsonNode> spans = new ArrayList<>();
            for (String line : Files.readAllLines(telemetryPath)) {
                if (!line.isBlank()) {
                    JsonNode entry = mapper.readTree(line);
                    if ("span".equals(property(entry, "type"))) {
                        spans.add(entry);
                    }
                }
            }
            assertFalse(spans.isEmpty(), "Expected exported spans");
            for (JsonNode span : spans) {
                assertEquals(sourceName, property(span.path("instrumentationScope"), "name"));
                assertFalse(span.path("status").path("code").asInt() == 2, "Unexpected error span: " + span);
            }

            List<JsonNode> invocations = operation(spans, "invoke_agent");
            assertEquals(2, invocations.size(), "Invocation spans: " + invocations);
            List<JsonNode> roots = invocations.stream().filter(s -> {
                String parent = property(s, "parentSpanId");
                return parent == null || parent.isEmpty() || parent.equals("0000000000000000");
            }).toList();
            assertEquals(1, roots.size(), "Root spans: " + roots);
            JsonNode root = roots.get(0);
            assertEquals(sessionId, attribute(root, "gen_ai.conversation.id"));
            String rootId = property(root, "spanId");
            String traceId = property(root, "traceId");
            assertNotNull(rootId);
            assertNotNull(traceId);

            List<JsonNode> tasks = operation(spans, "execute_tool").stream()
                    .filter(s -> "task".equals(attribute(s, "gen_ai.tool.name"))).toList();
            assertEquals(1, tasks.size(), "Task spans: " + tasks);
            JsonNode task = tasks.get(0);
            assertEquals(rootId, property(task, "parentSpanId"));
            List<JsonNode> children = invocations.stream()
                    .filter(s -> property(task, "spanId").equals(property(s, "parentSpanId"))).toList();
            assertEquals(1, children.size(), "Child spans: " + children);
            JsonNode child = children.get(0);
            assertEquals(traceId, property(task, "traceId"));
            assertEquals(traceId, property(child, "traceId"));

            List<JsonNode> chats = operation(spans, "chat");
            assertEquals(4, chats.size(), "Chat spans: " + chats);
            List<JsonNode> parentChats = chats.stream().filter(s -> rootId.equals(property(s, "parentSpanId")))
                    .toList();
            assertEquals(2, parentChats.size());
            for (JsonNode chat : parentChats) {
                assertEquals(traceId, property(chat, "traceId"));
            }
            List<JsonNode> childChats = chats.stream()
                    .filter(s -> property(child, "spanId").equals(property(s, "parentSpanId"))).toList();
            assertEquals(2, childChats.size(), "Child chats: " + childChats);
            for (JsonNode chat : childChats) {
                assertEquals(traceId, property(chat, "traceId"));
                assertEquals("sub-agent", attribute(chat, "github.copilot.initiator"));
            }
            List<JsonNode> requestingChats = childChats.stream()
                    .filter(s -> attribute(s, "gen_ai.output.messages").contains("\"view\"")).toList();
            assertEquals(1, requestingChats.size());
            String requestingInput = attribute(requestingChats.get(0), "gen_ai.input.messages");
            assertTrue(requestingInput == null || !requestingInput.contains("SUBAGENT_OTEL_FILE_CONTENT"));
            List<JsonNode> finals = childChats.stream()
                    .filter(s -> attribute(s, "gen_ai.output.messages").contains("SUBAGENT_OTEL_CHILD_DONE")).toList();
            assertEquals(1, finals.size(), "Final child chats: " + finals);
            assertTrue(attribute(finals.get(0), "gen_ai.input.messages").contains("SUBAGENT_OTEL_FILE_CONTENT"));
        }
    }

    private static List<JsonNode> operation(List<JsonNode> spans, String name) {
        return spans.stream().filter(s -> name.equals(attribute(s, "gen_ai.operation.name"))).toList();
    }

    private static String attribute(JsonNode span, String name) {
        return property(span.path("attributes"), name);
    }

    private static String property(JsonNode value, String name) {
        JsonNode property = value.path(name);
        if (property.isMissingNode() || property.isNull()) {
            return null;
        }
        return property.isTextual() ? property.asText() : property.toString();
    }
}
