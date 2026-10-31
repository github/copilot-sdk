package copilot

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

// dispatchAndCaptureReply dispatches event through the session's normal event
// path and returns the method and params of the first JSON-RPC request the
// session sends back.
func dispatchAndCaptureReply(t *testing.T, configure func(*Session), event SessionEvent) (string, map[string]json.RawMessage) {
	t.Helper()

	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	t.Cleanup(func() {
		stdinR.Close()
		stdinW.Close()
		stdoutR.Close()
		stdoutW.Close()
	})

	client := jsonrpc2.NewClient(stdinW, stdoutR)
	client.Start()
	t.Cleanup(client.Stop)

	session := &Session{
		SessionID:       "session-1",
		client:          client,
		RPC:             rpc.NewSessionRPC(client, "session-1"),
		handlers:        make([]*sessionHandler, 0),
		commandHandlers: make(map[string]CommandHandler),
		eventCh:         make(chan SessionEvent, 128),
		eventDone:       make(chan struct{}),
	}
	go session.processEvents()
	t.Cleanup(session.stopEventProcessing)
	configure(session)

	frameCh := make(chan []byte, 1)
	go func() {
		frame, err := readTestJSONRPCFrame(stdinR)
		if err != nil {
			return
		}
		frameCh <- frame
		var request struct {
			ID json.RawMessage `json:"id"`
		}
		_ = json.Unmarshal(frame, &request)
		response, _ := json.Marshal(map[string]any{
			"jsonrpc": "2.0",
			"id":      request.ID,
			"result":  map[string]any{"success": true},
		})
		_, _ = fmt.Fprintf(stdoutW, "Content-Length: %d\r\n\r\n%s", len(response), response)
	}()

	session.dispatchEvent(event)

	select {
	case frame := <-frameCh:
		var request struct {
			Method string                     `json:"method"`
			Params map[string]json.RawMessage `json:"params"`
		}
		if err := json.Unmarshal(frame, &request); err != nil {
			t.Fatalf("failed to unmarshal request frame: %v", err)
		}
		return request.Method, request.Params
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for the session to answer the request")
		return "", nil
	}
}

func TestElicitationHandlerFailureCancelsRequest(t *testing.T) {
	tests := []struct {
		name    string
		handler ElicitationHandler
	}{
		{
			name: "error",
			handler: func(ElicitationContext) (ElicitationResult, error) {
				return ElicitationResult{}, errors.New("form failed")
			},
		},
		{
			name: "panic",
			handler: func(ElicitationContext) (ElicitationResult, error) {
				panic("form handler bug")
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			method, params := dispatchAndCaptureReply(t, func(s *Session) {
				s.registerElicitationHandler(tt.handler)
			}, SessionEvent{Data: &ElicitationRequestedData{RequestID: "elicitation-1", Message: "Pick one"}})

			if method != "session.ui.handlePendingElicitation" {
				t.Fatalf("method = %q, want session.ui.handlePendingElicitation", method)
			}
			if got := string(params["requestId"]); got != `"elicitation-1"` {
				t.Errorf("requestId = %s, want \"elicitation-1\"", got)
			}
			var result struct {
				Action string `json:"action"`
			}
			if err := json.Unmarshal(params["result"], &result); err != nil {
				t.Fatalf("failed to unmarshal result: %v", err)
			}
			if result.Action != "cancel" {
				t.Errorf("action = %q, want cancel", result.Action)
			}
		})
	}
}

func TestCommandHandlerFailureReportsError(t *testing.T) {
	tests := []struct {
		name      string
		handler   CommandHandler
		wantError string
	}{
		{
			name: "error",
			handler: func(CommandContext) error {
				return errors.New("deploy failed")
			},
			wantError: "deploy failed",
		},
		{
			name: "panic",
			handler: func(CommandContext) error {
				panic("deploy handler bug")
			},
			wantError: "command panic: deploy handler bug",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			method, params := dispatchAndCaptureReply(t, func(s *Session) {
				s.registerCommands([]CommandDefinition{{Name: "deploy", Handler: tt.handler}})
			}, SessionEvent{Data: &CommandExecuteData{
				RequestID:   "command-1",
				CommandName: "deploy",
				Command:     "/deploy production",
				Args:        "production",
			}})

			if method != "session.commands.handlePendingCommand" {
				t.Fatalf("method = %q, want session.commands.handlePendingCommand", method)
			}
			if got := string(params["requestId"]); got != `"command-1"` {
				t.Errorf("requestId = %s, want \"command-1\"", got)
			}
			var errMsg string
			if err := json.Unmarshal(params["error"], &errMsg); err != nil {
				t.Fatalf("failed to unmarshal error: %v", err)
			}
			if errMsg != tt.wantError {
				t.Errorf("error = %q, want %q", errMsg, tt.wantError)
			}
		})
	}
}
