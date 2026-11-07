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
 * Static OAuth client configuration, if the server specifies one
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpOauthRequiredStaticClientConfig(
    /** OAuth client ID for the server */
    @JsonProperty("clientId") String clientId,
    /** Optional OAuth client secret for confidential static clients, when the runtime can resolve one */
    @JsonProperty("clientSecret") String clientSecret,
    /** Whether this is a public OAuth client */
    @JsonProperty("publicClient") Boolean publicClient,
    /** Optional non-default OAuth grant type. When set to 'client_credentials', the OAuth flow runs headlessly using the client_id + keychain-stored secret (no browser, no callback server). */
    @JsonProperty("grantType") String grantType,
    /** Configured OAuth scope string used when the server challenge omits scope or provides an empty scope */
    @JsonProperty("scope") String scope
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpOauthRequiredStaticClientConfig {}

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
        private String clientId;
        private String clientSecret;
        private Boolean publicClient;
        private String grantType;
        private String scope;

        /**
         * Sets the {@code clientId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientId(String value) {
            this.clientId = value;
            return this;
        }

        /**
         * Sets the {@code clientSecret} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientSecret(String value) {
            this.clientSecret = value;
            return this;
        }

        /**
         * Sets the {@code publicClient} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder publicClient(Boolean value) {
            this.publicClient = value;
            return this;
        }

        /**
         * Sets the {@code grantType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder grantType(String value) {
            this.grantType = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthRequiredStaticClientConfig build() {
            if (clientId == null) throw new IllegalStateException("Missing required property: clientId");
            return new McpOauthRequiredStaticClientConfig(clientId, clientSecret, publicClient, grantType, scope);
        }
    }

    /**
     * Creates a static OAuth client configuration without an explicit scope.
     * @deprecated Use named construction via {@link #builder()}.
     * @param clientId OAuth client ID for the server
     * @param clientSecret Optional OAuth client secret for confidential static clients, when the runtime can resolve one
     * @param publicClient Whether this is a public OAuth client
     * @param grantType Optional non-default OAuth grant type. When set to 'client_credentials', the OAuth flow runs headlessly using the client_id + keychain-stored secret (no browser, no callback server).
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpOauthRequiredStaticClientConfig(
        String clientId,
        String clientSecret,
        Boolean publicClient,
        String grantType
    ) {
        this(clientId, clientSecret, publicClient, grantType, null);
    }
}
