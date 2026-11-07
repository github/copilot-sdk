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
 * A pre-registered OAuth client for this attempt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthFlowStaticClientConfig(
    /** Pre-registered client identifier. */
    @JsonProperty("clientId") String clientId,
    /** Ephemeral secret that the runtime never persists. */
    @JsonProperty("clientSecret") String clientSecret,
    /** Whether this client has no secret. Defaults to true. */
    @JsonProperty("publicClient") Boolean publicClient,
    /** OAuth grant override. */
    @JsonProperty("grantType") McpOauthLoginGrantType grantType,
    /** Scope used when the server omits its challenge scope. */
    @JsonProperty("scope") String scope
) {
}
