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
    }
}
