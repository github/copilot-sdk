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
    @JsonProperty("id") String id,
    @JsonProperty("ordinal") Long ordinal,
    @JsonProperty("title") String title,
    @JsonProperty("detail") String detail,
    @JsonProperty("status") FactoryPhaseStatus status,
    @JsonProperty("lastEnteredRunAttempt") Long lastEnteredRunAttempt,
    @JsonProperty("entryCount") Long entryCount,
    @JsonProperty("startedAt") Long startedAt,
    @JsonProperty("completedAt") Long completedAt,
    @JsonProperty("accumulatedActiveMs") Long accumulatedActiveMs,
    @JsonProperty("currentActiveMs") Long currentActiveMs,
    @JsonProperty("totalAgentCount") Long totalAgentCount,
    @JsonProperty("liveAgentCount") Long liveAgentCount
) {
}
