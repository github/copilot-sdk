/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wire-level coverage for {@link ToolDefinition#isTerminal()}. */
class ToolDefinitionIsTerminalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void isTerminalSerializesAsCamelCaseWhenSet() throws Exception {
        ToolDefinition definition = new ToolDefinition("clear_context", "Clear the conversation",
                Map.of("type", "object"), null, null, null, null, null, true);

        JsonNode node = MAPPER.valueToTree(definition);

        assertTrue(node.has("isTerminal"), "isTerminal should be serialized");
        assertTrue(node.get("isTerminal").asBoolean(), "isTerminal should be true");
    }

    @Test
    void isTerminalIsOmittedWhenNull() throws Exception {
        ToolDefinition definition = new ToolDefinition("plain", "A plain tool", Map.of("type", "object"), null, null,
                null, null, null, null);

        JsonNode node = MAPPER.valueToTree(definition);

        assertFalse(node.has("isTerminal"), "isTerminal should be omitted when null");
    }

    @Test
    void sevenArgumentConstructorStillCompilesAndLeavesTerminalityUnset() throws Exception {
        // Guards source compatibility for call sites written before isTerminal
        // was added as a record component.
        ToolDefinition definition = new ToolDefinition("legacy", "Legacy call site", Map.of("type", "object"), null,
                null, null, null);

        assertEquals(null, definition.isTerminal());
        assertFalse(MAPPER.valueToTree(definition).has("isTerminal"));
    }
}
