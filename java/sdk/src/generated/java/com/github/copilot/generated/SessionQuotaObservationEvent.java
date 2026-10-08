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
 * Session event "session.quota_observation". A provider-owned quota observation, distinct from per-call usage and charge accounting.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionQuotaObservationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.quota_observation"; }

    @JsonProperty("data")
    private SessionQuotaObservationEventData data;

    public SessionQuotaObservationEventData getData() { return data; }
    public void setData(SessionQuotaObservationEventData data) { this.data = data; }

    /** Data payload for {@link SessionQuotaObservationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionQuotaObservationEventData(
        /** The admitted provider's state observation. Admission observations never contain quantities or reset/percentage semantics. */
        @JsonProperty("observation") ProviderQuotaState observation
    ) {
    }
}
