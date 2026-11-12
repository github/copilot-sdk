// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc_test

import (
	"encoding/json"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestExistingRPCResultNumbersRetainFloat64(t *testing.T) {
	var settings rpc.UserSettingsGetResult
	var models rpc.ModelList
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
