// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

// requestCancelledCode is the standard JSON-RPC error code for a request its
// sender cancelled.
const requestCancelledCode = -32800

// handleCancellableRequest answers a runtime request by running a client
// callback with a context that is cancelled when the runtime cancels the
// request ($/cancelRequest, which cancels ctx) or the connection closes.
// Cancellation stops waiting for the callback and answers with the standard
// request-cancelled error; any later result is ignored. label names the
// callback in errors, such as "Proxy resolution".
//
// testHook is for tests only: it runs in the callback goroutine with "starting"
// before that goroutine checks for cancellation, and with "skipped" when
// cancellation keeps the callback from starting.
func handleCancellableRequest[Request, Result any](
	ctx context.Context,
	params json.RawMessage,
	label string,
	connectionClosed <-chan struct{},
	testHook func(stage string),
	callback func(ctx context.Context, request Request) (Result, error),
) (json.RawMessage, *jsonrpc2.Error) {
	var request Request
	if err := json.Unmarshal(params, &request); err != nil {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInvalidParams.Code, Message: fmt.Sprintf("Invalid params: %v", err)}
	}
	callbackCtx, cancel := context.WithCancel(ctx)
	defer cancel()
	select {
	case <-ctx.Done():
		return nil, callbackCancellationError(ctx, label)
	case <-connectionClosed:
		return nil, callbackCancellationError(ctx, label)
	default:
	}
	go func() {
		select {
		case <-connectionClosed:
			cancel()
		case <-callbackCtx.Done():
		}
	}()

	type outcome struct {
		result Result
		err    error
	}
	outcomes := make(chan outcome, 1)
	go func() {
		defer func() {
			if r := recover(); r != nil {
				outcomes <- outcome{err: fmt.Errorf("%s callback panicked: %v", strings.ToLower(label), r)}
			}
		}()
		if testHook != nil {
			testHook("starting")
		}
		// The request may be cancelled, or the connection closed, after the check
		// above but before this goroutine runs: do not start an obsolete callback,
		// such as a credential prompt. The handler then answers the cancellation.
		select {
		case <-callbackCtx.Done():
		case <-connectionClosed:
		default:
			result, err := callback(callbackCtx, request)
			outcomes <- outcome{result: result, err: err}
			return
		}
		if testHook != nil {
			testHook("skipped")
		}
	}()

	var answer outcome
	select {
	case <-callbackCtx.Done():
		return nil, callbackCancellationError(ctx, label)
	case <-connectionClosed:
		return nil, callbackCancellationError(ctx, label)
	case answer = <-outcomes:
	}
	// A callback can answer as the connection closes, before the watcher above
	// cancels callbackCtx: discard that answer too.
	select {
	case <-connectionClosed:
		return nil, callbackCancellationError(ctx, label)
	default:
	}
	if callbackCtx.Err() != nil {
		return nil, callbackCancellationError(ctx, label)
	}
	if answer.err != nil {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: answer.err.Error()}
	}
	raw, err := json.Marshal(answer.result)
	if err != nil {
		return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: fmt.Sprintf("Failed to marshal response: %v", err)}
	}
	return raw, nil
}

// callbackCancellationError reports why a request ended before its callback
// answered: requestCtx is cancelled by $/cancelRequest; otherwise the
// connection closed.
func callbackCancellationError(requestCtx context.Context, label string) *jsonrpc2.Error {
	if requestCtx.Err() != nil {
		return &jsonrpc2.Error{Code: requestCancelledCode, Message: label + " request cancelled"}
	}
	return &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: label + " connection closed"}
}
