/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
    private McpOauthProbeResultNoAuthRequiredHttpResponse httpResponse;

    public McpOauthProbeResultNoAuthRequiredHttpResponse getHttpResponse() { return httpResponse; }
    public void setHttpResponse(McpOauthProbeResultNoAuthRequiredHttpResponse httpResponse) { this.httpResponse = httpResponse; }


    /** Raw HTTP response details from the OAuth auth challenge, as observed by the runtime. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthProbeResultNoAuthRequiredHttpResponse(
        /** HTTP status code returned with the auth challenge. */
        @JsonProperty("statusCode") Long statusCode,
        /** HTTP response headers as observed by the runtime. Order and casing are transport-dependent, and duplicate header names may appear multiple times. */
        @JsonProperty("headers") List<HeaderEntry> headers,
        /** Complete UTF-8 response body for host-specific challenge handling, including an empty string for an empty body. Omitted when the complete body is not valid UTF-8; body read failures fail the HTTP operation rather than exposing a partial response. */
        @JsonProperty("body") String body
    ) {
    }
}
