// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"testing"
)

func TestSessionEventsAsTrajectoryPreservesWireEvents(t *testing.T) {
	const input = `{"id":"event-1","parentId":null,"timestamp":"2026-01-01T00:00:00Z","type":"user.message","data":{"content":"Fix the build"}}`
	var event SessionEvent
	if err := json.Unmarshal([]byte(input), &event); err != nil {
		t.Fatalf("Unmarshal failed: %v", err)
	}

	request := TaskAnalyticsClassifyTrajectoryRequest{Events: SessionEventsAsTrajectory([]SessionEvent{event})}
	encoded, err := json.Marshal(request)
	if err != nil {
		t.Fatalf("Marshal failed: %v", err)
	}

	var wire struct {
		Events []struct {
			ID   string `json:"id"`
			Type string `json:"type"`
			Data struct {
				Content string `json:"content"`
			} `json:"data"`
		} `json:"events"`
	}
	if err := json.Unmarshal(encoded, &wire); err != nil {
		t.Fatalf("Unmarshal wire request failed: %v", err)
	}
	if len(wire.Events) != 1 || wire.Events[0].ID != "event-1" || wire.Events[0].Type != "user.message" || wire.Events[0].Data.Content != "Fix the build" {
		t.Fatalf("Unexpected wire events: %s", encoded)
	}
}
