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
 * Whether to enable full allow-all permissions for the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsSetAllowAllParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Whether to enable full allow-all permissions */
    @JsonProperty("enabled") Boolean enabled,
    /** Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers. */
    @JsonProperty("source") PermissionsSetAllowAllSource source
) {
}
