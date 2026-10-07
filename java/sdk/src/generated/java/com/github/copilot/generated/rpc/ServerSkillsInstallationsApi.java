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
     * Inventory request under an explicitly selected existing session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> list(SkillsInstallationsListParams params) {
        return caller.invoke("skills.installations.list", params, SkillInstallationManagementResult.class);
    }

    /**
     * Inventory request under an explicitly selected existing session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> recover(SkillsInstallationsRecoverParams params) {
        return caller.invoke("skills.installations.recover", params, SkillInstallationManagementResult.class);
    }

    /**
     * Existing-operation control. A new session selector is deliberately not accepted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> status(SkillsInstallationsStatusParams params) {
        return caller.invoke("skills.installations.status", params, SkillInstallationManagementResult.class);
    }

    /**
     * Existing-operation control. A new session selector is deliberately not accepted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> cancel(SkillsInstallationsCancelParams params) {
        return caller.invoke("skills.installations.cancel", params, SkillInstallationManagementResult.class);
    }

    /**
     * Persisted enablement update for one owned Skill installation.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> setEnabled(SkillsInstallationsSetEnabledParams params) {
        return caller.invoke("skills.installations.setEnabled", params, SkillInstallationManagementResult.class);
    }

}
