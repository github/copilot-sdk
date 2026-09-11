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
    }
}
