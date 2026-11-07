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
 * Proxy environment for one authenticated GitHub request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchProxyEnvironment(
    /** Proxy for HTTP requests. */
    @JsonProperty("httpProxy") String httpProxy,
    /** Proxy for HTTPS requests. */
    @JsonProperty("httpsProxy") String httpsProxy,
    /** Hosts that bypass the proxy. */
    @JsonProperty("noProxy") String noProxy
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubFetchProxyEnvironment {}

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
        private String httpProxy;
        private String httpsProxy;
        private String noProxy;

        /**
         * Sets the {@code httpProxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder httpProxy(String value) {
            this.httpProxy = value;
            return this;
        }

        /**
         * Sets the {@code httpsProxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder httpsProxy(String value) {
            this.httpsProxy = value;
            return this;
        }

        /**
         * Sets the {@code noProxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder noProxy(String value) {
            this.noProxy = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchProxyEnvironment build() {
            return new GitHubFetchProxyEnvironment(httpProxy, httpsProxy, noProxy);
        }
    }
}
