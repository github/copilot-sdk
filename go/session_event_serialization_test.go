package copilot

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

var _ rpc.SessionEvent = SessionEvent{}
var _ SessionEvent = rpc.SessionEvent{}
var _ rpc.SessionEventData = (*UserMessageData)(nil)
var _ SessionEventData = (*rpc.UserMessageData)(nil)
var _ rpc.EmbeddedTextResourceContents = EmbeddedTextResourceContents{}
var _ EmbeddedTextResourceContents = rpc.EmbeddedTextResourceContents{}

func TestSessionEventAutoTier(t *testing.T) {
	for _, eventType := range []string{"session.start", "session.resume"} {
		for _, tier := range []AutoTier{"", AutoTierEfficiency, AutoTierBalance, AutoTierIntelligence, AutoTierFast} {
			t.Run(eventType+"/"+string(tier), func(t *testing.T) {
				data := map[string]any{
					"sessionId": "test-session", "version": 1,
					"producer": "copilot", "copilotVersion": "1.0.82-1",
					"startTime":  "2026-08-28T00:00:00Z",
					"resumeTime": "2026-08-28T00:00:00Z", "eventCount": 1,
				}
				if tier != "" {
					data["autoTier"] = tier
				}
				wire, err := json.Marshal(map[string]any{
					"id":        "00000000-0000-0000-0000-000000000001",
					"timestamp": "2026-08-28T00:00:00Z", "parentId": nil,
					"type": eventType, "data": data,
				})
				if err != nil {
					t.Fatal(err)
				}
				var event SessionEvent
				if err := json.Unmarshal(wire, &event); err != nil {
					t.Fatal(err)
				}
				var actual *AutoTier
				switch eventType {
				case "session.start":
					actual = event.Data.(*SessionStartData).AutoTier
				case "session.resume":
					actual = event.Data.(*SessionResumeData).AutoTier
				}
				if tier == "" {
					if actual != nil {
						t.Fatalf("expected omitted autoTier, got %v", *actual)
					}
				} else if actual == nil || *actual != tier {
					t.Fatalf("expected autoTier %q, got %v", tier, actual)
				}
			})
		}
	}
}

