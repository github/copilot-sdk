/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SectionOverride;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import com.github.copilot.rpc.SystemMessageSections;
import com.github.copilot.rpc.SystemPromptSections;

/**
 * Failsafe integration test that validates {@link SystemMessageSections}
 * constants are recognized by the live Copilot CLI runtime.
 * <p>
 * Uses a transform callback on the {@code identity} section to assert the
 * runtime invokes the callback with non-empty content — proving the constant is
 * a valid section identifier understood by the runtime.
 * <p>
 * Requires the CLI to be installed and the user to be signed in. Uses
 * {@link TestUtil#findCliPath()} so the test harness binary is found in CI.
 */
@SuppressWarnings("deprecation")
class SystemMessageSectionsIT {

    private static CopilotClient client;

    @BeforeAll
    static void setup() throws Exception {
        String cliPath = TestUtil.findCliPath();
        CopilotClientOptions options = new CopilotClientOptions().setCliPath(cliPath).setUseLoggedInUser(true);
        client = new CopilotClient(options);
        client.start().get(30, TimeUnit.SECONDS);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Verifies that a transform callback on {@link SystemMessageSections#IDENTITY}
     * is invoked by the runtime with non-empty section content.
     * <p>
     * This proves the constant {@code "identity"} is a real section ID that the
     * runtime recognizes and populates.
     */
    @Test
    void transformOnIdentitySectionReceivesNonEmptyContent() throws Exception {
        // Thread-safe container to capture what the runtime passes to our transform
        ConcurrentHashMap<String, String> capturedContent = new ConcurrentHashMap<>();

        var systemMessage = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
                .setSections(Map.of(SystemMessageSections.IDENTITY, new SectionOverride().setTransform(content -> {
                    capturedContent.put("identity", content);
                    return CompletableFuture.completedFuture(content);
                }), SystemMessageSections.TONE, new SectionOverride().setTransform(content -> {
                    capturedContent.put("tone", content);
                    return CompletableFuture.completedFuture(content);
                })));

        CopilotSession session = client.createSession(new SessionConfig().setSystemMessage(systemMessage)
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);

        try {
            // Send a message to trigger the runtime to build the system message
            // (transforms fire during session creation or first message)
            session.sendAndWait(new MessageOptions().setPrompt("Say hello"), 60_000).get(90, TimeUnit.SECONDS);

            // Assert: identity transform was invoked with non-empty content
            String identityContent = capturedContent.get("identity");
            assertNotNull(identityContent, "Expected identity transform callback to be invoked by the runtime");
            assertTrue(!identityContent.isBlank(), "Expected identity section content to be non-empty but was blank");

            // Assert: tone transform was also invoked
            String toneContent = capturedContent.get("tone");
            assertNotNull(toneContent, "Expected tone transform callback to be invoked by the runtime");
            assertTrue(!toneContent.isBlank(), "Expected tone section content to be non-empty but was blank");
        } finally {
            session.close();
        }
    }

    /**
     * Verifies that the deprecated {@link SystemPromptSections} constants resolve
     * to the same values as {@link SystemMessageSections} — ensuring backward
     * compatibility.
     */
    @Test
    void deprecatedSystemPromptSectionsMatchesSystemMessageSections() {
        // These are compile-time constants so this test guards against accidental
        // divergence if someone edits one class but not the other.
        assertEquals(SystemMessageSections.IDENTITY, SystemPromptSections.IDENTITY);
        assertEquals(SystemMessageSections.TONE, SystemPromptSections.TONE);
        assertEquals(SystemMessageSections.TOOL_EFFICIENCY, SystemPromptSections.TOOL_EFFICIENCY);
        assertEquals(SystemMessageSections.ENVIRONMENT_CONTEXT, SystemPromptSections.ENVIRONMENT_CONTEXT);
        assertEquals(SystemMessageSections.CODE_CHANGE_RULES, SystemPromptSections.CODE_CHANGE_RULES);
        assertEquals(SystemMessageSections.GUIDELINES, SystemPromptSections.GUIDELINES);
        assertEquals(SystemMessageSections.SAFETY, SystemPromptSections.SAFETY);
        assertEquals(SystemMessageSections.TOOL_INSTRUCTIONS, SystemPromptSections.TOOL_INSTRUCTIONS);
        assertEquals(SystemMessageSections.CUSTOM_INSTRUCTIONS, SystemPromptSections.CUSTOM_INSTRUCTIONS);
        assertEquals(SystemMessageSections.RUNTIME_INSTRUCTIONS, SystemPromptSections.RUNTIME_INSTRUCTIONS);
        assertEquals(SystemMessageSections.LAST_INSTRUCTIONS, SystemPromptSections.LAST_INSTRUCTIONS);
    }
}
