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
 * Variant {@code response} of {@link GitHubFetchResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubFetchResultResponse extends GitHubFetchResult {

    @JsonProperty("kind")
    private final String kind = "response";

    @Override
    public String getKind() { return kind; }

    /** HTTP status, including non-success responses. */
    @JsonProperty("status")
    private Long status;

    /** HTTP status text. */
    @JsonProperty("statusText")
    private String statusText;

    /** Final response URL. */
    @JsonProperty("url")
    private String url;

    /** Response headers. */
    @JsonProperty("headers")
    private List<GitHubFetchHeader> headers;

    /** Complete response bytes encoded as base64. */
    @JsonProperty("bodyBase64")
    private String bodyBase64;

    /** Formatted non-success response for a named operation. */
    @JsonProperty("httpError")
    private GitHubFetchHttpError httpError;

    public Long getStatus() { return status; }
    public void setStatus(Long status) { this.status = status; }

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public List<GitHubFetchHeader> getHeaders() { return headers; }
    public void setHeaders(List<GitHubFetchHeader> headers) { this.headers = headers; }

    public String getBodyBase64() { return bodyBase64; }
    public void setBodyBase64(String bodyBase64) { this.bodyBase64 = bodyBase64; }

    public GitHubFetchHttpError getHttpError() { return httpError; }
    public void setHttpError(GitHubFetchHttpError httpError) { this.httpError = httpError; }
}
