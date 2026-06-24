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
 * Request ID of a pending `exit_plan_mode.requested` event and the user's response.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiHandlePendingExitPlanModeParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The unique request ID from the exit_plan_mode.requested event */
    @JsonProperty("requestId") String requestId,
    /** Schema for the `UIExitPlanModeResponse` type. */
    @JsonProperty("response") UIExitPlanModeResponse response
) {
}
