/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code queue} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionQueueApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionQueueApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionQueuePendingItemsResult> pendingItems() {
        return caller.invoke("session.queue.pendingItems", java.util.Map.of("sessionId", this.sessionId), SessionQueuePendingItemsResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionQueueRemoveMostRecentResult> removeMostRecent() {
        return caller.invoke("session.queue.removeMostRecent", java.util.Map.of("sessionId", this.sessionId), SessionQueueRemoveMostRecentResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> clear() {
        return caller.invoke("session.queue.clear", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

}
