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
 * Public, persistence-independent projection of an autopilot objective.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutopilotObjectiveState(
    /** Session-local objective identifier. */
    @JsonProperty("id") Long id,
    /** User-provided objective text. */
    @JsonProperty("objective") String objective,
    /** Current normalized lifecycle status. */
    @JsonProperty("status") AutopilotObjectiveStatus status,
    /** Number of objective turns started. */
    @JsonProperty("turnCount") Long turnCount,
    /** Optional reason the objective is paused. */
    @JsonProperty("pauseReason") String pauseReason,
    /** Optional summary recorded when the objective completed. */
    @JsonProperty("completionSummary") String completionSummary,
    /** Exact lifetime AI-credit consumption in integer nano-AIU, encoded as a decimal string. */
    @JsonProperty("creditCountNanoAiu") String creditCountNanoAiu,
    /** Current per-window credit limit and consumption, when configured. */
    @JsonProperty("creditLimit") AutopilotObjectiveCreditLimit creditLimit
) {
}
