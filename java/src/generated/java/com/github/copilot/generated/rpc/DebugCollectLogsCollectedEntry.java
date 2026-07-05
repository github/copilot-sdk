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
 * A file included in the redacted debug bundle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsCollectedEntry(
    /** Relative path of the file in the staged bundle/archive. */
    @JsonProperty("bundlePath") String bundlePath,
    /** Source category for this entry. */
    @JsonProperty("source") DebugCollectLogsSource source,
    /** Redacted output size in bytes. */
    @JsonProperty("sizeBytes") Long sizeBytes
) {
}
