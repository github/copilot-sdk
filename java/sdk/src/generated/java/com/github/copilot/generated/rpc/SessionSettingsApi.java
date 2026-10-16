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
 * API methods for the {@code settings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class SessionSettingsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionSettingsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Returns a redacted snapshot of session runtime settings, with secrets and raw feature flags excluded. Internal: the runtime settings shape is a runtime-internal surface and is deliberately kept out of the public SDK, because consumers should not depend on the runtime's internal settings layout. It remains callable in-process and is expected to be reworked as the runtime internals are consolidated.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionSettingsSnapshot> snapshot() {
        return caller.invoke("session.settings.snapshot", java.util.Map.of("sessionId", this.sessionId), SessionSettingsSnapshot.class);
    }

    /**
     * Evaluates a named Rust-owned settings predicate without exposing raw feature flags. Internal: the raw feature-flag names and composition are runtime-internal, so this predicate-evaluation helper is kept out of the public SDK surface and is callable in-process only.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionSettingsEvaluatePredicateResult> evaluatePredicate(SessionSettingsEvaluatePredicateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.settings.evaluatePredicate", _p, SessionSettingsEvaluatePredicateResult.class);
    }

}
