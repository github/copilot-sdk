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
 * API methods for the {@code customizations} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCustomizationsApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionCustomizationsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * For local sessions, reconciles repository context and discovered instructions, plugins, skills, agents, hooks, MCP servers, and extensions after files appear or change under the working directory. Independent component failures are returned in outcomes and errors; a rejected call can have partially applied earlier steps. Remote sessions must reload on their agent host instead. The model-facing context is rebuilt on the next turn.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<CustomizationsReloadResult> reload() {
        return caller.invoke("session.customizations.reload", java.util.Map.of("sessionId", this.sessionId), CustomizationsReloadResult.class);
    }

}
