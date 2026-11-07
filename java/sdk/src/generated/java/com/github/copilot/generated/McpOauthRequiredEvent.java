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
 * Session event "mcp.oauth_required". OAuth authentication request for an MCP server
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthRequiredEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.oauth_required"; }

    @JsonProperty("data")
    private McpOauthRequiredEventData data;

    public McpOauthRequiredEventData getData() { return data; }
    public void setData(McpOauthRequiredEventData data) { this.data = data; }

    /** Data payload for {@link McpOauthRequiredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthRequiredEventData(
        /** Unique identifier for this OAuth request; used to respond via session.mcp.oauth.handlePendingRequest */
        @JsonProperty("requestId") String requestId,
        /** Display name of the MCP server that requires OAuth */
        @JsonProperty("serverName") String serverName,
        /** URL of the MCP server that requires OAuth */
        @JsonProperty("serverUrl") String serverUrl,
        /** Static OAuth client configuration, if the server specifies one */
        @JsonProperty("staticClientConfig") McpOauthRequiredStaticClientConfig staticClientConfig,
        /** OAuth WWW-Authenticate parameters parsed from the auth challenge, if available */
        @JsonProperty("wwwAuthenticateParams") McpOauthWWWAuthenticateParams wwwAuthenticateParams,
        /** Raw HTTP response details from the OAuth auth challenge, as observed by the runtime. Header order and casing are transport-dependent, and duplicate header names may appear multiple times. */
        @JsonProperty("httpResponse") McpOauthHttpResponse httpResponse,
        /** Raw OAuth protected-resource metadata document fetched for the MCP server, if available */
        @JsonProperty("resourceMetadata") String resourceMetadata,
        /** Why the runtime is requesting host-provided OAuth credentials. */
        @JsonProperty("reason") McpOauthRequestReason reason,
        /** Whether this Refresh request accepts kind runtimeRefresh to continue noninteractive grant renewal within its original authorization reservation. Missing or false means unsupported; the host must retain its legacy response behavior. */
        @JsonProperty("runtimeRefreshSupported") Boolean runtimeRefreshSupported
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public McpOauthRequiredEventData {}

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
            private String requestId;
            private String serverName;
            private String serverUrl;
            private McpOauthRequiredStaticClientConfig staticClientConfig;
            private McpOauthWWWAuthenticateParams wwwAuthenticateParams;
            private McpOauthHttpResponse httpResponse;
            private String resourceMetadata;
            private McpOauthRequestReason reason;
            private Boolean runtimeRefreshSupported;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code serverName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverName(String value) {
                this.serverName = value;
                return this;
            }

            /**
             * Sets the {@code serverUrl} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverUrl(String value) {
                this.serverUrl = value;
                return this;
            }

            /**
             * Sets the {@code staticClientConfig} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder staticClientConfig(McpOauthRequiredStaticClientConfig value) {
                this.staticClientConfig = value;
                return this;
            }

            /**
             * Sets the {@code wwwAuthenticateParams} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder wwwAuthenticateParams(McpOauthWWWAuthenticateParams value) {
                this.wwwAuthenticateParams = value;
                return this;
            }

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
             * Sets the {@code resourceMetadata} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder resourceMetadata(String value) {
                this.resourceMetadata = value;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(McpOauthRequestReason value) {
                this.reason = value;
                return this;
            }

            /**
             * Sets the {@code runtimeRefreshSupported} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder runtimeRefreshSupported(Boolean value) {
                this.runtimeRefreshSupported = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public McpOauthRequiredEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (serverUrl == null) throw new IllegalStateException("Missing required property: serverUrl");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                return new McpOauthRequiredEventData(requestId, serverName, serverUrl, staticClientConfig, wwwAuthenticateParams, httpResponse, resourceMetadata, reason, runtimeRefreshSupported);
            }
        }
    }
}
