// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"io"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestSessionsOpenPreservesUnkeyedLiteralsAndWire(t *testing.T) {
	emit := false
	suppress := true
	sessionID := "legacy"
	cases := []struct {
		params SessionOpenParams
		want   map[string]any
	}{
		{
			SessionsOpenCreate{&emit, nil},
			map[string]any{"kind": "create", "emitStart": false},
		},
		{
			SessionsOpenResume{nil, &emit, sessionID, &suppress},
			map[string]any{
				"kind": "resume", "resume": false, "sessionId": sessionID,
				"suppressResumeWorkspaceMetadataWriteback": true,
			},
		},
	}
	expected := SessionOpenResult{
		nil, nil, nil, map[string]any{"legacy": true},
		&sessionID, []string{"startup"}, SessionsOpenStatusCreated,
	}

	clientReader, clientWriter := io.Pipe()
	serverReader, serverWriter := io.Pipe()
	client := jsonrpc2.NewClient(clientWriter, serverReader)
	server := jsonrpc2.NewClient(serverWriter, clientReader)
	requests := make(chan map[string]any, len(cases))
	server.SetRequestHandler("sessions.open", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request map[string]any
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		requests <- request
		return json.RawMessage(`{"sessionId":"legacy","startupPrompts":["startup"],"status":"created","sessionApi":{"legacy":true},"nativeConstructionPlan":{"host":"private"}}`), nil
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
	for _, testCase := range cases {
		result, err := sessions.Open(t.Context(), &testCase.params)
		if err != nil {
			t.Fatal(err)
		}
		if !reflect.DeepEqual(result, &expected) {
			t.Fatalf("open result = %#v, want %#v", result, &expected)
		}
		if actual := <-requests; !reflect.DeepEqual(actual, testCase.want) {
			t.Fatalf("open request = %#v, want %#v", actual, testCase.want)
		}
		encoded, err := json.Marshal(result)
		if err != nil {
			t.Fatal(err)
		}
		var publicResult map[string]any
		if err := json.Unmarshal(encoded, &publicResult); err != nil {
			t.Fatal(err)
		}
		if plan, exists := publicResult["nativeConstructionPlan"]; exists {
			t.Fatalf("private host plan reached the public result: %#v", plan)
		}
	}
}
