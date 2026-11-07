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
 * Derive a credential key through the selected account and store owner.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthStoreKeyParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** MCP endpoint whose credentials the key identifies. */
    @JsonProperty("serverUrl") String serverUrl,
    /** Static OAuth client identity, when configured. */
    @JsonProperty("clientId") String clientId,
    /** Metadata document identity when no static client is configured. */
    @JsonProperty("clientIdMetadataUrl") String clientIdMetadataUrl,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store,
    /** Preserve hostless credential and account selection. */
    @JsonProperty("sessionless") Boolean sessionless
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionMcpOauthStoreKeyParams {}

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
        private String sessionId;
        private String serverUrl;
        private String clientId;
        private String clientIdMetadataUrl;
        private Object store;
        private Boolean sessionless;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
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
         * Sets the {@code clientIdMetadataUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientIdMetadataUrl(String value) {
            this.clientIdMetadataUrl = value;
            return this;
        }

        /**
         * Sets the {@code store} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder store(Object value) {
            this.store = value;
            return this;
        }

        /**
         * Sets the {@code sessionless} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionless(Boolean value) {
            this.sessionless = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthStoreKeyParams build() {
            if (serverUrl == null) throw new IllegalStateException("Missing required property: serverUrl");
            return new SessionMcpOauthStoreKeyParams(sessionId, serverUrl, clientId, clientIdMetadataUrl, store, sessionless);
        }
    }
}
