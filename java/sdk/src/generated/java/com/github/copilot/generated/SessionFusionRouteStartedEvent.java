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
 * Session event "session.fusion_route_started". Experimental transient signal that HydraFusion routing has started for an eligible turn.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionRouteStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_route_started"; }

    @JsonProperty("data")
    private SessionFusionRouteStartedEventData data;

    public SessionFusionRouteStartedEventData getData() { return data; }
    public void setData(SessionFusionRouteStartedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionRouteStartedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionRouteStartedEventData(
        /** Identifier for this routing attempt before a durable Fusion turn exists. */
        @JsonProperty("attemptId") String attemptId,
        /** Kind of turn being routed. */
        @JsonProperty("turnKind") FusionTurnKind turnKind,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion routing policy requested for the turn. */
        @JsonProperty("policy") String policy
    ) {
    }
}
