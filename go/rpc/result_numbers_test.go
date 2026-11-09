// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc_test

import (
	"bytes"
	"encoding/json"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestAzureMLCompensationPreservesOpaqueNumbers(t *testing.T) {
	input := []byte(`{"before":{"providerName":"provider","modelId":"model","provider":{"revision":9007199254740993},"model":{"metadata":{"revision":9007199254740993,"nested":[null,true,9007199254740995]}}}}`)
	var result rpc.AzureMlRegistrationResult
	if err := json.Unmarshal(input, &result); err != nil {
		t.Fatal(err)
	}
	encoded, err := json.Marshal(rpc.AzureMlRegistrationRequest{
		Before: result.Before, LeaseID: "lease", Name: "family",
		Operation: "restore", Registry: "azureml",
	})
	if err != nil {
		t.Fatal(err)
	}
	decode := func(data []byte) map[string]any {
		t.Helper()
		decoder := json.NewDecoder(bytes.NewReader(data))
		decoder.UseNumber()
		var value map[string]any
		if err := decoder.Decode(&value); err != nil {
			t.Fatal(err)
		}
		return value
	}
	want, got := decode(input)["before"], decode(encoded)["before"]
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("Compensation rows changed:\ngot: %#v\nwant: %#v\nwire: %s", got, want, encoded)
	}
}

func TestLocalModelDescriptorsPreserveOpaqueNumbers(t *testing.T) {
	input := []byte(`{"metadata":{"revision":9007199254740993,"nested":[null,true,9007199254740995]},"serveOptions":{"seed":9007199254740993,"nested":{"revision":9007199254740995}}}`)
	var descriptor rpc.LocalModelDescriptor
	var prepared rpc.LocalModelsPrepareResult
	decode := func(data []byte) map[string]any {
		t.Helper()
		decoder := json.NewDecoder(bytes.NewReader(data))
		decoder.UseNumber()
		var value map[string]any
		if err := decoder.Decode(&value); err != nil {
			t.Fatal(err)
		}
		return value
	}
	want := decode(input)
	for _, test := range []struct {
		name   string
		result any
	}{
		{"snapshot descriptor", &descriptor},
		{"prepare result", &prepared},
	} {
		t.Run(test.name, func(t *testing.T) {
			if err := json.Unmarshal(input, test.result); err != nil {
				t.Fatal(err)
			}
			encoded, err := json.Marshal(test.result)
			if err != nil {
				t.Fatal(err)
			}
			got := decode(encoded)
			for _, field := range []string{"metadata", "serveOptions"} {
				if !reflect.DeepEqual(got[field], want[field]) {
					t.Errorf("%s changed:\ngot: %#v\nwant: %#v\nwire: %s", field, got[field], want[field], encoded)
				}
			}
		})
	}
}

func TestSavedProviderConfigurationPreservesLocalModelNumbers(t *testing.T) {
	var request rpc.SavedProviderConfigurationAddModelRequest
	if err := json.Unmarshal([]byte(`{"provider":{"name":"local","baseUrl":"model.gguf","localModel":{"seed":9007199254740993,"nested":[9007199254740995]}},"model":{"id":"model","provider":"local"}}`), &request); err != nil {
		t.Fatal(err)
	}
	encoded, err := json.Marshal(request)
	if err != nil {
		t.Fatal(err)
	}
	for _, exact := range []string{"9007199254740993", "9007199254740995"} {
		if !bytes.Contains(encoded, []byte(exact)) {
			t.Errorf("local-model number %s changed in save request: %s", exact, encoded)
		}
	}
}

func TestFeatureFlagAssignmentSnapshotPreservesNumbers(t *testing.T) {
	const response = `{"ParameterGroups":[{"Parameters":{"nullable":null,"opaque":9007199254740993}}]}`
	const snapshotJSON = `{"response":` + response + `,"expFlags":{}}`
	for _, name := range []string{"configure result", "update result", "snapshot", "read snapshot"} {
		t.Run(name, func(t *testing.T) {
			var assignmentResponse any
			switch name {
			case "configure result":
				var result rpc.SessionFeatureFlagsConfigureResult
				if err := json.Unmarshal([]byte(snapshotJSON), &result); err != nil {
					t.Fatal(err)
				}
				assignmentResponse = result.Response
			case "update result":
				var result rpc.SessionFeatureFlagsUpdateResult
				if err := json.Unmarshal([]byte(snapshotJSON), &result); err != nil {
					t.Fatal(err)
				}
				assignmentResponse = result.Response
			case "read snapshot":
				var result rpc.FeatureFlagServiceReadResult
				if err := json.Unmarshal([]byte(`{"snapshot":`+snapshotJSON+`}`), &result); err != nil {
					t.Fatal(err)
				}
				assignmentResponse = result.Snapshot.Response
			case "snapshot":
				var snapshot rpc.FeatureFlagServiceSnapshot
				if err := json.Unmarshal([]byte(snapshotJSON), &snapshot); err != nil {
					t.Fatal(err)
				}
				assignmentResponse = snapshot.Response
			}
			update := rpc.FeatureFlagServiceUpdateParams{
				Assignments: &rpc.FeatureFlagAssignmentsUpdate{Response: &assignmentResponse},
			}
			forwarded, err := json.Marshal(update)
			if err != nil {
				t.Fatal(err)
			}
			var decoded struct {
				Assignments struct {
					Response json.RawMessage `json:"response"`
				} `json:"assignments"`
			}
			if err := json.Unmarshal(forwarded, &decoded); err != nil {
				t.Fatal(err)
			}
			var compact bytes.Buffer
			if err := json.Compact(&compact, decoded.Assignments.Response); err != nil {
				t.Fatal(err)
			}
			if compact.String() != response {
				t.Fatalf("assignment document changed:\ngot:  %s\nwant: %s", compact.String(), response)
			}
		})
	}
}

