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
 * A file that a conversation-and-files rewind would restore.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryRewindFilePreview(
    /** Absolute path of the captured file. */
    @JsonProperty("path") String path,
    /** Aggregate change made across the discarded turns. */
    @JsonProperty("changeType") HistoryRewindChangeType changeType,
    /** Lines added across the discarded turns. */
    @JsonProperty("linesAdded") Long linesAdded,
    /** Lines removed across the discarded turns. */
    @JsonProperty("linesRemoved") Long linesRemoved
) {
}
