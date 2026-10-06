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
 * API methods for the {@code skills} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSkillsApi {

    private final RpcCaller caller;

    /** API methods for the {@code skills.installations} sub-namespace. */
    public final ServerSkillsInstallationsApi installations;
    /** API methods for the {@code skills.config} sub-namespace. */
    public final ServerSkillsConfigApi config;

    /** @param caller the RPC transport function */
    ServerSkillsApi(RpcCaller caller) {
        this.caller = caller;
        this.installations = new ServerSkillsInstallationsApi(caller);
        this.config = new ServerSkillsConfigApi(caller);
    }

    /**
     * Side-effect-free planning of one verified Agent Finder Skill candidate.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> planInstall(SkillsPlanInstallParams params) {
        return caller.invoke("skills.planInstall", params, SkillInstallationManagementResult.class);
    }

    /**
     * Applies exactly one retained verified Skill installation plan.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationResult> applyInstall(SkillsApplyInstallParams params) {
        return caller.invoke("skills.applyInstall", params, SkillInstallationResult.class);
    }

    /**
     * Read-only preparation of one owned Skill removal under fresh selected-session authority.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> planUninstall(SkillsPlanUninstallParams params) {
        return caller.invoke("skills.planUninstall", params, SkillInstallationManagementResult.class);
    }

    /**
     * One-use application of the exact retained Skill removal plan.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationResult> applyUninstall(SkillsApplyUninstallParams params) {
        return caller.invoke("skills.applyUninstall", params, SkillInstallationResult.class);
    }

    /**
     * Optional project paths and additional skill directories to include in discovery.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillsDiscoverResult> discover(SkillsDiscoverParams params) {
        return caller.invoke("skills.discover", params, SkillsDiscoverResult.class);
    }

    /**
     * Optional project paths to enumerate.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillsGetDiscoveryPathsResult> getDiscoveryPaths(SkillsGetDiscoveryPathsParams params) {
        return caller.invoke("skills.getDiscoveryPaths", params, SkillsGetDiscoveryPathsResult.class);
    }

}
