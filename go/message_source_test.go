package copilot

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"reflect"
	"strings"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/propagation"
)

func messageSourceTestCases() []struct {
	name  string
	value MessageSource
	wire  string
} {
	return []struct {
		name  string
		value MessageSource
		wire  string
	}{
		{name: "omitted"},
		{name: "user", value: MessageSourceUser, wire: "user"},
		{name: "system", value: MessageSourceSystem, wire: "system"},
		{name: "agent", value: MessageSourceAgent("reviewer"), wire: "agent-reviewer"},
		{name: "empty agent id", value: MessageSourceAgent(""), wire: "agent-"},
		{name: "opaque agent id", value: MessageSourceAgent(" Agent/É "), wire: "agent- Agent/É "},
		{name: "prefixed agent id", value: MessageSourceAgent("agent-reviewer"), wire: "agent-agent-reviewer"},
	}
}

func TestSession_SendMessageSource(t *testing.T) {
	previousPropagator := otel.GetTextMapPropagator()
	otel.SetTextMapPropagator(propagation.TraceContext{})
	defer otel.SetTextMapPropagator(previousPropagator)

	const traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
	const tracestate = "vendor=value"

	for _, source := range messageSourceTestCases() {
		t.Run(source.name, func(t *testing.T) {
			for _, mode := range []string{"", "enqueue", "immediate"} {
				name := mode
				if name == "" {
					name = "defaults"
				}
				t.Run(name, func(t *testing.T) {
					ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
					defer cancel()
					options := MessageOptions{Prompt: "hello", Source: source.value}
					want := map[string]any{"sessionId": "session-1", "prompt": "hello"}
					if source.wire != "" {
						want["source"] = source.wire
					}
					if mode != "" {
						options.Mode = mode
						options.AgentMode = AgentModePlan
						options.DisplayPrompt = "display text"
						options.Attachments = []Attachment{
							&AttachmentFile{Path: "/workspace/main.go", DisplayName: "main.go"},
						}
						options.RequestHeaders = map[string]string{"X-Test": "value"}
						ctx = contextWithTraceParent(ctx, traceparent, tracestate)
						want["mode"] = mode
						want["agentMode"] = "plan"
						want["displayPrompt"] = "display text"
						want["attachments"] = []any{
							map[string]any{"type": "file", "path": "/workspace/main.go", "displayName": "main.go"},
						}
						want["requestHeaders"] = map[string]any{"X-Test": "value"}
						want["traceparent"] = traceparent
						want["tracestate"] = tracestate
					}

					params := captureMessageSourceRequest(t, nil, nil, func(session *Session) {
						messageID, err := session.Send(ctx, options)
						if err != nil {
							t.Fatalf("Send failed: %v", err)
						}
						if messageID != "message-1" {
							t.Fatalf("expected message-1, got %q", messageID)
						}
					})
					if !reflect.DeepEqual(params, want) {
						t.Fatalf("unexpected session.send params:\ngot  %#v\nwant %#v", params, want)
					}
				})
			}
		})
	}
}

