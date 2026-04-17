/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code capabilities.changed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CapabilitiesChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "capabilities.changed"; }

    @JsonProperty("data")
    private CapabilitiesChangedEventData data;

    public CapabilitiesChangedEventData getData() { return data; }
    public void setData(CapabilitiesChangedEventData data) { this.data = data; }

    /** Data payload for {@link CapabilitiesChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CapabilitiesChangedEventData(
        /** UI capability changes */
        @JsonProperty("ui") CapabilitiesChangedEventDataUi ui
    ) {

        /** UI capability changes */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record CapabilitiesChangedEventDataUi(
            /** Whether elicitation is now supported */
            @JsonProperty("elicitation") Boolean elicitation
        ) {
        }
    }
}
