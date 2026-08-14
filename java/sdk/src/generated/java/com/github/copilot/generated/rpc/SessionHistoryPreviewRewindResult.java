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
 * Files and aggregate changes for a prospective rewind.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryPreviewRewindResult(
    /** Whether file restore is available for this session. This is authoritative: switch on it and read `reason` only when it is false. */
    @JsonProperty("available") Boolean available,
    /** Why file restore is unavailable, when applicable. Populated only when `available` is false and never set when `available` is true. */
    @JsonProperty("reason") HistoryRewindUnavailableReason reason,
    /** Number of unique files in the preview. */
    @JsonProperty("fileCount") Long fileCount,
    /** Files ordered by path. */
    @JsonProperty("files") List<HistoryRewindFilePreview> files
) {
}
