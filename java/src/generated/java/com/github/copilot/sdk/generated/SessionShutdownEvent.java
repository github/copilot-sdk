/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The {@code session.shutdown} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionShutdownEvent extends SessionEvent {

    @Override
    public String getType() { return "session.shutdown"; }

    @JsonProperty("data")
    private SessionShutdownEventData data;

    public SessionShutdownEventData getData() { return data; }
    public void setData(SessionShutdownEventData data) { this.data = data; }

    /** Data payload for {@link SessionShutdownEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionShutdownEventData(
        /** Whether the session ended normally ("routine") or due to a crash/fatal error ("error") */
        @JsonProperty("shutdownType") ShutdownType shutdownType,
        /** Error description when shutdownType is "error" */
        @JsonProperty("errorReason") String errorReason,
        /** Total number of premium API requests used during the session */
        @JsonProperty("totalPremiumRequests") Double totalPremiumRequests,
        /** Cumulative time spent in API calls during the session, in milliseconds */
        @JsonProperty("totalApiDurationMs") Double totalApiDurationMs,
        /** Unix timestamp (milliseconds) when the session started */
        @JsonProperty("sessionStartTime") Double sessionStartTime,
        /** Aggregate code change metrics for the session */
        @JsonProperty("codeChanges") ShutdownCodeChanges codeChanges,
        /** Per-model usage breakdown, keyed by model identifier */
        @JsonProperty("modelMetrics") Map<String, ShutdownModelMetric> modelMetrics,
        /** Model that was selected at the time of shutdown */
        @JsonProperty("currentModel") String currentModel,
        /** Total tokens in context window at shutdown */
        @JsonProperty("currentTokens") Double currentTokens,
        /** System message token count at shutdown */
        @JsonProperty("systemTokens") Double systemTokens,
        /** Non-system message token count at shutdown */
        @JsonProperty("conversationTokens") Double conversationTokens,
        /** Tool definitions token count at shutdown */
        @JsonProperty("toolDefinitionsTokens") Double toolDefinitionsTokens
    ) {
    }
}
