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
 * Result of collecting a redacted debug bundle.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDebugCollectLogsResult(
    /** Destination kind that was written. */
    @JsonProperty("kind") DebugCollectLogsResultKind kind,
    /** Actual archive path or staging directory path written. This may differ from the requested path when no-overwrite suffixing or fallback-to-temp-directory was needed. */
    @JsonProperty("path") String path,
    /** Files included in the redacted bundle. */
    @JsonProperty("entries") List<DebugCollectLogsCollectedEntry> entries,
    /** Optional files or directories that could not be included. */
    @JsonProperty("skippedEntries") List<DebugCollectLogsSkippedEntry> skippedEntries
) {
}
