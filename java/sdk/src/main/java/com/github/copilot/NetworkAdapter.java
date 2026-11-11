/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.NetworkAuthenticateBasicOptions;
import com.github.copilot.generated.rpc.NetworkAuthenticateBasicRequest;
import com.github.copilot.generated.rpc.NetworkAuthenticateBasicResult;
import com.github.copilot.generated.rpc.NetworkAuthenticateNegotiateOptions;
import com.github.copilot.generated.rpc.NetworkAuthenticateNegotiateRequest;
import com.github.copilot.generated.rpc.NetworkAuthenticateNegotiateResult;
import com.github.copilot.generated.rpc.NetworkResolveProxyOptions;
import com.github.copilot.generated.rpc.NetworkResolveProxyRequest;
import com.github.copilot.generated.rpc.NetworkResolveProxyResult;
import com.github.copilot.generated.rpc.NetworkSetProviderRequest;
import com.github.copilot.generated.rpc.ServerRpc;
import com.github.copilot.rpc.NetworkConfig;

/**
 * Registers the configured network callbacks and bridges the runtime's
 * {@code network.resolveProxy}, {@code network.authenticateNegotiate} and
 * {@code network.authenticateBasic} reverse RPC requests to them.
 * <p>
 * Only configured callbacks are registered and answered; each one honors
 * {@code $/cancelRequest} and connection close through
 * {@link CancellableCallbackAdapter}.
 */
final class NetworkAdapter {

    private final NetworkSetProviderRequest registration;
    private final List<CancellableCallbackAdapter<?, ?>> callbacks;

    private NetworkAdapter(NetworkSetProviderRequest registration, List<CancellableCallbackAdapter<?, ?>> callbacks) {
        this.registration = registration;
        this.callbacks = callbacks;
    }

    /**
     * Snapshots the configuration's callbacks and scope.
     *
     * @return the adapter, or {@code null} when no callback is configured
     */
    static NetworkAdapter create(NetworkConfig config, Executor executor) {
        if (config == null) {
            return null;
        }
        var resolveProxy = config.getResolveProxy();
        var authenticateNegotiate = config.getAuthenticateNegotiate();
        var authenticateBasic = config.getAuthenticateBasic();
        if (resolveProxy == null && authenticateNegotiate == null && authenticateBasic == null) {
            return null;
        }
        var callbacks = new ArrayList<CancellableCallbackAdapter<?, ?>>(3);
        if (resolveProxy != null) {
            callbacks.add(new CancellableCallbackAdapter<>("network.resolveProxy", "Proxy resolution",
                    NetworkResolveProxyRequest.class, resolveProxy,
                    value -> NetworkResolveProxyResult.builder().proxy(value).build(), executor));
        }
        if (authenticateNegotiate != null) {
            callbacks.add(new CancellableCallbackAdapter<>("network.authenticateNegotiate", "Negotiate authentication",
                    NetworkAuthenticateNegotiateRequest.class, authenticateNegotiate,
                    value -> NetworkAuthenticateNegotiateResult.builder().token(value).build(), executor));
        }
        if (authenticateBasic != null) {
            // A null answer is sent as an empty result: no credentials.
            callbacks.add(new CancellableCallbackAdapter<>("network.authenticateBasic", "Basic authentication",
                    NetworkAuthenticateBasicRequest.class, authenticateBasic,
                    value -> value != null ? value : NetworkAuthenticateBasicResult.builder().build(), executor));
        }
        var registration = new NetworkSetProviderRequest().setScope(config.getScope());
        if (resolveProxy != null) {
            registration.setResolveProxy(NetworkResolveProxyOptions.builder().build());
        }
        if (authenticateNegotiate != null) {
            registration.setAuthenticateNegotiate(NetworkAuthenticateNegotiateOptions.builder().build());
        }
        if (authenticateBasic != null) {
            registration.setAuthenticateBasic(NetworkAuthenticateBasicOptions.builder().build());
        }
        return new NetworkAdapter(registration, List.copyOf(callbacks));
    }

    void registerHandlers(JsonRpcClient rpc) {
        callbacks.forEach(callback -> callback.registerHandlers(rpc));
    }

    /**
     * Registers the configured callbacks with the runtime.
     */
    CompletableFuture<Void> register(ServerRpc serverRpc) {
        return serverRpc.network.setProvider(registration);
    }

    void handleCancel(JsonRpcClient rpc, JsonNode params) {
        callbacks.forEach(callback -> callback.handleCancel(rpc, params));
    }

    void closePending() {
        callbacks.forEach(CancellableCallbackAdapter::closePending);
    }
}
