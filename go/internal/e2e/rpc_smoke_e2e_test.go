// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestGeneratedRPCRoundTrip(t *testing.T) {
	client := testharness.NewTestContext(t).NewClient()
	t.Cleanup(client.ForceStop)
	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Start failed: %v", err)
	}

	result, err := client.RPC.Ping(t.Context(), &rpc.PingRequest{Message: copilot.String("typed rpc test")})
	if err != nil {
		t.Fatalf("RPC.Ping failed: %v", err)
	}
	if result.Message != "pong: typed rpc test" || result.Timestamp.IsZero() {
		t.Fatalf("RPC.Ping returned %#v", result)
	}

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	t.Cleanup(func() { _ = session.Disconnect() })

	mode, err := session.RPC.Mode.Get(t.Context())
	if err != nil {
		t.Fatalf("RPC.Mode.Get failed: %v", err)
	}
	if mode == nil || *mode != rpc.SessionModeInteractive {
		t.Fatalf("RPC.Mode.Get returned %v, want interactive", mode)
	}
}
