// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

var _ interface {
	Close(context.Context, *SessionsCloseRequest) (*SessionsCloseResult, error)
	CloseWithOptions(context.Context, *SessionsCloseOptions) (*SessionsCloseResult, error)
} = (*ServerSessionsAPI)(nil)

func TestSessionsClosePreservesUnkeyedRequestLiterals(t *testing.T) {
	request := SessionsCloseRequest{"legacy"}
	data, err := json.Marshal(request)
	if err != nil {
		t.Fatal(err)
	}
	if string(data) != `{"sessionId":"legacy"}` {
		t.Fatalf("legacy request = %s", data)
	}
}

func TestSessionsCloseOptionsPreserveWireContract(t *testing.T) {
	clientReader, clientWriter := io.Pipe()
	serverReader, serverWriter := io.Pipe()
	client := jsonrpc2.NewClient(clientWriter, serverReader)
	server := jsonrpc2.NewClient(serverWriter, clientReader)
	requests := make(chan map[string]any, 4)
	server.SetRequestHandler("sessions.close", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request map[string]any
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		requests <- request
		return json.RawMessage(`{}`), nil
	})
	client.Start()
	server.Start()
	t.Cleanup(func() {
		client.Stop()
		server.Stop()
		_ = clientWriter.Close()
		_ = clientReader.Close()
		_ = serverWriter.Close()
		_ = serverReader.Close()
	})

	sessions := NewServerRPC(client).Sessions
	legacy := SessionsCloseRequest{"legacy"}
	if _, err := sessions.Close(t.Context(), &legacy); err != nil {
		t.Fatal(err)
	}
	options := SessionsCloseOptions{SessionID: "legacy"}
	if _, err := sessions.CloseWithOptions(t.Context(), &options); err != nil {
		t.Fatal(err)
	}
	for _, detach := range []bool{false, true} {
		options := SessionsCloseOptions{SessionID: "legacy", DetachSessionEndHooks: &detach}
		if _, err := sessions.CloseWithOptions(t.Context(), &options); err != nil {
			t.Fatal(err)
		}
		if options.SessionID != "legacy" || options.DetachSessionEndHooks == nil || *options.DetachSessionEndHooks != detach {
			t.Fatalf("caller options changed: %#v", options)
		}
	}
	want := []map[string]any{
		{"sessionId": "legacy"},
		{"sessionId": "legacy"},
		{"sessionId": "legacy", "detachSessionEndHooks": false},
		{"sessionId": "legacy", "detachSessionEndHooks": true},
	}
	for _, expected := range want {
		if actual := <-requests; !reflect.DeepEqual(actual, expected) {
			t.Fatalf("close request = %#v, want %#v", actual, expected)
		}
	}
}

func ExampleSessionsCloseOptions() {
	detach := true
	options := SessionsCloseOptions{SessionID: "session", DetachSessionEndHooks: &detach}
	data, _ := json.Marshal(options)
	fmt.Println(string(data))
	// Output: {"detachSessionEndHooks":true,"sessionId":"session"}
}
