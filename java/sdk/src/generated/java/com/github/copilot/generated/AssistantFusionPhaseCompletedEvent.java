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
 * Session event "assistant.fusion_phase_completed". Experimental durable HydraFusion phase output and lossless replay checkpoint.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_completed"; }

    @JsonProperty("data")
    private AssistantFusionPhaseCompletedEventData data;

    public AssistantFusionPhaseCompletedEventData getData() { return data; }
    public void setData(AssistantFusionPhaseCompletedEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseCompletedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AssistantFusionPhaseCompletedEventData.$NullableDeserializer.class)
    public record AssistantFusionPhaseCompletedEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the completed phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase that completed. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** Semantic role assigned to the completed phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executed. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Concrete model that executed the phase. */
        @JsonProperty("model") String model,
        /** Explicit reasoning effort selected for this phase, if supplied. */
        @CopilotExperimental
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Durable outcome status of the phase. */
        @JsonProperty("status") FusionPhaseStatus status,
        /** Provider-normalized textual output produced by the phase. */
        @JsonProperty("content") String content,
        /** Structured judge or critic verdict, when the phase produces one. */
        @JsonProperty("verdict") String verdict,
        /** Elapsed execution time for the phase in milliseconds. */
        @JsonProperty("durationMs") Double durationMs,
        /** Aggregate concrete-model usage consumed by the phase. */
        @JsonProperty("usage") FusionPhaseUsage usage,
        /** Exact provider-normalized message used to reconstruct canonical model history. */
        @JsonProperty("projectionMessage") Object projectionMessage,
        /** Projection action for the exact internal message. */
        @JsonProperty("projectionMode") FusionProjectionMode projectionMode,
        /** Terminal request held outside canonical state until selected by the final commit. */
        @JsonProperty("stagedTerminal") FusionStagedTerminal stagedTerminal,
        /** True when a first pass (Cascade primary or Critique draft) ended with background work outstanding, whether or not the turn then committed it without review; absent otherwise. */
        @CopilotExperimental
        @JsonProperty("endedWithBackgroundWork") Boolean endedWithBackgroundWork,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantFusionPhaseCompletedEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantFusionPhaseCompletedEventData(String fusionId, String phaseId, FusionPhaseKind phaseKind, String role, FusionConversationScope conversationScope, String model, String reasoningEffort, FusionPhaseStatus status, String content, String verdict, Double durationMs, FusionPhaseUsage usage, Object projectionMessage, FusionProjectionMode projectionMode, FusionStagedTerminal stagedTerminal, Boolean endedWithBackgroundWork) {
            this(fusionId, phaseId, phaseKind, role, conversationScope, model, reasoningEffort, status, content, verdict, durationMs, usage, projectionMessage, projectionMode, stagedTerminal, endedWithBackgroundWork, java.util.Set.of());
        }

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
            private String content;
            private String verdict;
            private boolean $presence9;
            private Double durationMs;
            private FusionPhaseUsage usage;
            private Object projectionMessage;
            private boolean $presence12;
            private FusionProjectionMode projectionMode;
            private FusionStagedTerminal stagedTerminal;
            private Boolean endedWithBackgroundWork;

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
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder content(String value) {
                this.content = value;
                return this;
            }

            /**
             * Sets the {@code verdict} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder verdict(String value) {
                this.verdict = value;
                this.$presence9 = true;
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
             * Sets the {@code projectionMessage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder projectionMessage(Object value) {
                this.projectionMessage = value;
                this.$presence12 = true;
                return this;
            }

            /**
             * Sets the {@code projectionMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder projectionMode(FusionProjectionMode value) {
                this.projectionMode = value;
                return this;
            }

            /**
             * Sets the {@code stagedTerminal} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder stagedTerminal(FusionStagedTerminal value) {
                this.stagedTerminal = value;
                return this;
            }

            /**
             * Sets the {@code endedWithBackgroundWork} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder endedWithBackgroundWork(Boolean value) {
                this.endedWithBackgroundWork = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantFusionPhaseCompletedEventData build() {
                if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
                if (phaseId == null) throw new IllegalStateException("Missing required property: phaseId");
                if (phaseKind == null) throw new IllegalStateException("Missing required property: phaseKind");
                if (role == null) throw new IllegalStateException("Missing required property: role");
                if (conversationScope == null) throw new IllegalStateException("Missing required property: conversationScope");
                if (model == null) throw new IllegalStateException("Missing required property: model");
                if (status == null) throw new IllegalStateException("Missing required property: status");
                if (content == null) throw new IllegalStateException("Missing required property: content");
                if (!$presence9) throw new IllegalStateException("Missing required property: verdict");
                if (durationMs == null) throw new IllegalStateException("Missing required property: durationMs");
                if (usage == null) throw new IllegalStateException("Missing required property: usage");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence12 && projectionMessage == null) $explicitNulls.add("projectionMessage");
                return new AssistantFusionPhaseCompletedEventData(fusionId, phaseId, phaseKind, role, conversationScope, model, reasoningEffort, status, content, verdict, durationMs, usage, projectionMessage, projectionMode, stagedTerminal, endedWithBackgroundWork, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("projectionMessage") && projectionMessage == null) values.put("projectionMessage", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AssistantFusionPhaseCompletedEventData> {
            @Override
            public AssistantFusionPhaseCompletedEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(AssistantFusionPhaseCompletedEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("projectionMessage") && tree.get("projectionMessage").isNull()) $explicitNulls.add("projectionMessage");
                return new AssistantFusionPhaseCompletedEventData(readProperty(context, tree, "fusionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "phaseId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "phaseKind", new com.fasterxml.jackson.core.type.TypeReference<FusionPhaseKind>() {}), readProperty(context, tree, "role", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "conversationScope", new com.fasterxml.jackson.core.type.TypeReference<FusionConversationScope>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<FusionPhaseStatus>() {}), readProperty(context, tree, "content", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "verdict", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "durationMs", new com.fasterxml.jackson.core.type.TypeReference<Double>() {}), readProperty(context, tree, "usage", new com.fasterxml.jackson.core.type.TypeReference<FusionPhaseUsage>() {}), readProperty(context, tree, "projectionMessage", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "projectionMode", new com.fasterxml.jackson.core.type.TypeReference<FusionProjectionMode>() {}), readProperty(context, tree, "stagedTerminal", new com.fasterxml.jackson.core.type.TypeReference<FusionStagedTerminal>() {}), readProperty(context, tree, "endedWithBackgroundWork", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
            }

            private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JsonNode tree, String name,
                    com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
                if (!tree.has(name)) return null;
                return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
            }
        }
    }
}
