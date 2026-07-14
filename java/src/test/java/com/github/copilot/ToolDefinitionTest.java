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

    @Test
    void testMetadataIsSerialized() throws Exception {
        Map<String, Object> metadata = Map.of("github.com/copilot:safeForTelemetry",
                Map.of("name", true, "inputsNames", false));
        ToolDefinition tool = ToolDefinition.createWithMetadata("my_tool", "A tool", schema(),
                invocation -> CompletableFuture.completedFuture("ok"), metadata);

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertTrue(json.has("metadata"));
        assertTrue(json.get("metadata").has("github.com/copilot:safeForTelemetry"));
    }

    @Test
    void testMetadataOmittedWhenNull() throws Exception {
        ToolDefinition tool = ToolDefinition.create("my_tool", "A tool", schema(),
                invocation -> CompletableFuture.completedFuture("ok"));

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertFalse(json.has("metadata"));
    }

    @Test
    void testSevenArgConstructorLeavesMetadataNull() throws Exception {
        ToolDefinition tool = new ToolDefinition("my_tool", "A tool", schema(),
                invocation -> CompletableFuture.completedFuture("ok"), null, null, null);

        assertNull(tool.metadata());

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertFalse(json.has("metadata"));
    }

    @Test
    void testMetadataCopyMethodSerializes() throws Exception {
        Map<String, Object> metadata = Map.of("github.com/copilot:safeForTelemetry",
                Map.of("name", true, "inputsNames", false));
        ToolDefinition tool = ToolDefinition
                .create("my_tool", "A tool", schema(), invocation -> CompletableFuture.completedFuture("ok"))
                .metadata(metadata);

        assertEquals(metadata, tool.metadata());

        ObjectNode json = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(tool));

        assertTrue(json.get("metadata").has("github.com/copilot:safeForTelemetry"));
    }

    @Test
    void testChainingFlagsPreservesMetadata() throws Exception {
        Map<String, Object> metadata = Map.of("github.com/copilot:safeForTelemetry", Map.of("name", true));

        ToolDefinition metadataFirst = ToolDefinition
                .create("my_tool", "A tool", schema(), invocation -> CompletableFuture.completedFuture("ok"))
                .metadata(metadata).overridesBuiltInTool(true).skipPermission(true).defer(ToolDefer.NEVER);

        ToolDefinition flagsFirst = ToolDefinition
                .create("my_tool", "A tool", schema(), invocation -> CompletableFuture.completedFuture("ok"))
                .overridesBuiltInTool(true).skipPermission(true).defer(ToolDefer.NEVER).metadata(metadata);

        assertEquals(metadata, metadataFirst.metadata());
        assertEquals(metadata, flagsFirst.metadata());
        assertEquals(Boolean.TRUE, flagsFirst.overridesBuiltInTool());
        assertEquals(Boolean.TRUE, flagsFirst.skipPermission());
        assertEquals(ToolDefer.NEVER, flagsFirst.defer());
    }
}
