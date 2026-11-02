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
 * API methods for the {@code sandbox} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSandboxApi {

    private final RpcCaller caller;

    /** API methods for the {@code sandbox.proxyCa} sub-namespace. */
    public final ServerSandboxProxyCaApi proxyCa;
    /** API methods for the {@code sandbox.proxySecret} sub-namespace. */
    final ServerSandboxProxySecretApi proxySecret;
    /** API methods for the {@code sandbox.settings} sub-namespace. */
    final ServerSandboxSettingsApi settings;

    /** @param caller the RPC transport function */
    ServerSandboxApi(RpcCaller caller) {
        this.caller = caller;
        this.proxyCa = new ServerSandboxProxyCaApi(caller);
        this.proxySecret = new ServerSandboxProxySecretApi(caller);
        this.settings = new ServerSandboxSettingsApi(caller);
    }

    /**
     * Raises a user's sandbox settings to an administrator's managed sandbox floor and returns the result a session should actually run under. The floor can only tighten: an enterprise force-on is reflected even when the user's own `enabled` is false, and a user cannot lower a setting the floor raised. The result is normalized through the declared sandbox shape, so a key reorder or an undeclared passthrough key is not mistaken for a policy change by callers that diff two results. A host must call this rather than merge the two layers itself, because reimplementing the direction of the fold is what turns an administrator's floor into a suggestion.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxResolveFlooredConfigResult> resolveFlooredConfig(SandboxResolveFlooredConfigParams params) {
        return caller.invoke("sandbox.resolveFlooredConfig", params, SandboxResolveFlooredConfigResult.class);
    }

    /**
     * Builds the effective filesystem policy one `@`-file picker checks its suggestions against, so Git discovery and grant canonicalization run once for the picker's lifetime instead of on every debounced keystroke. Returns no policy when the sandbox is absent or disabled, which the paired filter reads as "keep every candidate". A host must pair this with `sandbox.filterPickerCandidates` rather than resolve a grant list itself, because the picker's grants are narrower than a file tool's — the same path a built-in tool may read can be one the picker must not offer.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxPreparePickerPolicyResult> preparePickerPolicy(SandboxPreparePickerPolicyParams params) {
        return caller.invoke("sandbox.preparePickerPolicy", params, SandboxPreparePickerPolicyResult.class);
    }

    /**
     * Checks a batch of `@`-file picker suggestions against a policy prepared by `sandbox.preparePickerPolicy` and returns one allow decision per candidate, positionally aligned with the request. Batched deliberately: each check canonicalizes a path component-by-component through the real filesystem, so one call amortizes that over a single hop rather than dozens while the user types. A directory the policy does not itself allow can still be kept as a waypoint to an allowed subtree. An absent policy keeps every candidate.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxFilterPickerCandidatesResult> filterPickerCandidates(SandboxFilterPickerCandidatesParams params) {
        return caller.invoke("sandbox.filterPickerCandidates", params, SandboxFilterPickerCandidatesResult.class);
    }

    /**
     * The sandbox a single named feature actually launches under, which is not the same as the session's sandbox. A feature is routed out of the sandbox by its own flag — `sandboxMcpServers`, `sandboxLspServers` — so an enabled session can still start one server confined and another free. Returns a disabled sandbox rather than an error when the host has no sandbox slice at all, so an absent config and an opted-out feature read alike at the launch site. A host must ask per feature rather than reuse the session config, because reading only `enabled` confines a server the user opted out of, or reports as sandboxed one that is not.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxEffectiveForOptionalResult> resolveForOptional(SandboxResolveForOptionalParams params) {
        return caller.invoke("sandbox.resolveForOptional", params, SandboxEffectiveForOptionalResult.class);
    }

    /**
     * Reports whether this machine can run an OS-level command sandbox at all. A host must ask before it offers sandboxing, because the answer is a property of the machine rather than of any setting: on Linux it depends on bubblewrap being installed, and on Windows on the OS build supporting BaseContainer. Probing it requires spawning a process the first time, so the answer is cached for the life of the runtime. A host that assumes support and enables the sandbox anyway produces a session in which every shell command fails.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Boolean> checkPlatformSupport() {
        return caller.invoke("sandbox.checkPlatformSupport", java.util.Map.of(), Boolean.class);
    }

    /**
     * Reports whether this Windows host's BaseContainer implementation can reach the host's loopback interface directly, which is what a local-network policy needs to avoid routing through the egress proxy. Like `sandbox.checkPlatformSupport`, the answer is a property of the machine's OS build rather than of any setting, and resolving it the first time spawns a platform probe. A host that offers the direct local-network option on a machine that lacks the capability produces a session whose sandboxed commands cannot reach local services. Always false off Windows.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Boolean> checkWindowsHostLoopbackSupport() {
        return caller.invoke("sandbox.checkWindowsHostLoopbackSupport", java.util.Map.of(), Boolean.class);
    }

    /**
     * The explanation a host shows when it refuses an explicit request to turn the sandbox on for a machine that cannot run one. Carries the platform-specific remedy — install bubblewrap, update Windows — and the documentation link, so a host that got `false` from `sandbox.checkPlatformSupport` does not have to compose its own reason and cannot describe the same machine differently from the runtime's own spawn path.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<String> getUnsupportedMessage() {
        return caller.invoke("sandbox.getUnsupportedMessage", java.util.Map.of(), String.class);
    }

    /**
     * The notice a host shows when a stored `sandbox` setting is being ignored for this session because the machine cannot enforce it. Distinct from `sandbox.getUnsupportedMessage`, which refuses an explicit request: this one reports that a setting the user already saved is inert, states that commands will run unsandboxed, and confirms the stored setting itself is left alone. A host that stays silent here leaves the user believing a sandbox is active.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<String> getUnsupportedSettingDisabledNotice() {
        return caller.invoke("sandbox.getUnsupportedSettingDisabledNotice", java.util.Map.of(), String.class);
    }

    /**
     * The warning a host prints at startup when a session asked for a sandbox on a machine that cannot run one. The third of the unsupported notices, and the only one that depends on the policy in force: it names the remedy the reader can actually act on, which is the stored setting for an unmanaged host, an administrator for a managed one, and waiting for the policy to resolve when the sandbox stayed on only because the policy could not be determined. Returns no notice when the machine does support a sandbox, so a host can call it unconditionally on the startup path. A host that composes its own warning tells a managed user to change a setting their policy will overwrite.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxUnsupportedStartupNoticeResult> getUnsupportedStartupNotice(SandboxGetUnsupportedStartupNoticeParams params) {
        return caller.invoke("sandbox.getUnsupportedStartupNotice", params, SandboxUnsupportedStartupNoticeResult.class);
    }

    /**
     * Whether one slash command stays reachable while sandbox enforcement is unavailable and the session is otherwise blocked. The permitted set is deliberately narrow — commands that only report state, and the few that let the user repair or leave the situation — and it is keyed on the arguments as well as the name, so `/sandbox status` is admitted where `/sandbox enable` is not. A host must ask rather than keep its own list: a command this returns false for is one the runtime would refuse anyway, and a divergent list either strands the user with no way out or admits a command that acts on an unenforced session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Boolean> checkEnforcementCommand(SandboxCheckEnforcementCommandParams params) {
        return caller.invoke("sandbox.checkEnforcementCommand", params, Boolean.class);
    }

    /**
     * Returns the documentation URL a host should point a user at when it declines or downgrades a sandbox setting. The runtime owns the address so every surface cites the same page and a host does not hard-code a link that later moves.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxDocsUrlResult> getDocsUrl() {
        return caller.invoke("sandbox.getDocsUrl", java.util.Map.of(), SandboxDocsUrlResult.class);
    }

    /**
     * Reports whether the host running this runtime can run the command sandbox, without starting a session or spawning a sandboxed command.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxGetHostSupportResult> getHostSupport() {
        return caller.invoke("sandbox.getHostSupport", java.util.Map.of(), SandboxGetHostSupportResult.class);
    }

}
