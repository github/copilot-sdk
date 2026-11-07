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
 * Proxies for a request target, in the order to try them.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkResolveProxyResult(
    /** PAC-style proxy list such as `PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT`, as returned by a PAC script or Electron's `session.resolveProxy`; `DIRECT` connects directly. Omitted, null or empty leaves the request to the runtime's own proxy resolution, as if no callback were registered: proxy environment variables, `NO_PROXY`, then the operating system's proxy settings and PAC. Entry types the runtime doesn't support, such as SOCKS and QUIC, are skipped. */
    @JsonProperty("proxy") String proxy
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NetworkResolveProxyResult {}

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public NetworkResolveProxyResult build() {
            return new NetworkResolveProxyResult(proxy);
        }
    }
}
