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
 * One file-picker suggestion offered up for the sandbox filesystem check.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxPickerCandidate(
    /** Absolute path the suggestion would attach. */
    @JsonProperty("path") String path,
    /** Whether the suggestion is a directory. A directory the policy does not itself allow may still be needed as a waypoint to an allowed subtree. */
    @JsonProperty("isDirectory") Boolean isDirectory
) {
}
