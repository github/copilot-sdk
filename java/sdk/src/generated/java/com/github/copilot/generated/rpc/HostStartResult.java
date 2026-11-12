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
 * Listener readiness, returned only after binding and the supervised participant's SDK handshake.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostStartResult(
    /** Caller-generated listener UUID. */
    @JsonProperty("hostId") String hostId,
    /** Actual bound WebSocket URL, including the allocated port. */
    @JsonProperty("url") String url,
    /** Secret connection token, absent when authentication is disabled. */
    @JsonProperty("token") String token,
    /** GitHub Mission Control environment ID, present when its relay transport is ready. */
    @JsonProperty("environmentId") String environmentId,
    /** Separate host process ID, when provided by a legacy runtime. Absent for in-process listeners. */
    @JsonProperty("pid") Long pid
) {
}
