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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A GitHub API request without caller-supplied auth identity or credential fields.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchRequestParams(
    /** A fresh request id. */
    @JsonProperty("requestId") Long requestId,
    /** Session whose auth manager to use. Omission selects the process manager. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque selector for a live host-owned credential binding. Only the owning host connection may use it. */
    @JsonProperty("authOwnerId") String authOwnerId,
    /** HTTP method. */
    @JsonProperty("method") String method,
    /** Absolute URL on the resolved GitHub API origin. */
    @JsonProperty("url") String url,
    /** Caller-supplied HTTP headers. */
    @JsonProperty("headers") List<GitHubFetchHeader> headers,
    /** Request bytes encoded as base64. Absent for a request without a body. */
    @JsonProperty("bodyBase64") String bodyBase64,
    /** API label used in runtime diagnostics. */
    @JsonProperty("apiName") String apiName,
    /** Optional retry overrides. */
    @JsonProperty("retryOptions") GitHubFetchRetryConfig retryOptions,
    /** Redirect behavior. Defaults to following redirects. */
    @JsonProperty("redirect") String redirect,
    /** Total request timeout in milliseconds. */
    @JsonProperty("timeoutMs") Long timeoutMs,
    /** Response read timeout in milliseconds. Defaults to 700000. */
    @JsonProperty("readTimeoutMs") Long readTimeoutMs,
    /** Maximum response body size in bytes. */
    @JsonProperty("bodyLimitBytes") Long bodyLimitBytes,
    /** Whether the request bypasses idle connections. */
    @JsonProperty("disableConnectionReuse") Boolean disableConnectionReuse,
    /** Explicit proxy environment. Omission uses the process environment. */
    @JsonProperty("proxyEnv") GitHubFetchProxyEnvironment proxyEnv,
    /** Addresses to pin for this request, without another DNS lookup. */
    @JsonProperty("pinnedAddresses") List<GitHubFetchPinnedAddress> pinnedAddresses,
    /** Whether the request permits loopback addresses. Defaults to true. */
    @JsonProperty("allowLocalhost") Boolean allowLocalhost,
    /** Whether the proxy uses Negotiate authentication. */
    @JsonProperty("proxyNegotiateAuth") Boolean proxyNegotiateAuth,
    /** Optional service principal for proxy Negotiate authentication. */
    @JsonProperty("proxyNegotiateSpn") String proxyNegotiateSpn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubFetchRequestParams {}

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
        private Long requestId;
        private String sessionId;
        private String authOwnerId;
        private String method;
        private String url;
        private List<GitHubFetchHeader> headers;
        private String bodyBase64;
        private String apiName;
        private GitHubFetchRetryConfig retryOptions;
        private String redirect;
        private Long timeoutMs;
        private Long readTimeoutMs;
        private Long bodyLimitBytes;
        private Boolean disableConnectionReuse;
        private GitHubFetchProxyEnvironment proxyEnv;
        private List<GitHubFetchPinnedAddress> pinnedAddresses;
        private Boolean allowLocalhost;
        private Boolean proxyNegotiateAuth;
        private String proxyNegotiateSpn;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(Long value) {
            this.requestId = value;
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
         * Sets the {@code authOwnerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authOwnerId(String value) {
            this.authOwnerId = value;
            return this;
        }

        /**
         * Sets the {@code method} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder method(String value) {
            this.method = value;
            return this;
        }

        /**
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headers(List<GitHubFetchHeader> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code bodyBase64} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder bodyBase64(String value) {
            this.bodyBase64 = value;
            return this;
        }

        /**
         * Sets the {@code apiName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder apiName(String value) {
            this.apiName = value;
            return this;
        }

        /**
         * Sets the {@code retryOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder retryOptions(GitHubFetchRetryConfig value) {
            this.retryOptions = value;
            return this;
        }

        /**
         * Sets the {@code redirect} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder redirect(String value) {
            this.redirect = value;
            return this;
        }

        /**
         * Sets the {@code timeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timeoutMs(Long value) {
            this.timeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code readTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder readTimeoutMs(Long value) {
            this.readTimeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code bodyLimitBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder bodyLimitBytes(Long value) {
            this.bodyLimitBytes = value;
            return this;
        }

        /**
         * Sets the {@code disableConnectionReuse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disableConnectionReuse(Boolean value) {
            this.disableConnectionReuse = value;
            return this;
        }

        /**
         * Sets the {@code proxyEnv} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder proxyEnv(GitHubFetchProxyEnvironment value) {
            this.proxyEnv = value;
            return this;
        }

        /**
         * Sets the {@code pinnedAddresses} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pinnedAddresses(List<GitHubFetchPinnedAddress> value) {
            this.pinnedAddresses = value;
            return this;
        }

        /**
         * Sets the {@code allowLocalhost} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowLocalhost(Boolean value) {
            this.allowLocalhost = value;
            return this;
        }

        /**
         * Sets the {@code proxyNegotiateAuth} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder proxyNegotiateAuth(Boolean value) {
            this.proxyNegotiateAuth = value;
            return this;
        }

        /**
         * Sets the {@code proxyNegotiateSpn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder proxyNegotiateSpn(String value) {
            this.proxyNegotiateSpn = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchRequestParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (method == null) throw new IllegalStateException("Missing required property: method");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            if (apiName == null) throw new IllegalStateException("Missing required property: apiName");
            return new GitHubFetchRequestParams(requestId, sessionId, authOwnerId, method, url, headers, bodyBase64, apiName, retryOptions, redirect, timeoutMs, readTimeoutMs, bodyLimitBytes, disableConnectionReuse, proxyEnv, pinnedAddresses, allowLocalhost, proxyNegotiateAuth, proxyNegotiateSpn);
        }
    }
}
