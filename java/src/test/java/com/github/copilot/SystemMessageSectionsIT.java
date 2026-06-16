/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.SystemMessageSections;
import com.github.copilot.rpc.SystemPromptSections;

/**
 * Failsafe integration test that validates {@link SystemMessageSections}
 * constants and the backward-compatible inheritance from
 * {@link SystemPromptSections}.
 */
@SuppressWarnings("deprecation")
class SystemMessageSectionsIT {

    /**
     * Verifies that the deprecated {@link SystemPromptSections} constants resolve
     * to the same values as {@link SystemMessageSections} — ensuring backward
     * compatibility.
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
     * Verifies that {@link SystemPromptSections} extends
     * {@link SystemMessageSections} — confirming the sealed hierarchy is correctly
     * wired.
     */
    @Test
    void systemPromptSectionsExtendsSystemMessageSections() {
        assertEquals(SystemMessageSections.class, SystemPromptSections.class.getSuperclass());
    }

    /**
     * Verifies that every {@code public static final String} field declared in
     * {@link SystemMessageSections} is accessible via the deprecated
     * {@link SystemPromptSections} (inheritance test).
     */
    @Test
    void allConstantsInheritedByDeprecatedClass() throws Exception {
        Set<String> parentConstants = Arrays.stream(SystemMessageSections.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers()) && f.getType() == String.class)
                .map(Field::getName).collect(Collectors.toSet());

        // Verify there are constants (sanity check)
        assertNotNull(parentConstants);
        assertEquals(11, parentConstants.size(), "Expected 11 section constants in SystemMessageSections");

        // Each constant should be accessible via the subclass
        for (String constantName : parentConstants) {
            Field parentField = SystemMessageSections.class.getDeclaredField(constantName);
            Field childField = SystemPromptSections.class.getField(constantName);
            assertEquals(parentField.get(null), childField.get(null),
                    "Constant " + constantName + " should have same value in both classes");
        }
    }
}
