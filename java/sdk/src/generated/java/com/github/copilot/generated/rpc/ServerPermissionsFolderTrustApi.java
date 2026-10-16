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
 * API methods for the {@code permissions.folderTrust} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerPermissionsFolderTrustApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerPermissionsFolderTrustApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reports whether a folder is trusted, reading the persisted trusted-folders list directly rather than through a session. Use this from a host that has no session yet, such as a one-shot command deciding whether to honor a checkout's own settings. It differs from the session-scoped check in two ways that matter: it takes the configuration directory to read the list from, instead of inheriting the session's, and it answers only from the persisted list, so trust a user granted for the lifetime of one session is not visible here. A host that has a session should prefer the session-scoped check, which also covers session-only trust.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<FolderTrustCheckResult> checkTrust(PermissionsFolderTrustCheckTrustParams params) {
        return caller.invoke("permissions.folderTrust.checkTrust", params, FolderTrustCheckResult.class);
    }

}
