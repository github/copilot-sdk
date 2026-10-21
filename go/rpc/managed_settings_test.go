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

func TestManagedSettingsResolveRequestEncoding(t *testing.T) {
	selectionID := "selected-account"
	for _, test := range []struct {
		name   string
		params *ManagedSettingsResolveRequest
		want   string
	}{
		{"nil", nil, `{}`},
		{"empty", &ManagedSettingsResolveRequest{}, `{}`},
		{"selected", &ManagedSettingsResolveRequest{SelectionID: &selectionID}, `{"selectionId":"selected-account"}`},
	} {
		t.Run(test.name, func(t *testing.T) {
			clientConn, serverConn := net.Pipe()
			defer clientConn.Close()
			defer serverConn.Close()
			client := jsonrpc2.NewClient(clientConn, clientConn)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			requests := make(chan json.RawMessage, 1)
			server.SetRequestHandler("managedSettings.resolve", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				requests <- params
				return json.RawMessage(`{}`), nil
			})
			client.Start()
			server.Start()
			defer client.Stop()
			defer server.Stop()
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			if _, err := NewServerRPC(client).ManagedSettings.Resolve(ctx, test.params); err != nil {
				t.Fatal(err)
			}
			select {
			case got := <-requests:
				if string(got) != test.want {
					t.Fatalf("params = %s, want %s", got, test.want)
				}
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
		})
	}
}
