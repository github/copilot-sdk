/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code sample.updated} session event.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SampleUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "sample.updated"; }

    @JsonProperty("data")
    private SampleUpdatedEventData data;

    public SampleUpdatedEventData getData() { return data; }
    public void setData(SampleUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link SampleUpdatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SampleUpdatedEventData(
        @JsonProperty("message") String message,
        @JsonProperty("sequence") Long sequence,
        @JsonProperty("active") Boolean active,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("note") String note
    ) {
    }
}
