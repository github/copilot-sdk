/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.CanvasActionInvokeParams;
import com.github.copilot.generated.rpc.CanvasCloseParams;
import com.github.copilot.generated.rpc.CanvasOpenParams;
import com.github.copilot.generated.rpc.CanvasOpenResult;

/**
 * Provider-side canvas lifecycle handler.
 * <p>
 * A session installs a single {@code CanvasHandler} via
 * {@link SessionConfig#setCanvasHandler(CanvasHandler)}. The handler receives
 * every inbound {@code canvas.open} / {@code canvas.close} /
 * {@code canvas.action.invoke} request the runtime issues for this session and
 * decides &mdash; typically by inspecting {@link CanvasOpenParams#canvasId()
 * canvasId} &mdash; which application-side canvas should handle the call. The
 * SDK does not maintain a per-canvas registry; multiplexing across declared
 * canvases is the implementor's responsibility.
 * <p>
 * {@link #onClose(CanvasCloseParams)} and
 * {@link #onAction(CanvasActionInvokeParams)} have default implementations, so
 * implementations only need to provide {@link #onOpen(CanvasOpenParams)}. Throw
 * (or fail the returned future with) a {@link CanvasException} to surface a
 * machine-readable error code to the runtime.
 * <p>
 * <strong>Experimental.</strong> Canvas configuration is part of an
 * experimental wire-protocol surface and may change or be removed in future SDK
 * or CLI releases.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public interface CanvasHandler {

    /**
     * Opens a new canvas instance.
     *
     * @param params
     *            the open request from the runtime
     * @return a future that completes with the open result
     */
    CompletableFuture<CanvasOpenResult> onOpen(CanvasOpenParams params);

    /**
     * Handles a non-lifecycle action declared by the canvas.
     * <p>
     * The default implementation fails the returned future with
     * {@link CanvasException#noHandler()}.
     *
     * @param params
     *            the action-invoke request from the runtime
     * @return a future that completes with the JSON-serializable action result
     */
    default CompletableFuture<Object> onAction(CanvasActionInvokeParams params) {
        return CompletableFuture.failedFuture(CanvasException.noHandler());
    }

    /**
     * Notified when a canvas instance is closed by the user, the agent, or the
     * host.
     * <p>
     * The default implementation is a no-op.
     *
     * @param params
     *            the close request from the runtime
     * @return a future that completes when the close has been handled
     */
    default CompletableFuture<Void> onClose(CanvasCloseParams params) {
        return CompletableFuture.completedFuture(null);
    }
}
