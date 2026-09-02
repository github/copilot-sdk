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
 * Permission mode to apply for the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsSetModeParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Permission mode to apply */
    @JsonProperty("mode") PermissionMode mode,
    /** Optional judge model id for assisted mode. When omitted, the session resolves the provider default: `gpt-5.5` for CAPI sessions and the active session model for BYOK sessions. */
    @JsonProperty("assistedApprovalModel") String assistedApprovalModel,
    /** Optional source for permission-mode telemetry. Defaults to `rpc` when omitted for SDK callers. */
    @JsonProperty("source") PermissionModeSource source
) {
}
