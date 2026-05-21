package copilot

import (
	"encoding/json"
	"testing"
)

// Real captures from the upstream Copilot CLI 1.0.51 at the time the
// timestamp-shape bug was reported (https://github.com/github/copilot-sdk/issues/1356):
//
//   - Windows CLI 1.0.51-2  -> JSON number: 1779352370134
//   - Linux   CLI 1.0.51    -> JSON string: "2026-05-21T08:29:54.042Z"
//
// The UnmarshalJSON added to PingResponse must accept both wire shapes,
// plus the stringified-epoch variant some legacy builds emit, while
// continuing to reject genuinely garbage values.

func TestPingResponse_UnmarshalJSON_NumericTimestamp(t *testing.T) {
	const raw = `{"message":"pong","timestamp":1779352370134,"protocolVersion":3}`
	var resp PingResponse
	if err := json.Unmarshal([]byte(raw), &resp); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if resp.Timestamp != 1779352370134 {
		t.Fatalf("Timestamp got %d, want 1779352370134", resp.Timestamp)
	}
	if resp.Message != "pong" {
		t.Fatalf("Message got %q, want pong", resp.Message)
	}
	if resp.ProtocolVersion == nil || *resp.ProtocolVersion != 3 {
		t.Fatalf("ProtocolVersion got %v, want 3", resp.ProtocolVersion)
	}
}

func TestPingResponse_UnmarshalJSON_ISO8601Timestamp(t *testing.T) {
	const raw = `{"message":"pong","timestamp":"2026-05-21T08:29:54.042Z","protocolVersion":3}`
	var resp PingResponse
	if err := json.Unmarshal([]byte(raw), &resp); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	// 2026-05-21T08:29:54.042Z == 1779352194042 ms since epoch.
	if resp.Timestamp != 1779352194042 {
		t.Fatalf("Timestamp got %d, want 1779352194042", resp.Timestamp)
	}
	if resp.Message != "pong" {
		t.Fatalf("Message got %q, want pong", resp.Message)
	}
	if resp.ProtocolVersion == nil || *resp.ProtocolVersion != 3 {
		t.Fatalf("ProtocolVersion got %v, want 3", resp.ProtocolVersion)
	}
}

func TestPingResponse_UnmarshalJSON_StringifiedEpoch(t *testing.T) {
	const raw = `{"message":"pong","timestamp":"1779352370134","protocolVersion":3}`
	var resp PingResponse
	if err := json.Unmarshal([]byte(raw), &resp); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if resp.Timestamp != 1779352370134 {
		t.Fatalf("Timestamp got %d, want 1779352370134", resp.Timestamp)
	}
}

func TestPingResponse_UnmarshalJSON_NullTimestamp(t *testing.T) {
	const raw = `{"message":"pong","timestamp":null,"protocolVersion":3}`
	var resp PingResponse
	if err := json.Unmarshal([]byte(raw), &resp); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if resp.Timestamp != 0 {
		t.Fatalf("Timestamp got %d, want 0", resp.Timestamp)
	}
}

func TestPingResponse_UnmarshalJSON_RejectsGarbageString(t *testing.T) {
	const raw = `{"message":"pong","timestamp":"not-a-date","protocolVersion":3}`
	var resp PingResponse
	if err := json.Unmarshal([]byte(raw), &resp); err == nil {
		t.Fatalf("expected error for garbage timestamp, got %+v", resp)
	}
}

func TestPingResponse_UnmarshalJSON_RejectsObject(t *testing.T) {
	const raw = `{"message":"pong","timestamp":{"oops":true},"protocolVersion":3}`
	var resp PingResponse
	if err := json.Unmarshal([]byte(raw), &resp); err == nil {
		t.Fatalf("expected error for object timestamp, got %+v", resp)
	}
}
