/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Parameters for acquiring or releasing the queued-lane drain pause. Acquisition is exclusive and non-idempotent: `paused: true` against an already-paused session fails with `queue_already_paused`. The pause is never released automatically — it is not tied to the caller's lifetime, so a client that exits without sending `paused: false` leaves the lane frozen. Release is unowned: `paused: false` clears the pause for any caller, including one that never acquired it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueueSetDrainPausedParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("paused") Boolean paused
) {
}
