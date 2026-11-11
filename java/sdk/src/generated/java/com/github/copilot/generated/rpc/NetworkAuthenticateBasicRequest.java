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
 * Requests credentials for a proxy's Basic authentication challenge.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkAuthenticateBasicRequest(
    /** Proxy that challenged, as `http://host:port` or `https://host:port`. */
    @JsonProperty("proxy") String proxy,
    /** Realm the proxy named in its challenge, if any. The proxy supplies it, so treat it as untrusted text. */
    @JsonProperty("realm") String realm,
    /** True when the proxy rejected the credentials the connection sent last, including ones the runtime remembered from an earlier answer: answer with different credentials, for example by asking the user, or with none. False when the connection has not sent credentials yet. */
    @JsonProperty("previousRejected") Boolean previousRejected
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NetworkAuthenticateBasicRequest {}

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
        private String proxy;
        private String realm;
        private Boolean previousRejected;

        /**
         * Sets the {@code proxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder proxy(String value) {
            this.proxy = value;
            return this;
        }

        /**
         * Sets the {@code realm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder realm(String value) {
            this.realm = value;
            return this;
        }

        /**
         * Sets the {@code previousRejected} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder previousRejected(Boolean value) {
            this.previousRejected = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public NetworkAuthenticateBasicRequest build() {
            if (proxy == null) throw new IllegalStateException("Missing required property: proxy");
            if (previousRejected == null) throw new IllegalStateException("Missing required property: previousRejected");
            return new NetworkAuthenticateBasicRequest(proxy, realm, previousRejected);
        }
    }
}
