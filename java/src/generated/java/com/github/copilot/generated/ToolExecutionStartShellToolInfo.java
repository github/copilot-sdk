/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Shell-aware path hints for a shell tool's command, captured at start time so consumers can snapshot a file's pre-image before the tool runs.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionStartShellToolInfo(
    /** File paths the command may read or write, derived from the command at start time. Produced by the same shell-aware extractor as PermissionRequestShell.possiblePaths, so it is present even when the command is auto-approved and no permission request fires. */
    @JsonProperty("possiblePaths") List<String> possiblePaths,
    /** Whether the command includes a file write redirection (e.g., > or >>). */
    @JsonProperty("hasWriteFileRedirection") Boolean hasWriteFileRedirection
) {
}
