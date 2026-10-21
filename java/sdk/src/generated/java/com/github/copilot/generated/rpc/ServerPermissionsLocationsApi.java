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
 * API methods for the {@code permissions.locations} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerPermissionsLocationsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerPermissionsLocationsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Drops stored per-location permission grants whose directory no longer exists, and reports which location keys were removed. Runs against the permission store under the runtime's own Copilot home, the same store `session.permissions.locations.resolve` reads. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract; it is migration plumbing, not a surface consumers are meant to depend on. It can become public once an SDK consumer needs to prune its own permission store.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<LocationPermissionsCleanupResult> cleanupOrphaned() {
        return caller.invoke("permissions.locations.cleanupOrphaned", java.util.Map.of(), LocationPermissionsCleanupResult.class);
    }

}
