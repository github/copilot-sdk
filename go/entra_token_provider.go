// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"encoding/json"
	"errors"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

// EntraAuthOptions configures the host application and optional host-owned acquisition.
//
// Experimental: this API may change or be removed.
type EntraAuthOptions struct {
	ClientID string
	// TokenProvider selects/revalidates identities and acquires resource tokens.
	// When nil, the runtime uses its built-in Entra sign-in.
	//
	// Experimental: this API may change or be removed.
	TokenProvider EntraTokenProvider
}

// EntraTokenProvider acquires the requested identity or resource token. Renewal
// must be silent and account-pinned. The context ends when this request or its
// original connection is cancelled. Unavailability never opens runtime sign-in.
//
// Experimental: this API may change or be removed.
type EntraTokenProvider func(context.Context, rpc.EntraTokenAcquireRequest) (rpc.EntraTokenAcquireResult, error)

func entraTokenHandler(provider EntraTokenProvider, connectionClosed <-chan struct{}) jsonrpc2.RequestContextHandler {
	return func(ctx context.Context, raw json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var params rpc.EntraTokenGetTokenRequest
		if err := json.Unmarshal(raw, &params); err != nil {
			return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInvalidParams.Code, Message: "Invalid Entra acquisition request"}
		}
		switch params.Request.(type) {
		case *rpc.EntraTokenAcquireRequestIdentity, *rpc.EntraTokenAcquireRequestToken:
		default:
			return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInvalidParams.Code, Message: "Invalid Entra acquisition request kind"}
		}

		acquisition, cancel := context.WithCancel(ctx)
		defer cancel()
		watcherDone := make(chan struct{})
		defer close(watcherDone)
		go func() {
			select {
			case <-connectionClosed:
				cancel()
			case <-watcherDone:
			}
		}()
		select {
		case <-connectionClosed:
			return nil, &jsonrpc2.Error{Code: -32800, Message: "Entra acquisition connection closed"}
		default:
		}
		if err := ctx.Err(); err != nil {
			return nil, &jsonrpc2.Error{Code: -32800, Message: "Entra acquisition cancelled"}
		}
		type outcome struct {
			result rpc.EntraTokenAcquireResult
			err    error
		}
		completed := make(chan outcome, 1)
		go func() {
			defer func() {
				if recover() != nil {
					completed <- outcome{err: errors.New("entra token provider panicked")}
				}
			}()
			result, err := provider(acquisition, params.Request)
			completed <- outcome{result, err}
		}()
		select {
		case <-acquisition.Done():
			return nil, &jsonrpc2.Error{Code: -32800, Message: "Entra acquisition cancelled"}
		case result := <-completed:
			select {
			case <-connectionClosed:
				return nil, &jsonrpc2.Error{Code: -32800, Message: "Entra acquisition connection closed"}
			default:
			}
			if ctx.Err() != nil {
				return nil, &jsonrpc2.Error{Code: -32800, Message: "Entra acquisition cancelled"}
			}
			if result.err != nil {
				return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: "Entra token provider failed"}
			}
			if result.result == nil {
				return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: "Entra token provider returned no result"}
			}
			encoded, err := json.Marshal(result.result)
			if err != nil {
				return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: "Invalid Entra token provider result"}
			}
			return encoded, nil
		}
	}
}
