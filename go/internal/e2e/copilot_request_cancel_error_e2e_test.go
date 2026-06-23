/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"errors"
	"io"
	"net/http"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// TestCopilotRequestCancelError covers the two terminal paths of
// CopilotRequestHandler that the happy-path handler and session-id tests never
// reach:
//
//   - error: the Transport returns an error for an inference request → the
//     adapter reports a transport error instead of hanging.
//   - cancel: the Transport blocks indefinitely on an inference request; when
//     the consumer aborts the turn the runtime cancels the in-flight request,
//     firing the request's context cancellation.

// --- error case ---

type throwingTransport struct {
	mu               sync.Mutex
	totalCalls       int
	callsBeforeError int
}

func (tr *throwingTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	tr.mu.Lock()
	tr.totalCalls++
	tr.mu.Unlock()

	if isInferenceURL(req.URL.String()) {
		// Drain the body so the request is fully consumed before erroring.
		if req.Body != nil {
			_, _ = io.Copy(io.Discard, req.Body)
		}
		tr.mu.Lock()
		tr.callsBeforeError++
		tr.mu.Unlock()
		return nil, errors.New("synthetic-callback-transport-failure")
	}
	return buildNonInferenceResponse(req.URL.String()), nil
}

func TestCopilotRequestError(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	transport := &throwingTransport{}
	handler := &copilot.CopilotRequestHandler{Transport: transport}
	client := newCopilotRequestClient(ctx, handler)
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create session: %v", err)
	}

	// The transport throws on inference; the agent layer surfaces it as an
	// error or an event rather than hanging.
	_, sendErr := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
	_ = session.Disconnect()

	transport.mu.Lock()
	total := transport.totalCalls
	before := transport.callsBeforeError
	transport.mu.Unlock()

	if total == 0 {
		t.Fatal("Expected the transport to be invoked")
	}
	if before == 0 {
		t.Fatal("Expected the inference transport call to be reached and raise")
	}
	if sendErr != nil && len(sendErr.Error()) == 0 {
		t.Fatal("Expected a non-empty error string when an error surfaces")
	}
}

// --- cancel case ---

type cancellingTransport struct {
	inferenceEntered atomic.Bool
	sawAbort         atomic.Bool
	abortSeen        chan struct{}
	once             sync.Once
}

func newCancellingTransport() *cancellingTransport {
	return &cancellingTransport{abortSeen: make(chan struct{})}
}

func (tr *cancellingTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	if !isInferenceURL(req.URL.String()) {
		return buildNonInferenceResponse(req.URL.String()), nil
	}
	if req.Body != nil {
		_, _ = io.Copy(io.Discard, req.Body)
	}
	tr.inferenceEntered.Store(true)
	// Block until the runtime cancels the request (via context cancellation).
	<-req.Context().Done()
	tr.sawAbort.Store(true)
	tr.once.Do(func() { close(tr.abortSeen) })
	return nil, req.Context().Err()
}

func waitFor(t *testing.T, predicate func() bool, timeout time.Duration) {
	t.Helper()
	deadline := time.Now().Add(timeout)
	for !predicate() {
		if time.Now().After(deadline) {
			t.Fatal("waitFor timed out")
		}
		time.Sleep(50 * time.Millisecond)
	}
}

func TestCopilotRequestCancel(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	transport := newCancellingTransport()
	handler := &copilot.CopilotRequestHandler{Transport: transport}
	client := newCopilotRequestClient(ctx, handler)
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create session: %v", err)
	}

	if _, err := session.Send(t.Context(), copilot.MessageOptions{Prompt: "Say OK."}); err != nil {
		t.Fatalf("send failed: %v", err)
	}
	waitFor(t, transport.inferenceEntered.Load, 60*time.Second)
	if err := session.Abort(t.Context()); err != nil {
		t.Fatalf("abort failed: %v", err)
	}

	select {
	case <-transport.abortSeen:
	case <-time.After(30 * time.Second):
		t.Fatal("Timed out waiting for the transport to observe runtime cancellation")
	}
	_ = session.Disconnect()

	if !transport.inferenceEntered.Load() {
		t.Fatal("Expected the inference transport call to be entered")
	}
	if !transport.sawAbort.Load() {
		t.Fatal("Expected the transport to observe the runtime-driven cancellation")
	}
}
