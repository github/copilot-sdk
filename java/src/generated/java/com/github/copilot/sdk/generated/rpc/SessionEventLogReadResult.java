/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.sdk.generated.SessionEvent;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Batch of session events returned by a read, with cursor and continuation metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionEventLogReadResult(
    /** Events are delivered in two batches per read: persisted events first (in append order), then ephemeral events (in seq order). When `waitMs > 0` and the catch-up batches were empty, post-wait events follow the same two-batch ordering. Persisted and ephemeral events do not interleave within a single read. */
    @JsonProperty("events") List<SessionEvent> events,
    /** Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. */
    @JsonProperty("cursor") String cursor,
    /** True when the read returned `max` events and more events are available immediately. When false, the next read with a non-zero `waitMs` will block until a new event arrives or the wait expires. */
    @JsonProperty("hasMore") Boolean hasMore,
    /** Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history. */
    @JsonProperty("cursorStatus") EventsCursorStatus cursorStatus
) {
}
