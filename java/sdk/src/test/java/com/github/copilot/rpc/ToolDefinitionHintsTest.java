/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.AllowCopilotExperimental;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Wire-level coverage for {@link ToolDefinition#idempotent()}.
 */
@AllowCopilotExperimental
class ToolDefinitionHintsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void hintSerializesWhenSetAndSurvivesOtherCopies() {
        ToolDefinition definition = ToolDefinition.create("lookup", "Look up", Map.of("type", "object"), null)
                .idempotent(true).isTerminal(true).namespace(new ToolNamespace("word"));

        JsonNode node = MAPPER.valueToTree(definition);
        assertTrue(node.get("idempotent").asBoolean());
        assertEquals("word", node.at("/namespace/name").asText());
    }

    @Test
    void hintIsOmittedWhenUnset() {
        ToolDefinition definition = new ToolDefinition("plain", "A plain tool", Map.of("type", "object"), null, null,
                null, null, null, true, null);

        assertNull(definition.idempotent());
        JsonNode node = MAPPER.valueToTree(definition);
        assertFalse(node.has("idempotent"));
    }
}
