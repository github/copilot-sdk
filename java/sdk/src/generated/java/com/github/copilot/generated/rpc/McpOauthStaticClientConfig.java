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
 * An OAuth client a server configuration pins, without its secret.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthStaticClientConfig(
    /** Configured OAuth client identifier. */
    @JsonProperty("clientId") String clientId,
    /** Whether the configuration declares a public client. Absent when the configuration does not say, which the flow reads as a public client. */
    @JsonProperty("publicClient") Boolean publicClient,
    /** Grant type, carried only when the configuration pins the headless `client_credentials` flow. Absent for the default interactive authorization-code flow. */
    @JsonProperty("grantType") McpServerConfigHttpOauthGrantType grantType,
    /** Configured OAuth scopes joined into one space-delimited scope value. Absent when the configuration specifies no scopes. */
    @JsonProperty("scope") String scope
) {
}
