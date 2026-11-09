package rpc

import (
	"encoding/json"
	"fmt"
	"testing"
)

func TestProgrammaticToolCallEventRoundTrip(t *testing.T) {
	for _, eventType := range []SessionEventType{
		SessionEventTypePermissionRequested,
		SessionEventTypeToolExecutionStart,
		SessionEventTypeToolExecutionComplete,
	} {
		for _, programID := range []any{nil, "", "program-parent"} {
			t.Run(fmt.Sprintf("%s/%v", eventType, programID), func(t *testing.T) {
				data := map[string]any{
					"requestId": "permission-1",
					"permissionRequest": map[string]any{
						"kind": "read", "path": "/workspace/file.txt", "intention": "Read",
					},
					"toolCallId": "child-1", "toolName": "read_file", "success": true,
				}
				if programID != nil {
					data["programToolCallId"] = programID
				}
				wire, err := json.Marshal(map[string]any{
					"type": eventType, "data": data, "checkpoint": true,
					"id": "event-1", "timestamp": "2026-01-01T00:00:00Z", "parentId": nil,
				})
				if err != nil {
					t.Fatal(err)
				}
				var event SessionEvent
				if err := json.Unmarshal(wire, &event); err != nil {
					t.Fatal(err)
				}
				payload := event.Data
				var parentID *string
				switch eventType {
				case SessionEventTypePermissionRequested:
					d, ok := payload.(*PermissionRequestedData)
					if !ok || d.RequestID != "permission-1" {
						t.Fatalf("lost typed permission payload: %#v", payload)
					}
					parentID = d.ProgramToolCallID
				case SessionEventTypeToolExecutionStart:
					d, ok := payload.(*ToolExecutionStartData)
					if !ok || d.ToolCallID != "child-1" {
						t.Fatalf("lost typed start payload: %#v", payload)
					}
					parentID = d.ProgramToolCallID
				case SessionEventTypeToolExecutionComplete:
					d, ok := payload.(*ToolExecutionCompleteData)
					if !ok || !d.Success {
						t.Fatalf("lost typed completion payload: %#v", payload)
					}
					parentID = d.ProgramToolCallID
				}
				if programID == nil {
					if parentID != nil {
						t.Fatalf("ordinary event has program ownership: %#v", payload)
					}
				} else if parentID == nil || *parentID != programID {
					t.Fatalf("lost program ownership %q: %#v", programID, payload)
				}
				if event.Type() != eventType || event.Checkpoint == nil || !*event.Checkpoint {
					t.Fatalf("lost event type or checkpoint: %#v", event)
				}
				encoded, err := json.Marshal(event)
				if err != nil {
					t.Fatal(err)
				}
				var envelope struct {
					Data map[string]any `json:"data"`
				}
				if err := json.Unmarshal(encoded, &envelope); err != nil {
					t.Fatal(err)
				}
				parent, present := envelope.Data["programToolCallId"]
				if parent != programID || present != (programID != nil) {
					t.Fatalf("ownership did not round-trip: %s", encoded)
				}
			})
		}
	}
}

func TestProgrammaticToolCallIDDecodesEscapedProperty(t *testing.T) {
	var event SessionEvent
	if err := json.Unmarshal([]byte(`{"type":"tool.execution_start","data":{
		"toolCallId":"child","toolName":"read_file","program\u0054oolCallId":"parent"
	}}`), &event); err != nil {
		t.Fatal(err)
	}
	if data, ok := event.Data.(*ToolExecutionStartData); !ok || data.ProgramToolCallID == nil || *data.ProgramToolCallID != "parent" {
		t.Fatalf("escaped ownership was lost: %#v", event.Data)
	}
}

func TestProgrammaticToolCallIDRejectsInvalidType(t *testing.T) {
	var event SessionEvent
	if err := json.Unmarshal([]byte(`{"type":"tool.execution_start","data":{
		"toolCallId":"child","toolName":"read_file","programToolCallId":42
	}}`), &event); err == nil {
		t.Fatal("expected invalid program ownership to fail decoding")
	}
}
