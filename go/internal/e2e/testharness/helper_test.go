package testharness

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestFinalAssistantMessageWaiterBeforeCompletionRPC(t *testing.T) {
	for _, method := range []string{
		"session.send",
		"session.tools.handlePendingToolCall",
		"session.permissions.handlePendingPermissionRequest",
	} {
		t.Run(method, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
			defer cancel()
			assistant := copilot.SessionEvent{Data: &copilot.AssistantMessageData{MessageID: "answer", Content: "4"}}
			f := newCompletionFixture(t, ctx, []copilot.SessionEvent{assistant})
			waiter := SubscribeToFinalAssistantMessage(f.session)
			defer waiter.Close()
			f.server.SetRequestHandler(method, f.beforeResponse(t, ctx, []copilot.SessionEvent{
				{Data: &copilot.AssistantMessageData{MessageID: "intermediate", Content: "thinking"}},
				assistant,
				{Data: &copilot.SessionIdleData{}},
				{Data: &copilot.SessionIdleData{}},
				{Data: &copilot.SessionIdleData{}},
			}))

			switch method {
			case "session.send":
				messageID, err := f.session.Send(ctx, copilot.MessageOptions{Prompt: "What is 2+2?"})
				if err != nil || messageID != "sent" {
					t.Fatalf("Send = %q, %v; want sent, nil", messageID, err)
				}
			case "session.tools.handlePendingToolCall":
				result, err := f.session.RPC.Tools.HandlePendingToolCall(ctx, &rpc.HandlePendingToolCallRequest{
					RequestID: "tool-request", Result: rpc.ExternalToolStringResult("4"),
				})
				if err != nil || !result.Success {
					t.Fatalf("HandlePendingToolCall = %+v, %v", result, err)
				}
			case "session.permissions.handlePendingPermissionRequest":
				result, err := f.session.RPC.Permissions.HandlePendingPermissionRequest(ctx, &rpc.PermissionDecisionRequest{
					RequestID: "permission-request", Result: &rpc.PermissionDecisionApproveOnce{},
				})
				if err != nil || !result.Success {
					t.Fatalf("HandlePendingPermissionRequest = %+v, %v", result, err)
				}
			}

			// The RPC response is withheld until all notifications have been
			// processed. No goroutine has called Wait, and history contains no idle.
			history, err := f.session.GetEvents(ctx)
			if err != nil || len(history) != 1 || history[0].Type() != copilot.SessionEventTypeAssistantMessage {
				t.Fatalf("Expected durable assistant-only history, got %+v, %v", history, err)
			}
			answer, err := waiter.Wait(ctx)
			requireCompletionAnswer(t, answer, err, "4")

			existing, err := GetFinalAssistantMessageFromHistory(ctx, f.session)
			requireCompletionAnswer(t, existing, err, "4")

			// A new waiter must not mistake a previous turn's durable answer for
			// completion of a turn it never observed.
			next := SubscribeToFinalAssistantMessage(f.session)
			cancelled, cancelNext := context.WithCancel(ctx)
			cancelNext()
			if answer, err := next.Wait(cancelled); answer != nil || !errors.Is(err, context.Canceled) {
				t.Fatalf("Late waiter = %+v, %v; want cancellation", answer, err)
			}
		})
	}
}

func TestFinalAssistantMessageWaiterRequiresMessageAndPropagatesErrors(t *testing.T) {
	for _, tc := range []struct {
		name    string
		events  []copilot.SessionEvent
		wantErr string
	}{
		{
			name:    "idle without assistant",
			events:  []copilot.SessionEvent{{Data: &copilot.SessionIdleData{}}},
			wantErr: "session became idle without an assistant message",
		},
		{
			name: "session error before idle",
			events: []copilot.SessionEvent{
				{Data: &copilot.SessionErrorData{Message: "model failed"}},
				{Data: &copilot.SessionErrorData{Message: "another error"}},
				{Data: &copilot.SessionIdleData{}},
			},
			wantErr: "model failed",
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
			defer cancel()
			// Prior-turn messages and errors must not satisfy or fail this wait.
			f := newCompletionFixture(t, ctx, []copilot.SessionEvent{
				{Data: &copilot.UserMessageData{Content: "previous prompt"}},
				{Data: &copilot.AssistantMessageData{MessageID: "previous", Content: "previous answer"}},
				{Data: &copilot.SessionErrorData{Message: "previous error"}},
			})
			waiter := SubscribeToFinalAssistantMessage(f.session)
			defer waiter.Close()
			f.server.SetRequestHandler("session.send", f.beforeResponse(t, ctx, tc.events))
			if _, err := f.session.Send(ctx, copilot.MessageOptions{Prompt: "hello"}); err != nil {
				t.Fatal(err)
			}
			answer, err := waiter.Wait(ctx)
			if answer != nil || err == nil || err.Error() != tc.wantErr {
				t.Fatalf("Wait = %+v, %v; want %q", answer, err, tc.wantErr)
			}
		})
	}
}