func TestSession_SendAndWaitMessageSource(t *testing.T) {
	for _, source := range messageSourceTestCases() {
		for _, mode := range []string{"", "enqueue", "immediate"} {
			for _, tc := range []struct {
				name        string
				events      []SessionEvent
				rpcError    *jsonrpc2.Error
				wantContent string
				wantError   string
			}{
				{
					name:   "idle without assistant",
					events: []SessionEvent{{Data: &SessionIdleData{}}},
				},
				{
					name: "assistant then idle",
					events: []SessionEvent{
						{Data: &AssistantMessageData{MessageID: "assistant-1", Content: "done"}},
						{Data: &SessionIdleData{}},
					},
					wantContent: "done",
				},
				{
					name:      "session error",
					events:    []SessionEvent{{Data: &SessionErrorData{Message: "model failed"}}},
					wantError: "session error: model failed",
				},
				{
					name:      "RPC error",
					rpcError:  &jsonrpc2.Error{Code: -32602, Message: "invalid prompt"},
					wantError: "invalid prompt",
				},
			} {
				t.Run(source.name+"/"+mode+"/"+tc.name, func(t *testing.T) {
					ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
					defer cancel()
					params := captureMessageSourceRequest(t, tc.rpcError, tc.events, func(session *Session) {
						result, err := session.SendAndWait(ctx, MessageOptions{
							Prompt:        "background update",
							Source:        source.value,
							Mode:          mode,
							AgentMode:     AgentModePlan,
							DisplayPrompt: "Background update",
							Attachments: []Attachment{
								&AttachmentFile{Path: "/workspace/main.go", DisplayName: "main.go"},
							},
							RequestHeaders: map[string]string{"X-Test": "value"},
						})
						if tc.wantError != "" {
							if err == nil || !strings.Contains(err.Error(), tc.wantError) {
								t.Fatalf("expected error containing %q, got %v", tc.wantError, err)
							}
							if tc.rpcError != nil {
								var rpcError *jsonrpc2.Error
								if !errors.As(err, &rpcError) || rpcError.Code != tc.rpcError.Code {
									t.Fatalf("expected wrapped RPC error, got %v", err)
								}
							}
						} else if err != nil {
							t.Fatalf("SendAndWait failed: %v", err)
						}
						if tc.wantContent == "" {
							if result != nil {
								t.Fatalf("expected no assistant message, got %#v", result)
							}
						} else {
							if result == nil {
								t.Fatal("expected an assistant message")
							}
							message, ok := result.Data.(*AssistantMessageData)
							if !ok || message.Content != tc.wantContent {
								t.Fatalf("unexpected assistant message: %#v", result.Data)
							}
						}
					})
					want := map[string]any{
						"sessionId":     "session-1",
						"prompt":        "background update",
						"agentMode":     "plan",
						"displayPrompt": "Background update",
						"attachments": []any{
							map[string]any{"type": "file", "path": "/workspace/main.go", "displayName": "main.go"},
						},
						"requestHeaders": map[string]any{"X-Test": "value"},
					}
					if source.wire != "" {
						want["source"] = source.wire
					}
					if mode != "" {
						want["mode"] = mode
					}
					if !reflect.DeepEqual(params, want) {
						t.Fatalf("unexpected session.send params:\ngot  %#v\nwant %#v", params, want)
					}
				})
			}
		}
	}
}

func captureMessageSourceRequest(t *testing.T, rpcError *jsonrpc2.Error, events []SessionEvent, invoke func(*Session)) map[string]any {
	t.Helper()

	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	defer stdinW.Close()
	defer stdoutR.Close()
	defer stdoutW.Close()

	client := jsonrpc2.NewClient(stdinW, stdoutR)
	client.Start()
	defer client.Stop()

	session := newSession("session-1", client, "", false)
	defer session.stopEventProcessing()

	paramsCh := make(chan map[string]any, 1)
	errCh := make(chan error, 1)
	go func() {
		frame, err := readTestJSONRPCFrame(stdinR)
		if err != nil {
			errCh <- err
			return
		}
		var request struct {
			ID     json.RawMessage `json:"id"`
			Method string          `json:"method"`
			Params map[string]any  `json:"params"`
		}
		if err := json.Unmarshal(frame, &request); err != nil {
			errCh <- err
			return
		}
		if request.Method != "session.send" {
			errCh <- fmt.Errorf("expected session.send, got %s", request.Method)
			return
		}
		response := map[string]any{"jsonrpc": "2.0", "id": request.ID}
		if rpcError != nil {
			response["error"] = rpcError
		} else {
			response["result"] = map[string]any{"messageId": "message-1"}
		}
		data, err := json.Marshal(response)
		if err != nil {
			errCh <- err
			return
		}
		if _, err := fmt.Fprintf(stdoutW, "Content-Length: %d\r\n\r\n%s", len(data), data); err != nil {
			errCh <- err
			return
		}
		for _, event := range events {
			session.dispatchEvent(event)
		}
		paramsCh <- request.Params
	}()

	invoke(session)
	select {
	case params := <-paramsCh:
		return params
	case err := <-errCh:
		t.Fatal(err)
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for session.send request")
	}
	return nil
}
