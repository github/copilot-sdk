/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"net/http"
	"sync/atomic"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

type llmConsumerCancelHandler struct {
	inferenceAttempts atomic.Int32
}

func (h *llmConsumerCancelHandler) OnLlmRequest(req *copilot.LlmInferenceRequest) error {
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

	// Consumer-initiated cancellation: the consumer's own upstream call was
	// aborted, so it tells the runtime to give up on this request. No response
	// head is ever produced; the runtime should see a transport failure rather
	// than hanging.
	llmDrainRequest(req)
	h.inferenceAttempts.Add(1)
	return req.ResponseBody.Error("upstream call aborted by consumer", "cancelled")
}

func TestLlmInferenceConsumerCancel(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	handler := &llmConsumerCancelHandler{}
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

	_, sendErr := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
	_ = session.Disconnect()

	// The runtime reached the inference step and the consumer's cancellation
	// terminated it (rather than the runtime hanging).
	if handler.inferenceAttempts.Load() == 0 {
		t.Fatal("Expected the inference callback to be attempted")
	}
	if sendErr != nil && len(sendErr.Error()) == 0 {
		t.Fatal("Expected a non-empty error string when a failure surfaces")
	}
}
