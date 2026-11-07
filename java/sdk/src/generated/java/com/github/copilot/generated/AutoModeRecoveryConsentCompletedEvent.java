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
 * Session event "auto_mode_recovery_consent.completed". Recovery consent offer closed; clients must dismiss its UI. Completion does not imply consent or recovery success. Events belong to the originating live session; resume can invalidate an offer without this event.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AutoModeRecoveryConsentCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "auto_mode_recovery_consent.completed"; }

    @JsonProperty("data")
    private AutoModeRecoveryConsentCompletedEventData data;

    public AutoModeRecoveryConsentCompletedEventData getData() { return data; }
    public void setData(AutoModeRecoveryConsentCompletedEventData data) { this.data = data; }

    /** Data payload for {@link AutoModeRecoveryConsentCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AutoModeRecoveryConsentCompletedEventData(
        /** Identifier of the settled offer; clients must dismiss the matching consent UI. */
        @JsonProperty("requestId") String requestId,
        /** Settlement reason, distinguishing user acceptance/decline and saved-preference acceptance from cancellation, handler loss, supersession, and emergency disablement. mode_changed withdraws an unanswered offer when the session enters Autopilot before recovery admission. */
        @JsonProperty("reason") String reason,
        /** User response when one was supplied. Absent when the runtime settled the offer without a user answer. */
        @JsonProperty("response") AutoModeSwitchResponse response
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AutoModeRecoveryConsentCompletedEventData {}

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
            private String reason;
            private AutoModeSwitchResponse response;

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
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(String value) {
                this.reason = value;
                return this;
            }

            /**
             * Sets the {@code response} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder response(AutoModeSwitchResponse value) {
                this.response = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AutoModeRecoveryConsentCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                return new AutoModeRecoveryConsentCompletedEventData(requestId, reason, response);
            }
        }
    }
}
