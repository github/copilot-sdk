/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Cursor, batch size, and optional long-poll/filter parameters for reading session events.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionEventLogReadRequest {

    /** Opaque cursor returned by a previous read in the same view. Omit on the first call to start from the beginning of the selected view. */
    @JsonProperty("cursor")
    private String cursor;

    /** Maximum number of events to return in this batch (1–1000, default 200). */
    @JsonProperty("max")
    private Long max;

    /** Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur. */
    @JsonProperty("waitMs")
    private Long waitMs;

    /** Either '*' to receive all event types, or a non-empty list of event types to receive */
    @JsonProperty("types")
    private Object types;

    /** Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers. */
    @JsonProperty("agentScope")
    private EventsAgentScope agentScope;

    /** Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope. */
    @JsonProperty("agentIds")
    private List<String> agentIds;

    /** Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion. */
    @JsonProperty("direction")
    private EventsReadDirection direction;

    /** When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only. */
    @JsonProperty("includeEphemeral")
    private Boolean includeEphemeral;

    /** Event stream to read. 'history' (default) returns original events and lazily follows predecessor journals, excluding checkpoint copies. 'replay' reads only the active replay head, including checkpoint state, without opening predecessor journals. Use replay for current state and fast transcript previews; use history to reconstruct the original visible transcript. Cursors must remain within the view that produced them. */
    @JsonProperty("view")
    private EventsReadView view;

    /**
     * Creates a request with its required inputs.
     */
    public SessionEventLogReadRequest() {
    }

    /**
     * Returns the {@code cursor} property.
     *
     * @return Opaque cursor returned by a previous read in the same view. Omit on the first call to start from the beginning of the selected view.
     */
    public String getCursor() {
        return cursor;
    }

    /**
     * Returns the {@code max} property.
     *
     * @return Maximum number of events to return in this batch (1–1000, default 200).
     */
    public Long getMax() {
        return max;
    }

    /**
     * Returns the {@code waitMs} property.
     *
     * @return Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur.
     */
    public Long getWaitMs() {
        return waitMs;
    }

    /**
     * Returns the {@code types} property.
     *
     * @return Either '*' to receive all event types, or a non-empty list of event types to receive
     */
    public Object getTypes() {
        return types;
    }

    /**
     * Returns the {@code agentScope} property.
     *
     * @return Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
     */
    public EventsAgentScope getAgentScope() {
        return agentScope;
    }

    /**
     * Returns the {@code agentIds} property.
     *
     * @return Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope.
     */
    public List<String> getAgentIds() {
        return agentIds;
    }

    /**
     * Returns the {@code direction} property.
     *
     * @return Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion.
     */
    public EventsReadDirection getDirection() {
        return direction;
    }

    /**
     * Returns the {@code includeEphemeral} property.
     *
     * @return When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only.
     */
    public Boolean getIncludeEphemeral() {
        return includeEphemeral;
    }

    /**
     * Returns the {@code view} property.
     *
     * @return Event stream to read. 'history' (default) returns original events and lazily follows predecessor journals, excluding checkpoint copies. 'replay' reads only the active replay head, including checkpoint state, without opening predecessor journals. Use replay for current state and fast transcript previews; use history to reconstruct the original visible transcript. Cursors must remain within the view that produced them.
     */
    public EventsReadView getView() {
        return view;
    }

    /**
     * Sets the {@code cursor} property.
     *
     * @param value Opaque cursor returned by a previous read in the same view. Omit on the first call to start from the beginning of the selected view.
     * @return this request
     */
    public SessionEventLogReadRequest setCursor(String value) {
        this.cursor = value;
        return this;
    }

    /**
     * Sets the {@code max} property.
     *
     * @param value Maximum number of events to return in this batch (1–1000, default 200).
     * @return this request
     */
    public SessionEventLogReadRequest setMax(Long value) {
        this.max = value;
        return this;
    }

    /**
     * Sets the {@code waitMs} property.
     *
     * @param value Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur.
     * @return this request
     */
    public SessionEventLogReadRequest setWaitMs(Long value) {
        this.waitMs = value;
        return this;
    }

    /**
     * Sets the {@code types} property.
     *
     * @param value Either '*' to receive all event types, or a non-empty list of event types to receive
     * @return this request
     */
    public SessionEventLogReadRequest setTypes(Object value) {
        this.types = value;
        return this;
    }

    /**
     * Sets the {@code agentScope} property.
     *
     * @param value Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
     * @return this request
     */
    public SessionEventLogReadRequest setAgentScope(EventsAgentScope value) {
        this.agentScope = value;
        return this;
    }

    /**
     * Sets the {@code agentIds} property.
     *
     * @param value Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope.
     * @return this request
     */
    public SessionEventLogReadRequest setAgentIds(List<String> value) {
        this.agentIds = value;
        return this;
    }

    /**
     * Sets the {@code direction} property.
     *
     * @param value Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion.
     * @return this request
     */
    public SessionEventLogReadRequest setDirection(EventsReadDirection value) {
        this.direction = value;
        return this;
    }

    /**
     * Sets the {@code includeEphemeral} property.
     *
     * @param value When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only.
     * @return this request
     */
    public SessionEventLogReadRequest setIncludeEphemeral(Boolean value) {
        this.includeEphemeral = value;
        return this;
    }

    /**
     * Sets the {@code view} property.
     *
     * @param value Event stream to read. 'history' (default) returns original events and lazily follows predecessor journals, excluding checkpoint copies. 'replay' reads only the active replay head, including checkpoint state, without opening predecessor journals. Use replay for current state and fast transcript previews; use history to reconstruct the original visible transcript. Cursors must remain within the view that produced them.
     * @return this request
     */
    public SessionEventLogReadRequest setView(EventsReadView value) {
        this.view = value;
        return this;
    }
}
