/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Cursor, batch size, and optional long-poll/filter parameters for reading session events.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
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
    /** Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). */
    @JsonProperty("waitMs") Long waitMs,
    /** Either '*' to receive all event types, or a non-empty list of event types to receive */
    @JsonProperty("types") Object types,
    /** Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers. */
    @JsonProperty("agentScope") EventsAgentScope agentScope
) {
}
