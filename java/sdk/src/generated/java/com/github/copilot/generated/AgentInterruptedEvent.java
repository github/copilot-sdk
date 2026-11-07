/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "agent.interrupted". Metadata for work the user interrupted while the agent was running
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AgentInterruptedEvent extends SessionEvent {

    @Override
    public String getType() { return "agent.interrupted"; }

    @JsonProperty("data")
    private AgentInterruptedEventData data;

    public AgentInterruptedEventData getData() { return data; }
    public void setData(AgentInterruptedEventData data) { this.data = data; }

    /** Data payload for {@link AgentInterruptedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AgentInterruptedEventData(
        /** What the agent was doing when the user interrupted it */
        @JsonProperty("activity") AgentInterruptedActivity activity,
        /** How long the interrupted work had been running, in milliseconds */
        @JsonProperty("elapsedMs") Double elapsedMs,
        /** Zero-based agentic-loop iteration the interrupt landed in */
        @JsonProperty("turn") Long turn,
        /** For an interrupted model call: the model the request targeted */
        @JsonProperty("model") String model,
        /** For an interrupted model call: the provider endpoint the request targeted */
        @JsonProperty("apiEndpoint") String apiEndpoint,
        /** For an interrupted model call: the transport the request used */
        @JsonProperty("transport") ModelCallFailureTransport transport,
        /** For an interrupted model call: the reasoning effort the request asked for */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** For an interrupted model call: whether the user interrupted before any token arrived or while the response was streaming */
        @JsonProperty("cancelPhase") AgentInterruptedCancelPhase cancelPhase,
        /** For a mid-stream interrupt: the observed time to first observable output, in milliseconds. Deliberately distinct from the `ttftMs` reported on a successful model call, which measures time to first stream event. */
        @JsonProperty("outputTtftMs") Double outputTtftMs,
        /** Names of the tools that were still running. More than one when the model requested a parallel fan-out. */
        @JsonProperty("toolNames") List<String> toolNames,
        /** Tool call identifiers that were still running */
        @JsonProperty("toolCallIds") List<String> toolCallIds,
        /** Subset of `toolNames` whose tool metadata marks the tool name as safe to record unhashed in telemetry. */
        @JsonProperty("safeToolNames") List<String> safeToolNames,
        /** For an interrupted background-agent batch: how many background sub-agents the stop swept. Counts accepted cancellations, so an agent cancelled as a cascade of its interrupted parent is covered by that parent rather than counted again. */
        @JsonProperty("interruptedAgentCount") Long interruptedAgentCount
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AgentInterruptedEventData {}

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
            private AgentInterruptedActivity activity;
            private Double elapsedMs;
            private Long turn;
            private String model;
            private String apiEndpoint;
            private ModelCallFailureTransport transport;
            private String reasoningEffort;
            private AgentInterruptedCancelPhase cancelPhase;
            private Double outputTtftMs;
            private List<String> toolNames;
            private List<String> toolCallIds;
            private List<String> safeToolNames;
            private Long interruptedAgentCount;

            /**
             * Sets the {@code activity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder activity(AgentInterruptedActivity value) {
                this.activity = value;
                return this;
            }

            /**
             * Sets the {@code elapsedMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder elapsedMs(Double value) {
                this.elapsedMs = value;
                return this;
            }

            /**
             * Sets the {@code turn} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turn(Long value) {
                this.turn = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code apiEndpoint} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiEndpoint(String value) {
                this.apiEndpoint = value;
                return this;
            }

            /**
             * Sets the {@code transport} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder transport(ModelCallFailureTransport value) {
                this.transport = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
                return this;
            }

            /**
             * Sets the {@code cancelPhase} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cancelPhase(AgentInterruptedCancelPhase value) {
                this.cancelPhase = value;
                return this;
            }

            /**
             * Sets the {@code outputTtftMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outputTtftMs(Double value) {
                this.outputTtftMs = value;
                return this;
            }

            /**
             * Sets the {@code toolNames} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolNames(List<String> value) {
                this.toolNames = value;
                return this;
            }

            /**
             * Sets the {@code toolCallIds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCallIds(List<String> value) {
                this.toolCallIds = value;
                return this;
            }

            /**
             * Sets the {@code safeToolNames} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder safeToolNames(List<String> value) {
                this.safeToolNames = value;
                return this;
            }

            /**
             * Sets the {@code interruptedAgentCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interruptedAgentCount(Long value) {
                this.interruptedAgentCount = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AgentInterruptedEventData build() {
                if (activity == null) throw new IllegalStateException("Missing required property: activity");
                if (elapsedMs == null) throw new IllegalStateException("Missing required property: elapsedMs");
                if (turn == null) throw new IllegalStateException("Missing required property: turn");
                return new AgentInterruptedEventData(activity, elapsedMs, turn, model, apiEndpoint, transport, reasoningEffort, cancelPhase, outputTtftMs, toolNames, toolCallIds, safeToolNames, interruptedAgentCount);
            }
        }
    }
}
