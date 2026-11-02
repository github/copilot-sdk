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
import javax.annotation.processing.Generated;

/**
 * One durable workflow progress record.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowProgressLine(
    /** Global monotonic sequence number within the run. */
    @JsonProperty("seq") Long seq,
    /** Resume attempt that emitted this record. */
    @JsonProperty("attempt") Long attempt,
    /** Phase active when the record was emitted, or null before any phase. */
    @JsonProperty("phaseId") String phaseId,
    /** Epoch milliseconds when the record was persisted. */
    @JsonProperty("recordedAt") Long recordedAt,
    /** Progress record kind. */
    @JsonProperty("kind") WorkflowLogLineKind kind,
    /** Prompt-safe progress text. */
    @JsonProperty("text") String text
) {
}
