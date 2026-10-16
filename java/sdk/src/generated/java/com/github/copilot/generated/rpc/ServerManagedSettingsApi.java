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

    /** API methods for the {@code managedSettings.permissions} sub-namespace. */
    public final ServerManagedSettingsPermissionsApi permissions;

    /** @param caller the RPC transport function */
    ServerManagedSettingsApi(RpcCaller caller) {
        this.caller = caller;
        this.permissions = new ServerManagedSettingsPermissionsApi(caller);
    }

    /**
     * Discovers device-managed settings the way the runtime's own device layer does, and reports the sandbox floor and the load outcome alongside the validated document. This differs from `managedSettings.read` in three ways a policy-enforcing host depends on: a document that validates to an empty object is reported as absent rather than as an empty policy, a discovery error does not suppress a document that still validates, and a failure is reported as a flag rather than only as error text. Hosts that compose the device layer into an effective policy use this, because `managedSettings.read` cannot distinguish "no device policy" from "device policy could not be read" and would silently hand the user's own layer the win.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsReadDeviceResult> readDevice() {
        return caller.invoke("managedSettings.readDevice", java.util.Map.of(), ManagedSettingsReadDeviceResult.class);
    }

    /**
     * Retains the supplied managed layers as this runtime process's startup policy and re-applies the result to every live session. Hosts that own their own short-lived process policy use this, so a guarded mutation is checked against the policy the host actually fetched. Retaining is a whole-process replacement rather than a merge, so a host that already resolved a good policy must not retain a partial one over it. The failure flags are honest inputs rather than assumptions: reporting a fetch or load that failed keeps the layers already retained, and the interim fail-closed state, instead of clearing them.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> retainPolicy(ManagedSettingsRetainPolicyParams params) {
        return caller.invoke("managedSettings.retainPolicy", params, Void.class);
    }

    /**
     * Stamps an effective managed policy's individually-controlled entries onto a user settings document, returning the settings a host should actually act on. The managed layer wins on every plugin spec and marketplace name it names, and leaves everything it does not name as the user wrote it. Hosts use this rather than merging the two documents themselves, because the merge is per entry rather than per document and a whole-document overlay would either discard the user's unmanaged entries or let a user entry outrank a managed one. Omitting the managed layer returns the settings unchanged, which is what an unmanaged host wants. Rejects when either document is present but is not a JSON object rather than treating it as absent, since silently dropping a malformed policy hands the user's layer the win on exactly the entries the policy exists to control.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsApplyControlsResult> applyControls(ManagedSettingsApplyControlsParams params) {
        return caller.invoke("managedSettings.applyControls", params, ManagedSettingsApplyControlsResult.class);
    }

    /**
     * Validates the managed settings a client is supplying for a session it is about to open, and returns them reduced to the keys a client is allowed to set. A client-supplied policy is not the same thing as an administrator's: it may only narrow permissions, so this accepts `permissions` and rejects every other key outright rather than ignoring it. Rejecting is the point — a host that silently dropped an unsupported key would report a policy as applied while running without it. Hosts call this before opening the session rather than letting the open fail, so a malformed policy is reported against the request that carried it. The runtime applies the same rule again when the session starts, so this cannot be used to smuggle a wider document past it. An accepted document that sets nothing comes back as an empty object, which is distinct from supplying nothing at all.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsParseClientValueResult> parseClientValue(ManagedSettingsParseClientValueParams params) {
        return caller.invoke("managedSettings.parseClientValue", params, ManagedSettingsParseClientValueResult.class);
    }

    /**
     * Composes the supplied server, device/MDM, device sandbox floor and policy-helper layers into the single effective managed policy a host should enforce, resolving each key across the layers by the runtime's own precedence rules. Hosts that assemble managed layers themselves use this so their precedence matches the runtime's exactly instead of reimplementing it. The composition is not a pure function of its inputs: an advisory sandbox force-on that arrived over the server channel is declined when this host cannot run a sandbox, so the result reflects the runtime host's capabilities, and the returned settings carry `sandboxAdvisoryFloorDeclined: true` when that happened. Enforced channels (device/MDM and policy helper) are never declined, and a server force-on carrying `failIfUnavailable` is preserved so the session fails loudly instead of quietly running unsandboxed. Rejects when a supplied layer is present but is not a JSON object, rather than treating it as unmanaged, because silently dropping a malformed policy would hand the user's own layer the win on exactly the keys the policy exists to control.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsResolveEffectiveValueResult> resolveEffectiveValue(ManagedSettingsResolveEffectiveValueParams params) {
        return caller.invoke("managedSettings.resolveEffectiveValue", params, ManagedSettingsResolveEffectiveValueResult.class);
    }

    /**
     * Decides what a host must do to its bypass-permissions state after a managed-policy resolution lands, given the state already in force and the account now signed in. A host holds two pieces of state — which layer disabled bypass mode, and for a server-delivered disable, the account it was pinned to — and cannot decide the next step from the new policy alone. Three rules make that so, and each one is a control rather than a preference. A device/MDM disable outranks the account, so a later server answer never lifts it. An unknown policy, which is what a failed fetch or a refusal this build cannot enforce yields, preserves whatever is in force instead of reverting to permissive. And a server disable is lifted only when the account it was pinned to is no longer the signed-in one, which is what makes signing in to an unrestricted account recover without a restart while a reconnect on the same restricted account does not. Hosts call this rather than reimplementing it because getting the pinning backwards either strands a disable on an account that never carried one or drops one that still applies. `setDisabledState` is omitted when the host must leave its stored flag untouched, which is distinct from being told to set it to false.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsResolveBypassDisableTransitionResult> resolveBypassDisableTransition(ManagedSettingsResolveBypassDisableTransitionParams params) {
        return caller.invoke("managedSettings.resolveBypassDisableTransition", params, ManagedSettingsResolveBypassDisableTransitionResult.class);
    }

    /**
     * Resolves managed (enterprise policy) settings for a host from every layer at once: the account's server-delivered policy, the device/MDM layer, and a platform policy helper. Hosts use this instead of fetching and composing the layers themselves, because the per-layer precedence and the fail-closed rules that apply when a layer cannot be confirmed are the runtime's own and must match the policy a session will later enforce. It does not retain or apply anything to process-global policy state, so a host may call it before any session exists — which is the point, since plugin activation and one-shot commands resolve policy before a session is available to carry it. The returned per-layer outcome flags are load-bearing rather than diagnostic: a host must distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case. Reports its diagnostics in `logMessages` rather than logging them, so a host that runs before a log sink exists still sees them.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsIngestResult> ingest(ManagedSettingsIngestParams params) {
        return caller.invoke("managedSettings.ingest", params, ManagedSettingsIngestResult.class);
    }

    /**
     * Resolves the managed (enterprise policy) settings a host must enforce from startup, and judges the account against the enterprise sign-in pin in the same pass. This composes the layers the way `managedSettings.ingest` does and then answers the two questions a host cannot answer for itself: which fail-closed floors apply when a layer could not be confirmed, and whether this credential is allowed to sign in at all. The sign-in verdict is the reason a host must not assemble this itself from `managedSettings.ingest` — a pinned organization is an access control, and a host that composed the layers but skipped the pin would let a developer sign in with a personal or another enterprise's account and escape every control that keys off the signed-in identity. The verdict is computed on every resolution, including one that is not retained, so declining to apply policy never widens access. Set `retainAndApply` to keep the resolved policy as this runtime process's startup policy; leave it false to resolve without side effects.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsResolveStartupPolicyResult> resolveStartupPolicy(ManagedSettingsResolveStartupPolicyParams params) {
        return caller.invoke("managedSettings.resolveStartupPolicy", params, ManagedSettingsResolveStartupPolicyResult.class);
    }

    /**
     * Resolves the managed `remoteControl` policy for an account on its own, for a host deciding whether a remote-control session may start. Composes the same layers as `managedSettings.ingest` and returns only that one key, so a host does not have to know which layer won. An absent policy means the account is unmanaged for this capability and the host may proceed; that default is safe only while absence really does mean "no policy", so a service refusal that says an account policy exists which this client is too old to enforce resolves to an explicit disabled policy instead of to absence. A resolution that cannot be completed is rejected rather than answered as absence, for the same reason.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ManagedSettingsLoadRemoteControlResult> loadRemoteControl(ManagedSettingsLoadRemoteControlParams params) {
        return caller.invoke("managedSettings.loadRemoteControl", params, ManagedSettingsLoadRemoteControlResult.class);
    }

    /**
     * Evaluates the device-channel enterprise sign-in pin (MDM plist, registry, or managed-settings file) against a credential, without resolving any server-delivered policy. This is the gate an entry point runs before it branches, and the only one that can refuse a developer's first sign-in: it is local, needs no credential, and a server-delivered pin only ever reaches accounts already inside the organization. It fails closed by construction — a discovery that cannot determine whether a pin exists refuses rather than allows, because a control that fails open under a triggerable condition is not a control. Hosts that also want the server channel should call `managedSettings.resolveStartupPolicy`, which folds both channels into one verdict.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ForceLoginOrgsDecision> resolveDeviceForceLoginOrgsGate(ManagedSettingsResolveDeviceForceLoginOrgsGateParams params) {
        return caller.invoke("managedSettings.resolveDeviceForceLoginOrgsGate", params, ForceLoginOrgsDecision.class);
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
