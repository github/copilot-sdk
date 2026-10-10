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
 * API methods for the {@code managedSettings.permissions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerManagedSettingsPermissionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerManagedSettingsPermissionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Pure, sessionless evaluation of operations against a supplied permissionsContext. Performs no policy discovery, I/O, prompting, navigation, permission grants, or sandbox changes. Obtain the context from managedSettings.resolve or session.managedSettings.get, or reuse an in-memory context. Results preserve input order and duplicates. Consumers must block deny, obtain approval for ask, and use their normal permission flow for unmanaged. Unknown policy returns failClosed with every operation denied. This is not a substitute for user permissions or other security controls.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsPermissionsEvaluateResult> evaluate(ManagedSettingsPermissionsEvaluateParams params) {
        return caller.invoke("managedSettings.permissions.evaluate", params, ManagedSettingsPermissionsEvaluateResult.class);
    }

}
