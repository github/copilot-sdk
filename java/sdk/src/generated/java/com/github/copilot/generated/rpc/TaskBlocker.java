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
 * Structured reason that the task cannot continue without intervention
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskBlocker(
    /** Category of intervention that blocked the task */
    @JsonProperty("kind") TaskBlockerKind kind,
    /** Controlled reason for the current blocked state */
    @JsonProperty("reason") PermissionRecoveryReason reason,
    /** Whether a later user response or steering message can resume the task */
    @JsonProperty("resumable") Boolean resumable,
    /** Permission-recovery episode that produced this blocker */
    @JsonProperty("permissionRecovery") PermissionRecoveryData permissionRecovery
) {
}
