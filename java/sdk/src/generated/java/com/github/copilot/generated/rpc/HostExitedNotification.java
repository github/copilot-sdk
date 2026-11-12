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
 * Reports a supervised listener's hosting-task termination and cleanup outcome.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostExitedNotification(
    /** Listener UUID. */
    @JsonProperty("hostId") String hostId,
    /** Cause of termination. */
    @JsonProperty("reason") HostExitReason reason,
    /** Process exit status when available; absent for in-process listener tasks. */
    @JsonProperty("exitCode") Long exitCode,
    /** Explicit startup or teardown failure, when present. */
    @JsonProperty("error") String error
) {
}
