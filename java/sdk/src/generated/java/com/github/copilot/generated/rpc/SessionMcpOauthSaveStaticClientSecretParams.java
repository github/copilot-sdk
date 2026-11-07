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
 * Persist a confidential client's static secret without exposing token-store CRUD.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthSaveStaticClientSecretParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Runtime OAuth credential key. */
    @JsonProperty("storeKey") String storeKey,
    /** Preserve hostless credential and account selection. */
    @JsonProperty("sessionless") Boolean sessionless,
    /** Secret to persist in the selected credential owner. */
    @JsonProperty("secret") String secret,
    /** Credential owner. Omission uses the authoritative session store. */
    @JsonProperty("store") Object store
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionMcpOauthSaveStaticClientSecretParams {}

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
        private String secret;
        private Object store;

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
         * Sets the {@code secret} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secret(String value) {
            this.secret = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMcpOauthSaveStaticClientSecretParams build() {
            if (storeKey == null) throw new IllegalStateException("Missing required property: storeKey");
            if (secret == null) throw new IllegalStateException("Missing required property: secret");
            return new SessionMcpOauthSaveStaticClientSecretParams(sessionId, storeKey, sessionless, secret, store);
        }
    }
}
