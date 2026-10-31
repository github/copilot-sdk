// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

const kerberosAuthRequestCancelledCode = -32800

// KerberosAuthMode selects when the runtime uses a [KerberosAuthConfig] callback.
type KerberosAuthMode = rpc.KerberosAuthMode

const (
	// KerberosAuthModeFallback uses the callback only when native credential lookup fails.
	KerberosAuthModeFallback = rpc.KerberosAuthModeFallback
	// KerberosAuthModeAlways never attempts native credential lookup.
	KerberosAuthModeAlways = rpc.KerberosAuthModeAlways
)

// KerberosAuthScope selects which traffic a [KerberosAuthConfig] callback authenticates.
type KerberosAuthScope = rpc.KerberosAuthScope

const (
	// KerberosAuthScopeConnection authenticates only requests made for
	// sessions this client creates or resumes.
	KerberosAuthScopeConnection = rpc.KerberosAuthScopeConnection
	// KerberosAuthScopeRuntime also authenticates the runtime's shared
	// background traffic. Only one connection at a time can hold this scope.
	KerberosAuthScopeRuntime = rpc.KerberosAuthScopeRuntime
)

// KerberosAuthAcquireRequest identifies the service principal that needs a token.
type KerberosAuthAcquireRequest = rpc.KerberosAuthAcquireRequest

// KerberosAuthConfig supplies Kerberos credentials from the SDK host for a
// requested service principal. The registration ends when the connection
// closes.
type KerberosAuthConfig struct {
	// Mode selects whether the callback is a fallback for native credential
	// lookup ([KerberosAuthModeFallback]) or replaces it ([KerberosAuthModeAlways]).
	Mode KerberosAuthMode
	// Scope selects which traffic the callback authenticates. When empty, the
	// runtime uses [KerberosAuthScopeConnection]. With [KerberosAuthScopeRuntime],
	// Start fails if another connection holds that scope.
	Scope KerberosAuthScope
	// Callback returns a fresh, single-use base64 SPNEGO token without the
	// "Negotiate " prefix, or "" when unavailable. request.Spn uses the
	// platform-independent service/host[@REALM] form; validate it before
	// issuing credentials. Returned errors are reported to the runtime.
	//
	// ctx is cancelled when the runtime stops waiting for the token (it cancels
	// the request after 30 seconds) or the connection closes. The SDK then
	// stops waiting and ignores any later result, so abandon the lookup
	// promptly once ctx is done.
	Callback func(ctx context.Context, request KerberosAuthAcquireRequest) (string, error)
}

func validateKerberosAuthConfig(config *KerberosAuthConfig) error {
	if config.Mode != KerberosAuthModeFallback && config.Mode != KerberosAuthModeAlways {
		return fmt.Errorf("invalid KerberosAuth.Mode %q: expected %q or %q", config.Mode, KerberosAuthModeFallback, KerberosAuthModeAlways)
	}
	if config.Callback == nil {
		return errors.New("KerberosAuth.Callback is required")
	}
	return nil
}

// kerberosAuthAdapter handles kerberosAuth.acquire directly rather than through
// the generated client-global handler, which does not pass the request context.
type kerberosAuthAdapter struct {
	callback         func(ctx context.Context, request KerberosAuthAcquireRequest) (string, error)
	connectionClosed <-chan struct{}
}

// handle runs the callback with a context that is cancelled when the runtime
// cancels the request ($/cancelRequest) or the connection closes. Cancellation
// stops waiting for the callback and answers with the standard request-cancelled
// error; any later result is ignored.
func (a *kerberosAuthAdapter) handle(ctx context.Context, params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var request rpc.KerberosAuthAcquireRequest
	if err := json.Unmarshal(params, &request); err != nil {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInvalidParams.Code, Message: fmt.Sprintf("Invalid params: %v", err)}
	}
	callbackCtx, cancel := context.WithCancel(ctx)
	defer cancel()
	select {
	case <-ctx.Done():
		return nil, kerberosAuthCancellationError(ctx)
	case <-a.connectionClosed:
		return nil, kerberosAuthCancellationError(ctx)
	default:
	}
	go func() {
		select {
		case <-a.connectionClosed:
			cancel()
		case <-callbackCtx.Done():
		}
	}()

	type outcome struct {
		token string
		err   error
	}
	outcomes := make(chan outcome, 1)
	go func() {
		defer func() {
			if r := recover(); r != nil {
				outcomes <- outcome{err: fmt.Errorf("kerberos auth callback panicked: %v", r)}
			}
		}()
		token, err := a.callback(callbackCtx, request)
		outcomes <- outcome{token: token, err: err}
	}()

	var result outcome
	select {
	case <-callbackCtx.Done():
		return nil, kerberosAuthCancellationError(ctx)
	case result = <-outcomes:
	}
	if callbackCtx.Err() != nil {
		return nil, kerberosAuthCancellationError(ctx)
	}
	if result.err != nil {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: result.err.Error()}
	}
	response := rpc.KerberosAuthAcquireResult{}
	if result.token != "" {
		response.Token = &result.token
	}
	raw, err := json.Marshal(response)
	if err != nil {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
	}
	return raw, nil
}

// kerberosAuthCancellationError reports why a request ended before the
// callback answered: requestCtx is cancelled by $/cancelRequest; otherwise the
// connection closed.
func kerberosAuthCancellationError(requestCtx context.Context) *jsonrpc2.Error {
	if requestCtx.Err() != nil {
		return &jsonrpc2.Error{Code: kerberosAuthRequestCancelledCode, Message: "Kerberos auth request cancelled"}
	}
	return &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: "Kerberos auth connection closed"}
}
