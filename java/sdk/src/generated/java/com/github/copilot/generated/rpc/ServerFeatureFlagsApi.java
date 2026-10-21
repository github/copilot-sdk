/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code featureFlags} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerFeatureFlagsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerFeatureFlagsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Resolves named gates against this installation's cached online-experiment assignment, for the account the runtime is already authenticated as. Unlike `featureFlags.resolve`, which layers static availability with the populations supplied, this reads the experiment assignment the runtime cached for its own current authentication, so a launch can honor a rollout before any session exists. The account is never taken from the request: naming one would read another account's cached assignments. The cached assignment answers immediately and any network refresh happens behind it, so this call does not wait on the network. Not available to an extension, which cannot carry the user's consent to read their assignment.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Map<String, Boolean>> resolveStartupGates(FeatureFlagsResolveStartupGatesParams params) {
        return caller.invoke("featureFlags.resolveStartupGates", params, RpcMapper.INSTANCE.getTypeFactory().constructMapType(Map.class, String.class, Boolean.class));
    }

    /**
     * Lists every feature flag this runtime declares, with the population each one is declared for, its experimental description, and the online-experiment flag it maps to. This is the static catalog: it describes how each flag is defined, not whether it is on for anyone. Use `featureFlags.resolve` for static values with caller overrides, not active runtime values or experiment assignments.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<FeatureFlagsMetadataResult> listMetadata() {
        return caller.invoke("featureFlags.listMetadata", java.util.Map.of(), FeatureFlagsMetadataResult.class);
    }

    /**
     * Computes static feature flags from the supplied population and overrides without changing runtime state or verifying audience membership. Starts from declared availability, then applies direct feature-flag environment values, user settings, and explicit `flagOverrides`. It does not apply `COPILOT_EXP_*` experiment assignments, authenticated startup gates, or active runtime state. Settings include `enabledFeatureFlags`, the legacy `feature_flags.enabled` list, and supported convenience settings. Supply `baseFlags` to use an existing flag set instead of declared availability. Omit `isStaffGitHub` or `isStaffMicrosoft` when membership was never recorded: a legacy staff account then counts as both populations.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Map<String, Boolean>> resolve(FeatureFlagsResolveParams params) {
        return caller.invoke("featureFlags.resolve", params, RpcMapper.INSTANCE.getTypeFactory().constructMapType(Map.class, String.class, Boolean.class));
    }

}
