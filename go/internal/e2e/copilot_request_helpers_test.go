/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"encoding/json"
	"io"
	"net/http"
	"regexp"
	"strings"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Shared synthetic-upstream helpers for the CopilotRequestHandler e2e tests.
//
// These tests have no recorded snapshots: the registered handler fabricates
// well-formed model responses and the runtime routes all of its model-layer
// HTTP/WebSocket traffic through that handler instead of the CAPI proxy. The
// helpers centralise the synthetic CAPI shapes (model catalog, policy,
// /responses SSE, /chat/completions) so each test focuses on the behaviour it
// is exercising.

const syntheticResponseText = "OK from the synthetic stream."

var streamTrueRe = regexp.MustCompile(`"stream"\s*:\s*true`)

func isStreamingRequest(body string) bool {
	return streamTrueRe.MatchString(body)
}

func isInferenceURL(url string) bool {
	u := strings.ToLower(url)
	return strings.HasSuffix(u, "/chat/completions") ||
		strings.HasSuffix(u, "/responses") ||
		strings.HasSuffix(u, "/v1/messages") ||
		strings.HasSuffix(u, "/messages")
}

func sseFrame(eventType string, data map[string]any) string {
	raw, _ := json.Marshal(data)
	return "event: " + eventType + "\ndata: " + string(raw) + "\n\n"
}

