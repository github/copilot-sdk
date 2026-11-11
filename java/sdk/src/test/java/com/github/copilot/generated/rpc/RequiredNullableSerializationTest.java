/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.AllowCopilotExperimental;

@AllowCopilotExperimental
class RequiredNullableSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RULE = """
            {
              "kind": "read",
              "argument": null
            }
            """;

    @Test
    void requiredNullableArgumentIsExplicit() throws Exception {
        var rule = MAPPER.readValue(RULE, PermissionRule.class);
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(rule));

        assertTrue(serialized.has("argument"), serialized.toString());
        assertTrue(serialized.get("argument").isNull());
        assertEquals(rule, MAPPER.treeToValue(serialized, PermissionRule.class));
    }

    @Test
    void populatedArgumentAndOptionalRequestFieldsRemainUnchanged() throws Exception {
        var input = (ObjectNode) MAPPER.readTree(RULE);
        input.put("kind", "Shell");
        input.put("argument", "git status");
        var rule = MAPPER.treeToValue(input, PermissionRule.class);
        var request = SessionPermissionsModifyRulesParams.builder().sessionId("fixture-session")
                .scope(PermissionsModifyRulesScope.SESSION).add(List.of(rule)).removeAll(true).build();
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(request));

        assertEquals("git status", serialized.get("add").get(0).get("argument").asText());
        assertEquals("fixture-session", serialized.get("sessionId").asText());
        assertTrue(serialized.get("removeAll").asBoolean());
        assertFalse(serialized.has("remove"));
        assertEquals(request, MAPPER.treeToValue(serialized, SessionPermissionsModifyRulesParams.class));
    }

    @Test
    void nestedRuleRetainsRequiredNullWithoutChangingOptionalRequestFields() throws Exception {
        var rule = MAPPER.readValue(RULE, PermissionRule.class);
        var request = SessionPermissionsModifyRulesParams.builder().sessionId("fixture-session")
                .scope(PermissionsModifyRulesScope.SESSION).add(List.of(rule)).build();
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(request));

        assertTrue(serialized.get("add").get(0).has("argument"), serialized.toString());
        assertTrue(serialized.get("add").get(0).get("argument").isNull());
        assertFalse(serialized.has("remove"));
        assertFalse(serialized.has("removeAll"));
        assertEquals("fixture-session", serialized.get("sessionId").asText());
        assertEquals("session", serialized.get("scope").asText());
    }
}
