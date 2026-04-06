/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.ElicitationContext;
import com.github.copilot.sdk.json.ElicitationHandler;
import com.github.copilot.sdk.json.ElicitationParams;
import com.github.copilot.sdk.json.ElicitationResult;
import com.github.copilot.sdk.json.ElicitationResultAction;
import com.github.copilot.sdk.json.ElicitationSchema;
import com.github.copilot.sdk.json.InputOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionCapabilities;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionUiCapabilities;

/**
 * Unit tests for the Elicitation feature and Session Capabilities.
 *
 * <p>
 * Ported from {@code ElicitationTests.cs} in the upstream dotnet SDK.
 * </p>
 */
class ElicitationTest {

    @Test
    void sessionCapabilitiesTypesAreProperlyStructured() {
        var capabilities = new SessionCapabilities().setUi(new SessionUiCapabilities().setElicitation(true));

        assertNotNull(capabilities.getUi());
        assertTrue(capabilities.getUi().getElicitation());

        // Test with null UI
        var emptyCapabilities = new SessionCapabilities();
        assertNull(emptyCapabilities.getUi());
    }

    @Test
    void defaultCapabilitiesAreEmpty() {
        var capabilities = new SessionCapabilities();

        assertNull(capabilities.getUi());
    }

    @Test
    void elicitationResultActionValues() {
        assertEquals("accept", ElicitationResultAction.ACCEPT.getValue());
        assertEquals("decline", ElicitationResultAction.DECLINE.getValue());
        assertEquals("cancel", ElicitationResultAction.CANCEL.getValue());
    }

    @Test
    void elicitationResultHasActionAndContent() {
        var content = Map.of("name", (Object) "Alice");
        var result = new ElicitationResult().setAction(ElicitationResultAction.ACCEPT).setContent(content);

        assertEquals(ElicitationResultAction.ACCEPT, result.getAction());
        assertEquals(content, result.getContent());
    }

    @Test
    void elicitationSchemaHasTypeAndProperties() {
        var properties = Map.of("name", (Object) Map.of("type", "string"));
        var schema = new ElicitationSchema().setType("object").setProperties(properties).setRequired(List.of("name"));

        assertEquals("object", schema.getType());
        assertEquals(properties, schema.getProperties());
        assertEquals(List.of("name"), schema.getRequired());
    }

    @Test
    void elicitationSchemaDefaultTypeIsObject() {
        var schema = new ElicitationSchema();

        assertEquals("object", schema.getType());
    }

    @Test
    void elicitationContextHasAllProperties() {
        var properties = Map.of("field", (Object) Map.of("type", "string"));
        var schema = new ElicitationSchema().setProperties(properties);

        var ctx = new ElicitationContext().setSessionId("session-1").setMessage("Please enter your name")
                .setRequestedSchema(schema).setMode("form").setElicitationSource("mcp-server").setUrl(null);

        assertEquals("session-1", ctx.getSessionId());
        assertEquals("Please enter your name", ctx.getMessage());
        assertEquals(schema, ctx.getRequestedSchema());
        assertEquals("form", ctx.getMode());
        assertEquals("mcp-server", ctx.getElicitationSource());
        assertNull(ctx.getUrl());
    }

    @Test
    void elicitationParamsHasMessageAndSchema() {
        var schema = new ElicitationSchema().setProperties(Map.of("field", (Object) Map.of("type", "string")));
        var params = new ElicitationParams().setMessage("Enter name").setRequestedSchema(schema);

        assertEquals("Enter name", params.getMessage());
        assertEquals(schema, params.getRequestedSchema());
    }

    @Test
    void inputOptionsHasAllFields() {
        var opts = new InputOptions().setTitle("My Title").setDescription("My Desc").setMinLength(1).setMaxLength(100)
                .setFormat("email").setDefaultValue("default@example.com");

        assertEquals("My Title", opts.getTitle());
        assertEquals("My Desc", opts.getDescription());
        assertEquals(1, opts.getMinLength());
        assertEquals(100, opts.getMaxLength());
        assertEquals("email", opts.getFormat());
        assertEquals("default@example.com", opts.getDefaultValue());
    }

    @Test
    void sessionConfigOnElicitationRequestIsCloned() {
        ElicitationHandler handler = ctx -> CompletableFuture
                .completedFuture(new ElicitationResult().setAction(ElicitationResultAction.ACCEPT));

        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setOnElicitationRequest(handler);

        var clone = config.clone();

        // Handler reference is shared (not deep-cloned), but the field is copied
        assertNotNull(clone.getOnElicitationRequest());
        assertSame(handler, clone.getOnElicitationRequest());
    }

    @Test
    void resumeConfigOnElicitationRequestIsCloned() {
        ElicitationHandler handler = ctx -> CompletableFuture
                .completedFuture(new ElicitationResult().setAction(ElicitationResultAction.CANCEL));

        var config = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setOnElicitationRequest(handler);

        var clone = config.clone();

        assertNotNull(clone.getOnElicitationRequest());
        assertSame(handler, clone.getOnElicitationRequest());
    }

    @Test
    void buildCreateRequestSetsRequestElicitationWhenHandlerPresent() {
        ElicitationHandler handler = ctx -> CompletableFuture
                .completedFuture(new ElicitationResult().setAction(ElicitationResultAction.ACCEPT));

        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setOnElicitationRequest(handler);

        var request = SessionRequestBuilder.buildCreateRequest(config);

        assertTrue(Boolean.TRUE.equals(request.getRequestElicitation()));
    }

    @Test
    void buildCreateRequestDoesNotSetRequestElicitationWhenNoHandler() {
        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        var request = SessionRequestBuilder.buildCreateRequest(config);

        assertNull(request.getRequestElicitation());
    }

    @Test
    void buildResumeRequestSetsRequestElicitationWhenHandlerPresent() {
        ElicitationHandler handler = ctx -> CompletableFuture
                .completedFuture(new ElicitationResult().setAction(ElicitationResultAction.ACCEPT));

        var config = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setOnElicitationRequest(handler);

        var request = SessionRequestBuilder.buildResumeRequest("session-1", config);

        assertTrue(Boolean.TRUE.equals(request.getRequestElicitation()));
    }
}
