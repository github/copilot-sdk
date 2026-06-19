/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"encoding/json"
	"net/http"
	"regexp"
	"strings"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Shared synthetic-upstream helpers for the LLM inference callback e2e tests.
//
// These tests have no recorded snapshots: the registered callback fabricates
// well-formed model responses and the runtime routes all of its model-layer
// HTTP/WebSocket traffic through that callback instead of the CAPI proxy. The
// helpers centralise the synthetic CAPI shapes (model catalog, policy,
// /responses SSE, /chat/completions) so each test focuses on the behaviour it
// is exercising.

const llmSyntheticText = "OK from the synthetic stream."

var llmStreamTrueRe = regexp.MustCompile(`"stream"\s*:\s*true`)

func llmStreamTrue(body string) bool {
	return llmStreamTrueRe.MatchString(body)
}

func llmIsInferenceURL(url string) bool {
	u := strings.ToLower(url)
	return strings.HasSuffix(u, "/chat/completions") ||
		strings.HasSuffix(u, "/responses") ||
		strings.HasSuffix(u, "/v1/messages") ||
		strings.HasSuffix(u, "/messages")
}

func llmSSE(eventType string, data map[string]any) string {
	raw, _ := json.Marshal(data)
	return "event: " + eventType + "\ndata: " + string(raw) + "\n\n"
}

func llmModelCatalog(supportedEndpoints []string) string {
	model := map[string]any{
		"id":                   "claude-sonnet-4.5",
		"name":                 "Claude Sonnet 4.5",
		"object":               "model",
		"vendor":               "Anthropic",
		"version":              "1",
		"preview":              false,
		"model_picker_enabled": true,
		"capabilities": map[string]any{
			"type":      "chat",
			"family":    "claude-sonnet-4.5",
			"tokenizer": "o200k_base",
			"limits": map[string]any{
				"max_context_window_tokens": 200000,
				"max_output_tokens":         8192,
			},
			"supports": map[string]any{
				"streaming":           true,
				"tool_calls":          true,
				"parallel_tool_calls": true,
				"vision":              true,
			},
		},
	}
	if supportedEndpoints != nil {
		model["supported_endpoints"] = supportedEndpoints
	}
	raw, _ := json.Marshal(map[string]any{"data": []any{model}})
	return string(raw)
}

// llmResponsesEvents returns the ordered /responses event objects the runtime's
// reducer expects. Used raw (one object == one WebSocket message) for the WS
// path and SSE-framed for the HTTP path.
func llmResponsesEvents(text, respID string) []map[string]any {
	return []map[string]any{
		{
			"type":     "response.created",
			"response": map[string]any{"id": respID, "object": "response", "status": "in_progress", "output": []any{}},
		},
		{
			"type":         "response.output_item.added",
			"output_index": 0,
			"item":         map[string]any{"id": "msg_1", "type": "message", "role": "assistant", "content": []any{}},
		},
		{
			"type":          "response.content_part.added",
			"output_index":  0,
			"content_index": 0,
			"part":          map[string]any{"type": "output_text", "text": ""},
		},
		{"type": "response.output_text.delta", "output_index": 0, "content_index": 0, "delta": text},
		{"type": "response.output_text.done", "output_index": 0, "content_index": 0, "text": text},
		{
			"type": "response.completed",
			"response": map[string]any{
				"id":     respID,
				"object": "response",
				"status": "completed",
				"output": []any{
					map[string]any{
						"id":      "msg_1",
						"type":    "message",
						"role":    "assistant",
						"content": []any{map[string]any{"type": "output_text", "text": text}},
					},
				},
				"usage": map[string]any{"input_tokens": 5, "output_tokens": 7, "total_tokens": 12},
			},
		},
	}
}

func llmDrainRequest(req *copilot.LlmInferenceRequest) string {
	var sb strings.Builder
	for frame := range req.RequestBody {
		sb.Write(frame)
	}
	return sb.String()
}

func llmRespondBuffered(req *copilot.LlmInferenceRequest, status int, headers http.Header, body string) error {
	llmDrainRequest(req)
	if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: status, Headers: headers}); err != nil {
		return err
	}
	if body != "" {
		if err := req.ResponseBody.Write([]byte(body)); err != nil {
			return err
		}
	}
	return req.ResponseBody.End()
}

// llmServiceNonInference serves the model catalog, model session and policy
// endpoints. Returns true when the request was one of those (and answered).
func llmServiceNonInference(req *copilot.LlmInferenceRequest) (bool, error) {
	url := strings.ToLower(req.URL)
	switch {
	case strings.HasSuffix(url, "/models"):
		return true, llmRespondBuffered(req, 200, http.Header{"content-type": {"application/json"}}, llmModelCatalog(nil))
	case strings.Contains(url, "/models/session"):
		return true, llmRespondBuffered(req, 200, http.Header{}, "{}")
	case strings.Contains(url, "/policy"):
		return true, llmRespondBuffered(req, 200, http.Header{}, `{"state":"enabled"}`)
	}
	return false, nil
}

