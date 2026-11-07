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
 * Session event "model.call_finished". Final lifecycle outcome for one logical model dispatch. A logical dispatch may include internal reconnect or fallback work, so event count is not provider HTTP-request count.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ModelCallFinishedEvent extends SessionEvent {

    @Override
    public String getType() { return "model.call_finished"; }

    @JsonProperty("data")
    private ModelCallFinishedEventData data;

    public ModelCallFinishedEventData getData() { return data; }
    public void setData(ModelCallFinishedEventData data) { this.data = data; }

    /** Data payload for {@link ModelCallFinishedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ModelCallFinishedEventData(
        /** Agent-loop iteration within the interaction that initiated the model dispatch */
        @JsonProperty("turnId") String turnId,
        /** Identifier of the user interaction that owns the model dispatch, matching assistant.turn_start.interactionId when available */
        @JsonProperty("interactionId") String interactionId,
        /** Monotonic elapsed time spent in the logical model dispatch, including any internal transport reconnect or fallback and excluding orchestrator retry backoff, tool execution, confirmations, and post-response processing */
        @JsonProperty("dispatchDurationMs") Double dispatchDurationMs,
        /** Final outcome after post-response acceptance processing */
        @JsonProperty("outcome") ModelCallFinishedOutcome outcome,
        /** Whether an accepted successful response requested the exact name and command semantics of a built-in file edit tool, including an external tool explicitly replacing that built-in name. Absent when the logical dispatch did not produce an accepted response. */
        @JsonProperty("containsBuiltInFileEditRequest") Boolean containsBuiltInFileEditRequest,
        /** Version of the built-in file-edit semantic classifier used for this event */
        @JsonProperty("editClassifierVersion") Long editClassifierVersion
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ModelCallFinishedEventData {}

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
            private String turnId;
            private String interactionId;
            private Double dispatchDurationMs;
            private ModelCallFinishedOutcome outcome;
            private Boolean containsBuiltInFileEditRequest;
            private Long editClassifierVersion;

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
             * Sets the {@code interactionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionId(String value) {
                this.interactionId = value;
                return this;
            }

            /**
             * Sets the {@code dispatchDurationMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder dispatchDurationMs(Double value) {
                this.dispatchDurationMs = value;
                return this;
            }

            /**
             * Sets the {@code outcome} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outcome(ModelCallFinishedOutcome value) {
                this.outcome = value;
                return this;
            }

            /**
             * Sets the {@code containsBuiltInFileEditRequest} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder containsBuiltInFileEditRequest(Boolean value) {
                this.containsBuiltInFileEditRequest = value;
                return this;
            }

            /**
             * Sets the {@code editClassifierVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder editClassifierVersion(Long value) {
                this.editClassifierVersion = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ModelCallFinishedEventData build() {
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                if (dispatchDurationMs == null) throw new IllegalStateException("Missing required property: dispatchDurationMs");
                if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
                if (editClassifierVersion == null) throw new IllegalStateException("Missing required property: editClassifierVersion");
                return new ModelCallFinishedEventData(turnId, interactionId, dispatchDurationMs, outcome, containsBuiltInFileEditRequest, editClassifierVersion);
            }
        }
    }
}
