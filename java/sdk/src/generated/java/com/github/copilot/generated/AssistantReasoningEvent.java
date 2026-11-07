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
 * Session event "assistant.reasoning". Assistant reasoning content for timeline display with complete thinking text
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantReasoningEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.reasoning"; }

    @JsonProperty("data")
    private AssistantReasoningEventData data;

    public AssistantReasoningEventData getData() { return data; }
    public void setData(AssistantReasoningEventData data) { this.data = data; }

    /** Data payload for {@link AssistantReasoningEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantReasoningEventData(
        /** Unique identifier for this reasoning block */
        @JsonProperty("reasoningId") String reasoningId,
        /** The complete extended thinking text from the model */
        @JsonProperty("content") String content,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** Identifier for the agent loop turn that produced this reasoning, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** Provider's completion / response identifier of the model call that produced this reasoning, matching the apiCallId of that call's assistant.message */
        @JsonProperty("apiCallId") String apiCallId,
        /** Model that produced this reasoning, if known */
        @JsonProperty("model") String model,
        /** Whether the model call that produced this reasoning used a user-managed provider or an on-device model */
        @JsonProperty("isByok") Boolean isByok
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param reasoningId Unique identifier for this reasoning block
         * @param content The complete extended thinking text from the model
         * @param rte Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantReasoningEventData(
            String reasoningId,
            String content,
            Boolean rte
        ) {
            this(reasoningId, content, rte, null, null, null, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantReasoningEventData {}

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
            private String reasoningId;
            private String content;
            private Boolean rte;
            private String turnId;
            private String apiCallId;
            private String model;
            private Boolean isByok;

            /**
             * Sets the {@code reasoningId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningId(String value) {
                this.reasoningId = value;
                return this;
            }

            /**
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder content(String value) {
                this.content = value;
                return this;
            }

            /**
             * Sets the {@code rte} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rte(Boolean value) {
                this.rte = value;
                return this;
            }

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

            /**
             * Sets the {@code apiCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiCallId(String value) {
                this.apiCallId = value;
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
             * Sets the {@code isByok} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isByok(Boolean value) {
                this.isByok = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantReasoningEventData build() {
                if (reasoningId == null) throw new IllegalStateException("Missing required property: reasoningId");
                if (content == null) throw new IllegalStateException("Missing required property: content");
                return new AssistantReasoningEventData(reasoningId, content, rte, turnId, apiCallId, model, isByok);
            }
        }
    }
}
