/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * The {@code session.handoff} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionHandoffEvent extends SessionEvent {

    @Override
    public String getType() { return "session.handoff"; }

    @JsonProperty("data")
    private SessionHandoffEventData data;

    public SessionHandoffEventData getData() { return data; }
    public void setData(SessionHandoffEventData data) { this.data = data; }

    /** Data payload for {@link SessionHandoffEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionHandoffEventData(
        /** ISO 8601 timestamp when the handoff occurred */
        @JsonProperty("handoffTime") OffsetDateTime handoffTime,
        /** Origin type of the session being handed off */
        @JsonProperty("sourceType") SessionHandoffEventDataSourceType sourceType,
        /** Repository context for the handed-off session */
        @JsonProperty("repository") SessionHandoffEventDataRepository repository,
        /** Additional context information for the handoff */
        @JsonProperty("context") String context,
        /** Summary of the work done in the source session */
        @JsonProperty("summary") String summary,
        /** Session ID of the remote session being handed off */
        @JsonProperty("remoteSessionId") String remoteSessionId,
        /** GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com) */
        @JsonProperty("host") String host
    ) {

        /** Origin type of the session being handed off */
        public enum SessionHandoffEventDataSourceType {
            /** The {@code remote} variant. */
            REMOTE("remote"),
            /** The {@code local} variant. */
            LOCAL("local");

            private final String value;
            SessionHandoffEventDataSourceType(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionHandoffEventDataSourceType fromValue(String value) {
                for (SessionHandoffEventDataSourceType v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionHandoffEventDataSourceType value: " + value);
            }
        }

        /** Repository context for the handed-off session */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionHandoffEventDataRepository(
            /** Repository owner (user or organization) */
            @JsonProperty("owner") String owner,
            /** Repository name */
            @JsonProperty("name") String name,
            /** Git branch name, if applicable */
            @JsonProperty("branch") String branch
        ) {
        }
    }
}
