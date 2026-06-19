/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"sync/atomic"
	"testing"

	"github.com/coder/websocket"
	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

const (
	llmHandlerHTTPText = "OK from synthetic HTTP upstream."
	llmHandlerWSText   = "OK from synthetic WS upstream."
)

type llmHandlerCounters struct {
	httpRequests       atomic.Int32
	httpResponses      atomic.Int32
	wsRequestMessages  atomic.Int32
	wsResponseMessages atomic.Int32
	upstreamWSRequests atomic.Int32
}

func llmSSEBody(text, respID string) string {
	var sb strings.Builder
	for _, event := range llmResponsesEvents(text, respID) {
		sb.WriteString(llmSSE(event["type"].(string), event))
	}
	return sb.String()
}

// startFakeUpstream brings up a real HTTP upstream (catalog / policy /
// responses-SSE) and a real WebSocket upstream that echoes the ordered
// /responses events per inbound message.
func startFakeUpstream(t *testing.T, counters *llmHandlerCounters) (httpURL, wsURL string) {
	t.Helper()

	httpSrv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := strings.ToLower(strings.SplitN(r.URL.Path, "?", 2)[0])
		_ = r.Body.Close
		switch {
		case strings.HasSuffix(path, "/models"):
			w.Header().Set("content-type", "application/json")
			_, _ = w.Write([]byte(llmModelCatalog(llmWSSupportedEndpoints)))
		case strings.HasSuffix(path, "/models/session"):
			w.Header().Set("content-type", "application/json")
			_, _ = w.Write([]byte("{}"))
		case strings.Contains(path, "/policy"):
			w.Header().Set("content-type", "application/json")
			_, _ = w.Write([]byte(`{"state":"enabled"}`))
		case strings.HasSuffix(path, "/responses"):
			w.Header().Set("content-type", "text/event-stream")
			_, _ = w.Write([]byte(llmSSEBody(llmHandlerHTTPText, "resp_stub_http")))
		default:
			w.Header().Set("content-type", "application/json")
			w.WriteHeader(http.StatusNotFound)
			_, _ = w.Write([]byte(`{"error":"not_found"}`))
		}
	}))
	t.Cleanup(httpSrv.Close)

	wsSrv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		c, err := websocket.Accept(w, r, &websocket.AcceptOptions{InsecureSkipVerify: true})
		if err != nil {
			return
		}
		defer c.Close(websocket.StatusNormalClosure, "")
		c.SetReadLimit(-1)
		bg := context.Background()
		for {
			_, _, readErr := c.Read(bg)
			if readErr != nil {
				return
			}
			counters.upstreamWSRequests.Add(1)
			for _, event := range llmResponsesEvents(llmHandlerWSText, "resp_stub_ws") {
				raw, _ := json.Marshal(event)
				if err := c.Write(bg, websocket.MessageText, raw); err != nil {
					return
				}
			}
		}
	}))
	t.Cleanup(wsSrv.Close)

	return httpSrv.URL, "ws://" + strings.TrimPrefix(wsSrv.URL, "http://")
}

type llmRewritingRoundTripper struct {
	base     *url.URL
	counters *llmHandlerCounters
	inner    http.RoundTripper
}

func (rt *llmRewritingRoundTripper) RoundTrip(req *http.Request) (*http.Response, error) {
	rt.counters.httpRequests.Add(1)
	req.URL.Scheme = rt.base.Scheme
	req.URL.Host = rt.base.Host
	req.Host = rt.base.Host
	req.Header.Set("x-test-mutated", "1")
	resp, err := rt.inner.RoundTrip(req)
	if err != nil {
		return nil, err
	}
	rt.counters.httpResponses.Add(1)
	resp.Header.Set("x-test-response-mutated", "1")
	return resp, nil
}

func TestLlmInferenceHandler(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	counters := &llmHandlerCounters{}
	httpURL, wsURL := startFakeUpstream(t, counters)

	httpBase, err := url.Parse(httpURL)
	if err != nil {
		t.Fatalf("Failed to parse upstream URL: %v", err)
	}
	wsBase, err := url.Parse(wsURL)
	if err != nil {
		t.Fatalf("Failed to parse upstream ws URL: %v", err)
	}

	handler := &copilot.LlmRequestHandler{
		Transport: &llmRewritingRoundTripper{
			base:     httpBase,
			counters: counters,
			inner:    http.DefaultTransport.(*http.Transport).Clone(),
		},
		OpenWebSocket: func(rctx *copilot.LlmRequestContext) (copilot.CopilotWebSocketHandler, error) {
			parsed, perr := url.Parse(rctx.URL)
			if perr != nil {
				return nil, perr
			}
			parsed.Scheme = wsBase.Scheme
			parsed.Host = wsBase.Host
			fwd := copilot.NewForwardingWebSocketHandler(parsed.String(), rctx.Headers)
			fwd.OnSendRequestMessage = func(data []byte) []byte {
				counters.wsRequestMessages.Add(1)
				return data
			}
			fwd.OnSendResponseMessage = func(data []byte) []byte {
				counters.wsResponseMessages.Add(1)
				return data
			}
			return fwd, nil
		},
	}

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

	// The HTTP seam fired — the runtime issued model-layer GETs (catalog,
	// policy) and possibly a single-shot inference through the RoundTripper.
	if counters.httpRequests.Load() == 0 {
		t.Fatal("Expected the HTTP RoundTripper to fire")
	}
	if counters.httpResponses.Load() == 0 {
		t.Fatal("Expected the HTTP response mutation to fire")
	}

	// The WebSocket seam fired — the main agent turn went over the WS path and
	// we observed messages in both directions.
	if counters.wsRequestMessages.Load() == 0 {
		t.Fatal("Expected runtime → upstream ws messages")
	}
	if counters.wsResponseMessages.Load() == 0 {
		t.Fatal("Expected upstream → runtime ws messages")
	}
	if counters.upstreamWSRequests.Load() == 0 {
		t.Fatal("Expected the upstream WS to receive request messages")
	}

	// Validate the final assistant response arrived (guards against truncated captures)
	text := assistantText(result)
	if !strings.Contains(text, "OK from synthetic") || !strings.Contains(text, "upstream") {
		t.Fatalf("Expected synthetic upstream content in assistant reply, got %q", text)
	}
}
