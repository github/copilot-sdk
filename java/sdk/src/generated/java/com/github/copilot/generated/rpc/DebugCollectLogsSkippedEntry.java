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
 * An optional debug bundle entry that could not be included.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsSkippedEntry(
    /** Relative path requested for this bundle entry. */
    @JsonProperty("bundlePath") String bundlePath,
    /** Server-local source path that could not be read. */
    @JsonProperty("path") String path,
    /** Reason the entry was skipped. */
    @JsonProperty("reason") String reason
) {
}
