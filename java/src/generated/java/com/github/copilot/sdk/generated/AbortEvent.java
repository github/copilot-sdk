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
 * The {@code abort} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AbortEvent extends SessionEvent {

    @Override
    public String getType() { return "abort"; }

    @JsonProperty("data")
    private AbortEventData data;

    public AbortEventData getData() { return data; }
    public void setData(AbortEventData data) { this.data = data; }

    /** Data payload for {@link AbortEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AbortEventData(
        /** Reason the current turn was aborted (e.g., "user initiated") */
        @JsonProperty("reason") String reason
    ) {
    }
}