func modelCatalogJSON(supportedEndpoints []string) string {
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

// responsesEvents returns the ordered /responses event objects the runtime's
// reducer expects. Used raw (one object == one WebSocket message) for the WS
// path and SSE-framed for the HTTP path.
func responsesEvents(text, respID string) []map[string]any {
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

// buildResponsesSSEBody returns a complete SSE body for a /responses streaming response.
func buildResponsesSSEBody(text, respID string) string {
	var sb strings.Builder
	for _, event := range responsesEvents(text, respID) {
		sb.WriteString(sseFrame(event["type"].(string), event))
	}
	return sb.String()
}

// buildAnthropicMessageSSEBody returns a complete Anthropic Messages SSE body for a
// streaming /messages response (message_start … message_stop). The buffered JSON
// message is only valid for a non-streaming request; a streaming request expects
// named SSE events or the runtime fails to finalize the message.
func buildAnthropicMessageSSEBody(text string) string {
	events := []struct {
		name string
		data map[string]any
	}{
		{"message_start", map[string]any{
			"type": "message_start",
			"message": map[string]any{
				"id": "msg_stub_1", "type": "message", "role": "assistant",
				"model": "claude-sonnet-4.5", "content": []any{},
				"stop_reason": nil, "stop_sequence": nil,
				"usage": map[string]any{"input_tokens": 5, "output_tokens": 1},
			},
		}},
		{"content_block_start", map[string]any{
			"type": "content_block_start", "index": 0,
			"content_block": map[string]any{"type": "text", "text": ""},
		}},
		{"content_block_delta", map[string]any{
			"type": "content_block_delta", "index": 0,
			"delta": map[string]any{"type": "text_delta", "text": text},
		}},
		{"content_block_stop", map[string]any{"type": "content_block_stop", "index": 0}},
		{"message_delta", map[string]any{
			"type":  "message_delta",
			"delta": map[string]any{"stop_reason": "end_turn", "stop_sequence": nil},
			"usage": map[string]any{"output_tokens": 7},
		}},
		{"message_stop", map[string]any{"type": "message_stop"}},
	}
	var sb strings.Builder
	for _, event := range events {
		sb.WriteString(sseFrame(event.name, event.data))
	}
	return sb.String()
}

// buildInferenceResponse synthesizes a well-formed inference HTTP response.
func buildInferenceResponse(url string, bodyText string) *http.Response {
	wantsStream := isStreamingRequest(bodyText)
	u := strings.ToLower(url)

	if strings.Contains(u, "/responses") {
		if wantsStream {
			return buildSSEResponse(buildResponsesSSEBody(syntheticResponseText, "resp_stub_1"))
		}
		events := responsesEvents(syntheticResponseText, "resp_stub_1")
		last := events[len(events)-1]["response"]
		raw, _ := json.Marshal(last)
		return buildJSONResponse(200, string(raw))
	}

	if strings.Contains(u, "/chat/completions") && wantsStream {
		base := func() map[string]any {
			return map[string]any{
				"id": "chatcmpl-stub-1", "object": "chat.completion.chunk",
				"created": 1, "model": "claude-sonnet-4.5",
			}
		}
		c1 := base()
		c1["choices"] = []any{map[string]any{"index": 0, "delta": map[string]any{"role": "assistant", "content": ""}, "finish_reason": nil}}
		c2 := base()
		c2["choices"] = []any{map[string]any{"index": 0, "delta": map[string]any{"content": syntheticResponseText}, "finish_reason": nil}}
		c3 := base()
		c3["choices"] = []any{map[string]any{"index": 0, "delta": map[string]any{}, "finish_reason": "stop"}}
		c3["usage"] = map[string]any{"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12}
		var sb strings.Builder
		for _, chunk := range []map[string]any{c1, c2, c3} {
			raw, _ := json.Marshal(chunk)
			sb.WriteString("data: " + string(raw) + "\n\n")
		}
		sb.WriteString("data: [DONE]\n\n")
		return buildSSEResponse(sb.String())
	}

	if strings.HasSuffix(u, "/messages") {
		if wantsStream {
			return buildSSEResponse(buildAnthropicMessageSSEBody(syntheticResponseText))
		}
		raw, _ := json.Marshal(map[string]any{
			"id":            "msg_stub_1",
			"type":          "message",
			"role":          "assistant",
			"model":         "claude-sonnet-4.5",
			"content":       []any{map[string]any{"type": "text", "text": syntheticResponseText}},
			"stop_reason":   "end_turn",
			"stop_sequence": nil,
			"usage":         map[string]any{"input_tokens": 5, "output_tokens": 7},
		})
		return buildJSONResponse(200, string(raw))
	}

	raw, _ := json.Marshal(map[string]any{
		"id": "chatcmpl-stub-1", "object": "chat.completion", "created": 1, "model": "claude-sonnet-4.5",
		"choices": []any{map[string]any{"index": 0, "message": map[string]any{"role": "assistant", "content": syntheticResponseText}, "finish_reason": "stop"}},
		"usage":   map[string]any{"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12},
	})
	return buildJSONResponse(200, string(raw))
}

// buildNonInferenceResponse serves catalog / session / policy endpoints.
func buildNonInferenceResponse(url string) *http.Response {
	u := strings.ToLower(url)
	switch {
	case strings.HasSuffix(u, "/models"):
		return buildJSONResponse(200, modelCatalogJSON(nil))
	case strings.Contains(u, "/models/session"):
		return buildJSONResponse(200, "{}")
	case strings.Contains(u, "/policy"):
		return buildJSONResponse(200, `{"state":"enabled"}`)
	}
	return buildJSONResponse(200, "{}")
}

func buildJSONResponse(status int, body string) *http.Response {
	return &http.Response{
		StatusCode: status,
		Status:     http.StatusText(status),
		Header:     http.Header{"Content-Type": {"application/json"}},
		Body:       io.NopCloser(strings.NewReader(body)),
	}
}

func buildSSEResponse(body string) *http.Response {
	return &http.Response{
		StatusCode: 200,
		Status:     "OK",
		Header:     http.Header{"Content-Type": {"text/event-stream"}, "Cache-Control": {"no-cache"}},
		Body:       io.NopCloser(strings.NewReader(body)),
	}
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

// newCopilotRequestClient builds a client wired to handler via RequestHandler.
// Each test that needs inference interception owns an isolated client carrying
// its own handler. extraEnv is appended to the spawned runtime's environment
// (e.g. to flip an ExP flag for the WS transport).
func newCopilotRequestClient(ctx *testharness.TestContext, handler *copilot.CopilotRequestHandler, extraEnv ...string) *copilot.Client {
	return ctx.NewClient(func(o *copilot.ClientOptions) {
		o.RequestHandler = handler
		if len(extraEnv) > 0 {
			o.Env = append(o.Env, extraEnv...)
		}
	})
}
