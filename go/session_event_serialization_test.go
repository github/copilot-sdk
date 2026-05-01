package copilot

import (
	"encoding/json"
	"testing"
)

func TestSessionEventAgentIDRoundTripsKnownEvent(t *testing.T) {
	event, err := UnmarshalSessionEvent([]byte(`{
		"id": "00000000-0000-0000-0000-000000000001",
		"timestamp": "2026-01-01T00:00:00Z",
		"parentId": null,
		"agentId": "agent-1",
		"type": "user.message",
		"data": {
			"content": "Hello"
		}
	}`))
	if err != nil {
		t.Fatalf("failed to unmarshal session event: %v", err)
	}

	if event.AgentID == nil || *event.AgentID != "agent-1" {
		t.Fatalf("expected agent ID to round-trip, got %v", event.AgentID)
	}
	if _, ok := event.Data.(*UserMessageData); !ok {
		t.Fatalf("expected user message data, got %T", event.Data)
	}

	data, err := event.Marshal()
	if err != nil {
		t.Fatalf("failed to marshal session event: %v", err)
	}

	var serialized map[string]any
	if err := json.Unmarshal(data, &serialized); err != nil {
		t.Fatalf("failed to unmarshal serialized session event: %v", err)
	}
	if serialized["agentId"] != "agent-1" {
		t.Fatalf("expected serialized agentId to round-trip, got %v", serialized["agentId"])
	}
}

func TestSessionEventAgentIDRoundTripsUnknownEvent(t *testing.T) {
	event, err := UnmarshalSessionEvent([]byte(`{
		"id": "00000000-0000-0000-0000-000000000002",
		"timestamp": "2026-01-01T00:00:00Z",
		"parentId": null,
		"agentId": "future-agent",
		"type": "future.feature_from_server",
		"data": {
			"key": "value"
		}
	}`))
	if err != nil {
		t.Fatalf("failed to unmarshal session event: %v", err)
	}

	if event.AgentID == nil || *event.AgentID != "future-agent" {
		t.Fatalf("expected agent ID to round-trip, got %v", event.AgentID)
	}
	if _, ok := event.Data.(*RawSessionEventData); !ok {
		t.Fatalf("expected raw session event data, got %T", event.Data)
	}

	data, err := event.Marshal()
	if err != nil {
		t.Fatalf("failed to marshal session event: %v", err)
	}

	var serialized map[string]any
	if err := json.Unmarshal(data, &serialized); err != nil {
		t.Fatalf("failed to unmarshal serialized session event: %v", err)
	}
	if serialized["agentId"] != "future-agent" {
		t.Fatalf("expected serialized agentId to round-trip, got %v", serialized["agentId"])
	}
}