func TestSessionEventAgentIDRoundTripsKnownEvent(t *testing.T) {
	var event SessionEvent
	if err := json.Unmarshal([]byte(`{
		"id": "00000000-0000-0000-0000-000000000001",
		"timestamp": "2026-01-01T00:00:00Z",
		"parentId": null,
		"agentId": "agent-1",
		"type": "user.message",
		"data": {
			"content": "Hello"
		}
	}`), &event); err != nil {
		t.Fatalf("failed to unmarshal session event: %v", err)
	}

	if event.AgentID == nil || *event.AgentID != "agent-1" {
		t.Fatalf("expected agent ID to round-trip, got %v", event.AgentID)
	}
	if _, ok := event.Data.(*UserMessageData); !ok {
		t.Fatalf("expected user message data, got %T", event.Data)
	}
	if event.Type() != SessionEventTypeUserMessage {
		t.Fatalf("expected user message type, got %q", event.Type())
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

func TestSessionEventTypeDerivedFromData(t *testing.T) {
	event := SessionEvent{
		Data: &UserMessageData{Content: "Hello"},
	}

	if event.Type() != SessionEventTypeUserMessage {
		t.Fatalf("expected user message type, got %q", event.Type())
	}

	data, err := event.Marshal()
	if err != nil {
		t.Fatalf("failed to marshal session event: %v", err)
	}

	var serialized map[string]any
	if err := json.Unmarshal(data, &serialized); err != nil {
		t.Fatalf("failed to unmarshal serialized session event: %v", err)
	}
	if serialized["type"] != string(SessionEventTypeUserMessage) {
		t.Fatalf("expected serialized type to be derived from data, got %v", serialized["type"])
	}
}

func TestSessionEventAgentIDRoundTripsUnknownEvent(t *testing.T) {
	var event SessionEvent
	if err := json.Unmarshal([]byte(`{
		"id": "00000000-0000-0000-0000-000000000002",
		"timestamp": "2026-01-01T00:00:00Z",
		"parentId": null,
		"agentId": "future-agent",
		"type": "future.feature_from_server",
		"data": {
			"key": "value"
		}
	}`), &event); err != nil {
		t.Fatalf("failed to unmarshal session event: %v", err)
	}

	if event.AgentID == nil || *event.AgentID != "future-agent" {
		t.Fatalf("expected agent ID to round-trip, got %v", event.AgentID)
	}
	rawData, ok := event.Data.(*RawSessionEventData)
	if !ok {
		t.Fatalf("expected raw session event data, got %T", event.Data)
	}
	if event.Type() != "future.feature_from_server" {
		t.Fatalf("expected unknown event type to be derived from raw event type, got %q", event.Type())
	}
	if rawData.EventType != "future.feature_from_server" {
		t.Fatalf("expected raw event type to round-trip, got %q", rawData.EventType)
	}
	if rawData.Type() != event.Type() {
		t.Fatalf("expected raw data type to match event type, got %q", rawData.Type())
	}
	var rawPayload map[string]any
	if err := json.Unmarshal(rawData.Raw, &rawPayload); err != nil {
		t.Fatalf("failed to unmarshal raw payload: %v", err)
	}
	if rawPayload["key"] != "value" {
		t.Fatalf("expected raw payload to preserve data, got %v", rawPayload)
	}
	if _, ok := rawPayload["type"]; ok {
		t.Fatalf("expected raw payload to exclude event type, got %v", rawPayload)
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
	if serialized["type"] != "future.feature_from_server" {
		t.Fatalf("expected serialized type to round-trip, got %v", serialized["type"])
	}
	serializedData, ok := serialized["data"].(map[string]any)
	if !ok {
		t.Fatalf("expected serialized data payload to be an object, got %T", serialized["data"])
	}
	if serializedData["key"] != "value" {
		t.Fatalf("expected serialized data payload to round-trip, got %v", serializedData)
	}
	if _, ok := serializedData["type"]; ok {
		t.Fatalf("expected serialized data to contain only the payload, got nested event object: %v", serializedData)
	}
}

func TestInternalSessionEventUsesRawFallback(t *testing.T) {
	var event SessionEvent
	if err := json.Unmarshal([]byte(`{
		"id": "00000000-0000-0000-0000-000000000003",
		"timestamp": "2026-01-01T00:00:00Z",
		"parentId": null,
		"type": "session.memory_changed",
		"data": {}
	}`), &event); err != nil {
		t.Fatalf("failed to unmarshal internal session event: %v", err)
	}

	if _, ok := event.Data.(*RawSessionEventData); !ok {
		t.Fatalf("expected internal event to use raw session event data, got %T", event.Data)
	}
	if event.Type() != "session.memory_changed" {
		t.Fatalf("expected internal event type to be preserved, got %q", event.Type())
	}
}

func TestRawSessionEventDataWithNilRawMarshalsAsNull(t *testing.T) {
	event := SessionEvent{
		Data: &RawSessionEventData{EventType: "future.event"},
	}

	data, err := event.Marshal()
	if err != nil {
		t.Fatalf("failed to marshal session event: %v", err)
	}
	if !json.Valid(data) {
		t.Fatalf("expected valid JSON, got %s", data)
	}

	var serialized map[string]any
	if err := json.Unmarshal(data, &serialized); err != nil {
		t.Fatalf("failed to unmarshal serialized session event: %v", err)
	}
	if serialized["type"] != "future.event" {
		t.Fatalf("expected serialized type to round-trip, got %v", serialized["type"])
	}
	if serialized["data"] != nil {
		t.Fatalf("expected missing raw data to marshal as null, got %v", serialized["data"])
	}
}

func TestManagedSettingsResolvedProvenanceRoundTrips(t *testing.T) {
	sources := []ManagedSettingsResolvedSource{
		ManagedSettingsResolvedSourceServer,
		ManagedSettingsResolvedSourceDevice,
		ManagedSettingsResolvedSourceClient,
		ManagedSettingsResolvedSourceMixed,
		ManagedSettingsResolvedSourceNone,
	}
	expectedSources := []string{"server", "device", "client", "mixed", "none"}
	for i, source := range sources {
		if string(source) != expectedSources[i] {
			t.Fatalf("expected source %q, got %q", expectedSources[i], source)
		}
	}

	clientManaged := true
	resolved := SessionManagedSettingsResolvedData{
		BypassPermissionsDisabled: true,
		ClientManaged:             &clientManaged,
		DeviceManaged:             false,
		FailClosed:                false,
		ManagedKeys:               []string{"permissions"},
		ServerManaged:             false,
		Source:                    ManagedSettingsResolvedSourceClient,
	}
	data, err := json.Marshal(resolved)
	if err != nil {
		t.Fatalf("failed to marshal managed settings resolution: %v", err)
	}

	var serialized map[string]any
	if err := json.Unmarshal(data, &serialized); err != nil {
		t.Fatalf("failed to inspect managed settings resolution: %v", err)
	}
	if serialized["source"] != "client" || serialized["clientManaged"] != true {
		t.Fatalf("expected client provenance, got %v", serialized)
	}

	var roundTripped SessionManagedSettingsResolvedData
	if err := json.Unmarshal(data, &roundTripped); err != nil {
		t.Fatalf("failed to round-trip managed settings resolution: %v", err)
	}
	if roundTripped.Source != ManagedSettingsResolvedSourceClient ||
		roundTripped.ClientManaged == nil ||
		!*roundTripped.ClientManaged {
		t.Fatalf("expected client provenance to round-trip, got %#v", roundTripped)
	}

	resolved.Source = ManagedSettingsResolvedSourceMixed
	resolved.ClientManaged = nil
	data, err = json.Marshal(resolved)
	if err != nil {
		t.Fatalf("failed to marshal mixed managed settings resolution: %v", err)
	}
	serialized = nil
	if err := json.Unmarshal(data, &serialized); err != nil {
		t.Fatalf("failed to inspect mixed managed settings resolution: %v", err)
	}
	if serialized["source"] != "mixed" {
		t.Fatalf("expected mixed provenance, got %v", serialized["source"])
	}
	if _, ok := serialized["clientManaged"]; ok {
		t.Fatalf("expected absent clientManaged to be omitted, got %v", serialized)
	}
}

func TestSessionRootUnionEventRoundTrips(t *testing.T) {
	cases := []struct {
		eventType SessionEventType
		payloads  []string
	}{
		{
			eventType: SessionEventTypeSessionIndexedSearch,
			payloads: []string{
				`{"kind":"status","state":"ready"}`,
				`{"kind":"startup","outcome":"failed","fileCount":0,"startupDurationMs":12.5,
				  "forcedByEnv":false,"warmStart":true,"disabledReason":"organization",
				  "errorMessage":"test startup diagnostic","eligible":false}`,
				`{"kind":"server_error","errorType":"unexpected_exit","exitCode":0,
				  "errorMessage":"test server diagnostic"}`,
				`{"kind":"incremental","phase":"updated","changedFileCount":0,"addedFileCount":2,
				  "deletedFileCount":1,"totalChangeCount":3,"walkDurationMs":0,
				  "updateDurationMs":1.25,"totalDurationMs":1.25}`,
				`{"kind":"future_variant","details":{"enabled":false,"count":0,"ratio":0.5}}`,
			},
		},
		{
			eventType: SessionEventTypeSandboxDecision,
			payloads: []string{
				`{"kind":"policy_resolved","control":"process","outcome":"resolved","platform":"linux",
				  "backend":"bubblewrap","policySource":"default_policy","enforcementPoint":"shell",
				  "readwritePathsCount":0,"readonlyPathsCount":1,"deniedPathsCount":0,
				  "addCurrentWorkingDirectory":true,"allowOutbound":false,"allowLocalNetwork":false,
				  "proxyMode":"none","allowBypass":false,"gitAuth":false,"ghAuth":false,"keychainAccess":false}`,
				`{"kind":"spawn_completed","control":"process","outcome":"succeeded","platform":"linux",
				  "backend":"bubblewrap","enforcementPoint":"shell","durationMs":1.5}`,
				`{"kind":"enforcement_state","control":"process","outcome":"engaged","platform":"linux",
				  "backend":"bubblewrap","enforcementPoint":"shell","command":null}`,
				`{"kind":"access_denied","control":"filesystem","outcome":"denied","platform":"linux",
				  "enforcementPoint":"builtin_filesystem","denialClass":"filesystem_read",
				  "attestation":"builtin_policy_checked","deniedResource":"/restricted/example"}`,
				`{"kind":"bypass_decided","control":"bypass","outcome":"declined","platform":"linux",
				  "enforcementPoint":"shell","source":"user_prompted","toolCallId":"tool-1"}`,
				`{"kind":"permissive_retry_decided","control":"process","outcome":"approved","platform":"linux",
				  "enforcementPoint":"shell","source":"model_requested"}`,
				`{"kind":"permissive_retry_completed","control":"process","outcome":"succeeded",
				  "platform":"linux","enforcementPoint":"shell"}`,
			},
		},
	}
	for _, test := range cases {
		for _, payload := range test.payloads {
			var expectedPayload map[string]any
			if err := json.Unmarshal([]byte(payload), &expectedPayload); err != nil {
				t.Fatal(err)
			}
			t.Run(string(test.eventType)+"/"+expectedPayload["kind"].(string), func(t *testing.T) {
				expected := map[string]any{
					"id":        "00000000-0000-0000-0000-000000000005",
					"timestamp": "2026-01-01T00:00:00Z",
					"parentId":  "00000000-0000-0000-0000-000000000004",
					"agentId":   "agent-1",
					"ephemeral": true,
					"type":      string(test.eventType),
					"data":      expectedPayload,
				}
				wire, err := json.Marshal(expected)
				if err != nil {
					t.Fatal(err)
				}
				var event SessionEvent
				if err := json.Unmarshal(wire, &event); err != nil {
					t.Fatal(err)
				}
				if event.Type() != test.eventType {
					t.Fatalf("expected event type %q, got %q", test.eventType, event.Type())
				}
				switch test.eventType {
				case SessionEventTypeSessionIndexedSearch:
					if _, ok := event.Data.(*SessionIndexedSearchData); !ok {
						t.Fatalf("expected indexed search data, got %T", event.Data)
					}
				case SessionEventTypeSandboxDecision:
					if _, ok := event.Data.(*SandboxDecisionData); !ok {
						t.Fatalf("expected sandbox decision data, got %T", event.Data)
					}
				}
				roundTripped, err := event.Marshal()
				if err != nil {
					t.Fatal(err)
				}
				var actual map[string]any
				if err := json.Unmarshal(roundTripped, &actual); err != nil {
					t.Fatal(err)
				}
				if !reflect.DeepEqual(actual, expected) {
					t.Fatalf("expected entire union event to round-trip:\nwant %v\ngot %v", expected, actual)
				}
			})
		}
	}
}

func TestSessionManagedSettingsResolvedEventRoundTrips(t *testing.T) {
	wire := []byte(`{
		"id": "00000000-0000-0000-0000-000000000004",
		"timestamp": "2026-01-01T00:00:00Z",
		"parentId": null,
		"ephemeral": true,
		"type": "session.managed_settings_resolved",
		"data": {
			"source": "client",
			"clientManaged": true,
			"serverManaged": false,
			"deviceManaged": false,
			"failClosed": true,
			"bypassPermissionsDisabled": true,
			"managedKeys": ["permissions"],
			"settings": {"permissions": {"deny": ["shell"]}}
		}
	}`)
	var event SessionEvent
	if err := json.Unmarshal(wire, &event); err != nil {
		t.Fatalf("failed to unmarshal managed settings event: %v", err)
	}
	resolved, ok := event.Data.(*SessionManagedSettingsResolvedData)
	if !ok {
		t.Fatalf("expected managed settings event payload, got %T", event.Data)
	}
	if resolved.Source != ManagedSettingsResolvedSourceClient ||
		resolved.ClientManaged == nil || !*resolved.ClientManaged || !resolved.FailClosed {
		t.Fatalf("expected managed settings provenance and fail-closed state, got %#v", resolved)
	}
	expectedSettings := map[string]any{
		"permissions": map[string]any{"deny": []any{"shell"}},
	}
	if !reflect.DeepEqual(resolved.Settings, expectedSettings) {
		t.Fatalf("expected effective settings %v, got %v", expectedSettings, resolved.Settings)
	}

	data, err := event.Marshal()
	if err != nil {
		t.Fatalf("failed to marshal managed settings event: %v", err)
	}
	var expected, actual map[string]any
	if err := json.Unmarshal(wire, &expected); err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(data, &actual); err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(actual, expected) {
		t.Fatalf("expected entire event to round-trip:\nwant %v\ngot %v", expected, actual)
	}
}

// The failure event is ephemeral: the runtime emits it when an Auto preference
// switch cannot mint a usable model, and never persists or replays it.
func TestSessionAutoTierSwitchFailedEvent(t *testing.T) {
	reasons := []AutoTierSwitchFailureReason{
		AutoTierSwitchFailureReasonPolicyRejected,
		AutoTierSwitchFailureReasonRequestFailed,
		AutoTierSwitchFailureReasonSetupFailed,
		AutoTierSwitchFailureReasonUnsupported,
	}
	for _, reason := range reasons {
		t.Run(string(reason), func(t *testing.T) {
			wire, err := json.Marshal(map[string]any{
				"id":        "00000000-0000-0000-0000-000000000001",
				"timestamp": "2026-08-28T00:00:00Z", "parentId": nil,
				"type": "session.auto_tier_switch_failed",
				"data": map[string]any{
					"effectiveAutoTier": AutoTierBalance,
					"requestedAutoTier": AutoTierFast,
					"reason":            reason,
				},
			})
			if err != nil {
				t.Fatal(err)
			}
			var event SessionEvent
			if err := json.Unmarshal(wire, &event); err != nil {
				t.Fatal(err)
			}
			data, ok := event.Data.(*SessionAutoTierSwitchFailedData)
			if !ok {
				t.Fatalf("expected *SessionAutoTierSwitchFailedData, got %T", event.Data)
			}
			if data.Reason != reason {
				t.Fatalf("expected reason %q, got %q", reason, data.Reason)
			}
			if data.EffectiveAutoTier == nil || *data.EffectiveAutoTier != AutoTierBalance {
				t.Fatalf("expected effective tier %q, got %v", AutoTierBalance, data.EffectiveAutoTier)
			}
			if data.RequestedAutoTier == nil || *data.RequestedAutoTier != AutoTierFast {
				t.Fatalf("expected requested tier %q, got %v", AutoTierFast, data.RequestedAutoTier)
			}
		})
	}
}

// A null requested tier means the attempt to return to provider-default Auto
// routing is what failed.
func TestSessionAutoTierSwitchFailedEventNullRequestedTier(t *testing.T) {
	wire := []byte(`{"id":"00000000-0000-0000-0000-000000000001","timestamp":"2026-08-28T00:00:00Z",` +
		`"parentId":null,"type":"session.auto_tier_switch_failed","data":{"effectiveAutoTier":"efficiency",` +
		`"requestedAutoTier":null,"reason":"unsupported"}}`)
	var event SessionEvent
	if err := json.Unmarshal(wire, &event); err != nil {
		t.Fatal(err)
	}
	data, ok := event.Data.(*SessionAutoTierSwitchFailedData)
	if !ok {
		t.Fatalf("expected *SessionAutoTierSwitchFailedData, got %T", event.Data)
	}
	if data.RequestedAutoTier != nil {
		t.Fatalf("expected nil requested tier, got %v", *data.RequestedAutoTier)
	}
	if data.EffectiveAutoTier == nil || *data.EffectiveAutoTier != AutoTierEfficiency {
		t.Fatalf("expected effective tier %q, got %v", AutoTierEfficiency, data.EffectiveAutoTier)
	}
}
