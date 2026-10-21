/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.AllowCopilotExperimental;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wire-level coverage for {@link ToolDefinition#namespace()}. */
@AllowCopilotExperimental
class ToolDefinitionNamespaceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void namespaceSerializesWithOptionalDescription() {
        ToolDefinition described = ToolDefinition
                .create("word_create", "Create a document", Map.of("type", "object"), null)
                .namespace(new ToolNamespace("word", "Create and edit Word documents"));
        JsonNode describedNode = MAPPER.valueToTree(described);
        assertEquals("word", describedNode.at("/namespace/name").asText());
        assertEquals("Create and edit Word documents", describedNode.at("/namespace/description").asText());

        JsonNode undescribedNode = MAPPER.valueToTree(described.namespace(new ToolNamespace("word")));
        assertEquals("word", undescribedNode.at("/namespace/name").asText());
        assertFalse(undescribedNode.get("namespace").has("description"));
    }

    @Test
    void namespaceIsOmittedWhenUnsetAndSurvivesOtherCopies() {
        ToolDefinition plain = ToolDefinition.create("plain", "A plain tool", Map.of("type", "object"), null);
        assertNull(plain.namespace());
        assertFalse(MAPPER.valueToTree(plain).has("namespace"));

        ToolDefinition copied = plain.namespace(new ToolNamespace("word")).skipPermission(true).isTerminal(true)
                .defer(ToolDefer.AUTO);
        assertEquals(new ToolNamespace("word"), copied.namespace());
    }
}