func TestFinalAssistantMessageWaiterBeforeHandlerRelease(t *testing.T) {
	ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
	defer cancel()
	f := newCompletionFixture(t, ctx, nil)
	waiter := SubscribeToFinalAssistantMessage(f.session)
	defer waiter.Close()
	complete := f.beforeResponse(t, ctx, []copilot.SessionEvent{
		{Data: &copilot.AssistantMessageData{Content: "released"}},
		{Data: &copilot.SessionIdleData{}},
	})
	entered := make(chan struct{})
	release := make(chan struct{})
	f.server.SetRequestHandler("session.send", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		close(entered)
		select {
		case <-release:
			return complete(params)
		case <-ctx.Done():
			return nil, &jsonrpc2.Error{Code: -32000, Message: ctx.Err().Error()}
		}
	})
	sent := make(chan error, 1)
	go func() {
		_, err := f.session.Send(ctx, copilot.MessageOptions{Prompt: "use the blocked handler"})
		sent <- err
	}()
	select {
	case <-entered:
	case <-ctx.Done():
		t.Fatal(ctx.Err())
	}
	close(release)
	select {
	case err := <-sent:
		if err != nil {
			t.Fatal(err)
		}
	case <-ctx.Done():
		t.Fatal(ctx.Err())
	}
	answer, err := waiter.Wait(ctx)
	requireCompletionAnswer(t, answer, err, "released")
}

func TestFinalAssistantMessageWaiterRequiresLiveIdle(t *testing.T) {
	ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
	defer cancel()
	assistant := copilot.SessionEvent{Data: &copilot.AssistantMessageData{Content: "not finished"}}
	f := newCompletionFixture(t, ctx, []copilot.SessionEvent{assistant})
	waiter := SubscribeToFinalAssistantMessage(f.session)
	defer waiter.Close()
	f.server.SetRequestHandler("session.send", f.beforeResponse(t, ctx, []copilot.SessionEvent{assistant}))
	if _, err := f.session.Send(ctx, copilot.MessageOptions{Prompt: "hello"}); err != nil {
		t.Fatal(err)
	}
	cancelled, cancelWait := context.WithCancel(ctx)
	cancelWait()
	if answer, err := waiter.Wait(cancelled); answer != nil || !errors.Is(err, context.Canceled) {
		t.Fatalf("Wait without idle = %+v, %v; want cancellation", answer, err)
	}
}

func TestEventWaitersBeforeAbortAndRecovery(t *testing.T) {
	ctx, cancel := context.WithTimeout(t.Context(), 2*time.Second)
	defer cancel()
	f := newCompletionFixture(t, ctx, nil)
	toolStart := SubscribeToEvent(f.session, copilot.SessionEventTypeToolExecutionStart)
	defer toolStart.Close()
	idle := SubscribeToEvent(f.session, copilot.SessionEventTypeSessionIdle)
	defer idle.Close()
	f.server.SetRequestHandler("session.send", f.beforeResponse(t, ctx, []copilot.SessionEvent{
		{Data: &copilot.ToolExecutionStartData{ToolCallID: "tool", ToolName: "shell"}},
	}))
	if _, err := f.session.Send(ctx, copilot.MessageOptions{Prompt: "start tool"}); err != nil {
		t.Fatal(err)
	}
	if _, err := toolStart.Wait(ctx); err != nil {
		t.Fatal(err)
	}

	f.server.SetRequestHandler("session.abort", f.beforeResponse(t, ctx, []copilot.SessionEvent{
		{Data: &copilot.SessionIdleData{}},
	}))
	if err := f.session.Abort(ctx); err != nil {
		t.Fatal(err)
	}
	if _, err := idle.Wait(ctx); err != nil {
		t.Fatal(err)
	}

	answerWaiter := SubscribeToEvent(f.session, copilot.SessionEventTypeAssistantMessage)
	defer answerWaiter.Close()
	f.server.SetRequestHandler("session.send", f.beforeResponse(t, ctx, []copilot.SessionEvent{
		{Data: &copilot.AssistantMessageData{Content: "recovered"}},
		{Data: &copilot.SessionIdleData{}},
	}))
	if _, err := f.session.Send(ctx, copilot.MessageOptions{Prompt: "recover"}); err != nil {
		t.Fatal(err)
	}
	answer, err := answerWaiter.Wait(ctx)
	requireCompletionAnswer(t, answer, err, "recovered")
}

