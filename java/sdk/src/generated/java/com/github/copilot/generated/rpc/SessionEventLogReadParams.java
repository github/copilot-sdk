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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Cursor, batch size, and optional long-poll/filter parameters for reading session events.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionEventLogReadParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque cursor returned by a previous read. Omit on the first call to start from the beginning of the session's persisted history. */
    @JsonProperty("cursor") String cursor,
    /** Maximum number of events to return in this batch (1–1000, default 200). */
    @JsonProperty("max") Long max,
    /** Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur. */
    @JsonProperty("waitMs") Long waitMs,
    /** Either '*' to receive all event types, or a non-empty list of event types to receive */
    @JsonProperty("types") Object types,
    /** Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers. */
    @JsonProperty("agentScope") EventsAgentScope agentScope,
    /** Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope. */
    @JsonProperty("agentIds") List<String> agentIds,
    /** Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion. */
    @JsonProperty("direction") EventsReadDirection direction,
    /** When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only. */
    @JsonProperty("includeEphemeral") Boolean includeEphemeral
) {
}
