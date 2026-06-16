/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SectionOverride;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import com.github.copilot.rpc.SystemMessageSections;
import com.github.copilot.rpc.SystemPromptSections;

/**
 * Failsafe integration test that validates {@link SystemMessageSections}
 * constants work correctly with the Copilot CLI via the replay proxy, and that
 * the deprecated {@link SystemPromptSections} inherits all constants.
 *
 * @see Snapshot:
 *      system_message_transform/should_invoke_transform_callbacks_with_section_content
 */
@SuppressWarnings("deprecation")
class SystemMessageSectionsIT {

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

    /**
     * Verifies that transform callbacks on {@link SystemMessageSections#IDENTITY}
     * and {@link SystemMessageSections#TONE} are invoked by the runtime with
     * non-empty section content via the replay proxy.
     *
     * @see Snapshot:
     *      system_message_transform/should_invoke_transform_callbacks_with_section_content
     */
    @Test
    void transformOnIdentitySectionReceivesNonEmptyContent() throws Exception {
        ctx.configureForTest("system_message_transform", "should_invoke_transform_callbacks_with_section_content");

        ConcurrentHashMap<String, String> capturedContent = new ConcurrentHashMap<>();

        var systemMessage = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
                .setSections(Map.of(SystemMessageSections.IDENTITY, new SectionOverride().setTransform(content -> {
                    capturedContent.put("identity", content);
                    return CompletableFuture.completedFuture(content);
                }), SystemMessageSections.TONE, new SectionOverride().setTransform(content -> {
                    capturedContent.put("tone", content);
                    return CompletableFuture.completedFuture(content);
                })));

        try (CopilotClient client = ctx.createClient()) {
            // Create the file the snapshot expects the CLI view tool to read
            Path testFile = ctx.getWorkDir().resolve("test.txt");
            Files.writeString(testFile, "Hello transform!");

            CopilotSession session = client.createSession(new SessionConfig().setSystemMessage(systemMessage)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);

            try {
                AssistantMessageEvent response = session
                        .sendAndWait(new MessageOptions()
                                .setPrompt("Read the contents of test.txt and tell me what it says"), 60_000)
                        .get(90, TimeUnit.SECONDS);

                assertNotNull(response, "Expected a response from the assistant");

                String identityContent = capturedContent.get("identity");
                assertNotNull(identityContent, "Expected identity transform callback to be invoked by the runtime");
                assertTrue(!identityContent.isBlank(),
                        "Expected identity section content to be non-empty but was blank");

                String toneContent = capturedContent.get("tone");
                assertNotNull(toneContent, "Expected tone transform callback to be invoked by the runtime");
                assertTrue(!toneContent.isBlank(), "Expected tone section content to be non-empty but was blank");
            } finally {
                session.close();
            }
        }
    }

    /**
     * Verifies that the deprecated {@link SystemPromptSections} constants resolve
     * to the same values as {@link SystemMessageSections}.
     */
    @Test
    void deprecatedSystemPromptSectionsMatchesSystemMessageSections() {
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

    /**
     * Verifies sealed hierarchy and exhaustive constant inheritance.
     */
    @Test
    void allConstantsInheritedByDeprecatedClass() throws Exception {
        assertEquals(SystemMessageSections.class, SystemPromptSections.class.getSuperclass());

        Set<String> parentConstants = Arrays.stream(SystemMessageSections.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers()) && f.getType() == String.class)
                .map(Field::getName).collect(Collectors.toSet());

        assertEquals(11, parentConstants.size(), "Expected 11 section constants in SystemMessageSections");

        for (String constantName : parentConstants) {
            Field parentField = SystemMessageSections.class.getDeclaredField(constantName);
            Field childField = SystemPromptSections.class.getField(constantName);
            assertEquals(parentField.get(null), childField.get(null),
                    "Constant " + constantName + " should have same value in both classes");
        }
    }
}
