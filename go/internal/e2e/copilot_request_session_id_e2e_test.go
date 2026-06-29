/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"io"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"net/http"
)

type interceptedRequest struct {
	url       string
	sessionID string
}

// recordingTransport intercepts every model-layer request, records its URL and
// session ID (extracted from the CopilotRequestContext attached to the
// http.Request), and synthesizes a well-formed response so turns complete.
type recordingTransport struct {
	mu      sync.Mutex
	records []interceptedRequest
}

func (rt *recordingTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	rctx := copilot.RequestContextFrom(req)
	sessionID := ""
	if rctx != nil {
		sessionID = rctx.SessionID
	}
	rt.mu.Lock()
	rt.records = append(rt.records, interceptedRequest{url: req.URL.String(), sessionID: sessionID})
	rt.mu.Unlock()

	bodyBytes := []byte(nil)
	if req.Body != nil {
		bodyBytes, _ = io.ReadAll(req.Body)
	}
	bodyText := string(bodyBytes)

	if isInferenceURL(req.URL.String()) {
		return buildInferenceResponse(req.URL.String(), bodyText), nil
	}
	return buildNonInferenceResponse(req.URL.String()), nil
}

func (rt *recordingTransport) inferenceRecords() []interceptedRequest {
	rt.mu.Lock()
	defer rt.mu.Unlock()
	var out []interceptedRequest
	for _, r := range rt.records {
		if isInferenceURL(r.url) {
			out = append(out, r)
		}
	}
	return out
}

func TestCopilotRequestSessionID(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	transport := &recordingTransport{}
	handler := &copilot.CopilotRequestHandler{Transport: transport}
	client := newCopilotRequestClient(ctx, handler)
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	var capiSessionID string

	t.Run("threads session id into a CAPI session", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		capiSessionID = session.SessionID

		result, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
		if err != nil {
			t.Fatalf("send_and_wait failed: %v", err)
		}
		_ = session.Disconnect()

		inference := transport.inferenceRecords()
		if len(inference) == 0 {
			t.Fatal("Expected at least one intercepted inference request")
		}
		for _, r := range inference {
			if r.sessionID != capiSessionID {
				t.Fatalf("CAPI inference request must carry session id %q, got %q", capiSessionID, r.sessionID)
			}
		}

		// Validate the final assistant response arrived (guards against truncated captures)
		if !strings.Contains(assistantText(result), "OK from the synthetic") {
			t.Fatalf("Expected synthetic content in assistant reply, got %q", assistantText(result))
		}
	})

	t.Run("threads session id into a BYOK session", func(t *testing.T) {
		before := len(transport.inferenceRecords())
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               "claude-sonnet-4.5",
			Provider: &copilot.ProviderConfig{
				Type:      "openai",
				WireAPI:   "responses",
				BaseURL:   "https://byok.invalid/v1",
				APIKey:    "byok-secret",
				ModelID:   "claude-sonnet-4.5",
				WireModel: "claude-sonnet-4.5",
			},
		})
		if err != nil {
			t.Fatalf("Failed to create BYOK session: %v", err)
		}
		byokSessionID := session.SessionID

		result, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."})
		if err != nil {
			t.Fatalf("send_and_wait failed: %v", err)
		}
		_ = session.Disconnect()

		inference := transport.inferenceRecords()
		if len(inference) <= before {
			t.Fatal("Expected at least one intercepted BYOK inference request")
		}
		for _, r := range inference[before:] {
			if r.sessionID != byokSessionID {
				t.Fatalf("BYOK inference request must carry session id %q, got %q", byokSessionID, r.sessionID)
			}
		}

		if byokSessionID == capiSessionID {
			t.Fatal("Expected per-session ids to differ between turns")
		}

		// Validate the final assistant response arrived (guards against truncated captures)
		if !strings.Contains(assistantText(result), "OK from the synthetic") {
			t.Fatalf("Expected synthetic content in assistant reply, got %q", assistantText(result))
		}
	})
}
