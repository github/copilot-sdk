package copilot

import (
	"context"
	"io"
	"reflect"
	"strings"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

type structuredInventory struct {
	Count int    `json:"count"`
	Color string `json:"color"`
}

func structuredMessage(content, origin string) SessionEvent {
	return SessionEvent{Data: &AssistantMessageData{
		MessageID: "assistant", OriginatingMessageID: ptr(origin), Content: content,
	}}
}

func TestStructuredOutputTypedSchemaAndCorrelation(t *testing.T) {
	subagent := structuredMessage(`{"count":999,"color":"wrong"}`, "message-1")
	subagent.AgentID = ptr("child")
	events := []SessionEvent{
		{Data: &SessionIdleData{}},
		{Data: &SessionErrorData{Message: "before this run"}},
		{Data: &UserMessageData{MessageID: ptr("message-1")}},
		structuredMessage(`{"count":42,"color":"red"}`, "message-1"),
		{Data: &SessionIdleData{Mode: ptr(SessionModeAutopilot)}},
		structuredMessage(`{"count":99,"color":"blue"}`, "message-1"),
		subagent,
		structuredMessage(`{"count":123,"color":"wrong"}`, "other"),
		{Data: &SessionIdleData{}},
	}
	params := captureSessionSendRequest(t, nil, events, true, func(session *Session) {
		options := MessageOptions{Prompt: "inventory"}
		result, err := SendAndWait[structuredInventory](t.Context(), session, options)
		if err != nil || result != (structuredInventory{Count: 99, Color: "blue"}) {
			t.Fatalf("unexpected typed result: %+v, %v", result, err)
		}
		if options.ResponseSchema != nil {
			t.Fatal("mutated caller options")
		}
		if len(session.handlers) != 0 {
			t.Fatal("structured wait leaked subscription")
		}
	})
	format := params["responseFormat"].(map[string]any)
	contract := format["jsonSchema"].(map[string]any)
	schema := contract["schema"].(map[string]any)
	if format["type"] != "json_schema" || contract["strict"] != true || schema["type"] != "object" {
		t.Fatalf("unexpected response format: %#v", format)
	}
	if schema["additionalProperties"] != false {
		t.Fatalf("typed struct schema must be closed for strict output: %#v", schema)
	}
	properties := schema["properties"].(map[string]any)
	if properties["count"].(map[string]any)["type"] != "integer" || properties["color"].(map[string]any)["type"] != "string" {
		t.Fatalf("incorrect inferred schema: %#v", schema)
	}
}

func TestStructuredOutputRawSchemaUnchanged(t *testing.T) {
	schema := map[string]any{"type": "object", "description": "unmodified"}
	events := []SessionEvent{structuredMessage(`{"count":42}`, "message-1"), {Data: &SessionIdleData{}}}
	params := captureMessageSourceRequest(t, nil, events, func(session *Session) {
		event, err := session.SendAndWait(t.Context(), MessageOptions{Prompt: "inventory", ResponseSchema: schema})
		if err != nil || event.Data.(*AssistantMessageData).Content != `{"count":42}` {
			t.Fatalf("unexpected raw result: %v, %v", event, err)
		}
	})
	got := params["responseFormat"].(map[string]any)["jsonSchema"].(map[string]any)["schema"]
	if !reflect.DeepEqual(got, schema) {
		t.Fatalf("schema changed: %#v", got)
	}
}

func TestStructuredOutputFailures(t *testing.T) {
	for _, tc := range []struct {
		name     string
		events   []SessionEvent
		rpcError *jsonrpc2.Error
		want     string
	}{
		{"missing", []SessionEvent{{Data: &UserMessageData{MessageID: ptr("message-1")}}, {Data: &SessionIdleData{}}}, nil, "without a structured"},
		{"blank", []SessionEvent{structuredMessage(" ", "message-1"), {Data: &SessionIdleData{}}}, nil, "without a structured"},
		{"aborted", []SessionEvent{structuredMessage(`{"count":1}`, "message-1"), {Data: &SessionIdleData{Aborted: ptr(true)}}}, nil, "aborted"},
		{"session error", []SessionEvent{{Data: &UserMessageData{MessageID: ptr("message-1")}}, {Data: &SessionErrorData{Message: "provider failed"}}}, nil, "provider failed"},
		{"admission", nil, &jsonrpc2.Error{Code: -32602, Message: "invalid schema"}, "invalid schema"},
		{"null", []SessionEvent{structuredMessage("null", "message-1"), {Data: &SessionIdleData{}}}, nil, "JSON null"},
		{"invalid json", []SessionEvent{structuredMessage("not JSON", "message-1"), {Data: &SessionIdleData{}}}, nil, "decode structured"},
		{"wrong type", []SessionEvent{structuredMessage(`{"count":"bad"}`, "message-1"), {Data: &SessionIdleData{}}}, nil, "decode structured"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			captureMessageSourceRequest(t, tc.rpcError, tc.events, func(session *Session) {
				ctx, cancel := context.WithTimeout(t.Context(), time.Second)
				defer cancel()
				_, err := SendAndWait[structuredInventory](ctx, session, MessageOptions{Prompt: "inventory"})
				if err == nil || !strings.Contains(err.Error(), tc.want) {
					t.Fatalf("expected %q, got %v", tc.want, err)
				}
				if len(session.handlers) != 0 {
					t.Fatal("leaked subscription")
				}
			})
		})
	}
}

func TestStructuredOutputTypedRejectsConflictingOptions(t *testing.T) {
	for _, options := range []MessageOptions{{Mode: "immediate"}, {ResponseSchema: map[string]any{}}} {
		_, err := SendAndWait[structuredInventory](t.Context(), &Session{}, options)
		if err == nil || !strings.Contains(err.Error(), "cannot specify") {
			t.Fatalf("expected pre-admission rejection, got %v", err)
		}
	}
}

func TestStructuredOutputDisconnectDuringAdmission(t *testing.T) {
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
	ctx, cancel := context.WithTimeout(t.Context(), time.Second)
	defer cancel()
	waiting := make(chan error, 1)
	go func() {
		_, err := SendAndWait[structuredInventory](ctx, session, MessageOptions{Prompt: "inventory"})
		waiting <- err
	}()
	if _, err := readTestJSONRPCFrame(stdinR); err != nil {
		t.Fatal(err)
	}
	session.stopEventProcessing()
	select {
	case err := <-waiting:
		if err == nil || !strings.Contains(err.Error(), "session closed") {
			t.Fatalf("expected session closure while admission pending, got %v", err)
		}
	case <-ctx.Done():
		t.Fatal("session closure did not interrupt admission")
	}
}
