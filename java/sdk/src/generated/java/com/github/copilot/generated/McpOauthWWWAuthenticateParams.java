/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * OAuth WWW-Authenticate parameters parsed from an MCP auth challenge
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpOauthWWWAuthenticateParams(
    /** Protected resource metadata URL from the WWW-Authenticate resource_metadata parameter, if present */
    @JsonProperty("resourceMetadataUrl") String resourceMetadataUrl,
    /** Requested OAuth scopes from the WWW-Authenticate scope parameter, if present */
    @JsonProperty("scope") String scope,
    /** OAuth error from the WWW-Authenticate error parameter, if present */
    @JsonProperty("error") String error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpOauthWWWAuthenticateParams {}

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
        private String resourceMetadataUrl;
        private String scope;
        private String error;

        /**
         * Sets the {@code resourceMetadataUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resourceMetadataUrl(String value) {
            this.resourceMetadataUrl = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(String value) {
            this.scope = value;
            return this;
        }

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthWWWAuthenticateParams build() {
            return new McpOauthWWWAuthenticateParams(resourceMetadataUrl, scope, error);
        }
    }
}
