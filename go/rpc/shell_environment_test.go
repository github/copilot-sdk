// Copyright (c) GitHub. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"maps"
	"net"
	"reflect"
	"slices"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestShellBuildInteractiveEnvironmentCollectionsOnWire(t *testing.T) {
	for _, test := range []struct {
		name        string
		params      ShellBuildInteractiveEnvironmentRequest
		environment string
		blocked     string
	}{
		{name: "nil collections", environment: `{}`, blocked: `[]`},
		{
			name:        "nil blocked names",
			params:      ShellBuildInteractiveEnvironmentRequest{HostEnvironment: map[string]string{"PATH": "/bin"}},
			environment: `{"PATH":"/bin"}`,
			blocked:     `[]`,
		},
		{
			name:        "nil environment",
			params:      ShellBuildInteractiveEnvironmentRequest{AdditionalBlockedNames: []string{"SECRET"}},
			environment: `{}`,
			blocked:     `["SECRET"]`,
		},
		{
			name:        "empty collections",
			params:      ShellBuildInteractiveEnvironmentRequest{HostEnvironment: map[string]string{}, AdditionalBlockedNames: []string{}},
			environment: `{}`,
			blocked:     `[]`,
		},
		{
			name:        "populated collections",
			params:      ShellBuildInteractiveEnvironmentRequest{HostEnvironment: map[string]string{"PATH": "/bin", "EMPTY": ""}, AdditionalBlockedNames: []string{"SECRET", "TOKEN"}},
			environment: `{"EMPTY":"","PATH":"/bin"}`,
			blocked:     `["SECRET","TOKEN"]`,
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			originalEnvironment := maps.Clone(test.params.HostEnvironment)
			originalBlocked := slices.Clone(test.params.AdditionalBlockedNames)
			assertCollections := func(raw []byte) {
				t.Helper()
				var fields map[string]json.RawMessage
				if err := json.Unmarshal(raw, &fields); err != nil {
					t.Fatal(err)
				}
				for key, expected := range map[string]string{
					"hostEnvironment":        test.environment,
					"additionalBlockedNames": test.blocked,
				} {
					if got := string(fields[key]); got != expected {
						t.Errorf("%s = %s, want %s; wire params: %s", key, got, expected, raw)
					}
				}
				if !reflect.DeepEqual(test.params.HostEnvironment, originalEnvironment) {
					t.Errorf("caller environment changed: got %#v, want %#v", test.params.HostEnvironment, originalEnvironment)
				}
				if !reflect.DeepEqual(test.params.AdditionalBlockedNames, originalBlocked) {
					t.Errorf("caller blocked names changed: got %#v, want %#v", test.params.AdditionalBlockedNames, originalBlocked)
				}
			}
			for _, params := range []any{test.params, &test.params} {
				raw, err := json.Marshal(params)
				if err != nil {
					t.Fatal(err)
				}
				assertCollections(raw)
			}

			clientConn, serverConn := net.Pipe()
			defer clientConn.Close()
			defer serverConn.Close()
			client := jsonrpc2.NewClient(clientConn, clientConn)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			requests := make(chan json.RawMessage, 1)
			server.SetRequestHandler("shell.buildInteractiveEnvironment", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				requests <- params
				return json.RawMessage(`{"environment":{}}`), nil
			})
			client.Start()
			server.Start()
			defer client.Stop()
			defer server.Stop()
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			if _, err := NewServerRPC(client).Shell.BuildInteractiveEnvironment(ctx, &test.params); err != nil {
				t.Fatal(err)
			}
			select {
			case raw := <-requests:
				assertCollections(raw)
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
		})
	}
}
