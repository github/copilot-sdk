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
 * A narrow token-validity query against the chosen credential owner.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthHasValidTokensParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Runtime OAuth credential key. */
    @JsonProperty("storeKey") String storeKey,
    /** Preserve hostless credential and account selection. */
    @JsonProperty("sessionless") Boolean sessionless,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store,
    /** Static client identity required for refresh, when configured. */
    @JsonProperty("staticClientConfig") McpOauthFlowStaticClientConfig staticClientConfig
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionMcpOauthHasValidTokensParams {}

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
        private String storeKey;
        private Boolean sessionless;
        private Object store;
        private McpOauthFlowStaticClientConfig staticClientConfig;

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
         * Sets the {@code storeKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder storeKey(String value) {
            this.storeKey = value;
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
         * Sets the {@code staticClientConfig} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staticClientConfig(McpOauthFlowStaticClientConfig value) {
            this.staticClientConfig = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthHasValidTokensParams build() {
            if (storeKey == null) throw new IllegalStateException("Missing required property: storeKey");
            return new SessionMcpOauthHasValidTokensParams(sessionId, storeKey, sessionless, store, staticClientConfig);
        }
    }
}
