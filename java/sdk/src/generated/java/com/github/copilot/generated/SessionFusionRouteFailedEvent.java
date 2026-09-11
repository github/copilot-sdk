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
 * Session event "session.fusion_route_failed". Experimental durable HydraFusion routing failure and the deterministic concrete fallback selected for the turn.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionRouteFailedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_route_failed"; }

    @JsonProperty("data")
    private SessionFusionRouteFailedEventData data;

    public SessionFusionRouteFailedEventData getData() { return data; }
    public void setData(SessionFusionRouteFailedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionRouteFailedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionRouteFailedEventData(
        /** Identifier of the routing attempt that failed. */
        @JsonProperty("attemptId") String attemptId,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion routing policy requested for the turn. */
        @JsonProperty("policy") String policy,
        /** Stable machine-readable reason for the routing failure. */
        @JsonProperty("reason") String reason,
        /** Provider or validation error detail, when available. */
        @JsonProperty("errorMessage") String errorMessage,
        /** Concrete model selected as the deterministic fallback. */
        @JsonProperty("fallbackModel") String fallbackModel,
        /** Elapsed routing time in milliseconds before the failure. */
        @JsonProperty("routingLatencyMs") Double routingLatencyMs
    ) {
    }
}
