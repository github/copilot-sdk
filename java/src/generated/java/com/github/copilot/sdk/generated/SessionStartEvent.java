/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Session event "session.start". Session initialization metadata including context and configuration
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "session.start"; }

    @JsonProperty("data")
    private SessionStartEventData data;

    public SessionStartEventData getData() { return data; }
    public void setData(SessionStartEventData data) { this.data = data; }

    /** Data payload for {@link SessionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionStartEventData(
        /** Unique identifier for the session */
        @JsonProperty("sessionId") String sessionId,
        /** Schema version number for the session event format */
        @JsonProperty("version") Long version,
        /** Identifier of the software producing the events (e.g., "copilot-agent") */
        @JsonProperty("producer") String producer,
        /** Version string of the Copilot application */
        @JsonProperty("copilotVersion") String copilotVersion,
        /** ISO 8601 timestamp when the session was created */
        @JsonProperty("startTime") OffsetDateTime startTime,
        /** Model selected at session creation time, if any */
        @JsonProperty("selectedModel") String selectedModel,
        /** Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed") */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Working directory and git context at session start */
        @JsonProperty("context") WorkingDirectoryContext context,
        /** Whether the session was already in use by another client at start time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** Whether this session supports remote steering via GitHub */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable,
        /** When set, identifies a parent session whose context this session continues — e.g., a detached headless rem-agent run launched on the parent's interactive shutdown. Telemetry from this session is reported under the parent's session_id. */
        @JsonProperty("detachedFromSpawningParentSessionId") String detachedFromSpawningParentSessionId
    ) {
    }
}
