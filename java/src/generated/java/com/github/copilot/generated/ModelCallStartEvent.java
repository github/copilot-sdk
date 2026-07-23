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
 * Session event "model.call_start". Model API dispatch metadata for internal telemetry
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallStartEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_start"; }

    @JsonProperty("data")
    private ModelCallStartEventData data;

    public ModelCallStartEventData getData() { return data; }
    public void setData(ModelCallStartEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallStartEventData(
        /** Identifier of the assistant turn that initiated the model call */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this API call, when known */
        @JsonProperty("model") String model
    ) {
    }
}
