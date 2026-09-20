// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"encoding/json"
	"fmt"
	"net"
	"sync/atomic"
	"testing"

	"github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestScenarioTestingControlStateE2E(t *testing.T) {
	t.Run("reports processing while scenario tool is running", func(t *testing.T) {
		fixture := newGeneratedRPCFixture(t, t.Context())
		fixture.server.SetRequestHandler("session.create", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return json.RawMessage(`{"sessionId":"processing-session"}`), nil
		})

		var processing atomic.Bool
		fixture.server.SetRequestHandler("session.metadata.isProcessing", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return mustJSON(t, map[string]any{"processing": processing.Load()}), nil
		})
		fixture.server.SetRequestHandler("session.metadata.activity", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			active := processing.Load()
			return mustJSON(t, map[string]any{
				"hasActiveWork": active,
				"abortable":     active,
			}), nil
		})

		toolStarted := make(chan struct{})
		releaseTool := make(chan struct{})
		toolCompleted := make(chan struct{})
		fixture.server.SetRequestHandler("session.tools.handlePendingToolCall", func(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			var params struct {
				RequestID string `json:"requestId"`
				Result    any    `json:"result"`
			}
			if err := json.Unmarshal(request, &params); err != nil {
				t.Errorf("Unmarshal tool result failed: %v", err)
			}
			if params.RequestID != "processing-request" || params.Result == nil {
				t.Errorf("Unexpected tool completion: %#v", params)
			}
			processing.Store(false)
			close(toolCompleted)
			return json.RawMessage(`{"success":true}`), nil
		})

		session, err := fixture.client.CreateSession(t.Context(), &copilot.SessionConfig{
			SessionID: "processing-session",
			Tools: []copilot.Tool{
				copilot.DefineTool("wait_for_scenario_control", "Waits for the scenario controller",
					func(_ struct{}, _ copilot.ToolInvocation) (string, error) {
						close(toolStarted)
						<-releaseTool
						return "SCENARIO_CONTROL_DONE", nil
					}),
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer session.Disconnect()

		assertProcessingState(t, session, false)

		processing.Store(true)
		writeScenarioNotification(t, fixture.conn, "session.event", map[string]any{
			"sessionId": session.SessionID,
			"event": scenarioEvent("tool-request", &copilot.ExternalToolRequestedData{
				Arguments:  map[string]any{},
				RequestID:  "processing-request",
				SessionID:  session.SessionID,
				ToolCallID: "processing-tool-call",
				ToolName:   "wait_for_scenario_control",
			}),
		})

		select {
		case <-toolStarted:
		case <-t.Context().Done():
			t.Fatal("Test context ended before tool handler started")
		}
		assertProcessingState(t, session, true)

		close(releaseTool)
		select {
		case <-toolCompleted:
		case <-t.Context().Done():
			t.Fatal("Test context ended before tool completion was handled")
		}
		assertProcessingState(t, session, false)
	})
}

func writeScenarioNotification(t *testing.T, conn net.Conn, method string, params any) {
	t.Helper()
	message, err := json.Marshal(map[string]any{
		"jsonrpc": "2.0",
		"method":  method,
		"params":  params,
	})
	if err != nil {
		t.Fatal(err)
	}
	frame := append([]byte(fmt.Sprintf("Content-Length: %d\r\n\r\n", len(message))), message...)
	if _, err := conn.Write(frame); err != nil {
		t.Fatal(err)
	}
}

func assertProcessingState(t *testing.T, session *copilot.Session, want bool) {
	t.Helper()
	state, err := session.RPC.Metadata.IsProcessing(t.Context())
	if err != nil {
		t.Fatalf("Metadata.IsProcessing failed: %v", err)
	}
	if state.Processing != want {
		t.Fatalf("Processing = %t, want %t", state.Processing, want)
	}
	activity, err := session.RPC.Metadata.Activity(t.Context())
	if err != nil {
		t.Fatalf("Metadata.Activity failed: %v", err)
	}
	if activity.HasActiveWork != want || activity.Abortable != want {
		t.Fatalf("Activity = %#v, want active/abortable %t", activity, want)
	}
}
