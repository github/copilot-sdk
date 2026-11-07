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
import com.github.copilot.generated.SessionEvent;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Batch of session events returned by a read, with cursor and continuation metadata.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsReadPersistedEventsResult(
    /** Session events for this batch, merged into a single stream in creation order: durable (persisted) events and ephemeral events interleave exactly as they were emitted. Set `includeEphemeral: false` to receive only durable events. Ephemeral events are never replayable once pruned from the in-memory ring, so a consumer that needs them should keep reading with a non-zero `waitMs`. For a backward (tail-first) read, the returned window contains persisted events only, still in chronological (oldest-to-newest) append order. */
    @JsonProperty("events") List<SessionEvent> events,
    /** Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. For a backward read this cursor pages toward OLDER events; keep passing `direction: backward` with it (the cursor is also self-describing, so backward paging continues correctly). */
    @JsonProperty("cursor") String cursor,
    /** True when more events are available in the read's direction. For a backward read, true means older persisted events remain before the returned window. A persisted-event page may contain fewer than `max` events because of its byte budget while still reporting hasMore true; continue according to this flag rather than the event count. */
    @JsonProperty("hasMore") Boolean hasMore,
    /** Cursor status: 'ok' means the cursor was applied successfully. For session.eventLog.read, 'expired' means the cursor referred to an event that no longer exists in active history and the read fell back to a boundary of the remaining history: the beginning for a forward read or the newest window for a backward read. That fallback may overlap already rendered events, so active-session consumers should reset, rebase, or deduplicate before continuing. sessions.readPersistedEvents has stricter snapshot semantics: 'expired' returns an empty terminal page and never switches to a replacement journal generation. Other persisted-read I/O failures are RPC errors with diagnostics, not cursor expiry. */
    @JsonProperty("cursorStatus") EventsCursorStatus cursorStatus
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsReadPersistedEventsResult {}

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
        private List<SessionEvent> events;
        private String cursor;
        private Boolean hasMore;
        private EventsCursorStatus cursorStatus;

        /**
         * Sets the {@code events} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder events(List<SessionEvent> value) {
            this.events = value;
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
         * Sets the {@code hasMore} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasMore(Boolean value) {
            this.hasMore = value;
            return this;
        }

        /**
         * Sets the {@code cursorStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cursorStatus(EventsCursorStatus value) {
            this.cursorStatus = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsReadPersistedEventsResult build() {
            if (events == null) throw new IllegalStateException("Missing required property: events");
            if (cursor == null) throw new IllegalStateException("Missing required property: cursor");
            if (hasMore == null) throw new IllegalStateException("Missing required property: hasMore");
            if (cursorStatus == null) throw new IllegalStateException("Missing required property: cursorStatus");
            return new SessionsReadPersistedEventsResult(events, cursor, hasMore, cursorStatus);
        }
    }
}
