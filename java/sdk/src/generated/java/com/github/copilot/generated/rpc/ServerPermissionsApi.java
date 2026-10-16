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
 * API methods for the {@code permissions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerPermissionsApi {

    private final RpcCaller caller;

    /** API methods for the {@code permissions.locations} sub-namespace. */
    final ServerPermissionsLocationsApi locations;
    /** API methods for the {@code permissions.folderTrust} sub-namespace. */
    final ServerPermissionsFolderTrustApi folderTrust;

    /** @param caller the RPC transport function */
    ServerPermissionsApi(RpcCaller caller) {
        this.caller = caller;
        this.locations = new ServerPermissionsLocationsApi(caller);
        this.folderTrust = new ServerPermissionsFolderTrustApi(caller);
    }

    /**
     * Interprets a verbatim managed `permissions` slice into the three policy questions a host asks before granting elevated permissions: whether bypass-permissions mode is forbidden, whether a policy exists that this build could not determine, and whether the layer supplies permission rules the session must enforce. An absent or empty slice means no managed policy applies. The runtime and its permission service share one policy type, so a new managed permission setting is interpreted in exactly one place; a host that reimplements the reading would silently diverge the first time that set grows. Hosts must fail closed on an undetermined policy rather than treating it as permission granted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<PermissionsEvaluateManagedPolicyResult> evaluateManagedPolicy(PermissionsEvaluateManagedPolicyParams params) {
        return caller.invoke("permissions.evaluateManagedPolicy", params, PermissionsEvaluateManagedPolicyResult.class);
    }

}
