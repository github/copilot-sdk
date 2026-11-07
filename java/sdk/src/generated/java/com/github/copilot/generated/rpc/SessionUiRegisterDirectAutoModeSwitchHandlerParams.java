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
 * Request parameters for the {@code session.ui.registerDirectAutoModeSwitchHandler} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiRegisterDirectAutoModeSwitchHandlerParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opt into active-request Auto recovery before starting a turn. Attach the handler separately, show the failure reason, and return the user's choice for the originating live session. Require a positive acknowledgment in the registration result. Dismiss offers on auto_mode_recovery_consent.completed and on session resume or disconnect. Omitted or false preserves legacy rate-limit switching. */
    @JsonProperty("autoModeRecovery") Boolean autoModeRecovery
) {

    /**
     * Creates a direct Auto-switch registration without recovery enrollment.
     *
     * @param sessionId Target session identifier
     */
    public SessionUiRegisterDirectAutoModeSwitchHandlerParams(
        String sessionId
    ) {
        this(sessionId, null);
    }
}
