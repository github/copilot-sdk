/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Schema for the `AgentInfo` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentInfo(
    /** Unique identifier of the custom agent */
    @JsonProperty("name") String name,
    /** Human-readable display name */
    @JsonProperty("displayName") String displayName,
    /** Description of the agent's purpose */
    @JsonProperty("description") String description,
    /** Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path. */
    @JsonProperty("path") String path
) {
}
