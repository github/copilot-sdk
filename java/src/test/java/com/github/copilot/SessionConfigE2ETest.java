/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.CopilotRequestTestSupport.SYNTHETIC_TEXT;
import static com.github.copilot.CopilotRequestTestSupport.newLlmClient;
import static com.github.copilot.CopilotRequestTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.SessionLimitsConfig;
import com.github.copilot.rpc.BlobAttachment;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * E2E tests for session configuration features.
 */
public class SessionConfigE2ETest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void testShouldApplyInstructionDirectoriesOnCreate() throws Exception {
        ctx.configureForTest("session_config", "should_apply_instructiondirectories_on_create");

        // Set up instruction directory with a custom instruction file
        Path projectDir = ctx.getWorkDir().resolve("instruction-create-project");
        Path instructionDir = ctx.getWorkDir().resolve("extra-create-instructions");
        Path instructionFilesDir = instructionDir.resolve(".github").resolve("instructions");
        String sentinel = "JAVA_CREATE_INSTRUCTION_DIRECTORIES_SENTINEL";
        Files.createDirectories(projectDir);
        Files.createDirectories(instructionFilesDir);
        Files.writeString(instructionFilesDir.resolve("extra.instructions.md"), "Always include " + sentinel + ".");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setWorkingDirectory(projectDir.toString())
                    .setInstructionDirectories(List.of(instructionDir.toString()))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            List<Map<String, Object>> exchanges = ctx.getExchanges();
            assertFalse(exchanges.isEmpty(), "Should have at least one exchange");
            String systemMessage = getSystemMessage(exchanges.get(0));
            assertNotNull(systemMessage, "System message should not be null");
            assertTrue(systemMessage.contains(sentinel),
                    "System message should contain the instruction sentinel: " + sentinel);
        }
    }

    @Test
    void testShouldApplyInstructionDirectoriesOnResume() throws Exception {
        ctx.configureForTest("session_config", "should_apply_instructiondirectories_on_resume");

        // Set up instruction directory with a custom instruction file
        Path projectDir = ctx.getWorkDir().resolve("instruction-resume-project");
        Path instructionDir = ctx.getWorkDir().resolve("extra-resume-instructions");
        Path instructionFilesDir = instructionDir.resolve(".github").resolve("instructions");
        String sentinel = "JAVA_RESUME_INSTRUCTION_DIRECTORIES_SENTINEL";
        Files.createDirectories(projectDir);
        Files.createDirectories(instructionFilesDir);
        Files.writeString(instructionFilesDir.resolve("extra.instructions.md"), "Always include " + sentinel + ".");

        try (CopilotClient client = ctx.createClient()) {
            // Create a session first
            CopilotSession session1 = client.createSession(new SessionConfig()
                    .setWorkingDirectory(projectDir.toString()).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            // Resume with instructionDirectories
            CopilotSession session2 = client.resumeSession(session1.getSessionId(),
                    new ResumeSessionConfig().setWorkingDirectory(projectDir.toString())
                            .setInstructionDirectories(List.of(instructionDir.toString()))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            session2.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            List<Map<String, Object>> exchanges = ctx.getExchanges();
            assertFalse(exchanges.isEmpty(), "Should have at least one exchange");
            String systemMessage = getSystemMessage(exchanges.get(0));
            assertNotNull(systemMessage, "System message should not be null");
            assertTrue(systemMessage.contains(sentinel),
                    "System message should contain the instruction sentinel: " + sentinel);
        }
    }

    @Test
    void testShouldForwardProviderWireModel() throws Exception {
        ctx.configureForTest("session_config", "should_forward_provider_wire_model");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setModel("claude-sonnet-4.5")
                            .setProvider(new ProviderConfig().setType("openai").setBaseUrl(ctx.getProxyUrl())
                                    .setApiKey("test-provider-key").setWireModel("test-wire-model")
                                    .setMaxOutputTokens(1024))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(30, TimeUnit.SECONDS);

            List<Map<String, Object>> exchanges = ctx.getExchanges();
            assertFalse(exchanges.isEmpty(), "Should have at least one exchange");
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) exchanges.get(0).get("request");
            assertEquals("test-wire-model", request.get("model"));
        }
    }

    @Test
    void testShouldUseProviderModelIdAsWireModel() throws Exception {
        ctx.configureForTest("session_config", "should_use_provider_model_id_as_wire_model");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setProvider(new ProviderConfig().setType("openai").setBaseUrl(ctx.getProxyUrl())
                            .setApiKey("test-provider-key").setModelId("claude-sonnet-4.5"))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(30, TimeUnit.SECONDS);

            List<Map<String, Object>> exchanges = ctx.getExchanges();
            assertFalse(exchanges.isEmpty(), "Should have at least one exchange");
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) exchanges.get(0).get("request");
            assertEquals("claude-sonnet-4.5", request.get("model"));
        }
    }

    @Test
    void testShouldApplySessionLimitsOnCreate() throws Exception {
        ctx.configureForTest("session_config", "should_apply_session_limits_on_create");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setSessionLimits(new SessionLimitsConfig(30.0))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            try {
                Map<String, Object> exchange = sendAndGetNextExchange(session,
                        "Acknowledge the current session limits.");

                assertSessionLimitsStatus(exchange, "30 AI credits");
            } finally {
                session.close();
            }
        }
    }

    @Test
    void testShouldApplySessionLimitsOnResume() throws Exception {
        ctx.configureForTest("session_config", "should_apply_session_limits_on_resume");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session1 = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            CopilotSession session2 = client.resumeSession(session1.getSessionId(),
                    new ResumeSessionConfig().setSessionLimits(new SessionLimitsConfig(30.0))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            try {
                Map<String, Object> exchange = sendAndGetNextExchange(session2,
                        "Acknowledge the current session limits.");

                assertSessionLimitsStatus(exchange, "30 AI credits");
            } finally {
                session2.close();
                session1.close();
            }
        }
    }

    @Test
    void testShouldApplyExcludedBuiltInAgentsOnCreate() throws Exception {
        ctx.configureForTest("session_config", "should_apply_excluded_built_in_agents_on_create");

        final String excludedAgent = "explore";
        final String prompt = "What is 1+1?";

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession baselineSession = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            try {
                Map<String, Object> baselineExchange = sendAndGetNextExchange(baselineSession, prompt);
                assertTrue(getTaskAgentTypes(baselineExchange).contains(excludedAgent));
            } finally {
                baselineSession.close();
            }

            CopilotSession excludedSession = client
                    .createSession(new SessionConfig().setExcludedBuiltInAgents(List.of(excludedAgent))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            try {
                List<String> agentTypes = getTaskAgentTypes(sendAndGetNextExchange(excludedSession, prompt));

                assertFalse(agentTypes.isEmpty(), "Expected task tool agent types");
                assertFalse(agentTypes.contains(excludedAgent), "Expected excluded built-in agent to be omitted");
            } finally {
                excludedSession.close();
            }
        }
    }

    @Test
    void testShouldApplyExcludedBuiltInAgentsOnResume() throws Exception {
        ctx.configureForTest("session_config", "should_apply_excluded_built_in_agents_on_resume");

        final String excludedAgent = "explore";

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session1 = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            CopilotSession session2 = client.resumeSession(session1.getSessionId(),
                    new ResumeSessionConfig().setExcludedBuiltInAgents(List.of(excludedAgent))
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            try {
                List<String> agentTypes = getTaskAgentTypes(sendAndGetNextExchange(session2, "What is 1+1?"));

                assertFalse(agentTypes.isEmpty(), "Expected task tool agent types");
                assertFalse(agentTypes.contains(excludedAgent), "Expected excluded built-in agent to be omitted");
            } finally {
                session2.close();
                session1.close();
            }
        }
    }

    @Test
    void testShouldEnableCitationsForAnthropicFileAttachmentsOnCreate() throws Exception {
        setupCapiAuth(ctx);
        var handler = new CopilotRequestTestSupport.RecordingRequestHandler(SYNTHETIC_TEXT);

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("claude-sonnet-4.5")
                    .setEnableCitations(true).setProvider(createAnthropicProvider())
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                session.sendAndWait(new MessageOptions().setPrompt("Summarize the attached PDF with citations enabled.")
                        .setAttachments(List.of(createPdfAttachment()))).get(60, TimeUnit.SECONDS);

                assertAnthropicDocumentCitationsEnabled(singleInferenceRequestBody(handler));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void testShouldEnableCitationsForAnthropicFileAttachmentsOnResume() throws Exception {
        setupCapiAuth(ctx);
        var handler = new CopilotRequestTestSupport.RecordingRequestHandler(SYNTHETIC_TEXT);

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session1 = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            CopilotSession session2 = client.resumeSession(session1.getSessionId(),
                    new ResumeSessionConfig().setModel("claude-sonnet-4.5").setEnableCitations(true)
                            .setProvider(createAnthropicProvider())
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            try {
                session2.sendAndWait(
                        new MessageOptions().setPrompt("Summarize the attached PDF with citations enabled.")
                                .setAttachments(List.of(createPdfAttachment())))
                        .get(60, TimeUnit.SECONDS);

                assertAnthropicDocumentCitationsEnabled(singleInferenceRequestBody(handler));
            } finally {
                session2.close();
                session1.close();
            }
        }
    }

    private Map<String, Object> sendAndGetNextExchange(CopilotSession session, String prompt) throws Exception {
        int existingCount = ctx.getExchanges().size();
        session.sendAndWait(new MessageOptions().setPrompt(prompt)).get(60, TimeUnit.SECONDS);

        List<Map<String, Object>> exchanges = ctx.getExchanges();
        assertTrue(exchanges.size() > existingCount, "Expected at least one new exchange");
        return exchanges.get(existingCount);
    }

    private static void assertSessionLimitsStatus(Map<String, Object> exchange, String expectedRemaining) {
        String content = null;
        for (Object message : getRequestMessages(exchange)) {
            if (message instanceof Map<?, ?> messageMap && "user".equals(messageMap.get("role"))) {
                Object messageContent = messageMap.get("content");
                if (messageContent instanceof String text && text.contains("<session_limits_status>")) {
                    content = text;
                    break;
                }
            }
        }

        assertNotNull(content, "Expected session limits status user message");
        assertTrue(content.contains("Remaining session limits: " + expectedRemaining + "."));
        assertTrue(content.contains("Be frugal; avoid optional exploration and unnecessary tool calls."));
    }

    private static List<String> getTaskAgentTypes(Map<String, Object> exchange) {
        Object toolsObj = getRequest(exchange).get("tools");
        assertInstanceOf(List.class, toolsObj, "Expected request tools");

        JsonNode parameters = null;
        for (Object toolObj : (List<?>) toolsObj) {
            if (toolObj instanceof Map<?, ?> toolMap && toolMap.get("function") instanceof Map<?, ?> functionMap
                    && "task".equals(functionMap.get("name"))) {
                parameters = MAPPER.valueToTree(functionMap.get("parameters"));
                break;
            }
        }

        assertNotNull(parameters, "Expected task tool parameters");
        JsonNode enumValues = parameters.path("properties").path("agent_type").path("enum");
        assertTrue(enumValues.isArray(), "Expected task agent_type enum");

        List<String> values = new ArrayList<>();
        enumValues.forEach(value -> {
            if (value.isTextual()) {
                values.add(value.asText());
            }
        });
        return values;
    }

    private static List<?> getRequestMessages(Map<String, Object> exchange) {
        Object messages = getRequest(exchange).get("messages");
        assertInstanceOf(List.class, messages, "Expected request messages");
        return (List<?>) messages;
    }

    private static Map<?, ?> getRequest(Map<String, Object> exchange) {
        Object request = exchange.get("request");
        assertInstanceOf(Map.class, request, "Expected exchange request");
        return (Map<?, ?>) request;
    }

    private static BlobAttachment createPdfAttachment() {
        String pdfText = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n";
        return new BlobAttachment()
                .setData(Base64.getEncoder().encodeToString(pdfText.getBytes(StandardCharsets.US_ASCII)))
                .setDisplayName("citation-source.pdf").setMimeType("application/pdf");
    }

    private static ProviderConfig createAnthropicProvider() {
        return new ProviderConfig().setType("anthropic").setBaseUrl("https://anthropic-citations.invalid/v1")
                .setApiKey("test-provider-key").setModelId("claude-sonnet-4.5").setWireModel("claude-sonnet-4.5");
    }

    private static String singleInferenceRequestBody(CopilotRequestTestSupport.RecordingRequestHandler handler) {
        List<CopilotRequestTestSupport.InterceptedRequest> requests = handler.inferenceRequests();
        assertEquals(1, requests.size(), "Expected one intercepted inference request");
        return requests.get(0).body();
    }

    private static void assertAnthropicDocumentCitationsEnabled(String requestBody) throws Exception {
        JsonNode root = MAPPER.readTree(requestBody);
        List<JsonNode> documentBlocks = new ArrayList<>();
        for (JsonNode message : root.path("messages")) {
            for (JsonNode block : message.path("content")) {
                if ("document".equals(block.path("type").asText())) {
                    documentBlocks.add(block);
                }
            }
        }

        assertEquals(1, documentBlocks.size(), "Expected one Anthropic document block");
        JsonNode documentBlock = documentBlocks.get(0);
        assertEquals("citation-source.pdf", documentBlock.path("title").asText());
        assertTrue(documentBlock.path("citations").path("enabled").asBoolean(false));
    }

    @SuppressWarnings("unchecked")
    private static String getSystemMessage(Map<String, Object> exchange) {
        // The exchange structure is: { request: { messages: [...] }, response: ...,
        // requestHeaders: ... }
        Object requestObj = exchange.get("request");
        if (!(requestObj instanceof Map<?, ?> request)) {
            return null;
        }
        Object messagesObj = request.get("messages");
        if (messagesObj instanceof List<?> messages) {
            for (Object msg : messages) {
                if (msg instanceof Map<?, ?> msgMap) {
                    if ("system".equals(msgMap.get("role"))) {
                        Object content = msgMap.get("content");
                        return content != null ? content.toString() : null;
                    }
                }
            }
        }
        return null;
    }
}
