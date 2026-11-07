/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The handshake failed for a reason other than authentication.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class McpOauthTestConnectionResultFailed extends McpOauthTestConnectionResult {

    @JsonProperty("status")
    private final String status = "failed";

    @Override
    public String getStatus() { return status; }

    /** Transport or session policy failure detail. */
    @JsonProperty("error")
    private String error;

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }


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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthTestConnectionResultFailed build() {
            if (error == null) throw new IllegalStateException("Missing required property: error");
            McpOauthTestConnectionResultFailed instance = new McpOauthTestConnectionResultFailed();
            instance.setError(error);
            return instance;
        }
    }
}
