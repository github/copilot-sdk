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
 * The `status.get` response, including optional cached error-reporting policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProtocolStatusResponse(
    /** The runtime package version. */
    @JsonProperty("version") String version,
    /** The runtime SDK protocol version. */
    @JsonProperty("protocolVersion") Long protocolVersion,
    /** Cached host policy. This field does not grant ownership or confirm network delivery. */
    @JsonProperty("hostDiagnostics") HostDiagnostics hostDiagnostics
) {
}
