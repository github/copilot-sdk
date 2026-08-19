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
 * Session event "factory.run_started". Ephemeral signal that a factory run attempt began executing.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class FactoryRunStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "factory.run_started"; }

    @JsonProperty("data")
    private FactoryRunStartedEventData data;

    public FactoryRunStartedEventData getData() { return data; }
    public void setData(FactoryRunStartedEventData data) { this.data = data; }

    /** Data payload for {@link FactoryRunStartedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FactoryRunStartedEventData(
        /** Identifier of the factory run that started. */
        @JsonProperty("runId") String runId,
        /** Name of the factory this run executes. Low cardinality by construction. */
        @JsonProperty("factoryName") String factoryName,
        /** Attempt number this start committed; a resumed run increments it. */
        @JsonProperty("attempt") Long attempt
    ) {
    }
}
