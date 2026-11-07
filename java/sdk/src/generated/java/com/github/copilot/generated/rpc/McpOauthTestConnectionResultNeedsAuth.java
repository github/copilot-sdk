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
        private String error;
        private String wwwAuthenticateHeader;
        private McpOauthWWWAuthenticateParams wwwAuthenticateParams;
        private McpOauthHttpResponse httpResponse;

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Sets the {@code wwwAuthenticateHeader} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wwwAuthenticateHeader(String value) {
            this.wwwAuthenticateHeader = value;
            return this;
        }

        /**
         * Sets the {@code wwwAuthenticateParams} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wwwAuthenticateParams(McpOauthWWWAuthenticateParams value) {
            this.wwwAuthenticateParams = value;
            return this;
        }

        /**
         * Sets the {@code httpResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder httpResponse(McpOauthHttpResponse value) {
            this.httpResponse = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthTestConnectionResultNeedsAuth build() {
            if (error == null) throw new IllegalStateException("Missing required property: error");
            McpOauthTestConnectionResultNeedsAuth instance = new McpOauthTestConnectionResultNeedsAuth();
            instance.setError(error);
            instance.setWwwAuthenticateHeader(wwwAuthenticateHeader);
            instance.setWwwAuthenticateParams(wwwAuthenticateParams);
            instance.setHttpResponse(httpResponse);
            return instance;
        }
    }
}
