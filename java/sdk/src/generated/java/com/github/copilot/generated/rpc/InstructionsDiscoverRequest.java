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
 * Optional project paths to include in instruction discovery.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class InstructionsDiscoverRequest {

    /** Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan). */
    @JsonProperty("projectPaths")
    private List<String> projectPaths;

    /** When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments. */
    @JsonProperty("excludeHostInstructions")
    private Boolean excludeHostInstructions;

    /** Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without ambient discovery, subject to instruction/managed disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots. */
    @JsonProperty("instructionDirectories")
    private List<String> instructionDirectories;

    /**
     * Creates a request with its required inputs.
     */
    public InstructionsDiscoverRequest() {
    }

    /**
     * Returns the {@code projectPaths} property.
     *
     * @return Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan).
     */
    public List<String> getProjectPaths() {
        return projectPaths;
    }

    /**
     * Returns the {@code excludeHostInstructions} property.
     *
     * @return When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments.
     */
    public Boolean getExcludeHostInstructions() {
        return excludeHostInstructions;
    }

    /**
     * Returns the {@code instructionDirectories} property.
     *
     * @return Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without ambient discovery, subject to instruction/managed disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
     */
    public List<String> getInstructionDirectories() {
        return instructionDirectories;
    }

    /**
     * Sets the {@code projectPaths} property.
     *
     * @param value Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan).
     * @return this request
     */
    public InstructionsDiscoverRequest setProjectPaths(List<String> value) {
        this.projectPaths = value;
        return this;
    }

    /**
     * Sets the {@code excludeHostInstructions} property.
     *
     * @param value When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments.
     * @return this request
     */
    public InstructionsDiscoverRequest setExcludeHostInstructions(Boolean value) {
        this.excludeHostInstructions = value;
        return this;
    }

    /**
     * Sets the {@code instructionDirectories} property.
     *
     * @param value Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without ambient discovery, subject to instruction/managed disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
     * @return this request
     */
    public InstructionsDiscoverRequest setInstructionDirectories(List<String> value) {
        this.instructionDirectories = value;
        return this;
    }
}
