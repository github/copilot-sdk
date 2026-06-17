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
 * Schema for the `AgentDiscoveryPath` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentDiscoveryPath(
    /** Absolute path of the search/create directory (may not exist on disk yet) */
    @JsonProperty("path") String path,
    /** Which tier this directory belongs to */
    @JsonProperty("scope") AgentDiscoveryPathScope scope,
    /** Whether this is the canonical directory to create a new agent in its tier. At most one entry per tier is preferred. */
    @JsonProperty("preferredForCreation") Boolean preferredForCreation,
    /** The input project path this directory was derived from (only for project scope) */
    @JsonProperty("projectPath") String projectPath
) {
}
