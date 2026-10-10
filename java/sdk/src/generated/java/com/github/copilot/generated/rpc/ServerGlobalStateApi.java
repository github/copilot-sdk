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
 * API methods for the {@code globalState} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerGlobalStateApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerGlobalStateApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reads the host's machine-wide state: which plugins are installed and the one-off flags and timestamps that record what the user has already been shown or migrated. This is the state that outlives a single session and a single workspace, so a host reads it to decide whether to run a first-launch step, offer an onboarding prompt, or skip one it has already completed. The stored credentials are deliberately not part of this result; a caller that needs an authenticated identity asks the account methods for it instead. Reading is non-destructive and every field is optional, because a fresh install has recorded nothing yet.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GlobalStateLoadResult> load() {
        return caller.invoke("globalState.load", java.util.Map.of(), GlobalStateLoadResult.class);
    }

    /**
     * Reads the host's machine-wide state exactly as `globalState.load` does, but from a caller-supplied configuration directory instead of the one the server resolved for itself. Use this when a consumer scopes a session to its own Copilot home — the SDK's per-session `configDir` override — so the state read matches the directory that session actually uses. An absent or empty `configDir` resolves the server's own home, making this identical to `globalState.load`. The stored credentials are omitted here for the same reason they are omitted from `globalState.load`: a caller that needs an authenticated identity asks the account methods instead, so pointing this at another directory cannot be used to read the credentials kept in it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GlobalStateLoadResult> loadForConfigDir(GlobalStateLoadForConfigDirParams params) {
        return caller.invoke("globalState.loadForConfigDir", params, GlobalStateLoadResult.class);
    }

    /**
     * Records one top-level key in the host's machine-wide state, the counterpart to `globalState.load`. A host calls this to remember that it has shown an onboarding step, asked a one-off question, or completed a migration, so the next run can skip it. Only the named key is replaced and the rest of the document is preserved, which lets two writers record different flags without overwriting each other; passing no value removes the key instead. Only the keys a host records itself are writable: `appTipShown`, `askedSetupTerminals`, `autoFeedbackLastPromptedAt`, `firstLaunchAt`, `recentModelIds`, `sandboxCredentialProxyCaDeclined` and `sandboxOnboardingShown`. Every other key is refused, including `installedPlugins`, the stored credentials, `trustedFolders`, the staff flags and the signed-in accounts. Plugin enablement must use the plugin APIs, which apply repository and managed-policy checks.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> writeKey(GlobalStateWriteKeyParams params) {
        return caller.invoke("globalState.writeKey", params, Void.class);
    }

}
