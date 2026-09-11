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
    /** True when more events are available in the read's direction. For a forward read, true means the batch returned `max` events and more are available immediately. For a backward read, true means older persisted events remain before the returned window. */
    @JsonProperty("hasMore") Boolean hasMore,
    /** Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read fell back to a boundary of the remaining history. For a forward read the fallback starts from the beginning of the remaining history; for a backward read it falls back to the tail (the newest window). Because the fallback page is a fresh boundary snapshot rather than a continuation of the requested cursor, it may overlap events the consumer has already rendered — a backward fallback to the tail in particular can repeat the newest window. On 'expired', consumers should reset or rebase their local pagination state (or deduplicate by event id) before continuing from the returned cursor rather than blindly appending/prepending the fallback page. */
    @JsonProperty("cursorStatus") EventsCursorStatus cursorStatus
) {
}
