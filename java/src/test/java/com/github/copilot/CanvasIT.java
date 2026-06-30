/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.CanvasAction;
import com.github.copilot.generated.rpc.CanvasActionInvokeParams;
import com.github.copilot.generated.rpc.CanvasCloseParams;
import com.github.copilot.generated.rpc.CanvasOpenParams;
import com.github.copilot.generated.rpc.CanvasOpenResult;
import com.github.copilot.generated.rpc.DiscoveredCanvas;
import com.github.copilot.generated.rpc.SessionCanvasActionInvokeParams;
import com.github.copilot.generated.rpc.SessionCanvasActionInvokeResult;
import com.github.copilot.generated.rpc.SessionCanvasListOpenResult;
import com.github.copilot.generated.rpc.SessionCanvasListResult;
import com.github.copilot.generated.rpc.SessionCanvasOpenParams;
import com.github.copilot.generated.rpc.SessionCanvasOpenResult;
import com.github.copilot.rpc.CanvasDeclaration;
import com.github.copilot.rpc.CanvasHandler;
import com.github.copilot.rpc.ExtensionInfo;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Failsafe integration test that exercises the host-side canvas declaration API
 * against the live Copilot CLI via the replay proxy. Mirrors
 * {@code rust/tests/e2e/canvas.rs}.
 * <p>
 * Canvas round-trips make no CAPI (model) calls, so the snapshots under
 * {@code test/snapshots/canvas/} have empty conversations.
 */
class CanvasIT {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    /** Records every inbound canvas callback so tests can assert on them. */
    private static final class RecordingCanvasHandler implements CanvasHandler {
        final List<CanvasOpenParams> openCalls = new CopyOnWriteArrayList<>();
        final List<CanvasActionInvokeParams> actionCalls = new CopyOnWriteArrayList<>();
        final List<CanvasCloseParams> closeCalls = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<CanvasOpenResult> onOpen(CanvasOpenParams params) {
            openCalls.add(params);
            return CompletableFuture.completedFuture(new CanvasOpenResult(
                    "https://example.com/counter/" + params.instanceId(), "Counter " + params.instanceId(), "ready"));
        }

        @Override
        public CompletableFuture<Object> onAction(CanvasActionInvokeParams params) {
            actionCalls.add(params);
            return CompletableFuture.completedFuture(Map.of("newValue", 42));
        }

        @Override
        public CompletableFuture<Void> onClose(CanvasCloseParams params) {
            closeCalls.add(params);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static SessionConfig canvasSessionConfig(CanvasHandler handler) {
        var declaration = new CanvasDeclaration("counter", "Counter", "Tracks a counter value.")
                .setActions(List.of(new CanvasAction("increment", "Increments the counter.", null)));
        return new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setRequestCanvasRenderer(true)
                .setRequestExtensions(true).setExtensionInfo(new ExtensionInfo("java-sdk-tests", "canvas-provider"))
                .setCanvases(List.of(declaration)).setCanvasHandler(handler);
    }

    @Test
    void canvasListDiscoversDeclaredCanvases() throws Exception {
        ctx.configureForTest("canvas", "canvas_list_discovers_declared_canvases");

        try (CopilotClient client = ctx.createClient()) {
            var handler = new RecordingCanvasHandler();
            CopilotSession session = client.createSession(canvasSessionConfig(handler)).get(30, TimeUnit.SECONDS);
            try {
                SessionCanvasListResult result = session.getRpc().canvas.list().get(30, TimeUnit.SECONDS);

                assertNotNull(result.canvases(), "canvases list must not be null");
                assertEquals(1, result.canvases().size());
                DiscoveredCanvas canvas = result.canvases().get(0);
                assertEquals("counter", canvas.canvasId());
                assertEquals("Counter", canvas.displayName());
                assertEquals("Tracks a counter value.", canvas.description());
            } finally {
                session.close();
            }
        }
    }

    @Test
    void canvasOpenRoundTrip() throws Exception {
        ctx.configureForTest("canvas", "canvas_open_round_trip");

        try (CopilotClient client = ctx.createClient()) {
            var handler = new RecordingCanvasHandler();
            CopilotSession session = client.createSession(canvasSessionConfig(handler)).get(30, TimeUnit.SECONDS);
            try {
                SessionCanvasListResult canvasList = session.getRpc().canvas.list().get(30, TimeUnit.SECONDS);
                DiscoveredCanvas canvas = canvasList.canvases().get(0);

                SessionCanvasOpenResult openResult = session.getRpc().canvas.open(new SessionCanvasOpenParams(null,
                        canvas.extensionId(), "counter", "counter-1", Map.of("start", 41))).get(30, TimeUnit.SECONDS);

                assertEquals("counter-1", openResult.instanceId());
                assertEquals("Counter counter-1", openResult.title());
                assertEquals("ready", openResult.status());
                assertEquals("https://example.com/counter/counter-1", openResult.url());

                assertEquals(1, handler.openCalls.size());
                assertEquals("counter", handler.openCalls.get(0).canvasId());
                assertEquals("counter-1", handler.openCalls.get(0).instanceId());

                SessionCanvasListOpenResult openList = session.getRpc().canvas.listOpen().get(30, TimeUnit.SECONDS);
                assertEquals(1, openList.openCanvases().size());
                assertEquals("counter-1", openList.openCanvases().get(0).instanceId());
            } finally {
                session.close();
            }
        }
    }

    @Test
    void canvasInvokeActionRoundTrip() throws Exception {
        ctx.configureForTest("canvas", "canvas_invoke_action_round_trip");

        try (CopilotClient client = ctx.createClient()) {
            var handler = new RecordingCanvasHandler();
            CopilotSession session = client.createSession(canvasSessionConfig(handler)).get(30, TimeUnit.SECONDS);
            try {
                SessionCanvasListResult canvasList = session.getRpc().canvas.list().get(30, TimeUnit.SECONDS);
                DiscoveredCanvas canvas = canvasList.canvases().get(0);

                session.getRpc().canvas
                        .open(new SessionCanvasOpenParams(null, canvas.extensionId(), "counter", "counter-2", Map.of()))
                        .get(30, TimeUnit.SECONDS);

                SessionCanvasActionInvokeResult result = session.getRpc().canvas.action
                        .invoke(new SessionCanvasActionInvokeParams(null, "counter-2", "increment", Map.of("delta", 1)))
                        .get(30, TimeUnit.SECONDS);

                assertNotNull(result.result());
                assertTrue(result.result() instanceof Map, "action result should be a JSON object");
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result.result();
                assertEquals(42, ((Number) resultMap.get("newValue")).intValue());

                assertEquals(1, handler.actionCalls.size());
                CanvasActionInvokeParams actionCall = handler.actionCalls.get(0);
                assertEquals("counter", actionCall.canvasId());
                assertEquals("counter-2", actionCall.instanceId());
                assertEquals("increment", actionCall.actionName());
            } finally {
                session.close();
            }
        }
    }
}
