// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"net"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestSettingsValuePresenceOnWire(t *testing.T) {
	for _, method := range []string{"user.settings.applyTopKey", "repo.settings.writeKey", "user.settings.edit"} {
		for _, test := range []struct {
			name    string
			value   any
			present bool
			delete  bool
		}{
			{name: "omitted"},
			{name: "explicit null", value: json.RawMessage("null"), present: true},
			{name: "value", value: "chosen", present: true},
			{name: "delete", delete: true},
		} {
			t.Run(method+"/"+test.name, func(t *testing.T) {
				clientConn, serverConn := net.Pipe()
				defer clientConn.Close()
				defer serverConn.Close()
				client := jsonrpc2.NewClient(clientConn, clientConn)
				server := jsonrpc2.NewClient(serverConn, serverConn)
				requests := make(chan json.RawMessage, 1)
				server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					requests <- params
					return json.RawMessage(`{}`), nil
				})
				client.Start()
				server.Start()
				defer client.Stop()
				defer server.Stop()
				ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
				defer cancel()
				api := NewInternalServerRPC(client)
				var err error
				switch method {
				case "user.settings.applyTopKey":
					var value *any
					if test.present {
						value = &test.value
					}
					_, err = api.User.Settings().ApplyTopKey(ctx, &UserSettingsApplyTopKeyRequest{
						CanonicalPath: []string{"model"},
						TopKey:        "model",
						Value:         value,
						Delete:        &test.delete,
					})
				case "repo.settings.writeKey":
					_, err = api.Repo.Settings().WriteKey(ctx, &RepoSettingsWriteKeyRequest{
						RepoRoot: t.TempDir(),
						Scope:    "repo",
						Key:      "model",
						Value:    test.value,
						Delete:   &test.delete,
					})
				case "user.settings.edit":
					operation := SettingsEditOperationSet
					if test.delete {
						operation = SettingsEditOperationRemove
					}
					_, err = api.User.Settings().Edit(ctx, &UserSettingsEditRequest{
						CanonicalPath: []string{"model"},
						Operation:     operation,
						Value:         test.value,
					})
				}
				if err != nil {
					t.Fatal(err)
				}
				select {
				case raw := <-requests:
					var fields map[string]json.RawMessage
					if err := json.Unmarshal(raw, &fields); err != nil {
						t.Fatal(err)
					}
					value, present := fields["value"]
					wantPresent := test.present || (method == "user.settings.edit" && !test.delete)
					if present != wantPresent {
						t.Fatalf("value presence = %v, want %v; wire params: %s", present, wantPresent, raw)
					}
					if present {
						expected, err := json.Marshal(test.value)
						if err != nil {
							t.Fatal(err)
						}
						if string(value) != string(expected) {
							t.Fatalf("value = %s, want %s; wire params: %s", value, expected, raw)
						}
					}
				case <-ctx.Done():
					t.Fatal(ctx.Err())
				}
			})
		}
	}
}
