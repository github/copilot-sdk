/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.AllowCopilotExperimental;

@AllowCopilotExperimental
class AgentRegistrySerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ENTRY = """
            {
              "schemaVersion": 1,
              "kind": "ui-server",
              "pid": 77,
              "host": "127.0.0.1",
              "port": 9090,
              "sessionId": null,
              "startedAt": "2026-01-01T00:00:00Z",
              "copilotVersion": "test"
            }
            """;

    @Test
    void discoveredEntryOmitsAbsentCredentialsAndOptionalSession() throws Exception {
        var entry = MAPPER.readValue(ENTRY, AgentRegistryLiveTargetEntry.class);
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(entry));

        assertFalse(serialized.has("token"), serialized.toString());
        assertFalse(serialized.has("sessionId"));
        assertFalse(serialized.has("sessionName"));
        assertEquals(entry, MAPPER.treeToValue(serialized, AgentRegistryLiveTargetEntry.class));
    }

    @Test
    void populatedTokenAndOptionalSessionRemainUnchanged() throws Exception {
        var input = (ObjectNode) MAPPER.readTree(ENTRY);
        input.put("token", "fixture-token");
        input.put("sessionId", "fixture-session");
        var entry = MAPPER.treeToValue(input, AgentRegistryLiveTargetEntry.class);
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(entry));

        assertEquals("fixture-token", serialized.get("token").asText());
        assertEquals("fixture-session", serialized.get("sessionId").asText());
    }

    @Test
    void nestedDiscoveredEntryPreservesMetadataWithoutCredentials() throws Exception {
        var entry = MAPPER.readValue(ENTRY, AgentRegistryLiveTargetEntry.class);
        var result = new AgentRegistryListEntriesResult(java.util.List.of(entry), null);
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(result));

        var nested = serialized.get("entries").get(0);
        assertFalse(nested.has("token"), serialized.toString());
        assertFalse(nested.has("sessionId"));
        assertEquals(77, nested.get("pid").asInt());
        assertFalse(serialized.has("error"));
        assertEquals(result, MAPPER.treeToValue(serialized, AgentRegistryListEntriesResult.class));
    }

    @Test
    void removeEntryOmitsOptionalHomeWithoutChangingRequiredFields() throws Exception {
        var request = new AgentRegistryRemoveEntryParams(null, "fixture-home", 77L);
        var serialized = MAPPER.readTree(MAPPER.writeValueAsString(request));

        assertFalse(serialized.has("copilotHome"));
        assertEquals("fixture-home", serialized.get("homeDir").asText());
        assertEquals(77, serialized.get("pid").asInt());
        assertEquals(request, MAPPER.treeToValue(serialized, AgentRegistryRemoveEntryParams.class));
    }

    /**
     * Prints a request for the native wire fixture.
     *
     * @param args
     *            the registry home directory
     * @throws Exception
     *             if serialization fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println(MAPPER.writeValueAsString(new AgentRegistryRemoveEntryParams(null, args[0], 77L)));
    }
}
