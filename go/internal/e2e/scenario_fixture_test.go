// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"context"
	"encoding/json"
	"net"
	"reflect"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func assertJSONSubset(t *testing.T, label string, expected, actual any) {
	t.Helper()
	switch expected := expected.(type) {
	case map[string]any:
		actual, ok := actual.(map[string]any)
		if !ok {
			t.Fatalf("%s = %#v, want object", label, actual)
		}
		for key, value := range expected {
			actualValue, exists := actual[key]
			if !exists {
				t.Fatalf("%s missing %q in %#v", label, key, actual)
			}
			assertJSONSubset(t, label+"."+key, value, actualValue)
		}
	case []any:
		actual, ok := actual.([]any)
		if !ok || len(actual) != len(expected) {
			t.Fatalf("%s = %#v, want %d items", label, actual, len(expected))
		}
		for i := range expected {
			assertJSONSubset(t, label, expected[i], actual[i])
		}
	default:
		if !reflect.DeepEqual(expected, actual) {
			t.Fatalf("%s = %#v, want %#v", label, actual, expected)
		}
	}
}

type generatedRPCFixture struct {
	client  *copilot.Client
	session *copilot.Session
	server  *jsonrpc2.Client
	conn    net.Conn
}

func newGeneratedRPCFixture(t *testing.T, ctx context.Context) *generatedRPCFixture {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = listener.Close() })

	type serverConnection struct {
		server *jsonrpc2.Client
		conn   net.Conn
	}
	ready := make(chan serverConnection, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		server := jsonrpc2.NewClient(conn, conn)
		t.Cleanup(server.Stop)
		for method, result := range map[string]string{
			"connect":                `{"ok":true,"protocolVersion":3,"version":"test"}`,
			"plugins.builtin.set":    `{}`,
			"session.create":         `{"sessionId":"generated-rpc-surface"}`,
			"session.options.update": `{"success":true}`,
			"session.detach":         `{"success":true}`,
		} {
			server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				return json.RawMessage(result), nil
			})
		}
		server.Start()
		ready <- serverConnection{server: server, conn: conn}
	}()

	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.URIConnection{URL: listener.Addr().String()},
	})
	t.Cleanup(client.ForceStop)
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		SessionID:           "generated-rpc-surface",
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatal(err)
	}

	select {
	case connection := <-ready:
		return &generatedRPCFixture{client: client, session: session, server: connection.server, conn: connection.conn}
	case <-ctx.Done():
		t.Fatal(ctx.Err())
		return nil
	}
}
