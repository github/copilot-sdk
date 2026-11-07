/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Usage attributed to one agent instance at session shutdown.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShutdownAgentMetric(
    /** Configured agent name, when this is a subagent */
    @JsonProperty("agentName") String agentName,
    /** Human-readable label for this subagent invocation, copied from the originating `subagent.started` event. For task-tool subagents this is the invocation's task description rather than the agent's configured display name, so group by `agentName` for stable per-agent labels. */
    @JsonProperty("agentDisplayName") String agentDisplayName,
    /** Time spent in model API calls by this agent, in milliseconds */
    @JsonProperty("totalApiDurationMs") Long totalApiDurationMs,
    /** Accumulated nano-AI units cost for this agent */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Per-model usage for this agent, keyed by model identifier */
    @JsonProperty("modelMetrics") Map<String, ShutdownModelMetric> modelMetrics
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ShutdownAgentMetric {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String agentName;
        private String agentDisplayName;
        private Long totalApiDurationMs;
        private Double totalNanoAiu;
        private Map<String, ShutdownModelMetric> modelMetrics;

        /**
         * Sets the {@code agentName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentName(String value) {
            this.agentName = value;
            return this;
        }

        /**
         * Sets the {@code agentDisplayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentDisplayName(String value) {
            this.agentDisplayName = value;
            return this;
        }

        /**
         * Sets the {@code totalApiDurationMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalApiDurationMs(Long value) {
            this.totalApiDurationMs = value;
            return this;
        }

        /**
         * Sets the {@code totalNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalNanoAiu(Double value) {
            this.totalNanoAiu = value;
            return this;
        }

        /**
         * Sets the {@code modelMetrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelMetrics(Map<String, ShutdownModelMetric> value) {
            this.modelMetrics = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ShutdownAgentMetric build() {
            if (totalApiDurationMs == null) throw new IllegalStateException("Missing required property: totalApiDurationMs");
            if (totalNanoAiu == null) throw new IllegalStateException("Missing required property: totalNanoAiu");
            if (modelMetrics == null) throw new IllegalStateException("Missing required property: modelMetrics");
            return new ShutdownAgentMetric(agentName, agentDisplayName, totalApiDurationMs, totalNanoAiu, modelMetrics);
        }
    }
}
