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
 * Configuration for the runtime-managed remote-control singleton.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteControlConfig(
    /** Whether remote export should be enabled. */
    @JsonProperty("remote") Boolean remote,
    /** Whether the MC session may steer the local session (write mode). */
    @JsonProperty("steerable") Boolean steerable,
    /** Whether the user explicitly requested remote (vs. implicit session-sync). Controls warning surfacing for missing-repo cases. */
    @JsonProperty("explicit") Boolean explicit,
    /** When true, suppresses timeline messages on successful setup. */
    @JsonProperty("silent") Boolean silent,
    /** Existing Mission Control task ID to attach the exported session to. */
    @JsonProperty("taskId") String taskId,
    /** Reattach to an existing MC session without creating a new one. */
    @JsonProperty("existingMcSession") RemoteControlConfigExistingMcSession existingMcSession
) {
}
