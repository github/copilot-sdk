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
 * A root user turn that the session can rewind to.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryRewindPoint(
    /** ID of the user.message event that begins the discarded suffix. */
    @JsonProperty("eventId") String eventId,
    /** User-visible message text for the turn. */
    @JsonProperty("userMessage") String userMessage,
    /** ISO timestamp of the user turn. */
    @JsonProperty("timestamp") String timestamp,
    /** Whether at least one file in this turn or a later turn can be restored. */
    @JsonProperty("canRestoreFiles") Boolean canRestoreFiles,
    /** Number of unique files in this turn and all later turns that have captured changes. */
    @JsonProperty("fileCount") Long fileCount,
    /** Whether this turn itself captured any file changes. */
    @JsonProperty("turnChangedFiles") Boolean turnChangedFiles,
    /** Lines added by this turn's captured file changes. */
    @JsonProperty("linesAdded") Long linesAdded,
    /** Lines removed by this turn's captured file changes. */
    @JsonProperty("linesRemoved") Long linesRemoved,
    /** Whether this turn was an automatically injected autopilot continuation. */
    @JsonProperty("isAutopilotContinuation") Boolean isAutopilotContinuation
) {
}
