// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"os"
	"path/filepath"
	"reflect"
	"testing"
)

// Generated serialization supplements the real MCP boundary tests in Node and .NET.
func TestMCPPromptResultsPreserveOpaqueJSON(t *testing.T) {
	raw, err := os.ReadFile(filepath.Join("..", "..", "test", "harness", "mcp-prompt-fixtures.json"))
	if err != nil {
		t.Fatal(err)
	}
	var fixtures map[string]json.RawMessage
	if err := json.Unmarshal(raw, &fixtures); err != nil {
		t.Fatal(err)
	}
	for _, name := range []string{"firstPage", "secondPage", "richPrompt"} {
		t.Run(name, func(t *testing.T) {
			var result any = &MCPPromptsListResult{}
			if name == "richPrompt" {
				result = &MCPPromptsGetResult{}
			}
			if err := json.Unmarshal(fixtures[name], result); err != nil {
				t.Fatal(err)
			}
			assertPromptJSON(t, result, fixtures[name])
		})
	}
}

func TestMCPPromptRequestsPreserveOptionalArguments(t *testing.T) {
	for _, wire := range []string{
		`{"serverName":"fixture","promptName":"rich"}`,
		`{"serverName":"fixture","promptName":"rich","arguments":{}}`,
		`{"serverName":"fixture","promptName":"rich","arguments":{"topic":"日本語","style":""}}`,
	} {
		t.Run(wire, func(t *testing.T) {
			var request MCPPromptsGetRequest
			if err := json.Unmarshal([]byte(wire), &request); err != nil {
				t.Fatal(err)
			}
			assertPromptJSON(t, request, []byte(wire))
		})
	}
	for _, wire := range []string{
		`{"serverName":"fixture"}`,
		`{"serverName":"fixture","cursor":"page:2/opaque+cursor="}`,
	} {
		var request MCPPromptsListRequest
		if err := json.Unmarshal([]byte(wire), &request); err != nil {
			t.Fatal(err)
		}
		assertPromptJSON(t, request, []byte(wire))
	}
}

func assertPromptJSON(t *testing.T, value any, expected []byte) {
	t.Helper()
	actual, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	var got, want any
	if err := json.Unmarshal(actual, &got); err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(expected, &want); err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("JSON mismatch\n got: %s\nwant: %s", actual, expected)
	}
}
