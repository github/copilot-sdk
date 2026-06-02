/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Session event "session.resume". Session resume metadata including current context and event count
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionResumeEvent extends SessionEvent {

    @Override
    public String getType() { return "session.resume"; }

    @JsonProperty("data")
    private SessionResumeEventData data;

    public SessionResumeEventData getData() { return data; }
    public void setData(SessionResumeEventData data) { this.data = data; }

    /** Data payload for {@link SessionResumeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionResumeEventData(
        /** ISO 8601 timestamp when the session was resumed */
        @JsonProperty("resumeTime") OffsetDateTime resumeTime,
        /** Total number of persisted events in the session at the time of resume */
        @JsonProperty("eventCount") Long eventCount,
        /** Model currently selected at resume time */
        @JsonProperty("selectedModel") String selectedModel,
        /** Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed") */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Context tier currently selected at resume time; null when no tier is active */
        @JsonProperty("contextTier") ContextTier contextTier,
        /** Updated working directory and git context at resume time */
        @JsonProperty("context") WorkingDirectoryContext context,
        /** Whether the session was already in use by another client at resume time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** True when this resume attached to a session that the runtime already had running in-memory (for example, an extension joining a session another client was actively driving). False (or omitted) for cold resumes — the runtime had to reconstitute the session from its persisted event log. */
        @JsonProperty("sessionWasActive") Boolean sessionWasActive,
        /** Whether this session supports remote steering via GitHub */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable,
        /** When true, tool calls and permission requests left in flight by the previous session lifetime remain pending after resume and the agentic loop awaits their results. User sends are queued behind the pending work until all such requests reach a terminal state. When false (the default), any such tool calls and permission requests are immediately marked as interrupted on resume. */
        @JsonProperty("continuePendingWork") Boolean continuePendingWork
    ) {
    }
}
