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
 * Session event "auto_mode_recovery.outcome". Authoritative final outcome of an active-request Auto recovery episode, not a retry-start notification or manual response regeneration. Normal response/error and session completion signals still settle the logical request.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AutoModeRecoveryOutcomeEvent extends SessionEvent {

    @Override
    public String getType() { return "auto_mode_recovery.outcome"; }

    @JsonProperty("data")
    private AutoModeRecoveryOutcomeEventData data;

    public AutoModeRecoveryOutcomeEventData getData() { return data; }
    public void setData(AutoModeRecoveryOutcomeEventData data) { this.data = data; }

    /** Data payload for {@link AutoModeRecoveryOutcomeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AutoModeRecoveryOutcomeEventData(
        /** Recovery episode identifier, also used by its consent offer when one was displayed. */
        @JsonProperty("requestId") String requestId,
        /** Original upstream failure code that qualified the request for recovery. */
        @JsonProperty("errorCode") String errorCode,
        /** Explicit model selected for the original failed request. */
        @JsonProperty("originalModel") String originalModel,
        /** Final recovery disposition, such as succeeded, failed, declined, cancelled, superseded, or unavailable. mode_changed means entering Autopilot stopped recovery before admission, including after consent was accepted. */
        @JsonProperty("status") String status,
        /** Whether a recovered Auto inference was allowed to proceed, reported on the final outcome rather than as a live start notification. True does not imply success. False when consent or preparation ended without a retry. */
        @JsonProperty("autoModeRecoveryAdmitted") Boolean autoModeRecoveryAdmitted
    ) {
    }
}
