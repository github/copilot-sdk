package rpc

import (
	"encoding/json"
	"testing"
)

func TestSandboxConfigAllowBypassJSON(t *testing.T) {
	allowBypass := true
	configured := SandboxConfig{Enabled: true, AllowBypass: &allowBypass}

	data, err := json.Marshal(configured)
	if err != nil {
		t.Fatalf("marshal configured sandbox: %v", err)
	}
	var wire map[string]any
	if err := json.Unmarshal(data, &wire); err != nil {
		t.Fatalf("unmarshal configured sandbox: %v", err)
	}
	if got := wire["allowBypass"]; got != true {
		t.Fatalf("allowBypass = %v, want true", got)
	}

	var roundTripped SandboxConfig
	if err := json.Unmarshal(data, &roundTripped); err != nil {
		t.Fatalf("round-trip configured sandbox: %v", err)
	}
	if roundTripped.AllowBypass == nil || !*roundTripped.AllowBypass {
		t.Fatal("round-tripped allowBypass = nil or false, want true")
	}

	data, err = json.Marshal(SandboxConfig{Enabled: true})
	if err != nil {
		t.Fatalf("marshal sandbox without bypass: %v", err)
	}
	wire = make(map[string]any)
	if err := json.Unmarshal(data, &wire); err != nil {
		t.Fatalf("unmarshal sandbox without bypass: %v", err)
	}
	if _, ok := wire["allowBypass"]; ok {
		t.Fatal("allowBypass was serialized when absent")
	}
}
