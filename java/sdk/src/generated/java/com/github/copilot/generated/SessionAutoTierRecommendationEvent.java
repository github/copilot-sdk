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
 * Session event "session.auto_tier_recommendation". Live-only Auto preference recommendation from Copilot API after a successful Auto model call.
 * @since 1.0.0
 */
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutoTierRecommendationEventData(
        /** Recommended Auto preference. */
        @JsonProperty("recommendedAutoTier") RecommendedAutoTier recommendedAutoTier
    ) {
    }
}
