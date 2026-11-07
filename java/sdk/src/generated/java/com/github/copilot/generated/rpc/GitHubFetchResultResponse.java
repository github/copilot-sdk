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


    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private Long status;
        private String statusText;
        private String url;
        private List<GitHubFetchHeader> headers;
        private String bodyBase64;
        private GitHubFetchHttpError httpError;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(Long value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code statusText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statusText(String value) {
            this.statusText = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(List<GitHubFetchHeader> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code bodyBase64} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder bodyBase64(String value) {
            this.bodyBase64 = value;
            return this;
        }

        /**
         * Sets the {@code httpError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder httpError(GitHubFetchHttpError value) {
            this.httpError = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchResultResponse build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (statusText == null) throw new IllegalStateException("Missing required property: statusText");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            if (bodyBase64 == null) throw new IllegalStateException("Missing required property: bodyBase64");
            GitHubFetchResultResponse instance = new GitHubFetchResultResponse();
            instance.setStatus(status);
            instance.setStatusText(statusText);
            instance.setUrl(url);
            instance.setHeaders(headers);
            instance.setBodyBase64(bodyBase64);
            instance.setHttpError(httpError);
            return instance;
        }
    }
}
