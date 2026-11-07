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
 * Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, callback handling, and static OAuth client selection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthLoginParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name of the remote MCP server to authenticate */
    @JsonProperty("serverName") String serverName,
    /** When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck. */
    @JsonProperty("forceReauth") Boolean forceReauth,
    /** Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees. */
    @JsonProperty("clientName") String clientName,
    /** Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return. */
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage,
    /** Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration. */
    @JsonProperty("clientId") String clientId,
    /** Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it. */
    @JsonProperty("clientSecret") String clientSecret,
    /** Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store. */
    @JsonProperty("publicClient") Boolean publicClient,
    /** Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified. */
    @JsonProperty("grantType") McpOauthLoginGrantType grantType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMcpOauthLoginParams {}

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
        private String sessionId;
        private String serverName;
        private Boolean forceReauth;
        private String clientName;
        private String callbackSuccessMessage;
        private String clientId;
        private String clientSecret;
        private Boolean publicClient;
        private McpOauthLoginGrantType grantType;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Sets the {@code forceReauth} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder forceReauth(Boolean value) {
            this.forceReauth = value;
            return this;
        }

        /**
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code callbackSuccessMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder callbackSuccessMessage(String value) {
            this.callbackSuccessMessage = value;
            return this;
        }

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
         * Sets the {@code clientSecret} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        public Builder grantType(McpOauthLoginGrantType value) {
            this.grantType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthLoginParams build() {
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            return new SessionMcpOauthLoginParams(sessionId, serverName, forceReauth, clientName, callbackSuccessMessage, clientId, clientSecret, publicClient, grantType);
        }
    }
}
