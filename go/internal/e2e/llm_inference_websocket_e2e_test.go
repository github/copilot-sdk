/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"encoding/json"
	"net/http"
	"strings"
	"sync"
	"sync/atomic"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

const llmWSText = "OK from the synthetic ws."

var llmWSSupportedEndpoints = []string{"/responses", "ws:/responses"}

type llmWebSocketHandler struct {
	mu             sync.Mutex
	received       []*copilot.LlmInferenceRequest
	wsRequestCount atomic.Int32
}

// handleHTTPInference answers single-shot HTTP inference requests (e.g. title
// generation) that don't pick the WebSocket transport.
func (h *llmWebSocketHandler) handleHTTPInference(req *copilot.LlmInferenceRequest) error {
	llmDrainRequest(req)
	if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: 200, Headers: http.Header{"content-type": {"text/event-stream"}}}); err != nil {
		return err
	}
	for _, event := range llmResponsesEvents(llmWSText, "resp_stub_ws_1") {
		if err := req.ResponseBody.Write([]byte(llmSSE(event["type"].(string), event))); err != nil {
			return err
		}
	}
	return req.ResponseBody.End()
}

func (h *llmWebSocketHandler) handleWebSocket(req *copilot.LlmInferenceRequest) error {
	// Ack the upgrade (status 101-equivalent) before any message flows.
	if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: 101, Headers: http.Header{}}); err != nil {
		return err
	}
	// One inbound chunk == one WS message (a response.create request).
	for range req.RequestBody {
		h.wsRequestCount.Add(1)
		for _, event := range llmResponsesEvents(llmWSText, "resp_stub_ws_1") {
			raw, _ := json.Marshal(event)
			if err := req.ResponseBody.Write(raw); err != nil {
				return nil
			}
		}
	}
	return req.ResponseBody.End()
}

func (h *llmWebSocketHandler) OnLlmRequest(req *copilot.LlmInferenceRequest) error {
	h.mu.Lock()
	h.received = append(h.received, req)
	h.mu.Unlock()

	if req.Transport == "websocket" {
		return h.handleWebSocket(req)
	}
	if llmIsInferenceURL(req.URL) {
		return h.handleHTTPInference(req)
	}
	return llmHandleNonInferenceModelTraffic(req, llmWSSupportedEndpoints)
}

func (h *llmWebSocketHandler) wsRequests() int {
	h.mu.Lock()
	defer h.mu.Unlock()
	n := 0
	for _, r := range h.received {
		if r.Transport == "websocket" {
			n++
		}
	}
	return n
}

func TestLlmInferenceWebSocket(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	handler := &llmWebSocketHandler{}
	client := newLlmClient(ctx, handler, "COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES=true")
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

	// The main agent turn (tools present, not single-shot) selected the
	// WebSocket transport and drove it through the callback.
	if handler.wsRequests() == 0 {
		t.Fatal("Expected at least one websocket request via the callback")
	}
	if handler.wsRequestCount.Load() == 0 {
		t.Fatal("Expected the runtime to send at least one ws message")
	}

	// Validate the final assistant response arrived (guards against truncated captures)
	if !strings.Contains(assistantText(result), "OK from the synthetic ws") {
		t.Fatalf("Expected synthetic ws content in assistant reply, got %q", assistantText(result))
	}
}
