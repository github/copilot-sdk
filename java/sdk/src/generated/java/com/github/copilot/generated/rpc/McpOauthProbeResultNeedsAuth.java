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
 * Variant {@code needs-auth} of {@link McpOauthProbeResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthProbeResultNeedsAuth extends McpOauthProbeResult {

    @JsonProperty("status")
    private final String status = "needs-auth";

    @Override
    public String getStatus() { return status; }

    /** HTTP 401 or 403 response returned by the server. */
    @JsonProperty("httpResponse")
    private McpOauthProbeResultNeedsAuthHttpResponse httpResponse;

    /** Why authentication is needed. */
    @JsonProperty("reason")
    private McpOauthProbeNeedsAuthReason reason;

    /** Parsed WWW-Authenticate challenge parameters, when present and parseable. */
    @JsonProperty("wwwAuthenticateParams")
    private McpOauthProbeResultNeedsAuthWwwAuthenticateParams wwwAuthenticateParams;

    public McpOauthProbeResultNeedsAuthHttpResponse getHttpResponse() { return httpResponse; }
    public void setHttpResponse(McpOauthProbeResultNeedsAuthHttpResponse httpResponse) { this.httpResponse = httpResponse; }

    public McpOauthProbeNeedsAuthReason getReason() { return reason; }
    public void setReason(McpOauthProbeNeedsAuthReason reason) { this.reason = reason; }

    public McpOauthProbeResultNeedsAuthWwwAuthenticateParams getWwwAuthenticateParams() { return wwwAuthenticateParams; }
    public void setWwwAuthenticateParams(McpOauthProbeResultNeedsAuthWwwAuthenticateParams wwwAuthenticateParams) { this.wwwAuthenticateParams = wwwAuthenticateParams; }


    /** Raw HTTP response details from the OAuth auth challenge, as observed by the runtime. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthProbeResultNeedsAuthHttpResponse(
        /** HTTP status code returned with the auth challenge. */
        @JsonProperty("statusCode") Long statusCode,
        /** HTTP response headers as observed by the runtime. Order and casing are transport-dependent, and duplicate header names may appear multiple times. */
        @JsonProperty("headers") List<HeaderEntry> headers,
        /** Complete UTF-8 response body for host-specific challenge handling, including an empty string for an empty body. Omitted when the complete body is not valid UTF-8; body read failures fail the HTTP operation rather than exposing a partial response. */
        @JsonProperty("body") String body
    ) {
    }

    /** OAuth WWW-Authenticate parameters parsed from an MCP auth challenge */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthProbeResultNeedsAuthWwwAuthenticateParams(
        /** Protected resource metadata URL from the WWW-Authenticate resource_metadata parameter, if present */
        @JsonProperty("resourceMetadataUrl") String resourceMetadataUrl,
        /** Requested OAuth scopes from the WWW-Authenticate scope parameter, if present */
        @JsonProperty("scope") String scope,
        /** OAuth error from the WWW-Authenticate error parameter, if present */
        @JsonProperty("error") String error
    ) {
    }
}
