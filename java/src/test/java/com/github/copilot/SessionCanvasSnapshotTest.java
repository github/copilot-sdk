/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.CanvasOpenedAvailability;
import com.github.copilot.generated.SessionCanvasClosedEvent;
import com.github.copilot.generated.SessionCanvasClosedEvent.SessionCanvasClosedEventData;
import com.github.copilot.generated.SessionCanvasOpenedEvent;
import com.github.copilot.generated.SessionCanvasOpenedEvent.SessionCanvasOpenedEventData;
import com.github.copilot.generated.rpc.CanvasInstanceAvailability;
import com.github.copilot.generated.rpc.OpenCanvasInstance;
import com.github.copilot.rpc.CreateSessionResponse;
import com.github.copilot.rpc.ResumeSessionResponse;

/**
 * Unit tests for the in-memory open-canvases snapshot maintained by
 * {@link CopilotSession}.
 * <p>
 * These are pure unit tests that don't require the Copilot CLI. They drive the
 * package-private {@code dispatchEvent} hook directly and assert the resulting
 * snapshot exposed by {@link CopilotSession#getOpenCanvases()}.
 */
public class SessionCanvasSnapshotTest {

    private CopilotSession session;

    @BeforeEach
    void setup() throws Exception {
        var constructor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        constructor.setAccessible(true);
        session = constructor.newInstance("test-session-id", null, null);
    }

    @Test
    void startsEmpty() {
        assertTrue(session.getOpenCanvases().isEmpty());
    }

    @Test
    void openedUpsertsCanvases() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));
        session.dispatchEvent(openedEvent("inst-2", "canvas-b", CanvasOpenedAvailability.READY));

        var canvases = session.getOpenCanvases();
        assertEquals(2, canvases.size());
        assertEquals(List.of("inst-1", "inst-2"), canvases.stream().map(OpenCanvasInstance::instanceId).toList());
        assertEquals(CanvasInstanceAvailability.READY, canvases.get(0).availability());
    }

    @Test
    void closedRemovesMatchingCanvas() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));
        session.dispatchEvent(openedEvent("inst-2", "canvas-b", CanvasOpenedAvailability.READY));

        session.dispatchEvent(closedEvent("inst-1"));

        var canvases = session.getOpenCanvases();
        assertEquals(1, canvases.size());
        assertEquals("inst-2", canvases.get(0).instanceId());
    }

    @Test
    void closedForAbsentInstanceIsNoOp() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));

        session.dispatchEvent(closedEvent("does-not-exist"));

        var canvases = session.getOpenCanvases();
        assertEquals(1, canvases.size());
        assertEquals("inst-1", canvases.get(0).instanceId());
    }

    @Test
    void closedWithEmptyInstanceIdIsNoOp() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));

        session.dispatchEvent(closedEvent(""));
        session.dispatchEvent(closedEvent(null));

        var canvases = session.getOpenCanvases();
        assertEquals(1, canvases.size());
        assertEquals("inst-1", canvases.get(0).instanceId());
    }

    @Test
    void openedWithMissingRequiredFieldsIsIgnored() {
        session.dispatchEvent(openedEvent("", "canvas-a", CanvasOpenedAvailability.READY));
        session.dispatchEvent(openedEvent("inst-1", "", CanvasOpenedAvailability.READY));
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", null));

        assertTrue(session.getOpenCanvases().isEmpty());
    }

    @Test
    void staleReemitReplacesInsteadOfDuplicating() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));

        // Provider unregister re-emits the same instance as "stale".
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.STALE));

        var canvases = session.getOpenCanvases();
        assertEquals(1, canvases.size());
        assertEquals(CanvasInstanceAvailability.STALE, canvases.get(0).availability());
    }

    @Test
    void getOpenCanvasesReturnsImmutableCopy() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));

        var canvases = session.getOpenCanvases();
        assertThrows(UnsupportedOperationException.class, () -> canvases.add(new OpenCanvasInstance("x", "ext", null,
                "c", null, null, null, null, null, CanvasInstanceAvailability.READY)));

        // Mutating the returned copy must not affect the session snapshot.
        assertEquals(1, session.getOpenCanvases().size());
    }

    @Test
    void setOpenCanvasesSeedsAndFiltersNulls() {
        var seed = new java.util.ArrayList<OpenCanvasInstance>();
        seed.add(new OpenCanvasInstance("inst-1", "ext", null, "canvas-a", null, null, null, null, null,
                CanvasInstanceAvailability.READY));
        seed.add(null);
        seed.add(new OpenCanvasInstance("inst-2", "ext", null, "canvas-b", null, null, null, null, null,
                CanvasInstanceAvailability.STALE));

        session.setOpenCanvases(seed);

        var canvases = session.getOpenCanvases();
        assertEquals(2, canvases.size());
        assertEquals(List.of("inst-1", "inst-2"), canvases.stream().map(OpenCanvasInstance::instanceId).toList());
    }

    @Test
    void setOpenCanvasesWithNullClears() {
        session.dispatchEvent(openedEvent("inst-1", "canvas-a", CanvasOpenedAvailability.READY));

        session.setOpenCanvases(null);

        assertTrue(session.getOpenCanvases().isEmpty());
    }

    @Test
    void createSessionResponseDeserializesOpenCanvases() throws Exception {
        ObjectMapper mapper = JsonRpcClient.getObjectMapper();
        String json = """
                {
                  "sessionId": "abc",
                  "workspacePath": "/tmp/ws",
                  "capabilities": {},
                  "openCanvases": [
                    { "instanceId": "inst-1", "extensionId": "ext", "canvasId": "canvas-a", "availability": "ready" }
                  ]
                }
                """;

        CreateSessionResponse response = mapper.readValue(json, CreateSessionResponse.class);

        assertNotNull(response.openCanvases());
        assertEquals(1, response.openCanvases().size());
        assertEquals("inst-1", response.openCanvases().get(0).instanceId());
        assertEquals(CanvasInstanceAvailability.READY, response.openCanvases().get(0).availability());
    }

    @Test
    void resumeSessionResponseDeserializesOpenCanvases() throws Exception {
        ObjectMapper mapper = JsonRpcClient.getObjectMapper();
        String json = """
                {
                  "sessionId": "abc",
                  "openCanvases": [
                    { "instanceId": "inst-1", "extensionId": "ext", "canvasId": "canvas-a", "availability": "stale" }
                  ]
                }
                """;

        ResumeSessionResponse response = mapper.readValue(json, ResumeSessionResponse.class);

        assertNotNull(response.openCanvases());
        assertEquals(1, response.openCanvases().size());
        assertEquals(CanvasInstanceAvailability.STALE, response.openCanvases().get(0).availability());
    }

    private static SessionCanvasOpenedEvent openedEvent(String instanceId, String canvasId,
            CanvasOpenedAvailability availability) {
        var event = new SessionCanvasOpenedEvent();
        event.setData(new SessionCanvasOpenedEventData(instanceId, "ext-id", "Ext Name", canvasId, "Title", "ok", null,
                null, null, availability));
        return event;
    }

    private static SessionCanvasClosedEvent closedEvent(String instanceId) {
        var event = new SessionCanvasClosedEvent();
        event.setData(new SessionCanvasClosedEventData(instanceId, "ext-id", "canvas-a"));
        return event;
    }
}
