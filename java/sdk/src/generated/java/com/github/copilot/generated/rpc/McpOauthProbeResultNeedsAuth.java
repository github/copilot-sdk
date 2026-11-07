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
    private McpOauthHttpResponse httpResponse;

    /** Why authentication is needed. */
    @JsonProperty("reason")
    private McpOauthProbeNeedsAuthReason reason;

    /** Parsed WWW-Authenticate challenge parameters, when present and parseable. */
    @JsonProperty("wwwAuthenticateParams")
    private McpOauthWWWAuthenticateParams wwwAuthenticateParams;

    public McpOauthHttpResponse getHttpResponse() { return httpResponse; }
    public void setHttpResponse(McpOauthHttpResponse httpResponse) { this.httpResponse = httpResponse; }

    public McpOauthProbeNeedsAuthReason getReason() { return reason; }
    public void setReason(McpOauthProbeNeedsAuthReason reason) { this.reason = reason; }

    public McpOauthWWWAuthenticateParams getWwwAuthenticateParams() { return wwwAuthenticateParams; }
    public void setWwwAuthenticateParams(McpOauthWWWAuthenticateParams wwwAuthenticateParams) { this.wwwAuthenticateParams = wwwAuthenticateParams; }


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
        private McpOauthHttpResponse httpResponse;
        private McpOauthProbeNeedsAuthReason reason;
        private McpOauthWWWAuthenticateParams wwwAuthenticateParams;

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
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(McpOauthProbeNeedsAuthReason value) {
            this.reason = value;
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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthProbeResultNeedsAuth build() {
            if (httpResponse == null) throw new IllegalStateException("Missing required property: httpResponse");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            McpOauthProbeResultNeedsAuth instance = new McpOauthProbeResultNeedsAuth();
            instance.setHttpResponse(httpResponse);
            instance.setReason(reason);
            instance.setWwwAuthenticateParams(wwwAuthenticateParams);
            return instance;
        }
    }
}
