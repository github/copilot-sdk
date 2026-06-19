/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// llmRecordingHandler answers every model-layer request with the synthetic
// non-inference fallback (catalog / session / policy, and empty JSON for the
// inference call itself). It records what it intercepted.
type llmRecordingHandler struct {
	mu       sync.Mutex
	received []*copilot.LlmInferenceRequest
}

func (h *llmRecordingHandler) OnLlmRequest(req *copilot.LlmInferenceRequest) error {
	h.mu.Lock()
	h.received = append(h.received, req)
	h.mu.Unlock()
	return llmHandleNonInferenceModelTraffic(req, nil)
}

func (h *llmRecordingHandler) snapshot() []*copilot.LlmInferenceRequest {
	h.mu.Lock()
	defer h.mu.Unlock()
	return append([]*copilot.LlmInferenceRequest(nil), h.received...)
}

func TestLlmInferenceCallback(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	handler := &llmRecordingHandler{}
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

	// The buffered fallback returns empty JSON for the inference call, which is
	// not a valid model response, so the turn fails; swallow that. What we
	// assert is that the runtime attempted the callback.
	_, _ = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
	_ = session.Disconnect()

	received := handler.snapshot()
	if len(received) == 0 {
		t.Fatal("Expected the runtime to invoke the inference callback")
	}

	var sawCatalog bool
	for _, r := range received {
		if !strings.HasPrefix(r.URL, "http://") && !strings.HasPrefix(r.URL, "https://") {
			t.Fatalf("Expected an absolute URL, got %q", r.URL)
		}
		if strings.HasSuffix(strings.ToLower(r.URL), "/models") {
			sawCatalog = true
		}
		if r.SessionID != "" && len(r.SessionID) == 0 {
			t.Fatal("session id should be non-empty when present")
		}
	}
	if !sawCatalog {
		t.Fatal("Expected to intercept the /models catalog request")
	}
}
