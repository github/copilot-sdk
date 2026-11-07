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
 * Session event "session.compaction_start". Context window breakdown at the start of LLM-powered conversation compaction
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompactionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "session.compaction_start"; }

    @JsonProperty("data")
    private SessionCompactionStartEventData data;

    public SessionCompactionStartEventData getData() { return data; }
    public void setData(SessionCompactionStartEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompactionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompactionStartEventData(
        /** Identifier shared with the matching compaction completion event */
        @JsonProperty("compactionAttemptId") String compactionAttemptId,
        /** Model identifier used for compaction, when known */
        @JsonProperty("model") String model,
        /** Token count from system message(s) at compaction start */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Token count from non-system messages (user, assistant, tool) at compaction start */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Token count from tool definitions at compaction start */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens,
        /** Total context tokens (system + conversation + tool definitions) at compaction start, when known */
        @JsonProperty("currentTokens") Long currentTokens,
        /** Model context window token limit the compaction is targeting, when known */
        @JsonProperty("tokenLimit") Long tokenLimit,
        /** Destination prompt-token budget when it differs from the effective compaction token limit */
        @JsonProperty("targetPromptTokenLimit") Long targetPromptTokenLimit,
        /** Destination model for a model-switch compaction when it differs from the compacting model; for a HydraFusion turn, the routed root model */
        @JsonProperty("switchTargetModel") String switchTargetModel,
        /** What initiated this compaction, when known */
        @JsonProperty("trigger") CompactionTrigger trigger,
        /** How the compaction ran relative to the conversation: "background" (alongside the turn), "turn_blocking" (the turn waited for it), "pre_prompt" (a non-interactive run waited before recording its prompt), or "direct" (an explicit compaction request). Open set; readers must tolerate other values. */
        @JsonProperty("execution") String execution,
        /** Number of trailing conversation messages (the turn's not-yet-answered input) kept verbatim after the summary instead of being compacted. Omitted when every message at compaction start is summarized. */
        @JsonProperty("retainedInputMessages") Long retainedInputMessages
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param compactionAttemptId Identifier shared with the matching compaction completion event
         * @param model Model identifier used for compaction, when known
         * @param systemTokens Token count from system message(s) at compaction start
         * @param conversationTokens Token count from non-system messages (user, assistant, tool) at compaction start
         * @param toolDefinitionsTokens Token count from tool definitions at compaction start
         * @param currentTokens Total context tokens (system + conversation + tool definitions) at compaction start, when known
         * @param tokenLimit Model context window token limit the compaction is targeting, when known
         * @param targetPromptTokenLimit Destination prompt-token budget when it differs from the effective compaction token limit
         * @param switchTargetModel Destination model for a model-switch compaction when it differs from the compacting model; for a HydraFusion turn, the routed root model
         * @param trigger What initiated this compaction, when known
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCompactionStartEventData(
            String compactionAttemptId,
            String model,
            Long systemTokens,
            Long conversationTokens,
            Long toolDefinitionsTokens,
            Long currentTokens,
            Long tokenLimit,
            Long targetPromptTokenLimit,
            String switchTargetModel,
            CompactionTrigger trigger
        ) {
            this(compactionAttemptId, model, systemTokens, conversationTokens, toolDefinitionsTokens, currentTokens, tokenLimit, targetPromptTokenLimit, switchTargetModel, trigger, null, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCompactionStartEventData {}

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
            private String compactionAttemptId;
            private String model;
            private Long systemTokens;
            private Long conversationTokens;
            private Long toolDefinitionsTokens;
            private Long currentTokens;
            private Long tokenLimit;
            private Long targetPromptTokenLimit;
            private String switchTargetModel;
            private CompactionTrigger trigger;
            private String execution;
            private Long retainedInputMessages;

            /**
             * Sets the {@code compactionAttemptId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder compactionAttemptId(String value) {
                this.compactionAttemptId = value;
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
             * Sets the {@code systemTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder systemTokens(Long value) {
                this.systemTokens = value;
                return this;
            }

            /**
             * Sets the {@code conversationTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder conversationTokens(Long value) {
                this.conversationTokens = value;
                return this;
            }

            /**
             * Sets the {@code toolDefinitionsTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDefinitionsTokens(Long value) {
                this.toolDefinitionsTokens = value;
                return this;
            }

            /**
             * Sets the {@code currentTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder currentTokens(Long value) {
                this.currentTokens = value;
                return this;
            }

            /**
             * Sets the {@code tokenLimit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tokenLimit(Long value) {
                this.tokenLimit = value;
                return this;
            }

            /**
             * Sets the {@code targetPromptTokenLimit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder targetPromptTokenLimit(Long value) {
                this.targetPromptTokenLimit = value;
                return this;
            }

            /**
             * Sets the {@code switchTargetModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder switchTargetModel(String value) {
                this.switchTargetModel = value;
                return this;
            }

            /**
             * Sets the {@code trigger} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder trigger(CompactionTrigger value) {
                this.trigger = value;
                return this;
            }

            /**
             * Sets the {@code execution} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder execution(String value) {
                this.execution = value;
                return this;
            }

            /**
             * Sets the {@code retainedInputMessages} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder retainedInputMessages(Long value) {
                this.retainedInputMessages = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCompactionStartEventData build() {
                return new SessionCompactionStartEventData(compactionAttemptId, model, systemTokens, conversationTokens, toolDefinitionsTokens, currentTokens, tokenLimit, targetPromptTokenLimit, switchTargetModel, trigger, execution, retainedInputMessages);
            }
        }
    }
}
