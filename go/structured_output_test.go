package copilot

import (
	"context"
	"fmt"
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

func TestWaitCorrelatesErrorsIndependentlyOfMessageEvents(t *testing.T) {
	for _, structured := range []bool{false, true} {
		for _, beforeAck := range []bool{false, true} {
			for _, source := range []string{"user", "assistant", "autopilot", "system", "session-memory-rollover", "marked", "pre-message", "steering"} {
				t.Run(fmt.Sprintf("%s/structured=%t/preAck=%t", source, structured, beforeAck), func(t *testing.T) {
					next := SessionEvent{Data: &UserMessageData{
						MessageID: ptr("other"), Content: "other", Source: ptr(source),
						IsAutopilotContinuation: ptr(source == "marked"),
					}}
					switch source {
					case "assistant":
						next = structuredMessage("other", "other")
					case "steering", "pre-message":
						next.Data.(*UserMessageData).Source = ptr("sdk")
					}
					startID, errorID := "message-1", "message-1"
					if source == "pre-message" {
						startID = "other"
					}
					if source == "user" || source == "assistant" {
						errorID = "other"
					}
					events := []SessionEvent{
						structuredMessage(`{"ok":true}`, startID),
						next,
						{Data: &SessionErrorData{Message: "provider failed", OriginatingMessageID: ptr(errorID)}},
						{Data: &SessionIdleData{}},
					}
					captureSessionSendRequest(t, nil, events, beforeAck, func(session *Session) {
						options := MessageOptions{Prompt: "waited"}
						if structured {
							options.ResponseSchema = map[string]any{"type": "object"}
						}
						ctx, cancel := context.WithTimeout(t.Context(), time.Second)
						defer cancel()
						result, err := session.SendAndWait(ctx, options)
						if source == "user" || source == "assistant" {
							if err != nil || result == nil || result.Data.(*AssistantMessageData).Content != `{"ok":true}` {
								t.Fatalf("foreign error affected wait: %+v, %v", result, err)
							}
						} else if err == nil || !strings.Contains(err.Error(), "provider failed") {
							t.Fatalf("own error was lost: %v", err)
						}
					})
				})
			}
		}
	}
}

func TestAutopilotTerminalCompletesCorrelatedWaitWithoutIdle(t *testing.T) {
	for _, structured := range []bool{false, true} {
		for _, beforeResponse := range []bool{false, true} {
			t.Run(fmt.Sprintf("structured=%t/preAck=%t", structured, beforeResponse), func(t *testing.T) {
				childStop := SessionEvent{AgentID: ptr("child"), Data: &SessionAutopilotStoppedData{
					Reason: "task_complete", RequestID: ptr("message-1"),
				}}
				final := structuredMessage(`{"count":7,"color":"blue"}`, "message-1")
				events := []SessionEvent{
					structuredMessage(`{"count":42,"color":"red"}`, "message-1"),
					{Data: &UserMessageData{
						Content: "Continue", MessageID: ptr("other"),
						Source: ptr("autopilot"), IsAutopilotContinuation: ptr(true),
					}},
					{Data: &SessionAutopilotStoppedData{Reason: "task_complete", RequestID: ptr("other")}},
					{Data: &SessionAutopilotStoppedData{Reason: "task_complete"}},
					childStop,
					{Data: &SessionIdleData{Mode: ptr(SessionModeAutopilot)}},
					final,
					{Data: &SessionAutopilotStoppedData{Reason: "task_complete", RequestID: ptr("message-1")}},
				}
				captureSessionSendRequest(t, nil, events, beforeResponse, func(session *Session) {
					ctx, cancel := context.WithTimeout(t.Context(), time.Second)
					defer cancel()
					options := MessageOptions{Prompt: "inventory"}
					if structured {
						options.ResponseSchema = map[string]any{"type": "object"}
					}
					result, err := session.SendAndWait(ctx, options)
					if err != nil || result == nil || result.Data.(*AssistantMessageData).Content != final.Data.(*AssistantMessageData).Content {
						t.Fatalf("expected correlated final without idle, got %+v, %v", result, err)
					}
				})
			})
		}
	}
}

func TestAutopilotTerminalErrorWithoutSessionError(t *testing.T) {
	for _, structured := range []bool{false, true} {
		t.Run(fmt.Sprintf("structured=%t", structured), func(t *testing.T) {
			events := []SessionEvent{{Data: &SessionAutopilotStoppedData{Reason: "error", RequestID: ptr("message-1")}}}
			captureSessionSendRequest(t, nil, events, true, func(session *Session) {
				ctx, cancel := context.WithTimeout(t.Context(), time.Second)
				defer cancel()
				options := MessageOptions{Prompt: "fail"}
				if structured {
					options.ResponseSchema = map[string]any{"type": "object"}
				}
				_, err := session.SendAndWait(ctx, options)
				if err == nil || !strings.Contains(err.Error(), "autopilot request failed") {
					t.Fatalf("expected terminal failure, got %v", err)
				}
			})
		})
	}
}

func TestAutopilotTerminalAbortIsAnErrorWithoutIdle(t *testing.T) {
	for _, structured := range []bool{false, true} {
		t.Run(fmt.Sprintf("structured=%t", structured), func(t *testing.T) {
			events := []SessionEvent{
				structuredMessage(`{"count":7,"color":"blue"}`, "message-1"),
				{Data: &SessionAutopilotStoppedData{Reason: "aborted", RequestID: ptr("message-1")}},
			}
			captureSessionSendRequest(t, nil, events, true, func(session *Session) {
				ctx, cancel := context.WithTimeout(t.Context(), time.Second)
				defer cancel()
				options := MessageOptions{Prompt: "cancel"}
				if structured {
					options.ResponseSchema = map[string]any{"type": "object"}
				}
				result, err := session.SendAndWait(ctx, options)
				if err == nil || !strings.Contains(strings.ToLower(err.Error()), "aborted") {
					t.Fatalf("expected aborted terminal failure, got %v", err)
				}
				if result != nil {
					t.Fatalf("aborted request returned partial response: %+v", result)
				}
			})
		})
	}
}

func TestAbortedIdleIsAnError(t *testing.T) {
	for _, structured := range []bool{false, true} {
		t.Run(fmt.Sprintf("structured=%t", structured), func(t *testing.T) {
			events := []SessionEvent{
				{Data: &UserMessageData{MessageID: ptr("message-1")}},
				structuredMessage(`{"count":7,"color":"blue"}`, "message-1"),
				{Data: &SessionIdleData{Aborted: ptr(true)}},
			}
			captureSessionSendRequest(t, nil, events, true, func(session *Session) {
				ctx, cancel := context.WithTimeout(t.Context(), time.Second)
				defer cancel()
				options := MessageOptions{Prompt: "cancel"}
				if structured {
					options.ResponseSchema = map[string]any{"type": "object"}
				}
				result, err := session.SendAndWait(ctx, options)
				if err == nil || !strings.Contains(strings.ToLower(err.Error()), "aborted") {
					t.Fatalf("expected aborted idle failure, got %v", err)
				}
				if result != nil {
					t.Fatalf("aborted request returned partial response: %+v", result)
				}
			})
		})
	}
}

func TestSendAndWaitIgnoresStaleAbortedIdle(t *testing.T) {
	events := []SessionEvent{
		{Data: &SessionIdleData{Aborted: ptr(true)}},
		{Data: &AssistantMessageData{MessageID: "stale-assistant", Content: "stale"}},
		{Data: &SessionIdleData{}},
		{Data: &UserMessageData{MessageID: ptr("message-1")}},
		{Data: &SessionIdleData{}},
	}
	captureMessageSourceRequest(t, nil, events, func(session *Session) {
		result, err := session.SendAndWait(t.Context(), MessageOptions{Prompt: "queued"})
		if err != nil {
			t.Fatalf("stale aborted idle ended the wait: %v", err)
		}
		if result != nil {
			t.Fatalf("expected no assistant message, got %+v", result)
		}
	})
}

func TestSendAndWaitIgnoresEarlierSuccessfulIdle(t *testing.T) {
	for _, beforeResponse := range []bool{false, true} {
		t.Run(fmt.Sprintf("preAck=%t", beforeResponse), func(t *testing.T) {
			events := []SessionEvent{
				structuredMessage("previous", "other"),
				{Data: &SessionIdleData{}},
				structuredMessage("current", "message-1"),
				{Data: &SessionIdleData{}},
			}
			captureSessionSendRequest(t, nil, events, beforeResponse, func(session *Session) {
				result, err := session.SendAndWait(t.Context(), MessageOptions{Prompt: "queued"})
				if err != nil || result == nil || result.Data.(*AssistantMessageData).Content != "current" {
					t.Fatalf("earlier idle ended the current request: %+v, %v", result, err)
				}
			})
		})
	}
}

func TestSendAndWaitCorrelatesPolicyBlockedPrompts(t *testing.T) {
	for _, aborted := range []bool{false, true} {
		for _, beforeResponse := range []bool{false, true} {
			t.Run(fmt.Sprintf("aborted=%t/preAck=%t", aborted, beforeResponse), func(t *testing.T) {
				events := []SessionEvent{
					{Data: &SessionWarningData{WarningType: "policy_blocked", OriginatingMessageID: ptr("other")}},
					{AgentID: ptr("child"), Data: &SessionWarningData{WarningType: "policy_blocked", OriginatingMessageID: ptr("message-1")}},
					{Data: &SessionIdleData{}},
					{Data: &SessionWarningData{WarningType: "policy_blocked", OriginatingMessageID: ptr("message-1")}},
					{Data: &SessionIdleData{Aborted: ptr(aborted)}},
				}
				captureSessionSendRequest(t, nil, events, beforeResponse, func(session *Session) {
					result, err := session.SendAndWait(t.Context(), MessageOptions{Prompt: "blocked"})
					if aborted {
						if err == nil || !strings.Contains(err.Error(), "aborted") {
							t.Fatalf("expected correlated abort, got %+v, %v", result, err)
						}
					} else if err != nil || result != nil {
						t.Fatalf("blocked prompt should return no message: %+v, %v", result, err)
					}
				})
			})
		}
	}
}

func TestSendAndWaitCompletesPolicyBlockedPromptOnCorrelatedWarning(t *testing.T) {
	stub, server := newSetToolsTestSession(t)
	session := newSession("session-1", stub.client, "", false)
	t.Cleanup(session.stopEventProcessing)
	ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
	defer cancel()

	type outcome struct {
		event *SessionEvent
		err   error
	}
	resultCh := make(chan outcome, 1)
	go func() {
		event, err := session.SendAndWait(ctx, MessageOptions{Prompt: "blocked"})
		resultCh <- outcome{event: event, err: err}
	}()
	request := server.expectRequest(t, "session.send")
	server.respond(t, request.ID, map[string]any{"messageId": "message-1"})

	idleProcessed := make(chan struct{}, 2)
	unsubscribe := session.On(func(event SessionEvent) {
		if _, ok := event.Data.(*SessionIdleData); ok {
			idleProcessed <- struct{}{}
		}
	})
	defer unsubscribe()

	session.dispatchEvent(SessionEvent{Data: &SessionWarningData{
		Message:              "blocked",
		WarningType:          "policy_blocked",
		OriginatingMessageID: ptr("other"),
	}})
	session.dispatchEvent(SessionEvent{Data: &SessionIdleData{}})
	select {
	case <-idleProcessed:
	case <-ctx.Done():
		t.Fatal("timed out waiting for the unrelated idle to be dispatched")
	}
	select {
	case result := <-resultCh:
		t.Fatalf("unrelated warning and idle ended the wait: %+v, %v", result.event, result.err)
	default:
	}

	session.dispatchEvent(SessionEvent{Data: &SessionWarningData{
		Message:              "blocked",
		WarningType:          "policy_blocked",
		OriginatingMessageID: ptr("message-1"),
	}})
	session.dispatchEvent(SessionEvent{Data: &SessionIdleData{}})
	select {
	case <-idleProcessed:
	case <-ctx.Done():
		t.Fatal("timed out waiting for the blocked request idle to be dispatched")
	}
	select {
	case result := <-resultCh:
		if result.err != nil {
			t.Fatalf("SendAndWait returned an error: %v", result.err)
		}
		if result.event != nil {
			t.Fatalf("expected no assistant message, got %+v", result.event)
		}
	case <-ctx.Done():
		t.Fatal("SendAndWait did not complete after the correlated policy warning")
	}
}

func TestAutopilotTerminalsIsolateConcurrentWaits(t *testing.T) {
	for _, structured := range []bool{false, true} {
		t.Run(fmt.Sprintf("structured=%t", structured), func(t *testing.T) {
			stub, server := newSetToolsTestSession(t)
			session := newSession("session-1", stub.client, "", false)
			t.Cleanup(session.stopEventProcessing)
			ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
			defer cancel()
			type outcome struct {
				event *SessionEvent
				err   error
			}
			results := make([]chan outcome, 0, 2)
			for _, id := range []string{"first", "second"} {
				result := make(chan outcome, 1)
				results = append(results, result)
				go func() {
					options := MessageOptions{Prompt: id}
					if structured {
						options.ResponseSchema = map[string]any{"type": "object"}
					}
					event, err := session.SendAndWait(ctx, options)
					result <- outcome{event, err}
				}()
				request := server.expectRequest(t, "session.send")
				server.respond(t, request.ID, map[string]any{"messageId": id})
			}
			for _, id := range []string{"first", "second"} {
				session.dispatchEvent(structuredMessage(id, id))
				session.dispatchEvent(SessionEvent{Data: &SessionAutopilotStoppedData{Reason: "task_complete", RequestID: ptr(id)}})
			}
			for i, id := range []string{"first", "second"} {
				select {
				case result := <-results[i]:
					if result.err != nil || result.event == nil || result.event.Data.(*AssistantMessageData).Content != id {
						t.Fatalf("request %s got %+v, %v", id, result.event, result.err)
					}
				case <-ctx.Done():
					t.Fatalf("request %s did not complete", id)
				}
			}
		})
	}
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
		{"aborted", []SessionEvent{{Data: &UserMessageData{MessageID: ptr("message-1")}}, structuredMessage(`{"count":1}`, "message-1"), {Data: &SessionIdleData{Aborted: ptr(true)}}}, nil, "aborted"},
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
