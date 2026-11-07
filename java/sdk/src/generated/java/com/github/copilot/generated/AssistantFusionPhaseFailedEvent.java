/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "assistant.fusion_phase_failed". Experimental durable typed HydraFusion phase failure and degradation transition.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseFailedEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_failed"; }

    @JsonProperty("data")
    private AssistantFusionPhaseFailedEventData data;

    public AssistantFusionPhaseFailedEventData getData() { return data; }
    public void setData(AssistantFusionPhaseFailedEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseFailedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseFailedEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the failed phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase that failed. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** Semantic role assigned to the failed phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executed. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Concrete model that attempted the phase. */
        @JsonProperty("model") String model,
        /** Explicit reasoning effort selected for this phase, if supplied. */
        @CopilotExperimental
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Durable outcome status of the phase. */
        @JsonProperty("status") FusionPhaseStatus status,
        /** Stable machine-readable reason for the phase failure. */
        @JsonProperty("reason") String reason,
        /** Elapsed execution time before the phase failed, in milliseconds. */
        @JsonProperty("durationMs") Double durationMs,
        /** Aggregate concrete-model usage consumed before the failure. */
        @JsonProperty("usage") FusionPhaseUsage usage,
        /** Provider or execution error detail, when available. */
        @JsonProperty("errorMessage") String errorMessage,
        /** Identifier of the fallback phase used to continue the turn after degradation. */
        @JsonProperty("degradedToPhaseId") String degradedToPhaseId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantFusionPhaseFailedEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static final class Builder {
            private Builder() {}
            private String fusionId;
            private String phaseId;
            private FusionPhaseKind phaseKind;
            private String role;
            private FusionConversationScope conversationScope;
            private String model;
            private String reasoningEffort;
            private FusionPhaseStatus status;
            private String reason;
            private Double durationMs;
            private FusionPhaseUsage usage;
            private String errorMessage;
            private String degradedToPhaseId;

            /**
             * Sets the {@code fusionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusionId(String value) {
                this.fusionId = value;
                return this;
            }

            /**
             * Sets the {@code phaseId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder phaseId(String value) {
                this.phaseId = value;
                return this;
            }

            /**
             * Sets the {@code phaseKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder phaseKind(FusionPhaseKind value) {
                this.phaseKind = value;
                return this;
            }

            /**
             * Sets the {@code role} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder role(String value) {
                this.role = value;
                return this;
            }

            /**
             * Sets the {@code conversationScope} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder conversationScope(FusionConversationScope value) {
                this.conversationScope = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
                return this;
            }

            /**
             * Sets the {@code status} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder status(FusionPhaseStatus value) {
                this.status = value;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder reason(String value) {
                this.reason = value;
                return this;
            }

            /**
             * Sets the {@code durationMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder durationMs(Double value) {
                this.durationMs = value;
                return this;
            }

            /**
             * Sets the {@code usage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder usage(FusionPhaseUsage value) {
                this.usage = value;
                return this;
            }

            /**
             * Sets the {@code errorMessage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder errorMessage(String value) {
                this.errorMessage = value;
                return this;
            }

            /**
             * Sets the {@code degradedToPhaseId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder degradedToPhaseId(String value) {
                this.degradedToPhaseId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantFusionPhaseFailedEventData build() {
                if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
                if (phaseId == null) throw new IllegalStateException("Missing required property: phaseId");
                if (phaseKind == null) throw new IllegalStateException("Missing required property: phaseKind");
                if (role == null) throw new IllegalStateException("Missing required property: role");
                if (conversationScope == null) throw new IllegalStateException("Missing required property: conversationScope");
                if (model == null) throw new IllegalStateException("Missing required property: model");
                if (status == null) throw new IllegalStateException("Missing required property: status");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                if (durationMs == null) throw new IllegalStateException("Missing required property: durationMs");
                if (usage == null) throw new IllegalStateException("Missing required property: usage");
                return new AssistantFusionPhaseFailedEventData(fusionId, phaseId, phaseKind, role, conversationScope, model, reasoningEffort, status, reason, durationMs, usage, errorMessage, degradedToPhaseId);
            }
        }
    }
}
