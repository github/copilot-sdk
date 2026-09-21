/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.AgentMode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.MessageSource;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.SessionConfig;

class ScenarioCoverageE2ETest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long TIMEOUT_SECONDS = 30;

    @Test
    void publicSessionScenarioComposesMessagesAndRemainsUsableAfterAbort() throws Exception {
        try (var runtime = new ScenarioTestCli(ScenarioCoverageE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try (var session = client
                    .createSession(new SessionConfig().setClientName("scenario-java").setModel("model-a")
                            .setReasoningEffort("high").setStreaming(true).setWorkingDirectory("Q:\\scenario-work")
                            .setAvailableTools(List.of("view", "grep")).setExcludedTools(List.of("shell"))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                String queuedId = session
                        .send(new MessageOptions().setPrompt("Queued scenario message")
                                .setDisplayPrompt("Queued display").setMode("enqueue").setSource(MessageSource.USER)
                                .setAgentMode(AgentMode.PLAN).setRequestHeaders(Map.of("x-scenario", "queued")))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("message-queued", queuedId);

                session.abort().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                session.setModel("model-b", "xhigh").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                session.log("Scenario recovered after abort", "warning", true, "https://example.test/scenario")
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                String immediateId = session
                        .send(new MessageOptions().setPrompt("Immediate scenario message").setMode("immediate")
                                .setSource(MessageSource.USER).setAgentMode(AgentMode.INTERACTIVE)
                                .setRequestHeaders(Map.of("x-scenario", "immediate")))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("message-immediate", immediateId);

                JsonNode create = parameters(runtime, "session.create");
                assertEquals("scenario-java", create.path("clientName").asText());
                assertEquals("model-a", create.path("model").asText());
                assertEquals("high", create.path("reasoningEffort").asText());
                assertTrue(create.path("streaming").asBoolean());
                assertEquals("Q:\\scenario-work", create.path("workingDirectory").asText());
                assertEquals(List.of("view", "grep"), MAPPER.convertValue(create.path("availableTools"), List.class));
                assertEquals("shell", create.path("excludedTools").get(0).asText());

                JsonNode queued = requestParameters(runtime, "session.send", "Queued scenario message");
                assertEquals("enqueue", queued.path("mode").asText());
                assertEquals("plan", queued.path("agentMode").asText());
                assertEquals("queued", queued.path("requestHeaders").path("x-scenario").asText());
                assertEquals("Queued display", queued.path("displayPrompt").asText());

                JsonNode immediate = requestParameters(runtime, "session.send", "Immediate scenario message");
                assertEquals("immediate", immediate.path("mode").asText());
                assertEquals("interactive", immediate.path("agentMode").asText());
                assertEquals("immediate", immediate.path("requestHeaders").path("x-scenario").asText());

                assertEquals(1, runtime.requestCount("session.abort"));
                assertEquals("model-b", parameters(runtime, "session.model.switchTo").path("modelId").asText());
                assertEquals("xhigh", parameters(runtime, "session.model.switchTo").path("reasoningEffort").asText());
                JsonNode log = parameters(runtime, "session.log");
                assertEquals("Scenario recovered after abort", log.path("message").asText());
                assertEquals("warning", log.path("level").asText());
                assertTrue(log.path("ephemeral").asBoolean());
            }
        }
    }

    @Test
    void publicClientScenarioListsResumesAndDeletesPersistedSession() throws Exception {
        try (var runtime = new ScenarioTestCli(ScenarioCoverageE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var sessions = client.listSessions().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(1, sessions.size());
            assertEquals("persisted-scenario", sessions.get(0).getSessionId());
            assertEquals("Persisted Java scenario", sessions.get(0).getSummary());
            assertTrue(sessions.get(0).isRemote());

            try (var resumed = client
                    .resumeSession("persisted-scenario",
                            new ResumeSessionConfig().setWorkingDirectory("Q:\\resumed-scenario")
                                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                assertEquals("persisted-scenario", resumed.getSessionId());
                assertEquals("message-resumed",
                        resumed.send(new MessageOptions().setPrompt("Continue persisted scenario")).get(TIMEOUT_SECONDS,
                                TimeUnit.SECONDS));
            }

            client.deleteSession("persisted-scenario").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonNode resume = parameters(runtime, "session.resume");
            assertEquals("persisted-scenario", resume.path("sessionId").asText());
            assertEquals("Q:\\resumed-scenario", resume.path("workingDirectory").asText());
            assertEquals("persisted-scenario", parameters(runtime, "session.delete").path("sessionId").asText());
        }
    }

    @Test
    void publicClientScenarioPingsAndReusesOneRuntimeAcrossSessions() throws Exception {
        try (var runtime = new ScenarioTestCli(ScenarioCoverageE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var ping = client.ping("scenario-ping").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("scenario-ping", ping.message());
            assertEquals(3, ping.protocolVersion());

            try (var first = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    var second = client
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                assertNotEquals(first.getSessionId(), second.getSessionId());
                assertEquals("message-session-one", first.send(new MessageOptions().setPrompt("Session one scenario"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertEquals("message-session-two", second.send(new MessageOptions().setPrompt("Session two scenario"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }

            assertEquals(1, runtime.requestCount("connect"));
            assertEquals(1, runtime.requestCount("ping"));
            assertEquals(2, runtime.requestCount("session.create"));
            assertEquals(2, runtime.requestCount("session.send"));
            assertEquals("scenario-ping", parameters(runtime, "ping").path("message").asText());
        }
    }

    private static CopilotClient createClient(ScenarioTestCli runtime) {
        var client = new CopilotClient(
                new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess()).setUseLoggedInUser(false));
        client.setInProcessTransportFactory(options -> runtime.open());
        return client;
    }

    private static JsonNode handle(JsonNode request) {
        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        return switch (method) {
            case "connect" -> json("""
                    {"ok":true,"protocolVersion":3,"version":"scenario-parity-test"}
                    """);
            case "ping" -> json("""
                    {"message":"scenario-ping","timestamp":"2026-09-18T12:00:00Z","protocolVersion":3}
                    """);
            case "session.create" -> sessionResult(params.path("sessionId").asText());
            case "session.resume" -> sessionResult(params.path("sessionId").asText());
            case "session.send" -> {
                String prompt = params.path("prompt").asText();
                String id = switch (prompt) {
                    case "Queued scenario message" -> "message-queued";
                    case "Immediate scenario message" -> "message-immediate";
                    case "Continue persisted scenario" -> "message-resumed";
                    case "Session one scenario" -> "message-session-one";
                    case "Session two scenario" -> "message-session-two";
                    default -> throw new AssertionError("Unexpected scenario prompt: " + prompt);
                };
                yield json("""
                        {"messageId":"%s"}
                        """.formatted(id));
            }
            case "session.abort" -> MAPPER.createObjectNode();
            case "session.detach" -> json("""
                    {"success":true}
                    """);
            case "session.model.switchTo" -> json("""
                    {"modelId":"model-b","deferred":false,"status":"applied","deprecationWarnings":[]}
                    """);
            case "session.log" -> json("""
                    {"eventId":"11111111-2222-3333-4444-555555555555"}
                    """);
            case "session.list" -> json("""
                    {"sessions":[{"sessionId":"persisted-scenario","startTime":"2026-09-18T10:00:00Z",
                      "modifiedTime":"2026-09-18T11:00:00Z","summary":"Persisted Java scenario",
                      "isRemote":true,"context":{"cwd":"Q:\\\\scenario-work","repository":"github/copilot-sdk",
                        "branch":"scenario-parity"}}]}
                    """);
            case "session.delete" -> json("""
                    {"success":true}
                    """);
            case "runtime.shutdown" -> MAPPER.createObjectNode();
            default -> throw new AssertionError("Unexpected scenario RPC method: " + method);
        };
    }

    private static JsonNode sessionResult(String sessionId) {
        return json("""
                {"sessionId":"%s","workspacePath":"Q:\\\\scenario-work","capabilities":null,"openCanvases":[]}
                """.formatted(sessionId));
    }

    private static JsonNode parameters(ScenarioTestCli runtime, String method) {
        return runtime.request(method).path("params");
    }

    private static JsonNode requestParameters(ScenarioTestCli runtime, String method, String prompt) {
        return runtime.requests(method).stream().map(request -> request.path("params"))
                .filter(params -> prompt.equals(params.path("prompt").asText())).findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + method + " request for prompt " + prompt));
    }

    private static JsonNode json(String value) {
        try {
            return MAPPER.readTree(value);
        } catch (Exception e) {
            throw new AssertionError("Invalid scenario fake-runtime JSON", e);
        }
    }
}
