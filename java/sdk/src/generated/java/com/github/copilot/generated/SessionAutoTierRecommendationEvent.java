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
 * Session event "session.auto_tier_recommendation". Live-only Auto preference recommendation from Copilot API after a successful Auto model call.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutoTierRecommendationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.auto_tier_recommendation"; }

    @JsonProperty("data")
    private SessionAutoTierRecommendationEventData data;

    public SessionAutoTierRecommendationEventData getData() { return data; }
    public void setData(SessionAutoTierRecommendationEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutoTierRecommendationEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutoTierRecommendationEventData(
        /** Recommended Auto preference. */
        @JsonProperty("recommendedAutoTier") RecommendedAutoTier recommendedAutoTier
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionAutoTierRecommendationEventData {}

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
            private RecommendedAutoTier recommendedAutoTier;

            /**
             * Sets the {@code recommendedAutoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder recommendedAutoTier(RecommendedAutoTier value) {
                this.recommendedAutoTier = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionAutoTierRecommendationEventData build() {
                if (recommendedAutoTier == null) throw new IllegalStateException("Missing required property: recommendedAutoTier");
                return new SessionAutoTierRecommendationEventData(recommendedAutoTier);
            }
        }
    }
}
