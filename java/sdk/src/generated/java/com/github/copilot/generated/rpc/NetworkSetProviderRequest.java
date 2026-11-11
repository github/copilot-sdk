/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Registers the calling SDK connection's network callbacks, replacing any previous registration.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NetworkSetProviderRequest {

    /** Traffic the callbacks serve; defaults to `connection`. */
    @JsonProperty("scope")
    private NetworkScope scope;

    /** Present when the client answers `network.resolveProxy`. */
    @JsonProperty("resolveProxy")
    private NetworkResolveProxyOptions resolveProxy;

    /** Present when the client answers `network.authenticateNegotiate`. */
    @JsonProperty("authenticateNegotiate")
    private NetworkAuthenticateNegotiateOptions authenticateNegotiate;

    /** Present when the client answers `network.authenticateBasic`. */
    @JsonProperty("authenticateBasic")
    private NetworkAuthenticateBasicOptions authenticateBasic;

    /**
     * Creates a request with its required inputs.
     */
    public NetworkSetProviderRequest() {
    }

    /**
     * Returns the {@code scope} property.
     *
     * @return Traffic the callbacks serve; defaults to `connection`.
     */
    public NetworkScope getScope() {
        return scope;
    }

    /**
     * Returns the {@code resolveProxy} property.
     *
     * @return Present when the client answers `network.resolveProxy`.
     */
    public NetworkResolveProxyOptions getResolveProxy() {
        return resolveProxy;
    }

    /**
     * Returns the {@code authenticateNegotiate} property.
     *
     * @return Present when the client answers `network.authenticateNegotiate`.
     */
    public NetworkAuthenticateNegotiateOptions getAuthenticateNegotiate() {
        return authenticateNegotiate;
    }

    /**
     * Returns the {@code authenticateBasic} property.
     *
     * @return Present when the client answers `network.authenticateBasic`.
     */
    public NetworkAuthenticateBasicOptions getAuthenticateBasic() {
        return authenticateBasic;
    }

    /**
     * Sets the {@code scope} property.
     *
     * @param value Traffic the callbacks serve; defaults to `connection`.
     * @return this request
     */
    public NetworkSetProviderRequest setScope(NetworkScope value) {
        this.scope = value;
        return this;
    }

    /**
     * Sets the {@code resolveProxy} property.
     *
     * @param value Present when the client answers `network.resolveProxy`.
     * @return this request
     */
    public NetworkSetProviderRequest setResolveProxy(NetworkResolveProxyOptions value) {
        this.resolveProxy = value;
        return this;
    }

    /**
     * Sets the {@code authenticateNegotiate} property.
     *
     * @param value Present when the client answers `network.authenticateNegotiate`.
     * @return this request
     */
    public NetworkSetProviderRequest setAuthenticateNegotiate(NetworkAuthenticateNegotiateOptions value) {
        this.authenticateNegotiate = value;
        return this;
    }

    /**
     * Sets the {@code authenticateBasic} property.
     *
     * @param value Present when the client answers `network.authenticateBasic`.
     * @return this request
     */
    public NetworkSetProviderRequest setAuthenticateBasic(NetworkAuthenticateBasicOptions value) {
        this.authenticateBasic = value;
        return this;
    }
}
