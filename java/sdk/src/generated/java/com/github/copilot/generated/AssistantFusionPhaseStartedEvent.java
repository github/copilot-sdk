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
 * Session event "assistant.fusion_phase_started". Experimental transient HydraFusion phase/model/role signal.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_started"; }

    @JsonProperty("data")
    private AssistantFusionPhaseStartedEventData data;

    public AssistantFusionPhaseStartedEventData getData() { return data; }
    public void setData(AssistantFusionPhaseStartedEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseStartedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseStartedEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the concrete phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase being executed. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** HydraFusion orchestration pattern containing the phase. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Semantic role assigned to the phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executes. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Concrete model executing the phase. */
        @JsonProperty("model") String model,
        /** Explicit reasoning effort selected for this phase, if supplied. */
        @CopilotExperimental
        @JsonProperty("reasoningEffort") String reasoningEffort
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantFusionPhaseStartedEventData {}

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
            private FusionPattern pattern;
            private String role;
            private FusionConversationScope conversationScope;
            private String model;
            private String reasoningEffort;

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
             * Sets the {@code pattern} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder pattern(FusionPattern value) {
                this.pattern = value;
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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantFusionPhaseStartedEventData build() {
                if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
                if (phaseId == null) throw new IllegalStateException("Missing required property: phaseId");
                if (phaseKind == null) throw new IllegalStateException("Missing required property: phaseKind");
                if (pattern == null) throw new IllegalStateException("Missing required property: pattern");
                if (role == null) throw new IllegalStateException("Missing required property: role");
                if (conversationScope == null) throw new IllegalStateException("Missing required property: conversationScope");
                if (model == null) throw new IllegalStateException("Missing required property: model");
                return new AssistantFusionPhaseStartedEventData(fusionId, phaseId, phaseKind, pattern, role, conversationScope, model, reasoningEffort);
            }
        }
    }
}
