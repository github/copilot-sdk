// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc_test

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestSettingsRedactionPreservesJSONValues(t *testing.T) {
	for _, input := range []string{
		`{"value":{"token":"******","extra":{"nullable":null,"items":[true,12.5,"text"]}}}`,
		`{"value":[null,{"arbitrary":"preserved"}]}`,
		`{"value":null}`,
		`{"value":false}`,
		`{"value":12.5}`,
		`{"value":"******"}`,
	} {
		t.Run(input, func(t *testing.T) {
			var decoded rpc.SettingsRedactValueForDisplayResult
			if err := json.Unmarshal([]byte(input), &decoded); err != nil {
				t.Fatal(err)
			}
			output, err := json.Marshal(decoded)
			if err != nil {
				t.Fatal(err)
			}
			var expected, actual any
			if err := json.Unmarshal([]byte(input), &expected); err != nil {
				t.Fatal(err)
			}
			if err := json.Unmarshal(output, &actual); err != nil {
				t.Fatal(err)
			}
			if !reflect.DeepEqual(actual, expected) {
				t.Fatalf("redacted JSON changed:\nactual: %s\nexpected: %s", output, input)
			}
		})
	}
}
