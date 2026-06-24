package e2e

import (
	"strings"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcServerMisc(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	sharedClient := ctx.NewClient()
	t.Cleanup(func() { sharedClient.ForceStop() })

	t.Run("should_reload_user_settings", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		if err := sharedClient.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		if _, err := sharedClient.RPC.User.Settings().Reload(t.Context()); err != nil {
			t.Fatalf("User.Settings.Reload failed: %v", err)
		}
	})

	t.Run("should_report_agent_registry_spawn_gate_closed", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		_, err := client.RPC.AgentRegistry.Spawn(t.Context(), &rpc.AgentRegistrySpawnRequest{Cwd: ctx.WorkDir})
		if err == nil {
			t.Fatal("Expected AgentRegistry.Spawn to be rejected by the closed spawn gate")
		}
		message := err.Error()
		assertPortedNoUnhandledMethod(t, message)
		assertPortedContainsFold(t, message, "agentRegistry.spawn")
		if !strings.Contains(strings.ToLower(message), "not enabled") && !strings.Contains(strings.ToLower(message), "no delegate") {
			t.Fatalf("Expected agentRegistry.spawn gate error, got %s", message)
		}
	})

	t.Run("should_shut_down_owned_runtime", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()

		if _, err := client.RPC.User.Settings().Reload(t.Context()); err != nil {
			t.Fatalf("User.Settings.Reload before shutdown failed: %v", err)
		}
		if _, err := client.RPC.Runtime.Shutdown(t.Context()); err != nil {
			t.Fatalf("Runtime.Shutdown failed: %v", err)
		}

		waitForRPCCondition(t, 15*time.Second, "runtime to stop serving RPCs after shutdown", func() (bool, error) {
			_, err := client.RPC.User.Settings().Reload(t.Context())
			return err != nil, nil
		})
	})

	t.Run("should_report_not_found_when_opening_session_without_context", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		result, err := client.RPC.Sessions.Open(t.Context(), nil)
		if err != nil {
			t.Fatalf("Sessions.Open failed: %v", err)
		}
		if result.Status != rpc.SessionsOpenStatusNotFound {
			t.Fatalf("Expected Sessions.Open status not_found, got %+v", result)
		}
		if result.SessionID != nil {
			t.Fatalf("Expected nil session ID for not_found, got %q", *result.SessionID)
		}
	})

	t.Run("should_reject_send_attachments_from_non_extension_connection", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		session := createPortedSession(t, sharedClient, nil)
		defer session.Disconnect()

		_, err := session.RPC.Extensions.SendAttachmentsToMessage(t.Context(), &rpc.SendAttachmentsToMessageParams{Attachments: []rpc.PushAttachment{}})
		if err == nil {
			t.Fatal("Expected SendAttachmentsToMessage from a normal SDK connection to fail")
		}
		message := err.Error()
		assertPortedNoUnhandledMethod(t, message)
		assertPortedContainsFold(t, message, "extension")
	})
}
