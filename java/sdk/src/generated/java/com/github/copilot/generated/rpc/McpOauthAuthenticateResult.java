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
 * Token projection for the existing host-delegated provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthAuthenticateResult(
    /** Access token from the runtime-owned store. */
    @JsonProperty("accessToken") String accessToken,
    /** Token scheme. Defaults to Bearer. */
    @JsonProperty("tokenType") String tokenType,
    /** Remaining lifetime in seconds when the issuer supplied an expiry. */
    @JsonProperty("expiresIn") Long expiresIn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpOauthAuthenticateResult {}

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
        private String accessToken;
        private String tokenType;
        private Long expiresIn;

        /**
         * Sets the {@code accessToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder accessToken(String value) {
            this.accessToken = value;
            return this;
        }

        /**
         * Sets the {@code tokenType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenType(String value) {
            this.tokenType = value;
            return this;
        }

        /**
         * Sets the {@code expiresIn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder expiresIn(Long value) {
            this.expiresIn = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthAuthenticateResult build() {
            if (accessToken == null) throw new IllegalStateException("Missing required property: accessToken");
            if (tokenType == null) throw new IllegalStateException("Missing required property: tokenType");
            return new McpOauthAuthenticateResult(accessToken, tokenType, expiresIn);
        }
    }
}
