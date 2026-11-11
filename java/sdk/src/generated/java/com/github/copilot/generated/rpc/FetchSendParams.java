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
 * An HTTP request through the shared network transport.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FetchSendParams(
    /** Connection-owned handle returned by fetch.create. Each handle can start only once. */
    @JsonProperty("requestId") String requestId,
    /** Absolute target URL. */
    @JsonProperty("url") String url,
    /** Response consumption mode. Stream returns headers immediately; text and file consume the complete body. */
    @JsonProperty("responseMode") FetchResponseMode responseMode,
    /** Destination on the runtime host, required only for file mode. Must not already exist.
The caller owns completed files. Failed or canceled requests remove their incomplete files. */
    @JsonProperty("destinationPath") String destinationPath,
    /** An existing connection-owned credential capability. Each same-origin hop resolves fresh credentials.
Conflicting headers fail. Cross-origin redirects drop credentials but retain the original authority.
HTTPS downgrades fail. Omission adds no service credentials. */
    @JsonProperty("credentialContextId") String credentialContextId,
    /** HTTP method. Defaults to GET. */
    @JsonProperty("method") String method,
    /** Header entries in input order. Repeated names are not combined. */
    @JsonProperty("headers") List<FetchHeader> headers,
    /** Request bytes encoded as standard base64. Omission sends no body. */
    @JsonProperty("body") String body,
    /** Redirect policy. Defaults to follow. */
    @JsonProperty("redirect") FetchRedirect redirect,
    /** Total request timeout in milliseconds, including body consumption. Omission sets no total deadline. */
    @JsonProperty("timeoutMs") Long timeoutMs,
    /** Read timeout in milliseconds. Defaults to 700000. Explicit zero is preserved. */
    @JsonProperty("readTimeoutMs") Long readTimeoutMs,
    /** Maximum decoded response bytes. Only text mode defaults to 1048576; file and stream modes have no default limit.
Explicit zero permits only an empty body. Exceeding the limit fails without truncation. */
    @JsonProperty("bodyLimitBytes") Long bodyLimitBytes,
    /** Disable idle connection reuse. Defaults to false. */
    @JsonProperty("disableConnectionReuse") Boolean disableConnectionReuse,
    /** Proxy overrides. Omission reads the process proxy environment per request. */
    @JsonProperty("proxyEnv") FetchProxyEnv proxyEnv,
    /** DNS address pins. Internal pins bypass proxies; public pins yield to configured proxies. */
    @JsonProperty("pinnedAddresses") List<String> pinnedAddresses,
    /** Permit loopback DNS pins. Defaults to false. This is not a general URL admission policy. */
    @JsonProperty("allowLocalhost") Boolean allowLocalhost,
    /** Force proxy Negotiate authentication when supported. Defaults to false; automatic proxy negotiation remains enabled. */
    @JsonProperty("proxyNegotiateAuth") Boolean proxyNegotiateAuth,
    /** Optional proxy Kerberos service principal override. */
    @JsonProperty("proxyNegotiateSpn") String proxyNegotiateSpn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FetchSendParams {}

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
        private String requestId;
        private String url;
        private FetchResponseMode responseMode;
        private String destinationPath;
        private String credentialContextId;
        private String method;
        private List<FetchHeader> headers;
        private String body;
        private FetchRedirect redirect;
        private Long timeoutMs;
        private Long readTimeoutMs;
        private Long bodyLimitBytes;
        private Boolean disableConnectionReuse;
        private FetchProxyEnv proxyEnv;
        private List<String> pinnedAddresses;
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
        public Builder requestId(String value) {
            this.requestId = value;
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
         * Sets the {@code responseMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder responseMode(FetchResponseMode value) {
            this.responseMode = value;
            return this;
        }

        /**
         * Sets the {@code destinationPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder destinationPath(String value) {
            this.destinationPath = value;
            return this;
        }

        /**
         * Sets the {@code credentialContextId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder credentialContextId(String value) {
            this.credentialContextId = value;
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
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headers(List<FetchHeader> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code body} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder body(String value) {
            this.body = value;
            return this;
        }

        /**
         * Sets the {@code redirect} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder redirect(FetchRedirect value) {
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
        public Builder proxyEnv(FetchProxyEnv value) {
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
        public Builder pinnedAddresses(List<String> value) {
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
        public FetchSendParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (url == null) throw new IllegalStateException("Missing required property: url");
            if (responseMode == null) throw new IllegalStateException("Missing required property: responseMode");
            return new FetchSendParams(requestId, url, responseMode, destinationPath, credentialContextId, method, headers, body, redirect, timeoutMs, readTimeoutMs, bodyLimitBytes, disableConnectionReuse, proxyEnv, pinnedAddresses, allowLocalhost, proxyNegotiateAuth, proxyNegotiateSpn);
        }
    }
}
