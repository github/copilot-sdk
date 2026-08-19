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
 * Variant {@code failed} of {@link McpOauthProbeResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthProbeResultFailed extends McpOauthProbeResult {

    @JsonProperty("status")
    private final String status = "failed";

    @Override
    public String getStatus() { return status; }

    /** Human-readable probe failure detail. */
    @JsonProperty("error")
    private String error;

    /** HTTP response returned by the server, when the probe reached the server and captured the complete response. */
    @JsonProperty("httpResponse")
    private McpOauthHttpResponse httpResponse;

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public McpOauthHttpResponse getHttpResponse() { return httpResponse; }
    public void setHttpResponse(McpOauthHttpResponse httpResponse) { this.httpResponse = httpResponse; }
}
