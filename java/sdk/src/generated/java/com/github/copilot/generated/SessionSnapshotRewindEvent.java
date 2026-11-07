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
 * Session event "session.snapshot_rewind". Session rewind details including target event and count of removed events
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionSnapshotRewindEvent extends SessionEvent {

    @Override
    public String getType() { return "session.snapshot_rewind"; }

    @JsonProperty("data")
    private SessionSnapshotRewindEventData data;

    public SessionSnapshotRewindEventData getData() { return data; }
    public void setData(SessionSnapshotRewindEventData data) { this.data = data; }

    /** Data payload for {@link SessionSnapshotRewindEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSnapshotRewindEventData(
        /** First removed event. Without `eventIds`, it and every event after it were removed */
        @JsonProperty("upToEventId") String upToEventId,
        /** Number of events that were removed by the rewind */
        @JsonProperty("eventsRemoved") Long eventsRemoved,
        /** The removed events, starting with `upToEventId`. Later events not listed were kept, such as a background agent's events that interleaved with a withdrawn turn */
        @JsonProperty("eventIds") List<String> eventIds
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionSnapshotRewindEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private String upToEventId;
            private Long eventsRemoved;
            private List<String> eventIds;

            /**
             * Sets the {@code upToEventId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder upToEventId(String value) {
                this.upToEventId = value;
                return this;
            }

            /**
             * Sets the {@code eventsRemoved} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder eventsRemoved(Long value) {
                this.eventsRemoved = value;
                return this;
            }

            /**
             * Sets the {@code eventIds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder eventIds(List<String> value) {
                this.eventIds = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionSnapshotRewindEventData build() {
                if (upToEventId == null) throw new IllegalStateException("Missing required property: upToEventId");
                if (eventsRemoved == null) throw new IllegalStateException("Missing required property: eventsRemoved");
                return new SessionSnapshotRewindEventData(upToEventId, eventsRemoved, eventIds);
            }
        }
    }
}
