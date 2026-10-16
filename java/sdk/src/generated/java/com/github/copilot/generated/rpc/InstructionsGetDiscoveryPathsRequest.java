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
 * Optional project paths to include when enumerating instruction discovery targets.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class InstructionsGetDiscoveryPathsRequest {

    /** Optional list of project directory paths. When omitted or empty, only the user-level targets are returned. */
    @JsonProperty("projectPaths")
    private List<String> projectPaths;

    /** When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`). */
    @JsonProperty("excludeHostInstructions")
    private Boolean excludeHostInstructions;

    /** Explicit discovery-only instruction roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent. */
    @JsonProperty("instructionDirectories")
    private List<String> instructionDirectories;

    /**
     * Creates a request with its required inputs.
     */
    public InstructionsGetDiscoveryPathsRequest() {
    }

    /**
     * Returns the {@code projectPaths} property.
     *
     * @return Optional list of project directory paths. When omitted or empty, only the user-level targets are returned.
     */
    public List<String> getProjectPaths() {
        return projectPaths;
    }

    /**
     * Returns the {@code excludeHostInstructions} property.
     *
     * @return When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`).
     */
    public Boolean getExcludeHostInstructions() {
        return excludeHostInstructions;
    }

    /**
     * Returns the {@code instructionDirectories} property.
     *
     * @return Explicit discovery-only instruction roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
     */
    public List<String> getInstructionDirectories() {
        return instructionDirectories;
    }

    /**
     * Sets the {@code projectPaths} property.
     *
     * @param value Optional list of project directory paths. When omitted or empty, only the user-level targets are returned.
     * @return this request
     */
    public InstructionsGetDiscoveryPathsRequest setProjectPaths(List<String> value) {
        this.projectPaths = value;
        return this;
    }

    /**
     * Sets the {@code excludeHostInstructions} property.
     *
     * @param value When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`).
     * @return this request
     */
    public InstructionsGetDiscoveryPathsRequest setExcludeHostInstructions(Boolean value) {
        this.excludeHostInstructions = value;
        return this;
    }

    /**
     * Sets the {@code instructionDirectories} property.
     *
     * @param value Explicit discovery-only instruction roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
     * @return this request
     */
    public InstructionsGetDiscoveryPathsRequest setInstructionDirectories(List<String> value) {
        this.instructionDirectories = value;
        return this;
    }
}
