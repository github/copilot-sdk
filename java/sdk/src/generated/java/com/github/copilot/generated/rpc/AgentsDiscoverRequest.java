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
 * Optional project paths to include in agent discovery.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AgentsDiscoverRequest {

    /** Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan). */
    @JsonProperty("projectPaths")
    private List<String> projectPaths;

    /** When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments. */
    @JsonProperty("excludeHostAgents")
    private Boolean excludeHostAgents;

    /** Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without ambient discovery, subject to managed/source disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots. */
    @JsonProperty("agentDirectories")
    private List<String> agentDirectories;

    /**
     * Creates a request with its required inputs.
     */
    public AgentsDiscoverRequest() {
    }

    /**
     * Returns the {@code projectPaths} property.
     *
     * @return Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan).
     */
    public List<String> getProjectPaths() {
        return projectPaths;
    }

    /**
     * Returns the {@code excludeHostAgents} property.
     *
     * @return When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments.
     */
    public Boolean getExcludeHostAgents() {
        return excludeHostAgents;
    }

    /**
     * Returns the {@code agentDirectories} property.
     *
     * @return Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without ambient discovery, subject to managed/source disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
     */
    public List<String> getAgentDirectories() {
        return agentDirectories;
    }

    /**
     * Sets the {@code projectPaths} property.
     *
     * @param value Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan).
     * @return this request
     */
    public AgentsDiscoverRequest setProjectPaths(List<String> value) {
        this.projectPaths = value;
        return this;
    }

    /**
     * Sets the {@code excludeHostAgents} property.
     *
     * @param value When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments.
     * @return this request
     */
    public AgentsDiscoverRequest setExcludeHostAgents(Boolean value) {
        this.excludeHostAgents = value;
        return this;
    }

    /**
     * Sets the {@code agentDirectories} property.
     *
     * @param value Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without ambient discovery, subject to managed/source disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
     * @return this request
     */
    public AgentsDiscoverRequest setAgentDirectories(List<String> value) {
        this.agentDirectories = value;
        return this;
    }
}
