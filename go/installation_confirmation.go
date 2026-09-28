// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

const installationConfirmationRequestCancelledCode = -32800

// InstallationConfirmationRequest is the typed review request for one sealed
// installation operation on its original connection.
//
// Experimental: InstallationConfirmationRequest may change or be removed.
type InstallationConfirmationRequest = rpc.InstallationConfirmationRequest

// InstallationConfirmationDecision is the explicit human decision returned by
// an installation confirmation handler.
//
// Experimental: InstallationConfirmationDecision may change or be removed.
type InstallationConfirmationDecision = rpc.InstallationDecision

const (
	// InstallationConfirmationDecisionCancel cancels the pending decision without granting consent.
	InstallationConfirmationDecisionCancel = rpc.InstallationDecisionCancel
	// InstallationConfirmationDecisionConfirm explicitly approves the exact review.
	InstallationConfirmationDecisionConfirm = rpc.InstallationDecisionConfirm
	// InstallationConfirmationDecisionDecline declines the reviewed operation.
	InstallationConfirmationDecisionDecline = rpc.InstallationDecisionDecline
)

// InstallationConfirmationContext is cancelled when the runtime retires this
// JSON-RPC request, including via numeric $/cancelRequest, or when the original
// connection is closed or the client is stopped. It does not cancel outbound
// installation or OAuth RPCs.
//
// Experimental: InstallationConfirmationContext may change or be removed.
type InstallationConfirmationContext = context.Context

// InstallationConfirmationHandler collects a fresh human decision for the
// complete runtime review.
//
// Match OperationID and PolicySessionID against the exact action registered by
// this client before displaying the review. Refuse unknown actions or
// incomplete reviews; never infer authority from the current session. The SDK
// echoes the original challenge and fingerprint, so the handler returns only a
// decision.
//
// Experimental: InstallationConfirmationHandler may change or be removed.
type InstallationConfirmationHandler interface {
	ConfirmInstallation(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error)
}

// InstallationConfirmationHandlerFunc adapts a function to
// [InstallationConfirmationHandler].
//
// Experimental: InstallationConfirmationHandlerFunc may change or be removed.
type InstallationConfirmationHandlerFunc func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error)

// ConfirmInstallation calls f(request, context).
func (f InstallationConfirmationHandlerFunc) ConfirmInstallation(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
	return f(request, context)
}

type installationConfirmationAdapter struct {
	handler          InstallationConfirmationHandler
	connectionClosed <-chan struct{}
	closed           chan struct{}
	closeOnce        sync.Once
}

func newInstallationConfirmationAdapter(handler InstallationConfirmationHandler, connectionClosed <-chan struct{}) *installationConfirmationAdapter {
	adapter := &installationConfirmationAdapter{
		handler:          handler,
		connectionClosed: connectionClosed,
		closed:           make(chan struct{}),
	}
	go adapter.watchConnectionClosed()
	return adapter
}

func (a *installationConfirmationAdapter) watchConnectionClosed() {
	select {
	case <-a.connectionClosed:
		a.close()
	case <-a.closed:
	}
}

func (a *installationConfirmationAdapter) close() {
	a.closeOnce.Do(func() {
		close(a.closed)
	})
}

func (a *installationConfirmationAdapter) connectionClosedSignal() <-chan struct{} {
	return a.closed
}

func (a *installationConfirmationAdapter) handle(ctx context.Context, params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var request rpc.InstallationConfirmationRequest
	if err := json.Unmarshal(params, &request); err != nil {
		return nil, &jsonrpc2.Error{
			Code:    jsonrpc2.ErrInvalidParams.Code,
			Message: fmt.Sprintf("Invalid installation confirmation review: %v", err),
		}
	}
	confirmationID := request.ConfirmationID
	reviewFingerprint := request.ReviewFingerprint
	connectionClosed := a.connectionClosedSignal()
	if cancellation := installationConfirmationCancellationError(ctx, connectionClosed); cancellation != nil {
		return nil, cancellation
	}
	handlerContext, stopWatchingConnection := a.handlerContext(ctx, connectionClosed)
	defer stopWatchingConnection()

	type outcome struct {
		decision InstallationConfirmationDecision
		err      error
	}
	outcomes := make(chan outcome, 1)
	go func() {
		defer func() {
			if r := recover(); r != nil {
				outcomes <- outcome{err: fmt.Errorf("installation confirmation handler panicked: %v", r)}
			}
		}()
		decision, err := a.handler.ConfirmInstallation(request, handlerContext)
		outcomes <- outcome{decision: decision, err: err}
	}()

	select {
	case <-ctx.Done():
		return nil, &jsonrpc2.Error{
			Code:    installationConfirmationRequestCancelledCode,
			Message: "Installation confirmation request cancelled",
		}
	case <-connectionClosed:
		return nil, &jsonrpc2.Error{
			Code:    jsonrpc2.ErrInternal.Code,
			Message: "Installation confirmation connection closed",
		}
	case outcome := <-outcomes:
		if cancellation := installationConfirmationCancellationError(ctx, connectionClosed); cancellation != nil {
			return nil, cancellation
		}
		if outcome.err != nil {
			return nil, &jsonrpc2.Error{Code: jsonrpc2.ErrInternal.Code, Message: outcome.err.Error()}
		}
		if outcome.decision != InstallationConfirmationDecisionCancel && outcome.decision != InstallationConfirmationDecisionConfirm && outcome.decision != InstallationConfirmationDecisionDecline {
			return nil, &jsonrpc2.Error{
				Code:    jsonrpc2.ErrInternal.Code,
				Message: "Invalid installation confirmation decision",
			}
		}
		response := rpc.InstallationConfirmationResponse{
			ConfirmationID:    confirmationID,
			Decision:          outcome.decision,
			ReviewFingerprint: reviewFingerprint,
		}
		raw, err := json.Marshal(response)
		if err != nil {
			return nil, &jsonrpc2.Error{
				Code:    jsonrpc2.ErrInternal.Code,
				Message: fmt.Sprintf("Installation confirmation serialisation failed: %v", err),
			}
		}
		return raw, nil
	}
}

func (a *installationConfirmationAdapter) handlerContext(ctx context.Context, connectionClosed <-chan struct{}) (InstallationConfirmationContext, func()) {
	handlerContext, cancel := context.WithCancel(ctx)
	watcherDone := make(chan struct{})
	go func() {
		select {
		case <-connectionClosed:
			cancel()
		case <-watcherDone:
		}
	}()
	return handlerContext, func() {
		close(watcherDone)
		cancel()
	}
}

func installationConfirmationCancellationError(ctx context.Context, connectionClosed <-chan struct{}) *jsonrpc2.Error {
	select {
	case <-ctx.Done():
		return &jsonrpc2.Error{
			Code:    installationConfirmationRequestCancelledCode,
			Message: "Installation confirmation request cancelled",
		}
	default:
	}
	select {
	case <-connectionClosed:
		return &jsonrpc2.Error{
			Code:    jsonrpc2.ErrInternal.Code,
			Message: "Installation confirmation connection closed",
		}
	default:
		return nil
	}
}
