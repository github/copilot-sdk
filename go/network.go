// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"encoding/json"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

// NetworkScope selects which traffic the [NetworkConfig] callbacks serve.
//
// Experimental: NetworkScope is part of an experimental API and may change or
// be removed.
type NetworkScope = rpc.NetworkScope

const (
	// NetworkScopeConnection serves only requests made for sessions this client
	// creates or resumes.
	NetworkScopeConnection = rpc.NetworkScopeConnection
	// NetworkScopeRuntime also serves the runtime's shared background traffic.
	// Only one connection at a time can hold this scope.
	NetworkScopeRuntime = rpc.NetworkScopeRuntime
)

// NetworkResolveProxyRequest identifies the request target whose proxies are
// requested.
//
// Experimental: NetworkResolveProxyRequest is part of an experimental API and
// may change or be removed.
type NetworkResolveProxyRequest = rpc.NetworkResolveProxyRequest

// NetworkAuthenticateNegotiateRequest identifies the service principal that
// needs a token.
//
// Experimental: NetworkAuthenticateNegotiateRequest is part of an experimental
// API and may change or be removed.
type NetworkAuthenticateNegotiateRequest = rpc.NetworkAuthenticateNegotiateRequest

// NetworkConfig holds the callbacks through which the SDK host takes over parts
// of the runtime's networking, for example when the runtime runs in WSL or a
// container and should use VS Code's proxy settings and the host's Kerberos
// tickets. Each non-nil callback replaces the runtime's own handling for the
// traffic in Scope; for a nil callback, the runtime handles that part itself.
// Without any callback, nothing is registered.
//
// Each callback's ctx is cancelled when the runtime stops waiting: it cancels
// the request (for example when its wait times out) or the connection closes.
// The SDK then stops waiting for the callback and ignores any later result, so
// abandon the lookup promptly once ctx is done. If that happens before the
// callback starts, the callback is not called at all. Returned errors and
// panics are reported to the runtime as callback failures.
//
// Experimental: NetworkConfig is part of an experimental API and may change or
// be removed.
type NetworkConfig struct {
	// Scope selects which traffic the callbacks serve. When empty, the runtime
	// uses [NetworkScopeConnection]. With [NetworkScopeRuntime], Start fails if
	// another connection holds that scope. The registration ends when the
	// connection closes.
	Scope NetworkScope
	// ResolveProxy returns the proxies for request.URL as a PAC-style list such
	// as "PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT" (the format
	// of Electron's session.resolveProxy); return "DIRECT" for a direct
	// connection, or "" to let the runtime resolve the proxy itself, as if no
	// callback were registered. Entry types the runtime doesn't support, such as
	// SOCKS and QUIC, are skipped. request.URL is the
	// request target as a PAC script sees it: without credentials or fragment,
	// and for https and wss also without path and query.
	//
	// The callback decides every request that would otherwise follow ambient
	// proxy settings: proxy environment variables, NO_PROXY and operating-system
	// proxy settings are not consulted unless it returns "", and a failing
	// callback fails the request rather than connecting directly. Requests to
	// loopback targets, and requests with an explicitly configured proxy, never
	// consult it. The runtime caches answers, including "", for a few seconds.
	ResolveProxy func(ctx context.Context, request NetworkResolveProxyRequest) (string, error)
	// AuthenticateNegotiate returns a fresh, single-use base64 SPNEGO token
	// without the "Negotiate " prefix for a proxy's Negotiate challenge, or ""
	// when unavailable. The runtime then does not look up Kerberos credentials
	// itself, so a missing token or a failing callback fails the request.
	// request.Spn uses the platform-independent service/host[@REALM] form;
	// validate it before issuing credentials. Never log or persist tokens.
	AuthenticateNegotiate func(ctx context.Context, request NetworkAuthenticateNegotiateRequest) (string, error)
}

// networkSetProviderRequest returns the network.setProvider registration for
// config, or nil when config configures no callback.
func networkSetProviderRequest(config *NetworkConfig) *rpc.NetworkSetProviderRequest {
	if config == nil || config.ResolveProxy == nil && config.AuthenticateNegotiate == nil {
		return nil
	}
	request := &rpc.NetworkSetProviderRequest{}
	if scope := config.Scope; scope != "" {
		request.Scope = &scope
	}
	if config.ResolveProxy != nil {
		request.ResolveProxy = &rpc.NetworkResolveProxyOptions{}
	}
	if config.AuthenticateNegotiate != nil {
		request.AuthenticateNegotiate = &rpc.NetworkAuthenticateNegotiateOptions{}
	}
	return request
}

// networkAdapter handles network.resolveProxy and network.authenticateNegotiate
// directly rather than through the generated client-global handler, which does
// not pass the request context.
type networkAdapter struct {
	config           NetworkConfig
	connectionClosed <-chan struct{}
	// tests only: see handleCancellableRequest
	testHookCallbackGoroutine func(stage string)
}

// register installs the handlers for the configured callbacks.
func (a *networkAdapter) register(client *jsonrpc2.Client) {
	if a.config.ResolveProxy != nil {
		client.SetRequestContextHandler("network.resolveProxy", a.resolveProxy)
	}
	if a.config.AuthenticateNegotiate != nil {
		client.SetRequestContextHandler("network.authenticateNegotiate", a.authenticateNegotiate)
	}
}

// resolveProxy answers network.resolveProxy; see handleCancellableRequest for
// cancellation.
func (a *networkAdapter) resolveProxy(ctx context.Context, params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	return handleCancellableRequest(ctx, params, "Proxy resolution", a.connectionClosed, a.testHookCallbackGoroutine,
		func(ctx context.Context, request NetworkResolveProxyRequest) (rpc.NetworkResolveProxyResult, error) {
			proxy, err := a.config.ResolveProxy(ctx, request)
			if err != nil || proxy == "" {
				return rpc.NetworkResolveProxyResult{}, err
			}
			return rpc.NetworkResolveProxyResult{Proxy: &proxy}, nil
		})
}

// authenticateNegotiate answers network.authenticateNegotiate; see
// handleCancellableRequest for cancellation.
func (a *networkAdapter) authenticateNegotiate(ctx context.Context, params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	return handleCancellableRequest(ctx, params, "Negotiate authentication", a.connectionClosed, a.testHookCallbackGoroutine,
		func(ctx context.Context, request NetworkAuthenticateNegotiateRequest) (rpc.NetworkAuthenticateNegotiateResult, error) {
			token, err := a.config.AuthenticateNegotiate(ctx, request)
			if err != nil || token == "" {
				return rpc.NetworkAuthenticateNegotiateResult{}, err
			}
			return rpc.NetworkAuthenticateNegotiateResult{Token: &token}, nil
		})
}
