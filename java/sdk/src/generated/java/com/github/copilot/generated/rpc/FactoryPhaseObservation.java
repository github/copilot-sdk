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
 * Durable lifecycle and timing for one factory phase.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryPhaseObservation(
    /** Phase identifier. */
    @JsonProperty("id") String id,
    /** Zero-based declared phase ordinal, or null for an undeclared phase. */
    @JsonProperty("ordinal") Long ordinal,
    /** Human-readable phase title. */
    @JsonProperty("title") String title,
    /** Optional human-readable phase detail. */
    @JsonProperty("detail") String detail,
    /** Derived lifecycle state of the phase. */
    @JsonProperty("status") FactoryPhaseStatus status,
    /** Most recent run attempt that entered this phase, or `0` if the phase has never been entered. */
    @JsonProperty("lastEnteredRunAttempt") Long lastEnteredRunAttempt,
    /** Number of times execution entered this phase. */
    @JsonProperty("entryCount") Long entryCount,
    /** Epoch milliseconds when this phase first started; for a skipped phase, the synthetic skip timestamp (equal to `completedAt`). */
    @JsonProperty("startedAt") Long startedAt,
    /** Epoch milliseconds when this phase completed; for a skipped phase, the synthetic skip timestamp (equal to `startedAt`). */
    @JsonProperty("completedAt") Long completedAt,
    /** Completed active time accumulated by this phase in milliseconds. */
    @JsonProperty("accumulatedActiveMs") Long accumulatedActiveMs,
    /** Current live active time for this phase in milliseconds. */
    @JsonProperty("currentActiveMs") Long currentActiveMs,
    /** Total direct agents associated with this phase. */
    @JsonProperty("totalAgentCount") Long totalAgentCount,
    /** Direct agents in this phase that are currently live. */
    @JsonProperty("liveAgentCount") Long liveAgentCount
) {
}
