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
 * API methods for the {@code managedSettings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerManagedSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerManagedSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Discovers device-managed settings from production MDM and managed-file sources, validates them against the runtime-owned managed-settings schema, and returns the canonical JSON without requiring a session. `managedSettings.resolve` returns the same device settings together with the account's server policy.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsReadResult> read() {
        return caller.invoke("managedSettings.read", java.util.Map.of(), ManagedSettingsReadResult.class);
    }

    /**
     * Force-refreshes enterprise managed settings for every account: wipes the persistent server-policy cache (the whole `<cacheHome>/managed-settings` directory) and drops this runtime process's in-memory retained server policy. It does not itself fetch policy — the effect is that the next time a session resolves managed settings for an account, that resolution re-fetches the account's org policy from the network instead of serving a cached response. Note that `managedSettings.read` returns only device/MDM settings and never triggers the account server-policy fetch, so a host implementing "sync account policy" should call `managedSettings.resolve` or start a fresh session resolution rather than treat a subsequent `managedSettings.read` as the refreshed org policy. Mirrors the invalidation a sign-out performs, broadened from the one signing-out account to all of them; device/MDM layers describe the machine, not the account, and are left untouched. Rejects if the on-disk cache cannot be removed.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> clearCache() {
        return caller.invoke("managedSettings.clearCache", java.util.Map.of(), Void.class);
    }

    /**
     * Resolves the effective enterprise managed settings without a session, from the device channel and, when an account is available, the account's server policy through the same per-account cache sessions use. A cached server policy less than an hour old is used without a fetch; otherwise the policy is fetched, and when the fetch fails a cached policy up to 24 hours old is used instead, unless `forceRemoteSettingsRefresh` requires a live fetch. With no account requested or signed in, it reports device policy only; signing out removes the account's cached policy. It can fetch server policy over the network when the cache is stale, so call it off latency-critical paths such as startup rather than before listing models. The policy helper is not run. `layers` lists each channel's document before merging, and `values` and `meta` carry typed effective values and their lock state for the keys typed so far.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsResolveResult> resolve() {
        return resolve(null);
    }

    /**
     * Resolves the effective enterprise managed settings without a session, from the device channel and, when an account is available, the account's server policy through the same per-account cache sessions use. A cached server policy less than an hour old is used without a fetch; otherwise the policy is fetched, and when the fetch fails a cached policy up to 24 hours old is used instead, unless `forceRemoteSettingsRefresh` requires a live fetch. With no account requested or signed in, it reports device policy only; signing out removes the account's cached policy. It can fetch server policy over the network when the cache is stale, so call it off latency-critical paths such as startup rather than before listing models. The policy helper is not run. `layers` lists each channel's document before merging, and `values` and `meta` carry typed effective values and their lock state for the keys typed so far.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsResolveResult> resolve(ManagedSettingsResolveParams params) {
        return caller.invoke("managedSettings.resolve", params == null ? java.util.Map.of() : params, ManagedSettingsResolveResult.class);
    }

    /**
     * Returns the managed-settings authoring JSON schema with descriptive `x-composition` annotations aligned with the shared settings-engine vocabulary. These annotations are not a complete runtime composition contract: model, effortLevel, and contextTier remain coupled. Use `managedSettings.compose` for the runtime's effective result. Performs no I/O.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsSchemaResult> schema() {
        return caller.invoke("managedSettings.schema", java.util.Map.of(), ManagedSettingsSchemaResult.class);
    }

    /**
     * Validates a candidate managed-settings document the way the runtime validates delivered policy, without applying it. Reports errors that would reject the document, warnings for content the runtime ignores, and the canonical document it would apply. Document text nested more than 64 levels deep is rejected. Performs no I/O.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsValidateResult> validate(ManagedSettingsValidateParams params) {
        return caller.invoke("managedSettings.validate", params, ManagedSettingsValidateResult.class);
    }

    /**
     * Merges candidate managed-settings documents for the device, server, and policy-helper channels into the effective settings the runtime would enforce on this host, using the same precedence and composition rules as live resolution, without applying them. Like live resolution, a server's advisory sandbox force-enable is declined on a host that cannot run the sandbox. Does not fetch policy or read policy files, but may perform blocking OS or subprocess probes for sandbox support. Preview documents are limited to 1 MiB and 64 levels of nesting.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsComposeResult> compose(ManagedSettingsComposeParams params) {
        return caller.invoke("managedSettings.compose", params, ManagedSettingsComposeResult.class);
    }

}
