/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.github.copilot.rpc.ToolDefer;
import com.github.copilot.rpc.ToolDefinition;

/**
 * Unit tests for {@link ToolDefinition} JSON serialization.
 */
public class ToolDefinitionTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private static Map<String, Object> schema() {
        return Map.of("type", "object", "properties",
                Map.of("query", Map.of("type", "string", "description", "Search query")), "required", List.of("query"));
    }

    @Test
    void testDeferIsSerialized() throws Exception {
        ToolDefinition tool = ToolDefinition.createWithDefer("lookup_issue", "Fetch issue details", schema(),
                invocation -> CompletableFuture.completedFuture("ok"), ToolDefer.AUTO);

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertEquals("auto", json.get("defer").asText());
    }

    @Test
    void testDeferOmittedWhenNull() throws Exception {
        ToolDefinition tool = ToolDefinition.create("lookup_issue", "Fetch issue details", schema(),
                invocation -> CompletableFuture.completedFuture("ok"));

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertFalse(json.has("defer"));
    }

    @Test
    void testDeferNeverIsSerialized() throws Exception {
        ToolDefinition tool = ToolDefinition.createWithDefer("lookup_issue", "Fetch issue details", schema(),
                invocation -> CompletableFuture.completedFuture("ok"), ToolDefer.NEVER);

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertEquals("never", json.get("defer").asText());
    }
}
