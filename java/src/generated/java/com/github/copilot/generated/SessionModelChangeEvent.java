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
 * Session event "session.model_change". Model change details including previous and new model identifiers
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
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Reasoning summary mode before the model change, if applicable */
        @JsonProperty("previousReasoningSummary") ReasoningSummary previousReasoningSummary,
        /** Reasoning summary mode after the model change, if applicable */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Context tier after the model change; null explicitly clears a previously selected tier */
        @JsonProperty("contextTier") SessionModelChangeEventDataContextTier contextTier,
        /** Reason the change happened, when not user-initiated. Currently `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path. UI clients can use this to render contextual copy. */
        @JsonProperty("cause") String cause
    ) {

        public enum SessionModelChangeEventDataContextTier {
            /** The {@code default} variant. */
            DEFAULT("default"),
            /** The {@code long_context} variant. */
            LONG_CONTEXT("long_context");

            private final String value;
            SessionModelChangeEventDataContextTier(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionModelChangeEventDataContextTier fromValue(String value) {
                for (SessionModelChangeEventDataContextTier v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionModelChangeEventDataContextTier value: " + value);
            }
        }
    }
}
