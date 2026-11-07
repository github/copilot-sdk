/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Pagination options for reading an inactive or active local session's persisted event journal.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsReadPersistedEventsParams(
    /** Session ID whose persisted event journal should be read. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque, process-local, single-use cursor returned by the previous persisted-event read. Omit on the first call and issue continuations sequentially; reusing the same cursor returns an expired terminal page. */
    @JsonProperty("cursor") String cursor,
    /** Maximum number of events to return in this batch (1–1000, default 200). Pages may contain fewer events to keep the serialized event array within a soft 1 MiB budget including resolved binary assets; one oversized event is returned alone to guarantee progress. */
    @JsonProperty("max") Long max,
    /** Direction to page through persisted history. Forward starts at the beginning; backward starts with the newest events. Events in each page remain chronological. This selects the initial read only; a continuation always uses the direction bound into its cursor. */
    @JsonProperty("direction") EventsReadDirection direction
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsReadPersistedEventsParams {}

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
        private String sessionId;
        private String cursor;
        private Long max;
        private EventsReadDirection direction;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code cursor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cursor(String value) {
            this.cursor = value;
            return this;
        }

        /**
         * Sets the {@code max} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder max(Long value) {
            this.max = value;
            return this;
        }

        /**
         * Sets the {@code direction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder direction(EventsReadDirection value) {
            this.direction = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsReadPersistedEventsParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            return new SessionsReadPersistedEventsParams(sessionId, cursor, max, direction);
        }
    }
}
