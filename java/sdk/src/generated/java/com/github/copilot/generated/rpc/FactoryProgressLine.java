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
 * One durable factory progress record.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryProgressLine(
    /** Global monotonic sequence number within the run. */
    @JsonProperty("seq") Long seq,
    /** Resume attempt that emitted this record. */
    @JsonProperty("attempt") Long attempt,
    /** Phase active when the record was emitted, or null before any phase. */
    @JsonProperty("phaseId") String phaseId,
    /** Epoch milliseconds when the record was persisted. */
    @JsonProperty("recordedAt") Long recordedAt,
    /** Progress record kind. */
    @JsonProperty("kind") FactoryLogLineKind kind,
    /** Prompt-safe progress text. */
    @JsonProperty("text") String text
) {
}
