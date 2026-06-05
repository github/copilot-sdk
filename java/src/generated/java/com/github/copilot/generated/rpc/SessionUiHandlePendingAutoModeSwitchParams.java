/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Request ID of a pending `auto_mode_switch.requested` event and the user's response.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiHandlePendingAutoModeSwitchParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The unique request ID from the auto_mode_switch.requested event */
    @JsonProperty("requestId") String requestId,
    /** User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline). */
    @JsonProperty("response") UIAutoModeSwitchResponse response
) {
}
