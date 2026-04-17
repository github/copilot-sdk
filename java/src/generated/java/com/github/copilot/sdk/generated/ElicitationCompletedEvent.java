/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The {@code elicitation.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ElicitationCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "elicitation.completed"; }

    @JsonProperty("data")
    private ElicitationCompletedEventData data;

    public ElicitationCompletedEventData getData() { return data; }
    public void setData(ElicitationCompletedEventData data) { this.data = data; }

    /** Data payload for {@link ElicitationCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ElicitationCompletedEventData(
        /** Request ID of the resolved elicitation request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed) */
        @JsonProperty("action") ElicitationCompletedEventDataAction action,
        /** The submitted form data when action is 'accept'; keys match the requested schema fields */
        @JsonProperty("content") Map<String, Object> content
    ) {

        /** The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed) */
        public enum ElicitationCompletedEventDataAction {
            /** The {@code accept} variant. */
            ACCEPT("accept"),
            /** The {@code decline} variant. */
            DECLINE("decline"),
            /** The {@code cancel} variant. */
            CANCEL("cancel");

            private final String value;
            ElicitationCompletedEventDataAction(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static ElicitationCompletedEventDataAction fromValue(String value) {
                for (ElicitationCompletedEventDataAction v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown ElicitationCompletedEventDataAction value: " + value);
            }
        }
    }
}
