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
 * Registers the calling SDK connection's network callbacks, replacing any previous registration.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkSetProviderParams(
    /** Traffic the callbacks serve; defaults to `connection`. */
    @JsonProperty("scope") NetworkScope scope,
    /** Present when the client answers `network.resolveProxy`. */
    @JsonProperty("resolveProxy") NetworkResolveProxyOptions resolveProxy,
    /** Present when the client answers `network.authenticateNegotiate`. */
    @JsonProperty("authenticateNegotiate") NetworkAuthenticateNegotiateOptions authenticateNegotiate
) {
}
