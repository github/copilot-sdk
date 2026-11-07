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
        public McpOauthProbeResultNoAuthRequired build() {
            if (httpResponse == null) throw new IllegalStateException("Missing required property: httpResponse");
            McpOauthProbeResultNoAuthRequired instance = new McpOauthProbeResultNoAuthRequired();
            instance.setHttpResponse(httpResponse);
            return instance;
        }
    }
}
