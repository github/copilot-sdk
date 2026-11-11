/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.NetworkAuthenticateBasicRequest;
import com.github.copilot.generated.rpc.NetworkAuthenticateBasicResult;
import com.github.copilot.generated.rpc.NetworkAuthenticateNegotiateRequest;
import com.github.copilot.generated.rpc.NetworkResolveProxyRequest;
import com.github.copilot.generated.rpc.NetworkScope;

/**
 * Network callbacks through which the SDK host takes over parts of the
 * runtime's networking, for example when the runtime runs in WSL or a container
 * and should use VS Code's proxy settings, the host's Kerberos tickets and the
 * host's proxy credentials.
 * <p>
 * Each configured callback replaces the runtime's own handling for the traffic
 * in {@link #getScope() scope}; for a callback left unset, the runtime handles
 * that part itself. The client registers the configured callbacks during
 * startup, before any session can be created, and startup fails if the
 * registration is rejected. Without any callback, nothing is registered.
 * <p>
 * Callbacks run on the client executor, as other SDK host callbacks do. An
 * executor that runs tasks on the calling thread runs them on the connection's
 * reader, so a callback that blocks there also holds up the runtime's other
 * messages, including its cancellation, until it returns. If the runtime
 * cancels a request (for example when its wait times out) or the connection
 * closes, a queued invocation is skipped, a pending returned future is
 * cancelled, and any later result is ignored. If the executor rejects a
 * callback, the request fails.
 * <p>
 * <strong>Experimental.</strong> This API may change or be removed without
 * notice.
 *
 * <pre>{@code
 * var options = new CopilotClientOptions().setNetwork(
 * 		new NetworkConfig().setScope(NetworkScope.RUNTIME).setResolveProxy(request -> resolveProxyFor(request.url()))
 * 				.setAuthenticateNegotiate(request -> acquireSpnegoToken(request.spn()))
 * 				.setAuthenticateBasic(request -> lookUpProxyCredentials(request.proxy())));
 * }</pre>
 *
 * @see CopilotClientOptions#setNetwork(NetworkConfig)
 * @since 1.0.0
 */
@CopilotExperimental
public final class NetworkConfig {

    private NetworkScope scope;
    private Function<NetworkResolveProxyRequest, CompletableFuture<String>> resolveProxy;
    private Function<NetworkAuthenticateNegotiateRequest, CompletableFuture<String>> authenticateNegotiate;
    private Function<NetworkAuthenticateBasicRequest, CompletableFuture<NetworkAuthenticateBasicResult>> authenticateBasic;

    /**
     * Gets the traffic the callbacks serve.
     *
     * @return the scope, or {@code null} for the runtime default
     *         ({@link NetworkScope#CONNECTION})
     */
    public NetworkScope getScope() {
        return scope;
    }

