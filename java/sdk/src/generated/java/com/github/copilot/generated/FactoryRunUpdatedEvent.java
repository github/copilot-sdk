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
 * Session event "factory.run_updated". Ephemeral invalidation signal for a changed factory run.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class FactoryRunUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "factory.run_updated"; }

    @JsonProperty("data")
    private FactoryRunUpdatedEventData data;

    public FactoryRunUpdatedEventData getData() { return data; }
    public void setData(FactoryRunUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link FactoryRunUpdatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FactoryRunUpdatedEventData(
        /** Factory run identifier. */
        @JsonProperty("runId") String runId,
        /** Monotonic revision now available for the run. */
        @JsonProperty("revision") Long revision
    ) {
    }
}
