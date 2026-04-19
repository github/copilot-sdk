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
 * The {@code session.model_change} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModelChangeEvent extends SessionEvent {

    @Override
    public String getType() { return "session.model_change"; }

    @JsonProperty("data")
    private SessionModelChangeEventData data;

    public SessionModelChangeEventData getData() { return data; }
    public void setData(SessionModelChangeEventData data) { this.data = data; }

    /** Data payload for {@link SessionModelChangeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModelChangeEventData(
        /** Model that was previously selected, if any */
        @JsonProperty("previousModel") String previousModel,
        /** Newly selected model identifier */
        @JsonProperty("newModel") String newModel,
        /** Reasoning effort level before the model change, if applicable */
        @JsonProperty("previousReasoningEffort") String previousReasoningEffort,
        /** Reasoning effort level after the model change, if applicable */
        @JsonProperty("reasoningEffort") String reasoningEffort
    ) {
    }
}
