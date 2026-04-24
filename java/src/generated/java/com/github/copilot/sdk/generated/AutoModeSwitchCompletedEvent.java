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
 * The {@code auto_mode_switch.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AutoModeSwitchCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "auto_mode_switch.completed"; }

    @JsonProperty("data")
    private AutoModeSwitchCompletedEventData data;

    public AutoModeSwitchCompletedEventData getData() { return data; }
    public void setData(AutoModeSwitchCompletedEventData data) { this.data = data; }

    /** Data payload for {@link AutoModeSwitchCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AutoModeSwitchCompletedEventData(
        /** Request ID of the resolved request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** The user's choice: 'yes', 'yes_always', or 'no' */
        @JsonProperty("response") String response
    ) {
    }
}
