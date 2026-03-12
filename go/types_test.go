package copilot

import (
	"encoding/json"
	"testing"
)

func TestPermissionRequestResultKind_Constants(t *testing.T) {
	tests := []struct {
		name     string
		kind     PermissionRequestResultKind
		expected string
	}{
		{"Approved", PermissionRequestResultKindApproved, "approved"},
		{"DeniedByRules", PermissionRequestResultKindDeniedByRules, "denied-by-rules"},
		{"DeniedCouldNotRequestFromUser", PermissionRequestResultKindDeniedCouldNotRequestFromUser, "denied-no-approval-rule-and-could-not-request-from-user"},
		{"DeniedInteractivelyByUser", PermissionRequestResultKindDeniedInteractivelyByUser, "denied-interactively-by-user"},
		{"NoResult", PermissionRequestResultKind("no-result"), "no-result"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if string(tt.kind) != tt.expected {
				t.Errorf("expected %q, got %q", tt.expected, string(tt.kind))
			}
		})
	}
}

func TestPermissionRequestResultKind_CustomValue(t *testing.T) {
	custom := PermissionRequestResultKind("custom-kind")
	if string(custom) != "custom-kind" {
		t.Errorf("expected %q, got %q", "custom-kind", string(custom))
	}
}

func TestPermissionRequestResult_JSONRoundTrip(t *testing.T) {
	tests := []struct {
		name string
		kind PermissionRequestResultKind
	}{
		{"Approved", PermissionRequestResultKindApproved},
		{"DeniedByRules", PermissionRequestResultKindDeniedByRules},
		{"DeniedCouldNotRequestFromUser", PermissionRequestResultKindDeniedCouldNotRequestFromUser},
		{"DeniedInteractivelyByUser", PermissionRequestResultKindDeniedInteractivelyByUser},
		{"NoResult", PermissionRequestResultKind("no-result")},
		{"Custom", PermissionRequestResultKind("custom")},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			original := PermissionRequestResult{Kind: tt.kind}
			data, err := json.Marshal(original)
			if err != nil {
				t.Fatalf("failed to marshal: %v", err)
			}

			var decoded PermissionRequestResult
			if err := json.Unmarshal(data, &decoded); err != nil {
				t.Fatalf("failed to unmarshal: %v", err)
			}

			if decoded.Kind != tt.kind {
				t.Errorf("expected kind %q, got %q", tt.kind, decoded.Kind)
			}
		})
	}
}

func TestPermissionRequestResult_JSONDeserialize(t *testing.T) {
	jsonStr := `{"kind":"denied-by-rules"}`
	var result PermissionRequestResult
	if err := json.Unmarshal([]byte(jsonStr), &result); err != nil {
		t.Fatalf("failed to unmarshal: %v", err)
	}

	if result.Kind != PermissionRequestResultKindDeniedByRules {
		t.Errorf("expected %q, got %q", PermissionRequestResultKindDeniedByRules, result.Kind)
	}
}

func TestPermissionRequestResult_JSONSerialize(t *testing.T) {
	result := PermissionRequestResult{Kind: PermissionRequestResultKindApproved}
	data, err := json.Marshal(result)
	if err != nil {
		t.Fatalf("failed to marshal: %v", err)
	}

	expected := `{"kind":"approved"}`
	if string(data) != expected {
		t.Errorf("expected %s, got %s", expected, string(data))
	}
}
