/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * An OAuth client a server configuration pins, without its secret.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthStaticClientConfig(
    /** Configured OAuth client identifier. */
    @JsonProperty("clientId") String clientId,
    /** Whether the configuration declares a public client. Absent when the configuration does not say, which the flow reads as a public client. */
    @JsonProperty("publicClient") Boolean publicClient,
    /** Grant type, carried only when the configuration pins the headless `client_credentials` flow. Absent for the default interactive authorization-code flow. */
    @JsonProperty("grantType") McpServerConfigHttpOauthGrantType grantType,
    /** Configured OAuth scopes joined into one space-delimited scope value. Absent when the configuration specifies no scopes. */
    @JsonProperty("scope") String scope
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpOauthStaticClientConfig {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String clientId;
        private Boolean publicClient;
        private McpServerConfigHttpOauthGrantType grantType;
        private String scope;

        /**
         * Sets the {@code clientId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientId(String value) {
            this.clientId = value;
            return this;
        }

        /**
         * Sets the {@code publicClient} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder grantType(McpServerConfigHttpOauthGrantType value) {
            this.grantType = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        public McpOauthStaticClientConfig build() {
            if (clientId == null) throw new IllegalStateException("Missing required property: clientId");
            return new McpOauthStaticClientConfig(clientId, publicClient, grantType, scope);
        }
    }
}
