/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.CanvasAction;
import com.github.copilot.generated.rpc.CanvasActionInvokeParams;
import com.github.copilot.generated.rpc.CanvasCloseParams;
import com.github.copilot.generated.rpc.CanvasOpenParams;
import com.github.copilot.generated.rpc.CanvasOpenResult;
import com.github.copilot.generated.rpc.OpenCanvasInstance;
import com.github.copilot.rpc.CanvasDeclaration;
import com.github.copilot.rpc.CanvasException;
import com.github.copilot.rpc.CanvasHandler;
import com.github.copilot.rpc.CanvasProviderIdentity;
import com.github.copilot.rpc.CreateSessionRequest;
import com.github.copilot.rpc.ExtensionInfo;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.ResumeSessionRequest;
import com.github.copilot.rpc.SessionConfig;

/**
 * Unit tests for the host-side canvas declaration API: wire-JSON mapping on
 * {@code session.create} / {@code session.resume}, plus {@link CanvasHandler}
 * defaults and {@link CanvasException} behavior.
 */
public class CanvasHostApiTest {

    private static JsonNode toJson(Object request) {
        return JsonRpcClient.getObjectMapper().valueToTree(request);
    }

    private static CanvasDeclaration counterCanvas() {
        return new CanvasDeclaration("counter", "Counter", "Tracks a counter value.")
                .setInputSchema(Map.of("type", "object"))
                .setActions(List.of(new CanvasAction("increment", "Increments the counter.", null)));
    }

    // =========================================================================
    // session.create wire mapping
    // =========================================================================

    @Test
    void testCreateRequestSerializesCanvasFields() {
        var config = new SessionConfig().setCanvases(List.of(counterCanvas()))
                .setCanvasHandler(params -> CompletableFuture.completedFuture(new CanvasOpenResult(null, null, null)))
                .setRequestCanvasRenderer(true).setRequestExtensions(true).setExtensionSdkPath("/tmp/sdk")
                .setExtensionInfo(new ExtensionInfo("github-app", "canvas-provider"))
                .setCanvasProvider(new CanvasProviderIdentity("app:builtin:main").setName("My App"));

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config, "sess-1");
        JsonNode json = toJson(request);

        assertTrue(json.has("canvases"), "canvases should be serialized");
        JsonNode canvases = json.get("canvases");
        assertEquals(1, canvases.size());
        JsonNode canvas = canvases.get(0);
        assertEquals("counter", canvas.get("id").asText());
        assertEquals("Counter", canvas.get("displayName").asText());
        assertEquals("Tracks a counter value.", canvas.get("description").asText());
        assertEquals("object", canvas.get("inputSchema").get("type").asText());
        assertEquals("increment", canvas.get("actions").get(0).get("name").asText());

        assertTrue(json.get("requestCanvasRenderer").asBoolean());
        assertTrue(json.get("requestExtensions").asBoolean());
        assertEquals("/tmp/sdk", json.get("extensionSdkPath").asText());

        JsonNode extInfo = json.get("extensionInfo");
        assertEquals("github-app", extInfo.get("source").asText());
        assertEquals("canvas-provider", extInfo.get("name").asText());

        JsonNode provider = json.get("canvasProvider");
        assertEquals("app:builtin:main", provider.get("id").asText());
        assertEquals("My App", provider.get("name").asText());

        // The SDK-side handler must never be serialized onto the wire request.
        assertFalse(json.has("canvasHandler"), "canvasHandler must not be serialized");
    }

    @Test
    void testCanvasProviderOmitsNameWhenNull() {
        var config = new SessionConfig().setCanvasProvider(new CanvasProviderIdentity("app:builtin:main"));

        JsonNode json = toJson(SessionRequestBuilder.buildCreateRequest(config, "sess-1"));
        JsonNode provider = json.get("canvasProvider");
        assertEquals("app:builtin:main", provider.get("id").asText());
        assertFalse(provider.has("name"), "name should be omitted when null");
    }

    @Test
    void testCreateRequestOmitsCanvasFieldsWhenUnset() {
        JsonNode json = toJson(SessionRequestBuilder.buildCreateRequest(new SessionConfig(), "sess-1"));
        assertFalse(json.has("canvases"));
        assertFalse(json.has("requestCanvasRenderer"));
        assertFalse(json.has("requestExtensions"));
        assertFalse(json.has("extensionSdkPath"));
        assertFalse(json.has("extensionInfo"));
        assertFalse(json.has("canvasProvider"));
    }

    // =========================================================================
    // session.resume wire mapping
    // =========================================================================

    @Test
    void testResumeRequestSerializesCanvasFields() {
        var openInstance = new OpenCanvasInstance("inst-1", "app:builtin:main", "My App", "counter", "Counter", null,
                null, null);
        var config = new ResumeSessionConfig().setCanvases(List.of(counterCanvas()))
                .setOpenCanvases(List.of(openInstance)).setRequestCanvasRenderer(true).setRequestExtensions(true)
                .setExtensionInfo(new ExtensionInfo("github-app", "canvas-provider"))
                .setCanvasProvider(new CanvasProviderIdentity("app:builtin:main"));

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sess-1", config);
        JsonNode json = toJson(request);

        assertEquals("counter", json.get("canvases").get(0).get("id").asText());
        assertEquals("inst-1", json.get("openCanvases").get(0).get("instanceId").asText());
        assertEquals("counter", json.get("openCanvases").get(0).get("canvasId").asText());
        assertTrue(json.get("requestCanvasRenderer").asBoolean());
        assertTrue(json.get("requestExtensions").asBoolean());
        assertEquals("github-app", json.get("extensionInfo").get("source").asText());
        assertEquals("app:builtin:main", json.get("canvasProvider").get("id").asText());
        assertFalse(json.get("canvasProvider").has("name"));
    }

    // =========================================================================
    // CanvasHandler defaults + CanvasException
    // =========================================================================

    @Test
    void testCanvasHandlerOnActionDefaultFailsWithNoHandler() {
        CanvasHandler handler = params -> CompletableFuture.completedFuture(new CanvasOpenResult(null, null, null));
        var params = new CanvasActionInvokeParams("sess-1", "ext", "counter", "inst-1", "increment", null, null, null);

        ExecutionException ex = assertThrows(ExecutionException.class, () -> handler.onAction(params).get());
        assertInstanceOf(CanvasException.class, ex.getCause());
        assertEquals("canvas_action_no_handler", ((CanvasException) ex.getCause()).getCode());
    }

    @Test
    void testCanvasHandlerOnCloseDefaultIsNoOp() throws Exception {
        CanvasHandler handler = params -> CompletableFuture.completedFuture(new CanvasOpenResult(null, null, null));
        var params = new CanvasCloseParams("sess-1", "ext", "counter", "inst-1", null, null);

        assertNull(handler.onClose(params).get());
    }

    @Test
    void testCanvasExceptionNoHandlerCode() {
        CanvasException ex = CanvasException.noHandler();
        assertEquals("canvas_action_no_handler", ex.getCode());
        assertNotNull(ex.getMessage());
    }

    @Test
    void testCanvasOpenParamsRoundTripsThroughHandler() throws Exception {
        CanvasHandler handler = params -> CompletableFuture
                .completedFuture(new CanvasOpenResult("https://example/" + params.canvasId(), "Counter", "ready"));
        var params = new CanvasOpenParams("sess-1", "ext", "counter", "inst-1", null, null, null);

        CanvasOpenResult result = handler.onOpen(params).get();
        assertEquals("https://example/counter", result.url());
        assertEquals("ready", result.status());
    }
}
