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
import com.github.copilot.generated.McpOauthWWWAuthenticateParams;
import javax.annotation.processing.Generated;

/**
 * The server refused the initialize request with an authentication challenge.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class McpOauthTestConnectionResultNeedsAuth extends McpOauthTestConnectionResult {

    @JsonProperty("status")
    private final String status = "needs-auth";

    @Override
    public String getStatus() { return status; }

    /** The transport's authentication error message. */
    @JsonProperty("error")
    private String error;

    /** Raw WWW-Authenticate header, when the refusing response supplies one. */
    @JsonProperty("wwwAuthenticateHeader")
    private String wwwAuthenticateHeader;

    /** Parsed challenge parameters from the refusing HTTP response. */
    @JsonProperty("wwwAuthenticateParams")
    private McpOauthWWWAuthenticateParams wwwAuthenticateParams;

    /** Exact refusing HTTP response, when the transport captured it. */
    @JsonProperty("httpResponse")
    private McpOauthHttpResponse httpResponse;

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getWwwAuthenticateHeader() { return wwwAuthenticateHeader; }
    public void setWwwAuthenticateHeader(String wwwAuthenticateHeader) { this.wwwAuthenticateHeader = wwwAuthenticateHeader; }

    public McpOauthWWWAuthenticateParams getWwwAuthenticateParams() { return wwwAuthenticateParams; }
    public void setWwwAuthenticateParams(McpOauthWWWAuthenticateParams wwwAuthenticateParams) { this.wwwAuthenticateParams = wwwAuthenticateParams; }

    public McpOauthHttpResponse getHttpResponse() { return httpResponse; }
    public void setHttpResponse(McpOauthHttpResponse httpResponse) { this.httpResponse = httpResponse; }
}
