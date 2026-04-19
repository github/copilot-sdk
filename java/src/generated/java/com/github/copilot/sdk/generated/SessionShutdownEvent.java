/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
        @JsonProperty("shutdownType") SessionShutdownEventDataShutdownType shutdownType,
        /** Error description when shutdownType is "error" */
        @JsonProperty("errorReason") String errorReason,
        /** Total number of premium API requests used during the session */
        @JsonProperty("totalPremiumRequests") Double totalPremiumRequests,
        /** Cumulative time spent in API calls during the session, in milliseconds */
        @JsonProperty("totalApiDurationMs") Double totalApiDurationMs,
        /** Unix timestamp (milliseconds) when the session started */
        @JsonProperty("sessionStartTime") Double sessionStartTime,
        /** Aggregate code change metrics for the session */
        @JsonProperty("codeChanges") SessionShutdownEventDataCodeChanges codeChanges,
        /** Per-model usage breakdown, keyed by model identifier */
        @JsonProperty("modelMetrics") Map<String, SessionShutdownEventDataModelMetricsValue> modelMetrics,
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

        /** Whether the session ended normally ("routine") or due to a crash/fatal error ("error") */
        public enum SessionShutdownEventDataShutdownType {
            /** The {@code routine} variant. */
            ROUTINE("routine"),
            /** The {@code error} variant. */
            ERROR("error");

            private final String value;
            SessionShutdownEventDataShutdownType(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionShutdownEventDataShutdownType fromValue(String value) {
                for (SessionShutdownEventDataShutdownType v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionShutdownEventDataShutdownType value: " + value);
            }
        }

        /** Aggregate code change metrics for the session */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionShutdownEventDataCodeChanges(
            /** Total number of lines added during the session */
            @JsonProperty("linesAdded") Double linesAdded,
            /** Total number of lines removed during the session */
            @JsonProperty("linesRemoved") Double linesRemoved,
            /** List of file paths that were modified during the session */
            @JsonProperty("filesModified") List<String> filesModified
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionShutdownEventDataModelMetricsValue(
            /** Request count and cost metrics */
            @JsonProperty("requests") SessionShutdownEventDataModelMetricsValueRequests requests,
            /** Token usage breakdown */
            @JsonProperty("usage") SessionShutdownEventDataModelMetricsValueUsage usage
        ) {

            /** Request count and cost metrics */
            @JsonIgnoreProperties(ignoreUnknown = true)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record SessionShutdownEventDataModelMetricsValueRequests(
                /** Total number of API requests made to this model */
                @JsonProperty("count") Double count,
                /** Cumulative cost multiplier for requests to this model */
                @JsonProperty("cost") Double cost
            ) {
            }

            /** Token usage breakdown */
            @JsonIgnoreProperties(ignoreUnknown = true)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record SessionShutdownEventDataModelMetricsValueUsage(
                /** Total input tokens consumed across all requests to this model */
                @JsonProperty("inputTokens") Double inputTokens,
                /** Total output tokens produced across all requests to this model */
                @JsonProperty("outputTokens") Double outputTokens,
                /** Total tokens read from prompt cache across all requests */
                @JsonProperty("cacheReadTokens") Double cacheReadTokens,
                /** Total tokens written to prompt cache across all requests */
                @JsonProperty("cacheWriteTokens") Double cacheWriteTokens,
                /** Total reasoning tokens produced across all requests to this model */
                @JsonProperty("reasoningTokens") Double reasoningTokens
            ) {
            }
        }
    }
}
