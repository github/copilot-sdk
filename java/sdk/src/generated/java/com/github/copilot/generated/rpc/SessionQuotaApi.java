/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code quota} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionQuotaApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionQuotaApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Gets the session's current quota and account projection without making a network request.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQuotaGetResult> get() {
        return caller.invoke("session.quota.get", java.util.Map.of("sessionId", this.sessionId), SessionQuotaGetResult.class);
    }

    /**
     * Refreshes the session's provider model catalog bypassing its cache, folds quota snapshots into session state, and returns the updated projection. Failures leave the last known quota intact.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQuotaRefreshResult> refresh() {
        return caller.invoke("session.quota.refresh", java.util.Map.of("sessionId", this.sessionId), SessionQuotaRefreshResult.class);
    }

    /**
     * Returns and clears the session's pending quota warnings. Reading or refreshing quota does not drain warnings.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<List<QuotaWarningProjection>> takeWarnings() {
        return caller.invoke("session.quota.takeWarnings", java.util.Map.of("sessionId", this.sessionId), RpcMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, QuotaWarningProjection.class));
    }

}
