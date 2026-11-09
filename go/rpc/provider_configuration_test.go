// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"reflect"
	"testing"
)

func TestToSavedProviderConfiguration(t *testing.T) {
	for _, kind := range []string{"openai", "azure", "anthropic", "winml", ""} {
		t.Run(kind, func(t *testing.T) {
			settings := map[string]any{
				"name":          "discovered",
				"baseUrl":       "http://127.0.0.1:1234",
				"wireApi":       "responses",
				"transport":     "http",
				"azure":         map[string]any{"apiVersion": "2025-01-01"},
				"localModel":    map[string]any{"device": "gpu", "future": []any{float64(1), true}},
				"modelProvider": "azure_openai",
			}
			if kind != "" {
				settings["type"] = kind
			}
			input := map[string]any{
				"apiKey":                 "fixture-key",
				"bearerToken":            "fixture-token",
				"headers":                map[string]string{"Authorization": "fixture-header"},
				"hasBearerTokenProvider": true,
			}
			for key, value := range settings {
				input[key] = value
			}
			encoded, err := json.Marshal(input)
			if err != nil {
				t.Fatal(err)
			}
			var provider NamedProviderConfig
			if err := json.Unmarshal(encoded, &provider); err != nil {
				t.Fatal(err)
			}
			original, err := json.Marshal(provider)
			if err != nil {
				t.Fatal(err)
			}
			encoded, err = json.Marshal(ToSavedProviderConfiguration(provider))
			if err != nil {
				t.Fatal(err)
			}
			var saved map[string]any
			if err := json.Unmarshal(encoded, &saved); err != nil {
				t.Fatal(err)
			}
			if !reflect.DeepEqual(saved, settings) {
				t.Fatalf("saved settings = %s; want %#v", encoded, settings)
			}
			after, err := json.Marshal(provider)
			if err != nil {
				t.Fatal(err)
			}
			if string(after) != string(original) {
				t.Fatalf("source provider changed: %s; was %s", after, original)
			}
		})
	}
}

func TestToSavedProviderConfigurationOmitsAbsentSettings(t *testing.T) {
	saved := ToSavedProviderConfiguration(NamedProviderConfig{
		Name: "minimal", BaseURL: "http://127.0.0.1:1234",
	})
	encoded, err := json.Marshal(saved)
	if err != nil {
		t.Fatal(err)
	}
	var result map[string]any
	if err := json.Unmarshal(encoded, &result); err != nil {
		t.Fatal(err)
	}
	want := map[string]any{"name": "minimal", "baseUrl": "http://127.0.0.1:1234"}
	if !reflect.DeepEqual(result, want) {
		t.Fatalf("saved settings = %s; want %#v", encoded, want)
	}
}