func requireCompletionAnswer(t *testing.T, event *copilot.SessionEvent, err error, content string) {
	t.Helper()
	if err != nil || event == nil {
		t.Fatalf("Expected assistant message, got %+v, %v", event, err)
	}
	if data, ok := event.Data.(*copilot.AssistantMessageData); !ok || data.Content != content {
		t.Fatalf("Expected assistant content %q, got %+v", content, event.Data)
	}
}

type completionFixture struct {
	session   *copilot.Session
	server    *jsonrpc2.Client
	conn      net.Conn
	nextFence int
}

// Uses the same minimal JSON-RPC server approach as the client unit tests, with
// a public TCP client so these tests exercise real session event dispatch.
func newCompletionFixture(t *testing.T, ctx context.Context, history []copilot.SessionEvent) *completionFixture {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = listener.Close() })
	historyResult, err := json.Marshal(map[string]any{"events": history})
	if err != nil {
		t.Fatal(err)
	}
	ready := make(chan *completionFixture, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		server := jsonrpc2.NewClient(conn, conn)
		t.Cleanup(server.Stop)
		for method, result := range map[string]string{
			"connect":                `{"ok":true,"protocolVersion":3,"version":"test"}`,
			"plugins.builtin.set":    `{}`,
			"session.create":         `{"sessionId":"completion-session"}`,
			"session.options.update": `{"success":true}`,
			"session.detach":         `{"success":true}`,
		} {
			server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				return []byte(result), nil
			})
		}
		server.SetRequestHandler("session.getMessages", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return historyResult, nil
		})
		server.Start()
		ready <- &completionFixture{server: server, conn: conn}
	}()
	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.URIConnection{URL: listener.Addr().String()},
	})
	t.Cleanup(func() { client.ForceStop() })
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		SessionID: "completion-session", OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatal(err)
	}
	select {
	case fixture := <-ready:
		fixture.session = session
		return fixture
	case <-ctx.Done():
		t.Fatal(ctx.Err())
		return nil
	}
}

// Called after the waiters are armed. A trailing notification acknowledges that
// every preceding event has run through the session's consumer, not just reached
// its queue. All RPCs in the fixture are sequential; notifications are written
// only inside the current request handler, before its response is written.
func (f *completionFixture) beforeResponse(t *testing.T, ctx context.Context, events []copilot.SessionEvent) jsonrpc2.RequestHandler {
	t.Helper()
	f.nextFence++
	fenceID := fmt.Sprintf("fence-%d", f.nextFence)
	delivered := make(chan struct{}, 1)
	unsubscribe := f.session.On(func(event copilot.SessionEvent) {
		if event.ID == fenceID {
			select {
			case delivered <- struct{}{}:
			default:
			}
		}
	})
	t.Cleanup(unsubscribe)
	var frames bytes.Buffer
	for _, event := range append(append([]copilot.SessionEvent(nil), events...), copilot.SessionEvent{
		ID: fenceID, Data: &copilot.SessionInfoData{Message: "delivery fence"},
	}) {
		if event.Type() == copilot.SessionEventTypeSessionIdle {
			event.Ephemeral = copilot.Bool(true)
		}
		data, err := json.Marshal(map[string]any{
			"jsonrpc": "2.0", "method": "session.event",
			"params": map[string]any{"sessionId": f.session.SessionID, "event": event},
		})
		if err != nil {
			t.Fatal(err)
		}
		fmt.Fprintf(&frames, "Content-Length: %d\r\n\r\n%s", len(data), data)
	}
	return func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		if _, err := f.conn.Write(frames.Bytes()); err != nil {
			return nil, &jsonrpc2.Error{Code: -32000, Message: err.Error()}
		}
		select {
		case <-delivered:
			return []byte(`{"messageId":"sent","success":true}`), nil
		case <-ctx.Done():
			return nil, &jsonrpc2.Error{Code: -32000, Message: ctx.Err().Error()}
		}
	}
}
