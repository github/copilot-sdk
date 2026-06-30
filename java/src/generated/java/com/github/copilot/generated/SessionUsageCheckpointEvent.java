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
 * Session event "session.usage_checkpoint". Durable session usage checkpoint for reconstructing aggregate accounting on resume
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionUsageCheckpointEvent extends SessionEvent {

    @Override
    public String getType() { return "session.usage_checkpoint"; }

    @JsonProperty("data")
    private SessionUsageCheckpointEventData data;

    public SessionUsageCheckpointEventData getData() { return data; }
    public void setData(SessionUsageCheckpointEventData data) { this.data = data; }

    /** Data payload for {@link SessionUsageCheckpointEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUsageCheckpointEventData(
        /** Session-wide accumulated nano-AI units cost at checkpoint time */
        @JsonProperty("totalNanoAiu") Double totalNanoAiu,
        /** Total number of premium API requests used at checkpoint time */
        @JsonProperty("totalPremiumRequests") Double totalPremiumRequests
    ) {
    }
}
