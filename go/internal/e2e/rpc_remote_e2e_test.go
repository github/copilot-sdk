package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/E2E/RpcRemoteE2ETests.cs (snapshot category "rpc_remote").
func TestRPCRemoteE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should treat remote off as no op or implemented error", func(t *testing.T) {
		session := createRemoteSession(t, client)
		defer session.Disconnect()

		mode := rpc.RemoteSessionModeOff
		result, err := session.RPC.Remote.Enable(t.Context(), &rpc.RemoteEnableRequest{Mode: &mode})
		if err != nil {
			assertImplementedRPCError(t, err, "session.remote.enable")
			return
		}
		if result.RemoteSteerable {
			t.Fatalf("Expected remote off to report RemoteSteerable=false, got %+v", result)
		}
		if result.URL != nil && *result.URL != "" {
			t.Fatalf("Expected remote off to return empty URL, got %q", *result.URL)
		}
	})

	t.Run("should treat remote disable as no op or implemented error", func(t *testing.T) {
		session := createRemoteSession(t, client)
		defer session.Disconnect()

		if _, err := session.RPC.Remote.Disable(t.Context()); err != nil {
			assertImplementedRPCError(t, err, "session.remote.disable")
		}
	})

	t.Run("should notify steerable changed event and persist flag", func(t *testing.T) {
		session := createRemoteSession(t, client)
		defer session.Disconnect()

		if _, err := session.RPC.Remote.NotifySteerableChanged(t.Context(), &rpc.RemoteNotifySteerableChangedRequest{RemoteSteerable: true}); err != nil {
			t.Fatalf("Remote.NotifySteerableChanged(true) failed: %v", err)
		}
		waitForRemoteSteerableEvent(t, session, true)

		if _, err := session.RPC.Remote.NotifySteerableChanged(t.Context(), &rpc.RemoteNotifySteerableChangedRequest{RemoteSteerable: false}); err != nil {
			t.Fatalf("Remote.NotifySteerableChanged(false) failed: %v", err)
		}
		waitForRemoteSteerableEvent(t, session, false)
	})
}

func createRemoteSession(t *testing.T, client *copilot.Client) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return session
}

func waitForRemoteSteerableEvent(t *testing.T, session *copilot.Session, expected bool) {
	t.Helper()
	waitForRPCCondition(t, 30*time.Second, "session.remote_steerable_changed event", func() (bool, error) {
		events, err := session.GetEvents(t.Context())
		if err != nil {
			return false, err
		}
		for _, event := range events {
			if data, ok := event.Data.(*copilot.SessionRemoteSteerableChangedData); ok && data.RemoteSteerable == expected {
				return true, nil
			}
		}
		return false, nil
	})
}

func assertImplementedRPCError(t *testing.T, err error, method string) {
	t.Helper()
	if strings.Contains(strings.ToLower(err.Error()), "unhandled method "+strings.ToLower(method)) {
		t.Fatalf("Expected implemented error for %s, got %v", method, err)
	}
}
