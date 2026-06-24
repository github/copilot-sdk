/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

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
    }

    @Test
    void fluentSetterReturnsSameInstance() {
        var capi = new CapiSessionOptions();

        assertSame(capi, capi.setEnableWebSocketResponses(true));
        assertEquals(Boolean.TRUE, capi.getEnableWebSocketResponses());
    }

    @Test
    void serializesEnableWebSocketResponses() {
        var capi = new CapiSessionOptions().setEnableWebSocketResponses(true);

        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(capi);

        assertTrue(json.get("enableWebSocketResponses").asBoolean());
    }

    @Test
    void omitsUnsetEnableWebSocketResponses() {
        var capi = new CapiSessionOptions();

        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(capi);

        assertTrue(json.path("enableWebSocketResponses").isMissingNode());
        assertEquals(0, json.size());
    }

    @Test
    void createRequestIncludesCapiWhenSet() {
        var config = new SessionConfig().setCapi(new CapiSessionOptions().setEnableWebSocketResponses(true));

        var request = SessionRequestBuilder.buildCreateRequest(config, "session-1");
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);

        assertNotNull(request.getCapi());
        assertTrue(json.get("capi").get("enableWebSocketResponses").asBoolean());
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
