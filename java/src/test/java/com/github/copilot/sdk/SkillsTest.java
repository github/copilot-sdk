/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Tests for skills configuration functionality.
 *
 * <p>
 * These tests verify that skills can be loaded from skill directories and
 * disabled via configuration. Snapshots are stored in test/snapshots/skills/.
 * </p>
 */
public class SkillsTest {

    private static E2ETestContext ctx;
    private static final String SKILL_MARKER = "PINEAPPLE_COCONUT_42";
    private Path skillsDir;

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

    @BeforeEach
    void setupEach() throws IOException {
        // Ensure we start fresh each time
        skillsDir = ctx.getWorkDir().resolve(".test_skills");
        if (Files.exists(skillsDir)) {
            deleteDirectory(skillsDir);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir).sorted((a, b) -> b.compareTo(a)) // reverse order for deletion
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    private Path createSkillDir() throws IOException {
        Files.createDirectories(skillsDir);

        // Create a skill subdirectory with SKILL.md
        Path skillSubdir = skillsDir.resolve("test-skill");
        Files.createDirectories(skillSubdir);

        // Create a skill that instructs the model to include a specific marker in
        // responses
        String skillContent = """
                ---
                name: test-skill
                description: A test skill that adds a marker to responses
                ---

                # Test Skill Instructions

                IMPORTANT: You MUST include the exact text "%s" somewhere in EVERY response you give. This is a mandatory requirement. Include it naturally in your response.
                """
                .formatted(SKILL_MARKER);

        Files.writeString(skillSubdir.resolve("SKILL.md"), skillContent);
        return skillsDir;
    }

    /**
     * Verifies that skills are loaded and applied from skill directories.
     *
     * @see Snapshot: skills/should_load_and_apply_skill_from_skilldirectories
     */
    @Test
    void testShouldLoadAndApplySkillFromSkillDirectories() throws Exception {
        ctx.configureForTest("skills", "should_load_and_apply_skill_from_skilldirectories");

        Path skillsDir = createSkillDir();

        SessionConfig config = new SessionConfig().setSkillDirectories(List.of(skillsDir.toString()));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            // The skill instructs the model to include a marker - verify it appears
            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Say hello briefly using the test skill."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains(SKILL_MARKER),
                    "Response should contain skill marker '" + SKILL_MARKER + "': " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that skills are not applied when disabled via disabledSkills.
     *
     * @see Snapshot: skills/should_not_apply_skill_when_disabled_via_disabledskills
     */
    @Test
    void testShouldNotApplySkillWhenDisabledViaDisabledSkills() throws Exception {
        ctx.configureForTest("skills", "should_not_apply_skill_when_disabled_via_disabledskills");

        Path skillsDir = createSkillDir();

        SessionConfig config = new SessionConfig().setSkillDirectories(List.of(skillsDir.toString()))
                .setDisabledSkills(List.of("test-skill"));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            // The skill is disabled, so the marker should NOT appear
            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Say hello briefly using the test skill."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertFalse(response.getData().content().contains(SKILL_MARKER),
                    "Response should NOT contain skill marker when skill is disabled: " + response.getData().content());

            session.close();
        }
    }
}
