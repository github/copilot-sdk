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
public record SessionEventLogReadResult(
    /** Session events for this batch, merged into a single stream in creation order: durable (persisted) events and ephemeral events interleave exactly as they were emitted. Set `includeEphemeral: false` to receive only durable events. Ephemeral events are never replayable once pruned from the in-memory ring, so a consumer that needs them should keep reading with a non-zero `waitMs`. For a backward (tail-first) read, the returned window contains persisted events only, still in chronological (oldest-to-newest) append order. */
    @JsonProperty("events") List<SessionEvent> events,
    /** Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. For a backward read this cursor pages toward OLDER events; keep passing `direction: backward` with it (the cursor is also self-describing, so backward paging continues correctly). */
    @JsonProperty("cursor") String cursor,
    /** True when more events are available in the read's direction. For a backward read, true means older persisted events remain before the returned window. A persisted-event page may contain fewer than `max` events because of its byte budget while still reporting hasMore true; continue according to this flag rather than the event count. */
    @JsonProperty("hasMore") Boolean hasMore,
    /** Cursor status: 'ok' means the cursor was applied successfully. For session.eventLog.read, 'expired' means the cursor referred to an event that no longer exists in active history and the read fell back to a boundary of the remaining history: the beginning for a forward read or the newest window for a backward read. That fallback may overlap already rendered events, so active-session consumers should reset, rebase, or deduplicate before continuing. sessions.readPersistedEvents has stricter snapshot semantics: 'expired' returns an empty terminal page and never switches to a replacement journal generation. Other persisted-read I/O failures are RPC errors with diagnostics, not cursor expiry. */
    @JsonProperty("cursorStatus") EventsCursorStatus cursorStatus
) {
}
