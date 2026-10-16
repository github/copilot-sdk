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
 * API methods for the {@code skills.installations} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSkillsInstallationsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSkillsInstallationsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Lists owned verified Agent Finder Skill installations for the selected existing session. Listing is never gated by the Skill-install feature flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> list(SkillsInstallationsListParams params) {
        return caller.invoke("skills.installations.list", params, SkillInstallationManagementResult.class);
    }

    /**
     * Reconciles interrupted owned Skill installation work for the selected existing session, then inspects owned inventory. Recovery is never gated by the Skill-install feature flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> recover(SkillsInstallationsRecoverParams params) {
        return caller.invoke("skills.installations.recover", params, SkillInstallationManagementResult.class);
    }

    /**
     * Inspects a known Skill installation operation on its original runtime connection. Status is never gated by the Skill-install feature flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> status(SkillsInstallationsStatusParams params) {
        return caller.invoke("skills.installations.status", params, SkillInstallationManagementResult.class);
    }

    /**
     * Requests cancellation of a known Skill installation operation before commit. Already-started durable work requires recovery instead of silent replay.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> cancel(SkillsInstallationsCancelParams params) {
        return caller.invoke("skills.installations.cancel", params, SkillInstallationManagementResult.class);
    }

    /**
     * Atomically persists enablement for one owned Agent Finder Skill and reconciles the selected bound session. Enablement is installation-scoped by receipt identity and is never gated by the Skill-install feature flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> setEnabled(SkillsInstallationsSetEnabledParams params) {
        return caller.invoke("skills.installations.setEnabled", params, SkillInstallationManagementResult.class);
    }

}
