/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"net/http"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

type llmCancellingHandler struct {
	inferenceEntered atomic.Bool
	sawAbort         atomic.Bool
	abortSeen        chan struct{}
	once             sync.Once
}

func newLlmCancellingHandler() *llmCancellingHandler {
	return &llmCancellingHandler{abortSeen: make(chan struct{})}
}

func (h *llmCancellingHandler) OnLlmRequest(req *copilot.LlmInferenceRequest) error {
	served, err := llmServiceNonInference(req)
	if err != nil {
		return err
	}
	if served {
		return nil
	}
	if !llmIsInferenceURL(req.URL) {
		return llmRespondBuffered(req, 200, http.Header{"content-type": {"application/json"}}, "{}")
	}

	// Inference: never produce a response. Wait for the runtime to cancel us,
	// recording the abort.
	llmDrainRequest(req)
	h.inferenceEntered.Store(true)
	<-req.Context.Done()
	h.sawAbort.Store(true)
	h.once.Do(func() { close(h.abortSeen) })
	// Runtime already dropped the request on cancel; the sink error is a no-op.
	_ = req.ResponseBody.Error("cancelled by upstream", "cancelled")
	return nil
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

func TestLlmInferenceCancel(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	handler := newLlmCancellingHandler()
	client := newLlmClient(ctx, handler)
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
	waitFor(t, handler.inferenceEntered.Load, 60*time.Second)
	if err := session.Abort(t.Context()); err != nil {
		t.Fatalf("abort failed: %v", err)
	}

	select {
	case <-handler.abortSeen:
	case <-time.After(30 * time.Second):
		t.Fatal("Timed out waiting for the consumer to observe runtime cancellation")
	}
	_ = session.Disconnect()

	if !handler.inferenceEntered.Load() {
		t.Fatal("Expected the inference callback to be entered")
	}
	if !handler.sawAbort.Load() {
		t.Fatal("Expected the consumer to observe the runtime-driven cancellation")
	}
}
