// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot_test

import (
	"encoding/json"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	_ "github.com/github/copilot-sdk/go/testdata/auto_mode_recovery_legacy"
)

func TestAutoModeRecoveryLegacyEventTypes(t *testing.T) {
	wire := []byte(`{"id":"capabilities","timestamp":"2026-11-05T00:00:00Z","type":"capabilities.changed","data":{"ui":{"autoModeRecovery":true,"canvases":true,"elicitation":false,"mcpApps":true}}}`)
	var legacy copilot.SessionEvent
	if err := json.Unmarshal(wire, &legacy); err != nil {
		t.Fatal(err)
	}
	data, ok := legacy.Data.(*copilot.CapabilitiesChangedData)
	if !ok {
		t.Fatalf("legacy payload type = %T", legacy.Data)
	}
	if data.UI == nil || data.UI.Canvases == nil || !*data.UI.Canvases ||
		data.UI.Elicitation == nil || *data.UI.Elicitation ||
		data.UI.MCPApps == nil || !*data.UI.MCPApps {
		t.Fatalf("legacy capability values = %#v", data.UI)
	}
	var aware copilot.SessionEventWithAutoModeRecovery
	if err := json.Unmarshal(wire, &aware); err != nil {
		t.Fatal(err)
	}
	extended, ok := aware.Data.(*copilot.CapabilitiesChangedDataWithAutoModeRecovery)
	if !ok || extended.UI == nil || extended.UI.AutoModeRecovery == nil || !*extended.UI.AutoModeRecovery {
		t.Fatalf("recovery-aware payload = %#v", aware.Data)
	}
	projected := extended.Legacy()
	if projected.UI == nil || projected.UI.MCPApps == nil || !*projected.UI.MCPApps {
		t.Fatalf("legacy projection = %#v", projected)
	}
	roundTrip, err := json.Marshal(aware)
	if err != nil {
		t.Fatal(err)
	}
	var decoded map[string]any
	if err := json.Unmarshal(roundTrip, &decoded); err != nil {
		t.Fatal(err)
	}
	payload, ok := decoded["data"].(map[string]any)
	if !ok {
		t.Fatalf("missing data in %s", roundTrip)
	}
	ui, ok := payload["ui"].(map[string]any)
	if !ok || ui["autoModeRecovery"] != true {
		t.Fatalf("recovery acknowledgment lost in %s", roundTrip)
	}
}
