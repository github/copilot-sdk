/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * E2E tests for session configuration features.
 */
public class SessionConfigE2ETest {

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
