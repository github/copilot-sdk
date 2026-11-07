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
 * OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthLoginResult(
    /** Runtime-issued owned flow identity; never a server name or installation operation ID. */
    @JsonProperty("loginId") String loginId,
    /** Explicit outcome for owned sign-in. Manual callers retain their legacy response shape. */
    @JsonProperty("status") McpOwnedOauthLoginStatus status,
    /** URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. For the default loopback flow, the runtime starts its listener before returning. With redirectUri, the host receives the callback and completes it through session.mcp.oauth.complete. The runtime continues the flow in the background and signals completion via session.mcp_server_status_changed. */
    @JsonProperty("authorizationUrl") String authorizationUrl,
    /** Opaque authorization identifier returned only for a host-managed redirect URI. The runtime also sends it as the OAuth state value, so the callback endpoint can read state and pass it with the full callback URL to session.mcp.oauth.complete. */
    @JsonProperty("authorizationId") String authorizationId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMcpOauthLoginResult {}

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
        private String loginId;
        private McpOwnedOauthLoginStatus status;
        private String authorizationUrl;
        private String authorizationId;

        /**
         * Sets the {@code loginId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder loginId(String value) {
            this.loginId = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(McpOwnedOauthLoginStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code authorizationUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authorizationUrl(String value) {
            this.authorizationUrl = value;
            return this;
        }

        /**
         * Sets the {@code authorizationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authorizationId(String value) {
            this.authorizationId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthLoginResult build() {
            return new SessionMcpOauthLoginResult(loginId, status, authorizationUrl, authorizationId);
        }
    }
}
