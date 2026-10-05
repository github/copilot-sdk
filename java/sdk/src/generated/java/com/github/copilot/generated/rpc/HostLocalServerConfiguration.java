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
 * Normalized local WebSocket listener settings.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record HostLocalServerConfiguration(
    /** Hostname or IP address to bind. */
    @JsonProperty("hostname") String hostname,
    /** Port to bind, with zero requesting OS allocation. */
    @JsonProperty("port") Long port,
    /** Secret connection token, absent when authentication is disabled. */
    @JsonProperty("token") String token,
    /** Whether the listener requires token authentication. */
    @JsonProperty("requireConnectionToken") Boolean requireConnectionToken
) {
}
