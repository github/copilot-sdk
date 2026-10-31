// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"testing"
)

func TestLifecycleDiagnosticEventRoundTrip(t *testing.T) {
	input := []byte(`{
		"type":"runtime.lifecycle.system_state",
		"timestamp":"2026-10-27T20:00:00Z",
		"data":{
			"sessionId":"session-1",
			"incarnationId":"incarnation-1",
			"system":"extensions",
			"systemRevision":1,
			"condition":"current",
			"changedOperation":{
				"operationId":"operation-1",
				"operationRevision":1,
				"trigger":"session_create",
				"phase":"loading",
				"step":"discover",
				"timing":{
					"startedAt":"2026-10-27T19:59:58Z",
					"phaseStartedAt":"2026-10-27T19:59:59Z",
					"transitionedAt":"2026-10-27T20:00:00Z"
				}
			},
			"activeOperations":[]
		}
	}`)

	var event LifecycleDiagnosticEvent
	if err := json.Unmarshal(input, &event); err != nil {
		t.Fatalf("unmarshal lifecycle diagnostic event: %v", err)
	}
	if event.Type() != LifecycleDiagnosticEventTypeRuntimeLifecycleSystemState {
		t.Fatalf("unexpected event type: %q", event.Type())
	}
	if _, ok := event.Data.(*RuntimeLifecycleSystemStateData); !ok {
		t.Fatalf("unexpected event data type: %T", event.Data)
	}

	output, err := json.Marshal(event)
	if err != nil {
		t.Fatalf("marshal lifecycle diagnostic event: %v", err)
	}
	var decoded map[string]any
	if err := json.Unmarshal(output, &decoded); err != nil {
		t.Fatalf("decode marshaled lifecycle diagnostic event: %v", err)
	}
	if decoded["type"] != "runtime.lifecycle.system_state" {
		t.Fatalf("marshaled type = %v", decoded["type"])
	}
	data, ok := decoded["data"].(map[string]any)
	if !ok || data["sessionId"] != "session-1" {
		t.Fatalf("marshaled data = %#v", decoded["data"])
	}
}
