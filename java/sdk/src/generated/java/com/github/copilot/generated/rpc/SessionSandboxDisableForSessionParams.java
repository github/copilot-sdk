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
 * Request to disable sandboxing for the current session while resolving an active sandbox-bypass permission prompt.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSandboxDisableForSessionParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Identifier of the exact pending sandbox-bypass permission request that authorized the session opt-out. */
    @JsonProperty("requestId") String requestId,
    /** Optional attribution for the permission decision. */
    @JsonProperty("decisionContext") PermissionDecisionContext decisionContext
) {
}
