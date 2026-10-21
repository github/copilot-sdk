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
 * API methods for the {@code contentExclusion} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerContentExclusionApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerContentExclusionApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Validates candidate content-exclusion policies and returns them normalized, without starting a session. This runs the same parser the runtime's own policy enforcement uses, so a host cannot accept a policy set the runtime would later reject, nor reject one it would have honored. Call it when policies arrive from outside the runtime — a command-line flag, a configuration file, a host's own settings — and must be checked before a session exists to apply them to. A malformed document fails with a message prefixed `Invalid JSON: `; a well-formed document that violates the policy schema fails with the offending field paths.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ContentExclusionValidatePoliciesResult> validatePolicies(ContentExclusionValidatePoliciesParams params) {
        return caller.invoke("contentExclusion.validatePolicies", params, ContentExclusionValidatePoliciesResult.class);
    }

}
