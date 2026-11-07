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
 * Session event "session.quota_observation". A provider-owned quota observation, distinct from per-call usage and charge accounting.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionQuotaObservationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.quota_observation"; }

    @JsonProperty("data")
    private SessionQuotaObservationEventData data;

    public SessionQuotaObservationEventData getData() { return data; }
    public void setData(SessionQuotaObservationEventData data) { this.data = data; }

    /** Data payload for {@link SessionQuotaObservationEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionQuotaObservationEventData(
        /** The admitted provider's state observation. Admission balances and percentages remain absent; separate monthly usage may report its own amount and cycle. */
        @JsonProperty("observation") ProviderQuotaState observation
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionQuotaObservationEventData {}

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
            private ProviderQuotaState observation;

            /**
             * Sets the {@code observation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder observation(ProviderQuotaState value) {
                this.observation = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionQuotaObservationEventData build() {
                if (observation == null) throw new IllegalStateException("Missing required property: observation");
                return new SessionQuotaObservationEventData(observation);
            }
        }
    }
}