    /**
     * Sets the traffic the callbacks serve.
     * <p>
     * {@link NetworkScope#CONNECTION} (the default) serves only requests made for
     * sessions this client creates or resumes. {@link NetworkScope#RUNTIME} also
     * serves the runtime's shared background traffic, such as catalog refreshes and
     * telemetry; only one connection at a time can hold it, so startup fails if
     * another client holds it. The scope applies to every configured callback, and
     * the registration ends when the connection closes.
     *
     * @param scope
     *            the scope, or {@code null} for the runtime default
     * @return this configuration for method chaining
     */
    public NetworkConfig setScope(NetworkScope scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Gets the proxy resolution callback.
     *
     * @return the callback, or {@code null} if the runtime resolves proxies itself
     */
    public Function<NetworkResolveProxyRequest, CompletableFuture<String>> getResolveProxy() {
        return resolveProxy;
    }

    /**
     * Sets the callback that decides the proxies for the runtime's requests.
     * <p>
     * The callback decides every request the runtime would otherwise route by
     * ambient proxy settings: proxy environment variables, {@code NO_PROXY} and
     * operating-system proxy settings are not consulted unless it answers
     * {@code null}, and a failing callback fails the request rather than connecting
     * directly. Requests to loopback targets, and requests with an explicitly
     * configured proxy, never consult it.
     * <p>
     * The request's {@code url()} is the request target as a PAC script sees it:
     * without credentials or fragment, and for {@code https} and {@code wss} also
     * without path and query. The returned future supplies a PAC-style list such as
     * {@code PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT} (the format
     * of Electron's {@code session.resolveProxy}); supply {@code DIRECT} for a
     * direct connection, or {@code null} or an empty string to let the runtime
     * resolve the proxy itself, as if no callback were registered. Entry types the
     * runtime doesn't support, such as SOCKS and QUIC, are skipped. The runtime
     * caches answers, including {@code null}, for 5 seconds and waits at most 10
     * seconds for the callback.
     *
     * @param resolveProxy
     *            the callback, or {@code null} to let the runtime resolve proxies
     *            itself
     * @return this configuration for method chaining
     */
    public NetworkConfig setResolveProxy(Function<NetworkResolveProxyRequest, CompletableFuture<String>> resolveProxy) {
        this.resolveProxy = resolveProxy;
        return this;
    }

    /**
     * Gets the Negotiate authentication callback.
     *
     * @return the callback, or {@code null} if the runtime looks up Kerberos
     *         credentials itself
     */
    public Function<NetworkAuthenticateNegotiateRequest, CompletableFuture<String>> getAuthenticateNegotiate() {
        return authenticateNegotiate;
    }

    /**
     * Sets the callback that supplies the Kerberos token for a proxy's Negotiate
     * challenge instead of the runtime's native credential lookup.
     * <p>
     * The request's {@code spn()} is a service principal in the
     * platform-independent {@code service/host[@REALM]} form; validate it before
     * issuing credentials. The returned future supplies a fresh, single-use base64
     * SPNEGO token without the {@code Negotiate } prefix, or {@code null} when
     * credentials are unavailable; a missing token or a failing callback fails the
     * request, unless the proxy also offers Basic and the
     * {@link #setAuthenticateBasic Basic callback} supplies credentials. The
     * runtime waits at most 30 seconds for the callback (less when the request's
     * own deadline is sooner). Tokens are not cached and must not be logged or
     * persisted. The callback supplies the initial SPNEGO token only; exchanges
     * requiring continuation tokens are not supported. Proxies chosen by the proxy
     * resolution callback are authenticated the same way.
     *
     * @param authenticateNegotiate
     *            the callback, or {@code null} to let the runtime look up Kerberos
     *            credentials itself
     * @return this configuration for method chaining
     */
    public NetworkConfig setAuthenticateNegotiate(
            Function<NetworkAuthenticateNegotiateRequest, CompletableFuture<String>> authenticateNegotiate) {
        this.authenticateNegotiate = authenticateNegotiate;
        return this;
    }

    /**
     * Gets the Basic authentication callback.
     *
     * @return the callback, or {@code null} if unset
     */
    public Function<NetworkAuthenticateBasicRequest, CompletableFuture<NetworkAuthenticateBasicResult>> getAuthenticateBasic() {
        return authenticateBasic;
    }

    /**
     * Sets the callback that supplies credentials for a proxy's Basic challenge.
     * <p>
     * Applies to requests the runtime tunnels through the proxy ({@code https} and
     * {@code wss}). The request's {@code proxy()} is the proxy that challenged, as
     * {@code http://host:port} or {@code https://host:port}; its {@code realm()},
     * if any, comes from the proxy, so treat it as untrusted text. The returned
     * future supplies the user name and password, or {@code null} (or an empty user
     * name) when credentials are unavailable, which fails the request with the
     * proxy's challenge; a failing callback fails the request. The user name must
     * not contain a colon, neither the user name nor the password may contain
     * control characters, and a {@code null} password means an empty one. The
     * runtime remembers the answer for that proxy and uses it for later
     * connections, until the proxy rejects it; then it calls again with
     * {@code previousRejected()} set, so answer with different credentials, for
     * example by asking the user, or with {@code null}. A later connection sends
     * the answer without waiting for a challenge, unless the runtime may try
     * Negotiate with that proxy first. A connection submits at most three sets of
     * credentials. While a call is pending, other requests through that proxy wait
     * for its answer. When a proxy offers both Negotiate and Basic, the runtime
     * prefers Negotiate and answers with Basic when Negotiate is unavailable or the
     * proxy rejects it. The runtime does not time the callback out: a request whose
     * own deadline passes stops waiting, but the answer is still remembered. Never
     * log or persist credentials.
     *
     * @param authenticateBasic
     *            the callback, or {@code null} to unset it
     * @return this configuration for method chaining
     */
    public NetworkConfig setAuthenticateBasic(
            Function<NetworkAuthenticateBasicRequest, CompletableFuture<NetworkAuthenticateBasicResult>> authenticateBasic) {
        this.authenticateBasic = authenticateBasic;
        return this;
    }
}
