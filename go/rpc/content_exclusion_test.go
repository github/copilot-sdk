// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc_test

import (
	"bytes"
	"encoding/json"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestValidatedPoliciesPreserveJSONExtensions(t *testing.T) {
	for _, input := range []string{
		`{"policies":[]}`,
		`{"policies":[{"rules":[{"paths":["src/**"],"source":{"name":"Security","type":"Organization"},"futureRuleField":{"items":[true,null,12.5]}}],"last_updated_at":1720000000,"scope":"all","futurePolicyField":{"nullable":null}}]}`,
		`{"policies":[{"rules":[{"paths":["src/**"],"source":{"name":"Security","type":"Organization"},"futureRuleField":9007199254740993}],"last_updated_at":1720000000,"scope":"all","futurePolicyField":9007199254740993}]}`,
	} {
		t.Run(input, func(t *testing.T) {
			var decoded rpc.ContentExclusionValidatePoliciesResult
			if err := json.Unmarshal([]byte(input), &decoded); err != nil {
				t.Fatal(err)
			}
			output, err := json.Marshal(decoded)
			if err != nil {
				t.Fatal(err)
			}
			var expected, actual any
			expectedDecoder := json.NewDecoder(bytes.NewBufferString(input))
			expectedDecoder.UseNumber()
			if err := expectedDecoder.Decode(&expected); err != nil {
				t.Fatal(err)
			}
			actualDecoder := json.NewDecoder(bytes.NewReader(output))
			actualDecoder.UseNumber()
			if err := actualDecoder.Decode(&actual); err != nil {
				t.Fatal(err)
			}
			if !reflect.DeepEqual(actual, expected) {
				t.Fatalf("policy fields changed:\nactual: %s\nexpected: %s", output, input)
			}
		})
	}
}
