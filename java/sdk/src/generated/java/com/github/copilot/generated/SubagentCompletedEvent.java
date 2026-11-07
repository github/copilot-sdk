/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "subagent.completed". Sub-agent completion details for successful execution
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.completed"; }

    @JsonProperty("data")
    private SubagentCompletedEventData data;

    public SubagentCompletedEventData getData() { return data; }
    public void setData(SubagentCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentCompletedEventData(
        /** Tool call ID of the parent tool invocation that spawned this sub-agent */
        @JsonProperty("toolCallId") String toolCallId,
        /** Internal name of the sub-agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the sub-agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** Model used by the sub-agent */
        @JsonProperty("model") String model,
        /** First model for which the sub-agent started an inference request, when one was dispatched */
        @JsonProperty("firstDispatchedModel") String firstDispatchedModel,
        /** Concrete model the user configured for this sub-agent via `/subagents`, when present */
        @JsonProperty("configuredModelPreference") String configuredModelPreference,
        /** Explicit model supplied by the parent agent on the task call, when present */
        @JsonProperty("explicitModelOverride") String explicitModelOverride,
        /** Whether the explicit task-call model matched the user's configured preference */
        @JsonProperty("explicitModelMatchesPreference") Boolean explicitModelMatchesPreference,
        /** Why an explicit task-call model did not become the effective model */
        @JsonProperty("modelOverrideReason") String modelOverrideReason,
        /** Authority or runtime mechanism responsible for sub-agent model selection */
        @JsonProperty("modelSelectionSource") SubagentModelSelectionSource modelSelectionSource,
        /** Whether the first model actually dispatched matched the user's configured preference */
        @JsonProperty("configuredModelMatchesActual") Boolean configuredModelMatchesActual,
        /** Total number of tool calls made by the sub-agent */
        @JsonProperty("totalToolCalls") Long totalToolCalls,
        /** Total tokens (input + output) consumed by the sub-agent */
        @JsonProperty("totalTokens") Long totalTokens,
        /** Wall-clock duration of the sub-agent execution in milliseconds */
        @JsonProperty("durationMs") Long durationMs,
        /** Whether the sub-agent was torn down by cancellation - its own abort, or an ancestor being killed - instead of finishing its work. Cancellation is not a failure, so the run still reports completion; this distinguishes a torn-down sub-agent from one that ran to the end. */
        @JsonProperty("cancelled") Boolean cancelled
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SubagentCompletedEventData {}

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
            private String toolCallId;
            private String agentName;
            private String agentDisplayName;
            private String model;
            private String firstDispatchedModel;
            private String configuredModelPreference;
            private String explicitModelOverride;
            private Boolean explicitModelMatchesPreference;
            private String modelOverrideReason;
            private SubagentModelSelectionSource modelSelectionSource;
            private Boolean configuredModelMatchesActual;
            private Long totalToolCalls;
            private Long totalTokens;
            private Long durationMs;
            private Boolean cancelled;

            /**
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

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
             * Sets the {@code firstDispatchedModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder firstDispatchedModel(String value) {
                this.firstDispatchedModel = value;
                return this;
            }

            /**
             * Sets the {@code configuredModelPreference} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder configuredModelPreference(String value) {
                this.configuredModelPreference = value;
                return this;
            }

            /**
             * Sets the {@code explicitModelOverride} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder explicitModelOverride(String value) {
                this.explicitModelOverride = value;
                return this;
            }

            /**
             * Sets the {@code explicitModelMatchesPreference} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder explicitModelMatchesPreference(Boolean value) {
                this.explicitModelMatchesPreference = value;
                return this;
            }

            /**
             * Sets the {@code modelOverrideReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelOverrideReason(String value) {
                this.modelOverrideReason = value;
                return this;
            }

            /**
             * Sets the {@code modelSelectionSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelSelectionSource(SubagentModelSelectionSource value) {
                this.modelSelectionSource = value;
                return this;
            }

            /**
             * Sets the {@code configuredModelMatchesActual} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder configuredModelMatchesActual(Boolean value) {
                this.configuredModelMatchesActual = value;
                return this;
            }

            /**
             * Sets the {@code totalToolCalls} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder totalToolCalls(Long value) {
                this.totalToolCalls = value;
                return this;
            }

            /**
             * Sets the {@code totalTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder totalTokens(Long value) {
                this.totalTokens = value;
                return this;
            }

            /**
             * Sets the {@code durationMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder durationMs(Long value) {
                this.durationMs = value;
                return this;
            }

            /**
             * Sets the {@code cancelled} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cancelled(Boolean value) {
                this.cancelled = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SubagentCompletedEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (agentName == null) throw new IllegalStateException("Missing required property: agentName");
                if (agentDisplayName == null) throw new IllegalStateException("Missing required property: agentDisplayName");
                return new SubagentCompletedEventData(toolCallId, agentName, agentDisplayName, model, firstDispatchedModel, configuredModelPreference, explicitModelOverride, explicitModelMatchesPreference, modelOverrideReason, modelSelectionSource, configuredModelMatchesActual, totalToolCalls, totalTokens, durationMs, cancelled);
            }
        }
    }
}
