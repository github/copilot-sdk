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
 * Session event "session.start". Session initialization metadata including context and configuration
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
        /** Provider of selectedModel at creation time, when explicitly selected. */
        @JsonProperty("providerId") String providerId,
        /** Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events. */
        @JsonProperty("reasoningEffortModel") String reasoningEffortModel,
        /** True when the reasoning effort is a managed-policy default bound to reasoningEffortModel. Omitted for agent-authored, user-authored, independent, and legacy effort. */
        @JsonProperty("reasoningEffortManaged") Boolean reasoningEffortManaged,
        /** Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed") */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Output verbosity level used for model calls, if applicable (e.g. "low", "medium", "high") */
        @JsonProperty("verbosity") Verbosity verbosity,
        /** Context tier selected at session creation time for models with tiered context pricing; null when no tier is selected (e.g., non-tiered model) */
        @JsonProperty("contextTier") ContextTier contextTier,
        /** True when contextTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("contextTierManaged") Boolean contextTierManaged,
        /** Auto routing preference selected at session creation time */
        @JsonProperty("autoTier") AutoTier autoTier,
        /** True when autoTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("autoTierManaged") Boolean autoTierManaged,
        /** Session limits configured at session creation time, if any */
        @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits,
        /** Working directory and git context at session start */
        @JsonProperty("context") WorkingDirectoryContext context,
        /** Per-session GitHub MCP override persisted for cold resume */
        @JsonProperty("githubMcpToolConfig") GitHubMcpToolConfig gitHubMcpToolConfig,
        /** Whether the session was already in use by another client at start time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** Whether this session supports remote steering via GitHub */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable,
        /** When set, identifies a parent session whose context this session continues — e.g., a detached headless rem-agent run launched on the parent's interactive shutdown. Telemetry from this session is reported under the parent's session_id. */
        @JsonProperty("detachedFromSpawningParentSessionId") String detachedFromSpawningParentSessionId
    ) {

        /**
         * Creates event data with the components it had before later optional fields were added.
         */
        public SessionStartEventData(
            String sessionId,
            Long version,
            String producer,
            String copilotVersion,
            OffsetDateTime startTime,
            String selectedModel,
            String reasoningEffort,
            String reasoningEffortModel,
            ReasoningSummary reasoningSummary,
            Verbosity verbosity,
            ContextTier contextTier,
            AutoTier autoTier,
            SessionLimitsConfig sessionLimits,
            WorkingDirectoryContext context,
            GitHubMcpToolConfig gitHubMcpToolConfig,
            Boolean alreadyInUse,
            Boolean remoteSteerable,
            String detachedFromSpawningParentSessionId
        ) {
            this(sessionId, version, producer, copilotVersion, startTime, selectedModel, null, reasoningEffort, reasoningEffortModel, null, reasoningSummary, verbosity, contextTier, null, autoTier, null, sessionLimits, context, gitHubMcpToolConfig, alreadyInUse, remoteSteerable, detachedFromSpawningParentSessionId);
        }
    }
}
