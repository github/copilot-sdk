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
     * Plans installation of a verified Agent Finder Skill candidate without writing files. The returned review is safe to present to a user and installing always leaves the Skill disabled until separately enabled.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> planInstall(SkillsPlanInstallParams params) {
        return caller.invoke("skills.planInstall", params, SkillInstallationManagementResult.class);
    }

    /**
     * Consumes one verified Skill installation plan, requests explicit human consent through installations.confirm on the original connection, then revalidates and installs the Skill disabled.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationResult> applyInstall(SkillsApplyInstallParams params) {
        return caller.invoke("skills.applyInstall", params, SkillInstallationResult.class);
    }

    /**
     * Prepares a read-only removal plan for an owned verified Agent Finder Skill installation. Uninstall planning is never gated by the Skill-install feature flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationManagementResult> planUninstall(SkillsPlanUninstallParams params) {
        return caller.invoke("skills.planUninstall", params, SkillInstallationManagementResult.class);
    }

    /**
     * Consumes an owned Skill removal plan, requests explicit human consent through installations.confirm, refuses drift, and removes the exact owned files through quarantine.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillInstallationResult> applyUninstall(SkillsApplyUninstallParams params) {
        return caller.invoke("skills.applyUninstall", params, SkillInstallationResult.class);
    }

    /**
     * Discovers skills across global and project sources.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillsDiscoverResult> discover(SkillsDiscoverParams params) {
        return caller.invoke("skills.discover", params, SkillsDiscoverResult.class);
    }

    /**
     * Returns the canonical directories where a client may create skills that the runtime will recognize, including ones that do not exist yet. Project directories become active once created.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SkillsGetDiscoveryPathsResult> getDiscoveryPaths(SkillsGetDiscoveryPathsParams params) {
        return caller.invoke("skills.getDiscoveryPaths", params, SkillsGetDiscoveryPathsResult.class);
    }

}