func TestExistingRPCResultNumbersRetainFloat64(t *testing.T) {
	var settings rpc.UserSettingsGetResult
	var models rpc.ModelList
	var prepared rpc.ModelProviderConfigurationPlan
	var prompt rpc.MCPPrompt
	var workflow rpc.WorkflowRunResult
	var canvas rpc.CanvasActionInvokeResult
	for _, test := range []struct {
		name   string
		input  string
		result any
		value  func() any
	}{
		{
			name:   "user setting value",
			input:  `{"settings":{"maxSessionCount":{"value":42,"default":10,"isDefault":false}}}`,
			result: &settings,
			value:  func() any { return settings.Settings["maxSessionCount"].Value },
		},
		{
			name:   "user setting default",
			input:  `{"settings":{"maxSessionCount":{"value":10,"default":42,"isDefault":false}}}`,
			result: &settings,
			value:  func() any { return settings.Settings["maxSessionCount"].Default },
		},
		{
			name:   "model metadata",
			input:  `{"models":[{"id":"model","name":"Model","capabilities":{},"metadata":{"revision":42}}]}`,
			result: &models,
			value:  func() any { return models.Models[0].Metadata["revision"] },
		},
		{
			name:   "prepared provider model metadata",
			input:  `{"model":{"id":"model","provider":"local","metadata":{"revision":42}}}`,
			result: &prepared,
			value:  func() any { return prepared.Model.Metadata["revision"] },
		},
		{
			name:   "MCP prompt metadata",
			input:  `{"name":"prompt","_meta":{"revision":42}}`,
			result: &prompt,
			value:  func() any { return prompt.Meta["revision"] },
		},
		{
			name:   "workflow result",
			input:  `{"runId":"run","status":"completed","result":42}`,
			result: &workflow,
			value:  func() any { return workflow.Result },
		},
		{
			name:   "canvas action result",
			input:  `{"result":42}`,
			result: &canvas,
			value:  func() any { return canvas.Result },
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			if err := json.Unmarshal([]byte(test.input), test.result); err != nil {
				t.Fatal(err)
			}
			value := test.value()
			number, ok := value.(float64)
			if !ok || number != 42 {
				t.Fatalf("expected float64(42), got %T (%v)", value, value)
			}
		})
	}
}

func TestProviderModelMetadataPreservesCompatibleNumbers(t *testing.T) {
	input := []byte(`{"id":"model","provider":"local","metadata":{"integer":42,"decimal":0.1,"exponent":1e2,"nested":[42.0,{"negative":-7}],"large":9007199254740993,"precise":1.234567890123456789}}`)
	var model rpc.ProviderModelConfig
	if err := json.Unmarshal(input, &model); err != nil {
		t.Fatal(err)
	}
	for key, want := range map[string]float64{"integer": 42, "decimal": 0.1, "exponent": 100} {
		if got, ok := model.Metadata[key].(float64); !ok || got != want {
			t.Errorf("%s: expected float64(%v), got %T (%v)", key, want, model.Metadata[key], model.Metadata[key])
		}
	}
	nested := model.Metadata["nested"].([]any)
	if got, ok := nested[0].(float64); !ok || got != 42 {
		t.Errorf("nested integer: expected float64(42), got %T (%v)", nested[0], nested[0])
	}
	negative := nested[1].(map[string]any)["negative"]
	if got, ok := negative.(float64); !ok || got != -7 {
		t.Errorf("nested negative: expected float64(-7), got %T (%v)", negative, negative)
	}
	encoded, err := json.Marshal(model)
	if err != nil {
		t.Fatal(err)
	}
	for _, exact := range []string{"9007199254740993", "1.234567890123456789"} {
		if !bytes.Contains(encoded, []byte(exact)) {
			t.Errorf("metadata number %s changed: %s", exact, encoded)
		}
	}
}
