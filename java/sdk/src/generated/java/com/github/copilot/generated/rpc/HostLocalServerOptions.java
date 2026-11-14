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
 * Local WebSocket transport options.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostLocalServerOptions(
    /** Listener hostname. Defaults to 127.0.0.1; explicit non-loopback binds are allowed. */
    @JsonProperty("hostname") String hostname,
    /** Listener port. Omitted or zero requests an OS-allocated port. */
    @JsonProperty("port") Long port,
    /** Nonempty connection token. Generated randomly when required and omitted. */
    @JsonProperty("token") String token,
    /** Require token authentication (default true). Cannot be false with a token. */
    @JsonProperty("requireConnectionToken") Boolean requireConnectionToken
) {
}
