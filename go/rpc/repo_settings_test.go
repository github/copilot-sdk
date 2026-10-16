// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc_test

import (
	"encoding/json"
	"fmt"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func ExampleRepoSettingsReadRawResult() {
	const input = `{"config":{"custom":{"items":[true,null,12.5]}},"invalid":false}`
	var result rpc.RepoSettingsReadRawResult
	if err := json.Unmarshal([]byte(input), &result); err != nil {
		panic(err)
	}
	output, err := json.Marshal(result)
	if err != nil {
		panic(err)
	}
	fmt.Println(string(output))
	// Output: {"config":{"custom":{"items":[true,null,12.5]}},"invalid":false}
}

func TestRawRepoSettingsPreserveUnknownJSON(t *testing.T) {
	for _, input := range []string{
		`{"config":{"custom":{"items":[true,null,12.5],"text":"preserved"}},"invalid":false}`,
		`{"config":{},"invalid":true}`,
	} {
		t.Run(input, func(t *testing.T) {
			var decoded rpc.RepoSettingsReadRawResult
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
				t.Fatalf("repository settings changed:\nactual: %s\nexpected: %s", output, input)
			}
		})
	}
}
