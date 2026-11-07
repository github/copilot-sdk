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
import com.github.copilot.generated.McpOauthWWWAuthenticateParams;
import javax.annotation.processing.Generated;

/**
 * Full host-driven authentication for an arbitrary, possibly unsaved, MCP URL.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthAuthenticateParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Correlation identity reserved through prepareFlow. */
    @JsonProperty("flowId") String flowId,
    /** MCP endpoint, without a requirement for saved configuration. */
    @JsonProperty("serverUrl") String serverUrl,
    /** Use environment session claims for a hostless compatibility call, while retaining this requester's egress authority. */
    @JsonProperty("sessionless") Boolean sessionless,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store,
    /** Proxy fallback when the session has not published an egress entry. */
    @JsonProperty("sandboxProxy") String sandboxProxy,
    /** Static OAuth client settings for this attempt. */
    @JsonProperty("staticClientConfig") McpOauthFlowStaticClientConfig staticClientConfig,
    /** Bypass cached grants for explicit account selection. */
    @JsonProperty("forceReauth") Boolean forceReauth,
    /** Fail with browser_required when silent authentication cannot complete. */
    @JsonProperty("browserless") Boolean browserless,
    /** Start device authorization instead of a browser flow. */
    @JsonProperty("forceDeviceCode") Boolean forceDeviceCode,
    /** Let the host present the authorization URL without a runtime browser launch. */
    @JsonProperty("skipBrowserOpen") Boolean skipBrowserOpen,
    /** Authorization timeout in milliseconds. Defaults to five minutes. */
    @JsonProperty("authorizationTimeoutMs") Long authorizationTimeoutMs,
    /** Override the device authorization poll interval in milliseconds. */
    @JsonProperty("deviceCodePollIntervalMs") Long deviceCodePollIntervalMs,
    /** Delay before the runtime offers device authorization. */
    @JsonProperty("deviceCodeOfferTimeoutMs") Long deviceCodeOfferTimeoutMs,
    /** Display name for new dynamic client registrations. */
    @JsonProperty("clientName") String clientName,
    /** Client metadata document URL. Omission uses the session identity. */
    @JsonProperty("clientIdMetadataUrl") String clientIdMetadataUrl,
    /** Text displayed after a successful loopback callback. */
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage,
    /** Fixed callback port. A busy port fails without fallback. */
    @JsonProperty("redirectPort") Long redirectPort,
    /** Reject the cached access token but permit a refresh grant. */
    @JsonProperty("skipCachedAccessToken") Boolean skipCachedAccessToken,
    /** Preserve previously granted scopes during additional consent. */
    @JsonProperty("includeExistingTokenScopes") Boolean includeExistingTokenScopes,
    /** Preserve replacement policy when another pending flow owns consent. */
    @JsonProperty("preservePendingFlowReplacementFlags") Boolean preservePendingFlowReplacementFlags,
    /** Challenge captured by the host, including an empty challenge. */
    @JsonProperty("wwwAuthenticateParams") McpOauthWWWAuthenticateParams wwwAuthenticateParams,
    /** Raw protected-resource metadata already fetched by the host. */
    @JsonProperty("resourceMetadata") String resourceMetadata
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionMcpOauthAuthenticateParams {}

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
        private String flowId;
        private String serverUrl;
        private Boolean sessionless;
        private Object store;
        private String sandboxProxy;
        private McpOauthFlowStaticClientConfig staticClientConfig;
        private Boolean forceReauth;
        private Boolean browserless;
        private Boolean forceDeviceCode;
        private Boolean skipBrowserOpen;
        private Long authorizationTimeoutMs;
        private Long deviceCodePollIntervalMs;
        private Long deviceCodeOfferTimeoutMs;
        private String clientName;
        private String clientIdMetadataUrl;
        private String callbackSuccessMessage;
        private Long redirectPort;
        private Boolean skipCachedAccessToken;
        private Boolean includeExistingTokenScopes;
        private Boolean preservePendingFlowReplacementFlags;
        private McpOauthWWWAuthenticateParams wwwAuthenticateParams;
        private String resourceMetadata;

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
         * Sets the {@code flowId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder flowId(String value) {
            this.flowId = value;
            return this;
        }

        /**
         * Sets the {@code serverUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serverUrl(String value) {
            this.serverUrl = value;
            return this;
        }

        /**
         * Sets the {@code sessionless} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionless(Boolean value) {
            this.sessionless = value;
            return this;
        }

        /**
         * Sets the {@code store} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder store(Object value) {
            this.store = value;
            return this;
        }

        /**
         * Sets the {@code sandboxProxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sandboxProxy(String value) {
            this.sandboxProxy = value;
            return this;
        }

        /**
         * Sets the {@code staticClientConfig} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder staticClientConfig(McpOauthFlowStaticClientConfig value) {
            this.staticClientConfig = value;
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
         * Sets the {@code browserless} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder browserless(Boolean value) {
            this.browserless = value;
            return this;
        }

        /**
         * Sets the {@code forceDeviceCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder forceDeviceCode(Boolean value) {
            this.forceDeviceCode = value;
            return this;
        }

        /**
         * Sets the {@code skipBrowserOpen} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipBrowserOpen(Boolean value) {
            this.skipBrowserOpen = value;
            return this;
        }

        /**
         * Sets the {@code authorizationTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authorizationTimeoutMs(Long value) {
            this.authorizationTimeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code deviceCodePollIntervalMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deviceCodePollIntervalMs(Long value) {
            this.deviceCodePollIntervalMs = value;
            return this;
        }

        /**
         * Sets the {@code deviceCodeOfferTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deviceCodeOfferTimeoutMs(Long value) {
            this.deviceCodeOfferTimeoutMs = value;
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
         * Sets the {@code clientIdMetadataUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientIdMetadataUrl(String value) {
            this.clientIdMetadataUrl = value;
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
         * Sets the {@code redirectPort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder redirectPort(Long value) {
            this.redirectPort = value;
            return this;
        }

        /**
         * Sets the {@code skipCachedAccessToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipCachedAccessToken(Boolean value) {
            this.skipCachedAccessToken = value;
            return this;
        }

        /**
         * Sets the {@code includeExistingTokenScopes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder includeExistingTokenScopes(Boolean value) {
            this.includeExistingTokenScopes = value;
            return this;
        }

        /**
         * Sets the {@code preservePendingFlowReplacementFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder preservePendingFlowReplacementFlags(Boolean value) {
            this.preservePendingFlowReplacementFlags = value;
            return this;
        }

        /**
         * Sets the {@code wwwAuthenticateParams} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder wwwAuthenticateParams(McpOauthWWWAuthenticateParams value) {
            this.wwwAuthenticateParams = value;
            return this;
        }

        /**
         * Sets the {@code resourceMetadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resourceMetadata(String value) {
            this.resourceMetadata = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthAuthenticateParams build() {
            if (flowId == null) throw new IllegalStateException("Missing required property: flowId");
            if (serverUrl == null) throw new IllegalStateException("Missing required property: serverUrl");
            return new SessionMcpOauthAuthenticateParams(sessionId, flowId, serverUrl, sessionless, store, sandboxProxy, staticClientConfig, forceReauth, browserless, forceDeviceCode, skipBrowserOpen, authorizationTimeoutMs, deviceCodePollIntervalMs, deviceCodeOfferTimeoutMs, clientName, clientIdMetadataUrl, callbackSuccessMessage, redirectPort, skipCachedAccessToken, includeExistingTokenScopes, preservePendingFlowReplacementFlags, wwwAuthenticateParams, resourceMetadata);
        }
    }
}
