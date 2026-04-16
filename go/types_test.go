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

func TestProviderConfig_JSONIncludesHeaders(t *testing.T) {
	config := ProviderConfig{
		BaseURL: "https://example.com/provider",
		Headers: map[string]string{"Authorization": "Bearer provider-token"},
	}

	data, err := json.Marshal(config)
	if err != nil {
		t.Fatalf("failed to marshal provider config: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("failed to unmarshal provider config: %v", err)
	}

	if decoded["baseUrl"] != "https://example.com/provider" {
		t.Fatalf("expected baseUrl to round-trip, got %v", decoded["baseUrl"])
	}
	headers, ok := decoded["headers"].(map[string]any)
	if !ok {
		t.Fatalf("expected headers object, got %T", decoded["headers"])
	}
	if headers["Authorization"] != "Bearer provider-token" {
		t.Fatalf("expected Authorization header, got %v", headers["Authorization"])
	}
}

func TestSessionSendRequest_JSONIncludesRequestHeaders(t *testing.T) {
	req := sessionSendRequest{
		SessionID:      "session-1",
		Prompt:         "hello",
		RequestHeaders: map[string]string{"Authorization": "Bearer turn-token"},
	}

	data, err := json.Marshal(req)
	if err != nil {
		t.Fatalf("failed to marshal session send request: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("failed to unmarshal session send request: %v", err)
	}

	if decoded["prompt"] != "hello" {
		t.Fatalf("expected prompt to round-trip, got %v", decoded["prompt"])
	}
	headers, ok := decoded["requestHeaders"].(map[string]any)
	if !ok {
		t.Fatalf("expected requestHeaders object, got %T", decoded["requestHeaders"])
	}
	if headers["Authorization"] != "Bearer turn-token" {
		t.Fatalf("expected Authorization header, got %v", headers["Authorization"])
	}
}
