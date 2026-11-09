/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code permissions.additionalDirectories} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPermissionsAdditionalDirectoriesApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionPermissionsAdditionalDirectoriesApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Returns the session's model-facing additional directory ledger, separate from its tool-access grants.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PermissionsAdditionalDirectoriesListResult> list() {
        return caller.invoke("session.permissions.additionalDirectories.list", java.util.Map.of("sessionId", this.sessionId), PermissionsAdditionalDirectoriesListResult.class);
    }

}
