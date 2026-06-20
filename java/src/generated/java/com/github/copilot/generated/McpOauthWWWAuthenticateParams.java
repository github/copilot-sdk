/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * OAuth WWW-Authenticate parameters parsed from an MCP auth challenge
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpOauthWWWAuthenticateParams(
    /** Protected resource metadata URL from the WWW-Authenticate resource_metadata parameter */
    @JsonProperty("resourceMetadataUrl") String resourceMetadataUrl,
    /** Requested OAuth scopes from the WWW-Authenticate scope parameter, if present */
    @JsonProperty("scope") String scope,
    /** OAuth error from the WWW-Authenticate error parameter, if present */
    @JsonProperty("error") String error
) {
}
