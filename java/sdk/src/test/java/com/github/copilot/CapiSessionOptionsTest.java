/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.JsonNode;

import com.github.copilot.rpc.AutoTier;
import com.github.copilot.rpc.CapiSessionOptions;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * Tests for CAPI provider-scoped session options.
 */
class CapiSessionOptionsTest {

    @Test
    void defaultsAreNull() {
        var capi = new CapiSessionOptions();

        assertNull(capi.getEnableWebSocketResponses());
        assertNull(capi.getAutoTier());
    }

    @Test
    void fluentSetterReturnsSameInstance() {
        var capi = new CapiSessionOptions();

        assertSame(capi, capi.setEnableWebSocketResponses(true));
        assertEquals(Boolean.TRUE, capi.getEnableWebSocketResponses());
        assertSame(capi, capi.setAutoTier(AutoTier.BALANCE));
        assertEquals(AutoTier.BALANCE, capi.getAutoTier());
    }

    @Test
    void serializesEnableWebSocketResponses() {
        var capi = new CapiSessionOptions().setEnableWebSocketResponses(true);

        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(capi);

        assertTrue(json.get("enableWebSocketResponses").asBoolean());
        assertTrue(json.path("autoTier").isMissingNode());
    }

    @Test
    void omitsUnsetEnableWebSocketResponses() {
        var capi = new CapiSessionOptions();

        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(capi);

        assertTrue(json.path("enableWebSocketResponses").isMissingNode());
        assertTrue(json.path("autoTier").isMissingNode());
        assertEquals(0, json.size());
    }

    @ParameterizedTest
    @CsvSource({"EFFICIENCY,efficiency", "BALANCE,balance", "INTELLIGENCE,intelligence"})
    void autoTierCanonicalValuesRoundTripAndForward(AutoTier tier, String value) throws Exception {
        var mapper = JsonRpcClient.getObjectMapper();
        var capi = new CapiSessionOptions().setAutoTier(tier);
        JsonNode json = mapper.valueToTree(capi);
        assertEquals(value, json.get("autoTier").asText());
        assertEquals(1, json.size());
        assertEquals(tier, mapper.treeToValue(json, CapiSessionOptions.class).getAutoTier());

        capi.setEnableWebSocketResponses(false);
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setModel("auto").setCapi(capi),
                "session-1");
        var resume = SessionRequestBuilder.buildResumeRequest("session-1", new ResumeSessionConfig().setCapi(capi));
        for (Object request : new Object[]{create, resume}) {
            JsonNode requestJson = mapper.valueToTree(request);
            assertEquals(value, requestJson.get("capi").get("autoTier").asText());
            assertFalse(requestJson.get("capi").get("enableWebSocketResponses").asBoolean());
            assertEquals(2, requestJson.get("capi").size());
        }
    }

    @Test
    void autoTierRejectsNoncanonicalValues() {
        for (String value : new String[]{"balanced", "Balance", "unknown"}) {
            assertThrows(IllegalArgumentException.class, () -> AutoTier.fromValue(value));
        }
        assertNull(AutoTier.fromValue(null));
    }

    @Test
    void clearingAutoTierOmitsIt() {
        var capi = new CapiSessionOptions().setAutoTier(AutoTier.BALANCE).setAutoTier(null);
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(capi);
        assertEquals(0, json.size());
    }

    @Test
    void createRequestIncludesCapiWhenSet() {
        var config = new SessionConfig().setCapi(new CapiSessionOptions().setEnableWebSocketResponses(true));

        var request = SessionRequestBuilder.buildCreateRequest(config, "session-1");
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);

        assertNotNull(request.getCapi());
        assertTrue(json.get("capi").get("enableWebSocketResponses").asBoolean());
        assertTrue(json.get("capi").path("autoTier").isMissingNode());
    }

    @Test
    void createRequestOmitsCapiWhenUnset() {
        var config = new SessionConfig();

        var request = SessionRequestBuilder.buildCreateRequest(config, "session-1");
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);

        assertNull(request.getCapi());
        assertTrue(json.path("capi").isMissingNode());
    }

    @Test
    void resumeRequestIncludesCapiWhenSet() {
        var config = new ResumeSessionConfig().setCapi(new CapiSessionOptions().setEnableWebSocketResponses(true));

        var request = SessionRequestBuilder.buildResumeRequest("session-1", config);
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);

        assertNotNull(request.getCapi());
        assertTrue(json.get("capi").get("enableWebSocketResponses").asBoolean());
        assertTrue(json.get("capi").path("autoTier").isMissingNode());
    }

    @Test
    void resumeRequestOmitsCapiWhenUnset() {
        var config = new ResumeSessionConfig();

        var request = SessionRequestBuilder.buildResumeRequest("session-1", config);
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);

        assertNull(request.getCapi());
        assertTrue(json.path("capi").isMissingNode());
    }

    @Test
    void sessionConfigCloneCopiesCapiReference() {
        var capi = new CapiSessionOptions().setEnableWebSocketResponses(true);

        var clone = new SessionConfig().setCapi(capi).clone();

        assertSame(capi, clone.getCapi());
    }

    @Test
    void resumeSessionConfigCloneCopiesCapiReference() {
        var capi = new CapiSessionOptions().setEnableWebSocketResponses(true);

        var clone = new ResumeSessionConfig().setCapi(capi).clone();

        assertSame(capi, clone.getCapi());
    }

    @Test
    void falseValueIsSerializedWhenExplicitlySet() {
        var capi = new CapiSessionOptions().setEnableWebSocketResponses(false);

        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(capi);

        assertFalse(json.get("enableWebSocketResponses").asBoolean());
    }
}
