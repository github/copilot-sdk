/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Optional project paths to include when enumerating instruction discovery targets.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionsGetDiscoveryPathsParams(
    /** Optional list of project directory paths. When omitted or empty, only the user-level targets are returned. */
    @JsonProperty("projectPaths") List<String> projectPaths,
    /** When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`). */
    @JsonProperty("excludeHostInstructions") Boolean excludeHostInstructions
) {
}
