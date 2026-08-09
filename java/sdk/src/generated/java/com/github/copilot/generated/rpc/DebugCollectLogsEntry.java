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
 * A caller-provided server-local file or directory to include in the debug bundle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsEntry(
    /** Kind of source path to include. */
    @JsonProperty("kind") DebugCollectLogsEntryKind kind,
    /** Server-local source path to read. */
    @JsonProperty("path") String path,
    /** Relative path to use inside the staged bundle/archive. */
    @JsonProperty("bundlePath") String bundlePath,
    /** How text content from this entry should be redacted. Defaults to plain-text. */
    @JsonProperty("redaction") DebugCollectLogsRedaction redaction,
    /** When true, collection fails if this entry cannot be read. Defaults to false, which records the entry in `skippedEntries`. */
    @JsonProperty("required") Boolean required
) {
}
