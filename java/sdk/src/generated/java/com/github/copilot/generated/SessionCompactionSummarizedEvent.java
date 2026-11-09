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
 * Session event "session.compaction_summarized". A background compaction's summarizer settled; its summary is not applied to the conversation yet. The matching session.compaction_complete follows once it is applied (or discarded) and remains authoritative.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompactionSummarizedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.compaction_summarized"; }

    @JsonProperty("data")
    private SessionCompactionSummarizedEventData data;

    public SessionCompactionSummarizedEventData getData() { return data; }
    public void setData(SessionCompactionSummarizedEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompactionSummarizedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompactionSummarizedEventData(
        /** Identifier shared with the matching compaction start and completion events */
        @JsonProperty("compactionAttemptId") String compactionAttemptId,
        /** Whether the summarizer produced a summary that can be applied */
        @JsonProperty("success") Boolean success,
        /** How the summarizer settled: "succeeded", "failed", or "cancelled". Open set; readers must tolerate other values. */
        @JsonProperty("outcome") String outcome,
        /** Bounded reason for a failed summarizer, using the same values as the completion event's failureReason. Unknown future values are preserved. */
        @JsonProperty("failureReason") String failureReason,
        /** Error message if the summarizer failed */
        @JsonProperty("error") String error,
        /** How the compaction ran relative to the conversation, as on the start event ("background"). Open set; readers must tolerate other values. */
        @JsonProperty("execution") String execution,
        /** Always true: the settled outcome still awaits being applied, which session.compaction_complete reports */
        @JsonProperty("applyPending") Boolean applyPending
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCompactionSummarizedEventData {}

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
            private Boolean success;
            private String outcome;
            private String failureReason;
            private String error;
            private String execution;
            private Boolean applyPending;

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
             * Sets the {@code success} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder success(Boolean value) {
                this.success = value;
                return this;
            }

            /**
             * Sets the {@code outcome} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outcome(String value) {
                this.outcome = value;
                return this;
            }

            /**
             * Sets the {@code failureReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder failureReason(String value) {
                this.failureReason = value;
                return this;
            }

            /**
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder error(String value) {
                this.error = value;
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
             * Sets the {@code applyPending} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder applyPending(Boolean value) {
                this.applyPending = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCompactionSummarizedEventData build() {
                if (compactionAttemptId == null) throw new IllegalStateException("Missing required property: compactionAttemptId");
                if (success == null) throw new IllegalStateException("Missing required property: success");
                if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
                if (execution == null) throw new IllegalStateException("Missing required property: execution");
                if (applyPending == null) throw new IllegalStateException("Missing required property: applyPending");
                return new SessionCompactionSummarizedEventData(compactionAttemptId, success, outcome, failureReason, error, execution, applyPending);
            }
        }
    }
}