// llmHandleNonInferenceModelTraffic serves every non-inference model-layer
// request, including an empty-JSON fallback for anything unrecognised.
func llmHandleNonInferenceModelTraffic(req *copilot.LlmInferenceRequest, supportedEndpoints []string) error {
	url := strings.ToLower(req.URL)
	switch {
	case strings.HasSuffix(url, "/models"):
		return llmRespondBuffered(req, 200, http.Header{"content-type": {"application/json"}}, llmModelCatalog(supportedEndpoints))
	case strings.Contains(url, "/models/session"):
		return llmRespondBuffered(req, 200, http.Header{}, "{}")
	case strings.Contains(url, "/policy"):
		return llmRespondBuffered(req, 200, http.Header{}, `{"state":"enabled"}`)
	}
	return llmRespondBuffered(req, 200, http.Header{"content-type": {"application/json"}}, "{}")
}

// llmHandleInference synthesizes a well-formed inference response, dispatching
// by URL and the request body's stream flag exactly as a real reverse proxy
// would.
func llmHandleInference(req *copilot.LlmInferenceRequest, text string) error {
	body := llmDrainRequest(req)
	wantsStream := llmStreamTrue(body)
	url := strings.ToLower(req.URL)

	if strings.Contains(url, "/responses") {
		events := llmResponsesEvents(text, "resp_stub_1")
		if !wantsStream {
			if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: 200, Headers: http.Header{"content-type": {"application/json"}}}); err != nil {
				return err
			}
			last := events[len(events)-1]["response"]
			raw, _ := json.Marshal(last)
			if err := req.ResponseBody.Write(raw); err != nil {
				return err
			}
			return req.ResponseBody.End()
		}
		if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: 200, Headers: http.Header{"content-type": {"text/event-stream"}}}); err != nil {
			return err
		}
		for _, event := range events {
			if err := req.ResponseBody.Write([]byte(llmSSE(event["type"].(string), event))); err != nil {
				return err
			}
		}
		return req.ResponseBody.End()
	}

	if strings.Contains(url, "/chat/completions") && wantsStream {
		if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: 200, Headers: http.Header{"content-type": {"text/event-stream"}}}); err != nil {
			return err
		}
		base := func() map[string]any {
			return map[string]any{
				"id":      "chatcmpl-stub-1",
				"object":  "chat.completion.chunk",
				"created": 1,
				"model":   "claude-sonnet-4.5",
			}
		}
		c1 := base()
		c1["choices"] = []any{map[string]any{"index": 0, "delta": map[string]any{"role": "assistant", "content": ""}, "finish_reason": nil}}
		c2 := base()
		c2["choices"] = []any{map[string]any{"index": 0, "delta": map[string]any{"content": text}, "finish_reason": nil}}
		c3 := base()
		c3["choices"] = []any{map[string]any{"index": 0, "delta": map[string]any{}, "finish_reason": "stop"}}
		c3["usage"] = map[string]any{"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12}
		for _, chunk := range []map[string]any{c1, c2, c3} {
			raw, _ := json.Marshal(chunk)
			if err := req.ResponseBody.Write([]byte("data: " + string(raw) + "\n\n")); err != nil {
				return err
			}
		}
		if err := req.ResponseBody.Write([]byte("data: [DONE]\n\n")); err != nil {
			return err
		}
		return req.ResponseBody.End()
	}

	if err := req.ResponseBody.Start(copilot.LlmInferenceResponseInit{Status: 200, Headers: http.Header{"content-type": {"application/json"}}}); err != nil {
		return err
	}
	raw, _ := json.Marshal(map[string]any{
		"id":      "chatcmpl-stub-1",
		"object":  "chat.completion",
		"created": 1,
		"model":   "claude-sonnet-4.5",
		"choices": []any{
			map[string]any{"index": 0, "message": map[string]any{"role": "assistant", "content": text}, "finish_reason": "stop"},
		},
		"usage": map[string]any{"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12},
	})
	if err := req.ResponseBody.Write(raw); err != nil {
		return err
	}
	return req.ResponseBody.End()
}

func assistantText(msg *copilot.SessionEvent) string {
	if msg == nil {
		return ""
	}
	if d, ok := msg.Data.(*copilot.AssistantMessageData); ok {
		return d.Content
	}
	return ""
}

// newLlmClient builds a client wired to handler via LlmInferenceConfig. The
// shared ctx harness client has no inference callback, so each inference test
// owns an isolated client carrying its own handler. extraEnv is appended to the
// spawned runtime's environment (e.g. to flip an ExP flag for the WS transport).
func newLlmClient(ctx *testharness.TestContext, handler copilot.LlmInferenceProvider, extraEnv ...string) *copilot.Client {
	return ctx.NewClient(func(o *copilot.ClientOptions) {
		o.LlmInference = &copilot.LlmInferenceConfig{Handler: handler}
		if len(extraEnv) > 0 {
			o.Env = append(o.Env, extraEnv...)
		}
	})
}
