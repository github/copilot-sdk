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
 * Registers the calling SDK connection's network callbacks, replacing any previous registration.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkSetProviderParams(
    /** Traffic the callbacks serve; defaults to `connection`. */
    @JsonProperty("scope") NetworkScope scope,
    /** Present when the client answers `network.resolveProxy`. */
    @JsonProperty("resolveProxy") NetworkResolveProxyOptions resolveProxy,
    /** Present when the client answers `network.authenticateNegotiate`. */
    @JsonProperty("authenticateNegotiate") NetworkAuthenticateNegotiateOptions authenticateNegotiate
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NetworkSetProviderParams {}

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
        private NetworkScope scope;
        private NetworkResolveProxyOptions resolveProxy;
        private NetworkAuthenticateNegotiateOptions authenticateNegotiate;

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(NetworkScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code resolveProxy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resolveProxy(NetworkResolveProxyOptions value) {
            this.resolveProxy = value;
            return this;
        }

        /**
         * Sets the {@code authenticateNegotiate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authenticateNegotiate(NetworkAuthenticateNegotiateOptions value) {
            this.authenticateNegotiate = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public NetworkSetProviderParams build() {
            return new NetworkSetProviderParams(scope, resolveProxy, authenticateNegotiate);
        }
    }
}
