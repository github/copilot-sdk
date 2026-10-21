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
 * Optional project paths to include when enumerating agent discovery directories.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AgentsGetDiscoveryPathsRequest {

    /** Optional list of project directory paths. When omitted or empty, only the user-level directory is returned. */
    @JsonProperty("projectPaths")
    private List<String> projectPaths;

    /** When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`). */
    @JsonProperty("excludeHostAgents")
    private Boolean excludeHostAgents;

    /** Explicit discovery-only agent roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent. */
    @JsonProperty("agentDirectories")
    private List<String> agentDirectories;

    /**
     * Creates a request with its required inputs.
     */
    public AgentsGetDiscoveryPathsRequest() {
    }

    /**
     * Returns the {@code projectPaths} property.
     *
     * @return Optional list of project directory paths. When omitted or empty, only the user-level directory is returned.
     */
    public List<String> getProjectPaths() {
        return projectPaths;
    }

    /**
     * Returns the {@code excludeHostAgents} property.
     *
     * @return When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`).
     */
    public Boolean getExcludeHostAgents() {
        return excludeHostAgents;
    }

    /**
     * Returns the {@code agentDirectories} property.
     *
     * @return Explicit discovery-only agent roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
     */
    public List<String> getAgentDirectories() {
        return agentDirectories;
    }

    /**
     * Sets the {@code projectPaths} property.
     *
     * @param value Optional list of project directory paths. When omitted or empty, only the user-level directory is returned.
     * @return this request
     */
    public AgentsGetDiscoveryPathsRequest setProjectPaths(List<String> value) {
        this.projectPaths = value;
        return this;
    }

    /**
     * Sets the {@code excludeHostAgents} property.
     *
     * @param value When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`).
     * @return this request
     */
    public AgentsGetDiscoveryPathsRequest setExcludeHostAgents(Boolean value) {
        this.excludeHostAgents = value;
        return this;
    }

    /**
     * Sets the {@code agentDirectories} property.
     *
     * @param value Explicit discovery-only agent roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
     * @return this request
     */
    public AgentsGetDiscoveryPathsRequest setAgentDirectories(List<String> value) {
        this.agentDirectories = value;
        return this;
    }
}
