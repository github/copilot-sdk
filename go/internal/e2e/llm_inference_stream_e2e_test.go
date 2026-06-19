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

type llmStreamingHandler struct {
	mu       sync.Mutex
	received []*copilot.LlmInferenceRequest
}

func (h *llmStreamingHandler) OnLlmRequest(req *copilot.LlmInferenceRequest) error {
	h.mu.Lock()
	h.received = append(h.received, req)
	h.mu.Unlock()
	if llmIsInferenceURL(req.URL) {
		return llmHandleInference(req, llmSyntheticText)
	}
	return llmHandleNonInferenceModelTraffic(req, nil)
}

func (h *llmStreamingHandler) inferenceCount() int {
	h.mu.Lock()
	defer h.mu.Unlock()
	n := 0
	for _, r := range h.received {
		if llmIsInferenceURL(r.URL) {
			n++
		}
	}
	return n
}

func TestLlmInferenceStream(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	handler := &llmStreamingHandler{}
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

	result, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
	if err != nil {
		t.Fatalf("send_and_wait failed: %v", err)
	}
	_ = session.Disconnect()

	if handler.inferenceCount() == 0 {
		t.Fatal("Expected at least one inference request via the callback")
	}

	// Validate the final assistant response arrived (guards against truncated captures)
	if !strings.Contains(assistantText(result), "OK from the synthetic") {
		t.Fatalf("Expected synthetic content in assistant reply, got %q", assistantText(result))
	}
}
