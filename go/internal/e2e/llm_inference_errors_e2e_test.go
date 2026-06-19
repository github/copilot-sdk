/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"errors"
	"net/http"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

type llmThrowingHandler struct {
	mu               sync.Mutex
	totalCalls       int
	callsBeforeError int
}

func (h *llmThrowingHandler) OnLlmRequest(req *copilot.LlmInferenceRequest) error {
	h.mu.Lock()
	h.totalCalls++
	h.mu.Unlock()

	served, err := llmServiceNonInference(req)
	if err != nil {
		return err
	}
	if served {
		return nil
	}

	url := strings.ToLower(req.URL)
	if strings.Contains(url, "/chat/completions") || strings.Contains(url, "/responses") {
		llmDrainRequest(req)
		h.mu.Lock()
		h.callsBeforeError++
		h.mu.Unlock()
		return errors.New("synthetic-callback-transport-failure")
	}

	return llmRespondBuffered(req, 200, http.Header{"content-type": {"application/json"}}, "{}")
}

func TestLlmInferenceErrors(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	handler := &llmThrowingHandler{}
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

	// The handler raises from the inference callback; the agent layer surfaces
	// it as an error or an event rather than hanging. The assertion is loose:
	// the inference call was attempted and the runtime did not hang.
	_, sendErr := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
	_ = session.Disconnect()

	handler.mu.Lock()
	total := handler.totalCalls
	before := handler.callsBeforeError
	handler.mu.Unlock()

	if total == 0 {
		t.Fatal("Expected the callback to be invoked")
	}
	if before == 0 {
		t.Fatal("Expected the inference callback to be reached and raise")
	}
	if sendErr != nil && len(sendErr.Error()) == 0 {
		t.Fatal("Expected a non-empty error string when an error surfaces")
	}
}
