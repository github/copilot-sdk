// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc_test

import (
	"encoding/json"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestCanvasCallbackNumbersRetainFloat64(t *testing.T) {
	var opened rpc.CanvasProviderOpenRequest
	if err := json.Unmarshal([]byte(`{"extensionId":"counter","canvasId":"counter","instanceId":"panel","sessionId":"session","input":{"startValue":3}}`), &opened); err != nil {
		t.Fatal(err)
	}
	var action rpc.CanvasProviderInvokeActionRequest
	if err := json.Unmarshal([]byte(`{"extensionId":"counter","canvasId":"counter","instanceId":"panel","sessionId":"session","actionName":"increment","input":{"amount":2}}`), &action); err != nil {
		t.Fatal(err)
	}
	number := func(input any, name string) float64 {
		t.Helper()
		value := input.(map[string]any)[name]
		number, ok := value.(float64)
		if !ok {
			t.Fatalf("%s: expected float64, got %T (%v)", name, value, value)
		}
		return number
	}
	if got := number(opened.Input, "startValue") + number(action.Input, "amount"); got != 5 {
		t.Fatalf("counter result: got %v, want 5", got)
	}
}
