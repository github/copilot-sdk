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
 * An OAuth challenge from a remote MCP server whose protected-resource metadata should be read.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthFetchProtectedResourceMetadataParams(
    /** URL of the remote MCP server that answered 401. */
    @JsonProperty("requestUrl") String requestUrl,
    /** Request headers the configured server sends, as a serialized JSON object. Omit to send none. */
    @JsonProperty("headersJson") String headersJson,
    /** Metadata URL named in the server's `WWW-Authenticate` challenge. Omit to try conventional metadata endpoints derived from the request URL. It is server-controlled, so it passes the same egress policy check as the request URL. */
    @JsonProperty("resourceMetadataUrl") String resourceMetadataUrl,
    /** User agent to send. Omit to use the runtime's own. */
    @JsonProperty("userAgent") String userAgent,
    /** Proxy the request must travel through. Omit to use the session's own egress settings. */
    @JsonProperty("sandboxProxy") String sandboxProxy,
    /** Locally owned session whose egress policy and sandbox proxy constrain the request. Omitted or nonlocal session IDs provide no local session egress state. */
    @JsonProperty("sessionId") String sessionId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpOauthFetchProtectedResourceMetadataParams {}

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
        private String requestUrl;
        private String headersJson;
        private String resourceMetadataUrl;
        private String userAgent;
        private String sandboxProxy;
        private String sessionId;

        /**
         * Sets the {@code requestUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestUrl(String value) {
            this.requestUrl = value;
            return this;
        }

        /**
         * Sets the {@code headersJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headersJson(String value) {
            this.headersJson = value;
            return this;
        }

        /**
         * Sets the {@code resourceMetadataUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resourceMetadataUrl(String value) {
            this.resourceMetadataUrl = value;
            return this;
        }

        /**
         * Sets the {@code userAgent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder userAgent(String value) {
            this.userAgent = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpOauthFetchProtectedResourceMetadataParams build() {
            if (requestUrl == null) throw new IllegalStateException("Missing required property: requestUrl");
            return new McpOauthFetchProtectedResourceMetadataParams(requestUrl, headersJson, resourceMetadataUrl, userAgent, sandboxProxy, sessionId);
        }
    }
}
