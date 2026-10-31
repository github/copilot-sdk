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
 * Privacy-safe reference to a permission-protected action that remains blocked for the current Autopilot epoch
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BlockedPermissionAction(
    /** Permission-recovery episode that owns this blocked action */
    @JsonProperty("episodeId") String episodeId,
    /** Controlled permission request kind for the blocked action */
    @JsonProperty("permissionKind") String permissionKind,
    /** Recovery transition that made the action binding for the epoch */
    @JsonProperty("recoveryReason") PermissionRecoveryReason recoveryReason,
    /** Tool call that produced the final blocked permission request. Consumers can join this identifier to labeled permission and tool events for user-facing details. */
    @JsonProperty("toolCallId") String toolCallId
) {
}
