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
 * Optional project paths to enumerate.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SkillsGetDiscoveryPathsRequest {

    /** Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned. */
    @JsonProperty("projectPaths")
    private List<String> projectPaths;

    /** Optional skill scan paths to exclude from discovery. */
    @JsonProperty("ignoredSkillsLocations")
    private List<String> ignoredSkillsLocations;

    /** When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments. */
    @JsonProperty("excludeHostSkills")
    private Boolean excludeHostSkills;

    /** Explicit discovery-only skill roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent. */
    @JsonProperty("skillDirectories")
    private List<String> skillDirectories;

    /**
     * Creates a request with its required inputs.
     */
    public SkillsGetDiscoveryPathsRequest() {
    }

    /**
     * Returns the {@code projectPaths} property.
     *
     * @return Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned.
     */
    public List<String> getProjectPaths() {
        return projectPaths;
    }

    /**
     * Returns the {@code ignoredSkillsLocations} property.
     *
     * @return Optional skill scan paths to exclude from discovery.
     */
    public List<String> getIgnoredSkillsLocations() {
        return ignoredSkillsLocations;
    }

    /**
     * Returns the {@code excludeHostSkills} property.
     *
     * @return When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments.
     */
    public Boolean getExcludeHostSkills() {
        return excludeHostSkills;
    }

    /**
     * Returns the {@code skillDirectories} property.
     *
     * @return Explicit discovery-only skill roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
     */
    public List<String> getSkillDirectories() {
        return skillDirectories;
    }

    /**
     * Sets the {@code projectPaths} property.
     *
     * @param value Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned.
     * @return this request
     */
    public SkillsGetDiscoveryPathsRequest setProjectPaths(List<String> value) {
        this.projectPaths = value;
        return this;
    }

    /**
     * Sets the {@code ignoredSkillsLocations} property.
     *
     * @param value Optional skill scan paths to exclude from discovery.
     * @return this request
     */
    public SkillsGetDiscoveryPathsRequest setIgnoredSkillsLocations(List<String> value) {
        this.ignoredSkillsLocations = value;
        return this;
    }

    /**
     * Sets the {@code excludeHostSkills} property.
     *
     * @param value When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments.
     * @return this request
     */
    public SkillsGetDiscoveryPathsRequest setExcludeHostSkills(Boolean value) {
        this.excludeHostSkills = value;
        return this;
    }

    /**
     * Sets the {@code skillDirectories} property.
     *
     * @param value Explicit discovery-only skill roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
     * @return this request
     */
    public SkillsGetDiscoveryPathsRequest setSkillDirectories(List<String> value) {
        this.skillDirectories = value;
        return this;
    }
}
