/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Parameters for (re)loading the merged LSP configuration set.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLspInitializeParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted. */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** Git root used as the boundary when traversing for project-level LSP configs (supports monorepos). */
    @JsonProperty("gitRoot") String gitRoot,
    /** Force re-initialization even when LSP configs were already loaded for the working directory. */
    @JsonProperty("force") Boolean force
) {
}
