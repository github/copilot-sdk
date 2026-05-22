package copilot

import (
	"encoding/json"
	"testing"
)

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

func TestProviderConfig_JSONIncludesAllFields(t *testing.T) {
	cfg := ProviderConfig{
		BaseURL:         "https://example.com/provider",
		APIKey:          "test-key",
		Headers:         map[string]string{"Authorization": "Bearer provider-token"},
		ModelID:         "gpt-4o",
		WireModel:       "my-finetune-v3",
		MaxPromptTokens: 100000,
		MaxOutputTokens: 4096,
	}

	data, err := json.Marshal(cfg)
	if err != nil {
		t.Fatalf("failed to marshal ProviderConfig: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("failed to unmarshal ProviderConfig: %v", err)
	}

	if decoded["baseUrl"] != "https://example.com/provider" {
		t.Errorf("expected baseUrl to round-trip, got %v", decoded["baseUrl"])
	}
	if decoded["modelId"] != "gpt-4o" {
		t.Errorf("expected modelId 'gpt-4o', got %v", decoded["modelId"])
	}
	if decoded["wireModel"] != "my-finetune-v3" {
		t.Errorf("expected wireModel 'my-finetune-v3', got %v", decoded["wireModel"])
	}
	if decoded["maxPromptTokens"] != float64(100000) {
		t.Errorf("expected maxPromptTokens 100000, got %v", decoded["maxPromptTokens"])
	}
	if decoded["maxOutputTokens"] != float64(4096) {
		t.Errorf("expected maxOutputTokens 4096, got %v", decoded["maxOutputTokens"])
	}
	headers, ok := decoded["headers"].(map[string]any)
	if !ok {
		t.Fatalf("expected headers object, got %T", decoded["headers"])
	}
	if headers["Authorization"] != "Bearer provider-token" {
		t.Errorf("expected Authorization header, got %v", headers["Authorization"])
	}
}

func TestProviderConfig_JSONOmitsUnsetTokenFields(t *testing.T) {
	cfg := ProviderConfig{BaseURL: "https://example.com/provider"}

	data, err := json.Marshal(cfg)
	if err != nil {
		t.Fatalf("failed to marshal ProviderConfig: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("failed to unmarshal ProviderConfig: %v", err)
	}

	for _, field := range []string{"modelId", "wireModel", "maxPromptTokens", "maxOutputTokens", "headers"} {
		if _, present := decoded[field]; present {
			t.Errorf("expected %q to be omitted when unset, got %v", field, decoded[field])
		}
	}
}

func TestCustomAgentConfig_JSONIncludesModel(t *testing.T) {
	cfg := CustomAgentConfig{
		Name:   "model-agent",
		Prompt: "You are a model agent.",
		Model:  "claude-haiku-4.5",
	}

	data, err := json.Marshal(cfg)
	if err != nil {
		t.Fatalf("failed to marshal CustomAgentConfig: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("failed to unmarshal CustomAgentConfig: %v", err)
	}

	if decoded["model"] != "claude-haiku-4.5" {
		t.Errorf("expected model 'claude-haiku-4.5', got %v", decoded["model"])
	}
	if decoded["name"] != "model-agent" {
		t.Errorf("expected name 'model-agent', got %v", decoded["name"])
	}
}

func TestCustomAgentConfig_JSONOmitsModelWhenEmpty(t *testing.T) {
	cfg := CustomAgentConfig{
		Name:   "no-model-agent",
		Prompt: "You are an agent without a model.",
	}

	data, err := json.Marshal(cfg)
	if err != nil {
		t.Fatalf("failed to marshal CustomAgentConfig: %v", err)
	}

	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("failed to unmarshal CustomAgentConfig: %v", err)
	}

	if _, present := decoded["model"]; present {
		t.Errorf("expected model to be omitted when empty, got %v", decoded["model"])
	}
}
