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
 * Session event "session.auto_tier_switch_failed". A transient Auto preference failure emitted when the runtime cannot mint or accept a usable model and token pair. The previously effective preference remains active, so SDK clients can surface a non-blocking failure without changing their committed-tier state. This event is ephemeral and is not persisted or replayed on resume.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutoTierSwitchFailedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.auto_tier_switch_failed"; }

    @JsonProperty("data")
    private SessionAutoTierSwitchFailedEventData data;

    public SessionAutoTierSwitchFailedEventData getData() { return data; }
    public void setData(SessionAutoTierSwitchFailedEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutoTierSwitchFailedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutoTierSwitchFailedEventData(
        /** Auto preference that remains effective after the failed request. */
        @JsonProperty("effectiveAutoTier") AutoTier effectiveAutoTier,
        /** Auto preference that failed to activate, or null when returning to provider-default routing failed. */
        @JsonProperty("requestedAutoTier") AutoTier requestedAutoTier,
        /** Low-cardinality failure outcome reported by Auto resolution. */
        @JsonProperty("reason") AutoTierSwitchFailureReason reason
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionAutoTierSwitchFailedEventData {}

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
            private AutoTier effectiveAutoTier;
            private AutoTier requestedAutoTier;
            private boolean $presence1;
            private AutoTierSwitchFailureReason reason;

            /**
             * Sets the {@code effectiveAutoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder effectiveAutoTier(AutoTier value) {
                this.effectiveAutoTier = value;
                return this;
            }

            /**
             * Sets the {@code requestedAutoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestedAutoTier(AutoTier value) {
                this.requestedAutoTier = value;
                this.$presence1 = true;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(AutoTierSwitchFailureReason value) {
                this.reason = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionAutoTierSwitchFailedEventData build() {
                if (!$presence1) throw new IllegalStateException("Missing required property: requestedAutoTier");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                return new SessionAutoTierSwitchFailedEventData(effectiveAutoTier, requestedAutoTier, reason);
            }
        }
    }
}
