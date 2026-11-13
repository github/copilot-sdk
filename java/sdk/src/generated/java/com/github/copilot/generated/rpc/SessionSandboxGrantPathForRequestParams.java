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
 * Request to accept the sandbox path grant offered on an active sandbox escalation permission prompt.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSandboxGrantPathForRequestParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Identifier of the exact pending sandbox escalation permission request whose sandboxPathGrant to accept. */
    @JsonProperty("requestId") String requestId,
    /** Optional attribution for the permission decision. */
    @JsonProperty("decisionContext") PermissionDecisionContext decisionContext
) {
}
