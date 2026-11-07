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
 * Variant {@code token} of {@link GitHubTokenAcquireResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class GitHubTokenAcquireResultToken extends GitHubTokenAcquireResult {

    @JsonProperty("kind")
    private final String kind = "token";

    @Override
    public String getKind() { return kind; }

    /** GitHub access token acquired by the SDK host. */
    @JsonProperty("accessToken")
    private String accessToken;

    /** OAuth token type. Defaults to bearer when omitted. */
    @JsonProperty("tokenType")
    private String tokenType;

    /** Remaining token lifetime in seconds when callback execution completes. It must exceed the one-hour preflight refresh threshold. */
    @JsonProperty("expiresIn")
    private Long expiresIn;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }


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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubTokenAcquireResultToken build() {
            if (accessToken == null) throw new IllegalStateException("Missing required property: accessToken");
            if (expiresIn == null) throw new IllegalStateException("Missing required property: expiresIn");
            GitHubTokenAcquireResultToken instance = new GitHubTokenAcquireResultToken();
            instance.setAccessToken(accessToken);
            instance.setTokenType(tokenType);
            instance.setExpiresIn(expiresIn);
            return instance;
        }
    }
}
