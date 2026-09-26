/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.AllowCopilotExperimental;

@AllowCopilotExperimental
class ConnectorSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void connectorConnectResult_roundTrips_all_variants() throws Exception {
        var connected = roundTrip("""
                {
                  "kind": "connected",
                  "status": {
                    "apiVersion": 1,
                    "availability": "enabled",
                    "accountId": "account-1",
                    "catalog": {
                      "revision": 7,
                      "refreshedAtMs": 1000,
                      "connectors": [{
                        "name": "outlook",
                        "displayName": "Outlook",
                        "description": "Mail and calendar",
                        "status": "connected",
                        "runtimeServerIds": ["connector-outlook"]
                      }]
                    },
                    "runtimeServers": [{
                      "runtimeServerId": "connector-outlook",
                      "connectorName": "outlook",
                      "status": "connected"
                    }],
                    "pendingConnections": 0
                  }
                }
                """, ConnectorConnectResultConnected.class);
        assertEquals(ConnectorAvailability.ENABLED, connected.getStatus().availability());
        assertEquals(ConnectorCatalogStatus.CONNECTED, connected.getStatus().catalog().connectors().get(0).status());
        assertEquals(ConnectorMcpStatus.CONNECTED, connected.getStatus().runtimeServers().get(0).status());

        var consentRequired = roundTrip("""
                {
                  "kind": "consent_required",
                  "consentUrl": "https://example.com/consent",
                  "continuationId": "continuation-1"
                }
                """, ConnectorConnectResultConsentRequired.class);
        assertEquals("https://example.com/consent", consentRequired.getConsentUrl());
        assertEquals("continuation-1", consentRequired.getContinuationId());

        var pending = roundTrip("""
                {
                  "kind": "pending",
                  "continuationId": "continuation-2"
                }
                """, ConnectorConnectResultPending.class);
        assertEquals("continuation-2", pending.getContinuationId());
    }

    @Test
    void connectorGeneratedRecords_preserve_wire_names_and_null_omission() throws Exception {
        var params = new SessionConnectorsContinueConnectionParams(null, "continuation-1", 4L, 500L, 10_000L);

        var json = MAPPER.readTree(MAPPER.writeValueAsString(params));

        assertFalse(json.has("sessionId"));
        assertEquals("continuation-1", json.get("continuationId").asText());
        assertEquals(4L, json.get("maxAttempts").asLong());
        assertEquals(500L, json.get("pollIntervalMs").asLong());
        assertEquals(10_000L, json.get("deadlineMs").asLong());
        assertEquals(params, MAPPER.treeToValue(json, SessionConnectorsContinueConnectionParams.class));
    }

    private static <T extends ConnectorConnectResult> T roundTrip(String json, Class<T> expectedType) throws Exception {
        var result = MAPPER.readValue(json, ConnectorConnectResult.class);
        var typedResult = assertInstanceOf(expectedType, result);
        assertEquals(typedResult.getKind(),
                MAPPER.readTree(MAPPER.writeValueAsString(typedResult)).get("kind").asText());

        var serialized = MAPPER.writeValueAsString(typedResult);
        return assertInstanceOf(expectedType, MAPPER.readValue(serialized, ConnectorConnectResult.class));
    }
}
