/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.McpOauthHttpResponse;
import javax.annotation.processing.Generated;

/**
 * Variant {@code no-auth-required} of {@link McpOauthProbeResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthProbeResultNoAuthRequired extends McpOauthProbeResult {

    @JsonProperty("status")
    private final String status = "no-auth-required";

    @Override
    public String getStatus() { return status; }

    /** HTTP response returned by the server. */
    @JsonProperty("httpResponse")
    private McpOauthHttpResponse httpResponse;

    public McpOauthHttpResponse getHttpResponse() { return httpResponse; }
    public void setHttpResponse(McpOauthHttpResponse httpResponse) { this.httpResponse = httpResponse; }
}
