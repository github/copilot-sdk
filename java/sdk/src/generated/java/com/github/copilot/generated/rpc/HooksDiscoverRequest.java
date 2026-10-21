/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Optional project paths and host-exclusion behavior for server-scoped hook discovery.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class HooksDiscoverRequest {

    /** Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion. */
    @JsonProperty("projectPaths")
    private List<String> projectPaths;

    /** When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings. */
    @JsonProperty("excludeHostHooks")
    private Boolean excludeHostHooks;

    /** Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without ambient discovery or repository file-hook enablement, subject to managed/hook disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots. */
    @JsonProperty("hookDirectories")
    private List<String> hookDirectories;

    /**
     * Creates a request with its required inputs.
     */
    public HooksDiscoverRequest() {
    }

    /**
     * Returns the {@code projectPaths} property.
     *
     * @return Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion.
     */
    public List<String> getProjectPaths() {
        return projectPaths;
    }

    /**
     * Returns the {@code excludeHostHooks} property.
     *
     * @return When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings.
     */
    public Boolean getExcludeHostHooks() {
        return excludeHostHooks;
    }

    /**
     * Returns the {@code hookDirectories} property.
     *
     * @return Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without ambient discovery or repository file-hook enablement, subject to managed/hook disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
     */
    public List<String> getHookDirectories() {
        return hookDirectories;
    }

    /**
     * Sets the {@code projectPaths} property.
     *
     * @param value Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion.
     * @return this request
     */
    public HooksDiscoverRequest setProjectPaths(List<String> value) {
        this.projectPaths = value;
        return this;
    }

    /**
     * Sets the {@code excludeHostHooks} property.
     *
     * @param value When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings.
     * @return this request
     */
    public HooksDiscoverRequest setExcludeHostHooks(Boolean value) {
        this.excludeHostHooks = value;
        return this;
    }

    /**
     * Sets the {@code hookDirectories} property.
     *
     * @param value Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without ambient discovery or repository file-hook enablement, subject to managed/hook disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
     * @return this request
     */
    public HooksDiscoverRequest setHookDirectories(List<String> value) {
        this.hookDirectories = value;
        return this;
    }
}
