/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.fusion_completed". Experimental durable aggregate outcome of a HydraFusion turn.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_completed"; }

    @JsonProperty("data")
    private SessionFusionCompletedEventData data;

    public SessionFusionCompletedEventData getData() { return data; }
    public void setData(SessionFusionCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionCompletedEventData(
        /** Stable identifier for the completed HydraFusion turn. */
        @JsonProperty("fusionId") String fusionId,
        /** Idempotency identifier for the authoritative final commit. */
        @JsonProperty("commitId") String commitId,
        /** Identifier of the session turn associated with the completion. */
        @JsonProperty("turnId") String turnId,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion orchestration pattern executed for the turn. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Stable aggregate outcome of the HydraFusion turn. */
        @JsonProperty("outcome") String outcome,
        /** Phase whose output supplied the authoritative final content. */
        @JsonProperty("finalSourcePhaseId") String finalSourcePhaseId,
        /** Concrete model that supplied the authoritative final content. */
        @JsonProperty("finalSourceModel") String finalSourceModel,
        /** Concrete model recommended for eligible follow-up turns. */
        @JsonProperty("followUpModel") String followUpModel,
        /** Reason the turn used a degraded route, when applicable. */
        @JsonProperty("degradedReason") String degradedReason,
        /** Number of concrete phases attempted by the turn. */
        @JsonProperty("phaseCount") Long phaseCount,
        /** Total concrete model requests made across all phases. */
        @JsonProperty("requestCount") Long requestCount,
        /** Total input tokens consumed across all phases. */
        @JsonProperty("inputTokens") Long inputTokens,
        /** Total output tokens produced across all phases. */
        @JsonProperty("outputTokens") Long outputTokens,
        /** Total cached input tokens reported across all phases. */
        @JsonProperty("cachedTokens") Long cachedTokens,
        /** Total tokens written to prompt cache across all phases. */
        @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
        /** Total normalized AI-unit cost reported across all phases, in nano-AIU. */
        @JsonProperty("totalNanoAiu") Double totalNanoAiu,
        /** Total elapsed execution time for the HydraFusion turn in milliseconds. */
        @JsonProperty("durationMs") Double durationMs
    ) {
    }
}
