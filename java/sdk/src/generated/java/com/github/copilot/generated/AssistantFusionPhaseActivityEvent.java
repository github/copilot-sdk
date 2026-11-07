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
 * Session event "assistant.fusion_phase_activity". Experimental content-safe activity signal for a running HydraFusion phase.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseActivityEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_activity"; }

    @JsonProperty("data")
    private AssistantFusionPhaseActivityEventData data;

    public AssistantFusionPhaseActivityEventData getData() { return data; }
    public void setData(AssistantFusionPhaseActivityEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseActivityEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseActivityEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the concrete phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase currently executing. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** HydraFusion orchestration pattern containing the phase. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Semantic role assigned to the phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executes. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Kind of real activity observed. */
        @JsonProperty("activity") FusionPhaseActivityKind activity,
        /** Cumulative private response bytes observed for this model call. The event never includes response text. */
        @JsonProperty("totalResponseSizeBytes") Long totalResponseSizeBytes,
        /** Opaque hashed correlation token for matching tool-started and tool-completed activity within this Fusion activity stream. It is not the tool call identifier exposed by tool lifecycle events. */
        @JsonProperty("toolCallId") String toolCallId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantFusionPhaseActivityEventData {}

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
            private FusionPhaseActivityKind activity;
            private Long totalResponseSizeBytes;
            private String toolCallId;

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
             * Sets the {@code activity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder activity(FusionPhaseActivityKind value) {
                this.activity = value;
                return this;
            }

            /**
             * Sets the {@code totalResponseSizeBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder totalResponseSizeBytes(Long value) {
                this.totalResponseSizeBytes = value;
                return this;
            }

            /**
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantFusionPhaseActivityEventData build() {
                if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
                if (phaseId == null) throw new IllegalStateException("Missing required property: phaseId");
                if (phaseKind == null) throw new IllegalStateException("Missing required property: phaseKind");
                if (pattern == null) throw new IllegalStateException("Missing required property: pattern");
                if (role == null) throw new IllegalStateException("Missing required property: role");
                if (conversationScope == null) throw new IllegalStateException("Missing required property: conversationScope");
                if (activity == null) throw new IllegalStateException("Missing required property: activity");
                return new AssistantFusionPhaseActivityEventData(fusionId, phaseId, phaseKind, pattern, role, conversationScope, activity, totalResponseSizeBytes, toolCallId);
            }
        }
    }
}
