/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code session.extensions_loaded} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionExtensionsLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.extensions_loaded"; }

    @JsonProperty("data")
    private SessionExtensionsLoadedEventData data;

    public SessionExtensionsLoadedEventData getData() { return data; }
    public void setData(SessionExtensionsLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionExtensionsLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExtensionsLoadedEventData(
        /** Array of discovered extensions and their status */
        @JsonProperty("extensions") List<SessionExtensionsLoadedEventDataExtensionsItem> extensions
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionExtensionsLoadedEventDataExtensionsItem(
            /** Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper') */
            @JsonProperty("id") String id,
            /** Extension name (directory name) */
            @JsonProperty("name") String name,
            /** Discovery source */
            @JsonProperty("source") SessionExtensionsLoadedEventDataExtensionsItemSource source,
            /** Current status: running, disabled, failed, or starting */
            @JsonProperty("status") SessionExtensionsLoadedEventDataExtensionsItemStatus status
        ) {

            /** Discovery source */
            public enum SessionExtensionsLoadedEventDataExtensionsItemSource {
                /** The {@code project} variant. */
                PROJECT("project"),
                /** The {@code user} variant. */
                USER("user");

                private final String value;
                SessionExtensionsLoadedEventDataExtensionsItemSource(String value) { this.value = value; }
                @com.fasterxml.jackson.annotation.JsonValue
                public String getValue() { return value; }
                @com.fasterxml.jackson.annotation.JsonCreator
                public static SessionExtensionsLoadedEventDataExtensionsItemSource fromValue(String value) {
                    for (SessionExtensionsLoadedEventDataExtensionsItemSource v : values()) {
                        if (v.value.equals(value)) return v;
                    }
                    throw new IllegalArgumentException("Unknown SessionExtensionsLoadedEventDataExtensionsItemSource value: " + value);
                }
            }

            /** Current status: running, disabled, failed, or starting */
            public enum SessionExtensionsLoadedEventDataExtensionsItemStatus {
                /** The {@code running} variant. */
                RUNNING("running"),
                /** The {@code disabled} variant. */
                DISABLED("disabled"),
                /** The {@code failed} variant. */
                FAILED("failed"),
                /** The {@code starting} variant. */
                STARTING("starting");

                private final String value;
                SessionExtensionsLoadedEventDataExtensionsItemStatus(String value) { this.value = value; }
                @com.fasterxml.jackson.annotation.JsonValue
                public String getValue() { return value; }
                @com.fasterxml.jackson.annotation.JsonCreator
                public static SessionExtensionsLoadedEventDataExtensionsItemStatus fromValue(String value) {
                    for (SessionExtensionsLoadedEventDataExtensionsItemStatus v : values()) {
                        if (v.value.equals(value)) return v;
                    }
                    throw new IllegalArgumentException("Unknown SessionExtensionsLoadedEventDataExtensionsItemStatus value: " + value);
                }
            }
        }
    }
}
