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
 * Schema for the `InstructionDiscoveryPath` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionDiscoveryPath(
    /** Absolute path of the file or directory (may not exist on disk yet) */
    @JsonProperty("path") String path,
    /** Which tier this target belongs to */
    @JsonProperty("location") InstructionDiscoveryPathLocation location,
    /** Whether the target is a single file or a directory of instruction files */
    @JsonProperty("kind") InstructionDiscoveryPathKind kind,
    /** Whether this is the canonical target to create new instructions in its tier. At most one entry per tier is preferred. */
    @JsonProperty("preferredForCreation") Boolean preferredForCreation,
    /** The input project path this target was derived from (only for repository targets) */
    @JsonProperty("projectPath") String projectPath
) {
}
