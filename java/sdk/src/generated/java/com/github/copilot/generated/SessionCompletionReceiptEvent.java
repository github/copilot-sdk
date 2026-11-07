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
 * Session event "session.completion_receipt". Behavior-neutral record of structured runtime facts present when an agent completion decision is accepted.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompletionReceiptEvent extends SessionEvent {

    @Override
    public String getType() { return "session.completion_receipt"; }

    @JsonProperty("data")
    private SessionCompletionReceiptEventData data;

    public SessionCompletionReceiptEventData getData() { return data; }
    public void setData(SessionCompletionReceiptEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompletionReceiptEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompletionReceiptEventData(
        /** Version of the completion receipt payload. */
        @JsonProperty("schemaVersion") Long schemaVersion,
        /** One-based accepted completion receipt ordinal in the durable session history. */
        @JsonProperty("attempt") Long attempt,
        /** Identifier of the assistant turn-end event that supplied the accepted completion boundary. This is the receipt's idempotency key, and always equals eventRange.endEventId. */
        @JsonProperty("sourceEventId") String sourceEventId,
        /** Inclusive durable event range summarized by this receipt. */
        @JsonProperty("eventRange") CompletionReceiptEventRange eventRange,
        /** Runtime reason the completion decision was accepted. */
        @JsonProperty("stopReason") CompletionReceiptStopReason stopReason,
        /** Final structured tool completion in the covered range, when one exists. */
        @JsonProperty("finalTool") CompletionReceiptFinalTool finalTool,
        /** Number of successful structured tool completions in the covered range. */
        @JsonProperty("successfulToolCount") Long successfulToolCount,
        /** Number of failed structured tool completions in the covered range. */
        @JsonProperty("failedToolCount") Long failedToolCount
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCompletionReceiptEventData {}

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
            private Long schemaVersion;
            private Long attempt;
            private String sourceEventId;
            private CompletionReceiptEventRange eventRange;
            private CompletionReceiptStopReason stopReason;
            private CompletionReceiptFinalTool finalTool;
            private Long successfulToolCount;
            private Long failedToolCount;

            /**
             * Sets the {@code schemaVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder schemaVersion(Long value) {
                this.schemaVersion = value;
                return this;
            }

            /**
             * Sets the {@code attempt} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder attempt(Long value) {
                this.attempt = value;
                return this;
            }

            /**
             * Sets the {@code sourceEventId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder sourceEventId(String value) {
                this.sourceEventId = value;
                return this;
            }

            /**
             * Sets the {@code eventRange} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder eventRange(CompletionReceiptEventRange value) {
                this.eventRange = value;
                return this;
            }

            /**
             * Sets the {@code stopReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder stopReason(CompletionReceiptStopReason value) {
                this.stopReason = value;
                return this;
            }

            /**
             * Sets the {@code finalTool} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder finalTool(CompletionReceiptFinalTool value) {
                this.finalTool = value;
                return this;
            }

            /**
             * Sets the {@code successfulToolCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder successfulToolCount(Long value) {
                this.successfulToolCount = value;
                return this;
            }

            /**
             * Sets the {@code failedToolCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder failedToolCount(Long value) {
                this.failedToolCount = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCompletionReceiptEventData build() {
                if (schemaVersion == null) throw new IllegalStateException("Missing required property: schemaVersion");
                if (attempt == null) throw new IllegalStateException("Missing required property: attempt");
                if (sourceEventId == null) throw new IllegalStateException("Missing required property: sourceEventId");
                if (eventRange == null) throw new IllegalStateException("Missing required property: eventRange");
                if (stopReason == null) throw new IllegalStateException("Missing required property: stopReason");
                if (successfulToolCount == null) throw new IllegalStateException("Missing required property: successfulToolCount");
                if (failedToolCount == null) throw new IllegalStateException("Missing required property: failedToolCount");
                return new SessionCompletionReceiptEventData(schemaVersion, attempt, sourceEventId, eventRange, stopReason, finalTool, successfulToolCount, failedToolCount);
            }
        }
    }
}
