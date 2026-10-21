/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Test
    void connectorCapabilities_missing_and_false_flags_remain_unsupported() throws Exception {
        var json = (ObjectNode) MAPPER.readTree("""
                {
                  "apiVersion": 1,
                  "availability": "enabled",
                  "consentContinuation": true,
                  "opaqueAccountSelection": true,
                  "maxPollAttempts": 5,
                  "maxPollIntervalMs": 2000,
                  "maxDeadlineMs": 30000
                }
                """);
        var legacy = MAPPER.treeToValue(json, SessionConnectorsGetCapabilitiesResult.class);

        assertNull(legacy.sessionAccountSelection());
        assertNull(legacy.targetedReconcile());
        assertFalse(Boolean.TRUE.equals(legacy.sessionAccountSelection()));
        assertFalse(Boolean.TRUE.equals(legacy.targetedReconcile()));
        assertEquals(json, MAPPER.readTree(MAPPER.writeValueAsString(legacy)));

        json.put("sessionAccountSelection", false);
        json.put("targetedReconcile", false);
        var unsupported = MAPPER.treeToValue(json, SessionConnectorsGetCapabilitiesResult.class);

        assertEquals(Boolean.FALSE, unsupported.sessionAccountSelection());
        assertEquals(Boolean.FALSE, unsupported.targetedReconcile());
        assertFalse(Boolean.TRUE.equals(unsupported.sessionAccountSelection()));
        assertFalse(Boolean.TRUE.equals(unsupported.targetedReconcile()));
        assertEquals(json, MAPPER.readTree(MAPPER.writeValueAsString(unsupported)));
    }

    @Test
    void connectorReconcile_preserves_target_and_omits_absent_optional_fields() throws Exception {
        var targeted = new SessionConnectorsReconcileRequest("account-1").setRefreshCatalog(false)
                .setForceConnectorName("outlook");
        var json = MAPPER.readTree(MAPPER.writeValueAsString(targeted));

        assertEquals(MAPPER.readTree("""
                {"accountId":"account-1","refreshCatalog":false,"forceConnectorName":"outlook"}
                """), json);

        var legacy = new SessionConnectorsReconcileParams(null, "account-1", null);
        var legacyJson = MAPPER.readTree(MAPPER.writeValueAsString(legacy));
        assertEquals(MAPPER.readTree("""
                {"accountId":"account-1"}
                """), legacyJson);
        assertEquals(legacy, MAPPER.treeToValue(legacyJson, SessionConnectorsReconcileParams.class));
    }

    @Test
    void connectorCatalogEntry_preserves_optional_presentation_metadata() throws Exception {
        var json = (ObjectNode) MAPPER.readTree("""
                {
                  "name": "outlook",
                  "displayName": "Outlook",
                  "description": "Mail and calendar",
                  "logo": "https://example.com/outlook.svg",
                  "tier": "standard",
                  "releaseTag": "preview",
                  "status": "not_connected",
                  "runtimeServerIds": []
                }
                """);
        var entry = MAPPER.treeToValue(json, ConnectorCatalogEntry.class);

        assertEquals("https://example.com/outlook.svg", entry.logo());
        assertEquals("standard", entry.tier());
        assertEquals("preview", entry.releaseTag());
        assertEquals(json, MAPPER.valueToTree(entry));

        json.remove(List.of("logo", "tier", "releaseTag"));
        var legacy = MAPPER.treeToValue(json, ConnectorCatalogEntry.class);

        assertNull(legacy.logo());
        assertNull(legacy.tier());
        assertNull(legacy.releaseTag());
        assertEquals(json, MAPPER.valueToTree(legacy));
    }

    @Test
    void connectorConnectResult_rejects_unknown_continuation_outcomes() {
        var failure = assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue("""
                {"kind":"verification_required","continuationId":"continuation-3"}
                """, ConnectorConnectResult.class));

        assertEquals("verification_required", failure.getTypeId());
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
