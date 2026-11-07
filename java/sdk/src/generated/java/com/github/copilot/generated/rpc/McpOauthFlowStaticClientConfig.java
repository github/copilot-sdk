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
 * A pre-registered OAuth client for this attempt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthFlowStaticClientConfig(
    /** Pre-registered client identifier. */
    @JsonProperty("clientId") String clientId,
    /** Ephemeral secret that the runtime never persists. */
    @JsonProperty("clientSecret") String clientSecret,
    /** Whether this client has no secret. Defaults to true. */
    @JsonProperty("publicClient") Boolean publicClient,
    /** OAuth grant override. */
    @JsonProperty("grantType") McpOauthLoginGrantType grantType,
    /** Scope used when the server omits its challenge scope. */
    @JsonProperty("scope") String scope
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpOauthFlowStaticClientConfig {}

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
        private McpOauthLoginGrantType grantType;
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
        public Builder grantType(McpOauthLoginGrantType value) {
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
        public McpOauthFlowStaticClientConfig build() {
            if (clientId == null) throw new IllegalStateException("Missing required property: clientId");
            return new McpOauthFlowStaticClientConfig(clientId, clientSecret, publicClient, grantType, scope);
        }
    }
}
