// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"encoding/json"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

func TestMCPStartConfigDecodesTypedLocalConfig(t *testing.T) {
	var request rpc.MCPStartServerRequest
	if err := json.Unmarshal([]byte(`{
		"serverName": "ide",
		"config": {
			"type": "local-http", "socketPath": "/ide/first.sock",
			"ide": {
				"ideName": "Test IDE", "workspaceFolder": "/workspace",
				"pid": 1, "lockFilePath": "/ide/lock", "clientVersion": "1.0",
				"clientPid": 2, "parentPid": 3
			}
		}
	}`), &request); err != nil {
		t.Fatal(err)
	}
	config, ok := any(request.Config).(*rpc.MCPServerConfigLocal)
	if !ok {
		t.Fatalf("decoded config = %T, want *rpc.MCPServerConfigLocal", request.Config)
	}
	config.SocketPath = "/ide/second.sock"
	data, err := json.Marshal(request)
	if err != nil {
		t.Fatal(err)
	}
	var wire struct {
		Config struct {
			Type       string `json:"type"`
			SocketPath string `json:"socketPath"`
		} `json:"config"`
	}
	if err := json.Unmarshal(data, &wire); err != nil {
		t.Fatal(err)
	}
	if wire.Config.Type != "local-http" || wire.Config.SocketPath != config.SocketPath {
		t.Fatalf("encoded config = %+v, want the changed local endpoint", wire.Config)
	}
}
