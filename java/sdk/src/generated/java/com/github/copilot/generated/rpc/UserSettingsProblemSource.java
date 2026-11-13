/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * One already-loaded settings layer to diagnose.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsProblemSource(
    /** Which layer this is: `user`, `repo`, or `local`. */
    @JsonProperty("kind") String kind,
    /** Caller-chosen display label for the layer, echoed back on each problem so a caller can group by layer without re-deriving its own naming. */
    @JsonProperty("label") String label,
    /** Absolute path of the file backing this layer. */
    @JsonProperty("filePath") String filePath,
    /** The layer's settings exactly as loaded, unnormalized. Empty when the file is missing or failed to parse. */
    @JsonProperty("config") Map<String, Object> config,
    /** True when the file itself failed to parse, so its keys could not be read at all. */
    @JsonProperty("invalid") Boolean invalid
) {
}
