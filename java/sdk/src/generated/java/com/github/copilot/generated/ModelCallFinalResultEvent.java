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
 * Session event "model.call_final_result". Internal telemetry result for one logical model operation after all orchestrator-owned retries settle
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallFinalResultEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_final_result"; }

    @JsonProperty("data")
    private ModelCallFinalResultEventData data;

    public ModelCallFinalResultEventData getData() { return data; }
    public void setData(ModelCallFinalResultEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallFinalResultEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallFinalResultEventData(
        /** Model identifier used by the final attempt */
        @JsonProperty("model") String model,
        /** Whether the final attempt used a bring-your-own-key provider */
        @JsonProperty("isByok") Boolean isByok,
        /** Bounded result of the final attempt */
        @JsonProperty("result") ModelCallFinalResult result
    ) {
    }
}
