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
 * API methods for the {@code sessionFs} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSessionFsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSessionFsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider. A registered provider is authoritative for path interpretation and filesystem facts used by workspace permission validation. Paths are interpreted lexically; home-relative paths (`~` and `~/...`) and Windows drive-relative paths such as `C:foo` are unsupported. Until provider-side canonicalization is supported, providers must not expose symlinks inside allowed roots that escape those roots.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionFsSetProviderResult> setProvider(SessionFsSetProviderParams params) {
        return caller.invoke("sessionFs.setProvider", params, SessionFsSetProviderResult.class);
    }

}
