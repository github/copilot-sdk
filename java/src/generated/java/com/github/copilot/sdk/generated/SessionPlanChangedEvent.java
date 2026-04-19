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
 * The {@code session.plan_changed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPlanChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.plan_changed"; }

    @JsonProperty("data")
    private SessionPlanChangedEventData data;

    public SessionPlanChangedEventData getData() { return data; }
    public void setData(SessionPlanChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionPlanChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPlanChangedEventData(
        /** The type of operation performed on the plan file */
        @JsonProperty("operation") SessionPlanChangedEventDataOperation operation
    ) {

        /** The type of operation performed on the plan file */
        public enum SessionPlanChangedEventDataOperation {
            /** The {@code create} variant. */
            CREATE("create"),
            /** The {@code update} variant. */
            UPDATE("update"),
            /** The {@code delete} variant. */
            DELETE("delete");

            private final String value;
            SessionPlanChangedEventDataOperation(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionPlanChangedEventDataOperation fromValue(String value) {
                for (SessionPlanChangedEventDataOperation v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionPlanChangedEventDataOperation value: " + value);
            }
        }
    }
}
