/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.McpServer;
import com.github.copilot.generated.rpc.McpServerMetadata;
import com.github.copilot.generated.rpc.McpServerSource;
import com.github.copilot.generated.rpc.McpServerStatus;

class McpServerLegacyConstructorCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void releasedConstructorOmitsTheOwnedMarker() {
        var metadata = new McpServerMetadata("use it");
        var server = new McpServer("server", McpServerStatus.CONNECTED, McpServerSource.USER, "plugin", "1.0.0",
                "Server", "failed", metadata);

        assertEquals("server", server.name());
        assertEquals(metadata, server.serverMetadata());
        assertNull(server.owned());
        assertFalse(MAPPER.valueToTree(server).has("owned"));
    }

    @Test
    void ownedMarkerDeserialisesThroughTheCanonicalRecord() throws Exception {
        var server = MAPPER.readValue("""
                {"name":"owned","status":"stopped","owned":{"installationId":"installation"}}
                """, McpServer.class);

        assertEquals("installation", server.owned().installationId());
        JsonNode json = MAPPER.valueToTree(server);
        assertEquals("installation", json.at("/owned/installationId").asText());
    }
}
