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
 * Updates the session's live policy without changing persistent host settings.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionManagementConfigureParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Expected workspace for host activation. Required when enabling an interactive CLI session. */
    @JsonProperty("cwd") String cwd,
    /** Enabling resets all session management operations to enabled, then applies overrides. */
    @JsonProperty("enabled") Boolean enabled,
    /** Peer boundary. Preserved when the master switch changes. */
    @JsonProperty("scope") SessionManagementScope scope,
    /** Individual tool overrides, applied after the master switch. */
    @JsonProperty("tools") SessionManagementToolOverrides tools
) {
}
