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
 * Options for collecting a redacted session debug bundle.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDebugCollectLogsParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Where the redacted bundle should be written. Use `archive` to produce a .tgz, or `directory` to stage redacted files for caller-managed upload/post-processing. */
    @JsonProperty("destination") Object destination,
    /** Which built-in session diagnostics to include. Omitted fields default to true. */
    @JsonProperty("include") DebugCollectLogsInclude include,
    /** Caller-provided server-local files or directories to include in addition to the runtime's built-in session diagnostics. This lets host applications add their own diagnostics without changing the API shape. */
    @JsonProperty("additionalEntries") List<DebugCollectLogsEntry> additionalEntries
) {
}
