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
 * Session event "auto_mode_recovery_consent.requested". Recovery consent for a still-active request. Respond using session.ui.handlePendingAutoModeSwitch. The offer has no timer. Entering Autopilot before recovery admission withdraws it. Dismiss it on auto_mode_recovery_consent.completed or on session resume, rebuild, or disconnect; resume can invalidate it without a completed event.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AutoModeRecoveryConsentRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "auto_mode_recovery_consent.requested"; }

    @JsonProperty("data")
    private AutoModeRecoveryConsentRequestedEventData data;

    public AutoModeRecoveryConsentRequestedEventData getData() { return data; }
    public void setData(AutoModeRecoveryConsentRequestedEventData data) { this.data = data; }

    /** Data payload for {@link AutoModeRecoveryConsentRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AutoModeRecoveryConsentRequestedEventData(
        /** One-use offer or recovery episode identifier bound to the originating live session. It cannot be carried across resume or used to replay a settled request. */
        @JsonProperty("requestId") String requestId,
        /** Explicit model selected for the failed request before Auto recovery. */
        @JsonProperty("originalModel") String originalModel,
        /** Whether this offer permits remembering consent with yes_always. Currently true only for per-model rate limits. */
        @JsonProperty("allowAlways") Boolean allowAlways,
        /** Original logical interaction identity, when available. Do not infer it from transcript order. */
        @JsonProperty("interactionId") String interactionId,
        /** Initiating user-message identity, when available, distinct from later steering messages. */
        @JsonProperty("originatingMessageId") String originatingMessageId,
        /** Eligible upstream failure code: model_degraded, model_overloaded, or user_model_rate_limited. */
        @JsonProperty("errorCode") String errorCode,
        /** Rate-limit reset information when supplied by the provider. This is not a consent deadline. */
        @JsonProperty("retryAfterSeconds") Double retryAfterSeconds
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AutoModeRecoveryConsentRequestedEventData {}

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
            private String requestId;
            private String originalModel;
            private Boolean allowAlways;
            private String interactionId;
            private String originatingMessageId;
            private String errorCode;
            private Double retryAfterSeconds;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code originalModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder originalModel(String value) {
                this.originalModel = value;
                return this;
            }

            /**
             * Sets the {@code allowAlways} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder allowAlways(Boolean value) {
                this.allowAlways = value;
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
             * Sets the {@code originatingMessageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder originatingMessageId(String value) {
                this.originatingMessageId = value;
                return this;
            }

            /**
             * Sets the {@code errorCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorCode(String value) {
                this.errorCode = value;
                return this;
            }

            /**
             * Sets the {@code retryAfterSeconds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder retryAfterSeconds(Double value) {
                this.retryAfterSeconds = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AutoModeRecoveryConsentRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (originalModel == null) throw new IllegalStateException("Missing required property: originalModel");
                if (allowAlways == null) throw new IllegalStateException("Missing required property: allowAlways");
                if (errorCode == null) throw new IllegalStateException("Missing required property: errorCode");
                return new AutoModeRecoveryConsentRequestedEventData(requestId, originalModel, allowAlways, interactionId, originatingMessageId, errorCode, retryAfterSeconds);
            }
        }
    }
}
