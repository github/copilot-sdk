/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.ToolResultObject;

/**
 * Verifies JSON (de)serialization of the {@code toolReferences} field on
 * {@link ToolResultObject}, including that it is omitted when {@code null} (via
 * {@code @JsonInclude(NON_NULL)}) and preserved by the backward-compatible
 * six-argument constructor.
 */
class ToolResultObjectSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void serializesToolReferences() {
        var result = new ToolResultObject("success", "found 2 tools", null, null, null, null,
                List.of("get_weather", "check_status"));

        JsonNode node = MAPPER.valueToTree(result);

        assertEquals("found 2 tools", node.get("textResultForLlm").asText());
        JsonNode refs = node.get("toolReferences");
        assertNotNull(refs);
        assertTrue(refs.isArray());
        assertEquals(2, refs.size());
        assertEquals("get_weather", refs.get(0).asText());
        assertEquals("check_status", refs.get(1).asText());
    }

    @Test
    void omitsToolReferencesWhenNull() {
        JsonNode node = MAPPER.valueToTree(ToolResultObject.success("ok"));

        assertFalse(node.has("toolReferences"));
    }

    @Test
    void sixArgConstructorLeavesToolReferencesNull() {
        var result = new ToolResultObject("success", "ok", null, null, null, null);

        assertNull(result.toolReferences());
        assertFalse(MAPPER.valueToTree(result).has("toolReferences"));
    }

    @Test
    void deserializesToolReferences() throws Exception {
        String json = "{\"resultType\":\"success\",\"textResultForLlm\":\"x\","
                + "\"toolReferences\":[\"alpha\",\"beta\"]}";

        ToolResultObject result = MAPPER.readValue(json, ToolResultObject.class);

        assertEquals(List.of("alpha", "beta"), result.toolReferences());
    }
}
