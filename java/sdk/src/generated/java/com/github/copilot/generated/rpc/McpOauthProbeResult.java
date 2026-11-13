/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Passive MCP OAuth probe result. `authenticated` means the server accepted the probe request while an OAuth-origin access token was attached; it does not prove the server required or independently validated that token. The probe does not make a second unauthenticated request. Failed is an expected probe-domain outcome; JSON-RPC errors are reserved for API-call failures.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpOauthProbeResultNoAuthRequired.class, name = "no-auth-required"),
    @JsonSubTypes.Type(value = McpOauthProbeResultAuthenticated.class, name = "authenticated"),
    @JsonSubTypes.Type(value = McpOauthProbeResultNeedsAuth.class, name = "needs-auth"),
    @JsonSubTypes.Type(value = McpOauthProbeResultFailed.class, name = "failed")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class McpOauthProbeResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the status discriminator
     */
    public abstract String getStatus();
}
