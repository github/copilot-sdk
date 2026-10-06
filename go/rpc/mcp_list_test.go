// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"io"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

var _ interface {
	List(context.Context) (*MCPServerList, error)
	ListConfigured(context.Context) (*MCPConfiguredServerList, error)
} = (*MCPAPI)(nil)

func TestMCPListUsesParameterlessWireContract(t *testing.T) {
	clientToServerReader, clientToServerWriter := io.Pipe()
	serverToClientReader, serverToClientWriter := io.Pipe()
	client := jsonrpc2.NewClient(clientToServerWriter, serverToClientReader)
	server := jsonrpc2.NewClient(serverToClientWriter, clientToServerReader)
	requests := make(chan map[string]any, 2)
	server.SetRequestHandler("session.mcp.list", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request map[string]any
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		requests <- request
		return json.RawMessage(`{"servers":[]}`), nil
	})
	server.SetRequestHandler("session.mcp.listConfigured", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request map[string]any
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		requests <- request
		return json.RawMessage(`{"servers":[]}`), nil
	})
	client.Start()
	server.Start()
	t.Cleanup(func() {
		client.Stop()
		server.Stop()
		_ = clientToServerWriter.Close()
		_ = clientToServerReader.Close()
		_ = serverToClientWriter.Close()
		_ = serverToClientReader.Close()
	})

	mcp := NewSessionRPC(client, "session-1").MCP
	if _, err := mcp.List(t.Context()); err != nil {
		t.Fatal(err)
	}
	if _, err := mcp.ListConfigured(t.Context()); err != nil {
		t.Fatal(err)
	}
	got := make([]map[string]any, 2)
	for i := range got {
		got[i] = <-requests
	}
	for _, request := range got {
		if request["sessionId"] != "session-1" || len(request) != 1 {
			t.Fatalf("request = %#v, want only the bound session ID", request)
		}
	}
}
