package e2e

import (
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpc(t *testing.T) {
	cliPath := testharness.CLIPath()
	if cliPath == "" {
		t.Fatal("CLI not found. Run 'npm install' in the nodejs directory first.")
	}

	t.Run("should call RPC.Ping with typed params and result", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		result, err := client.RPC.Ping(t.Context(), &rpc.PingParams{Message: copilot.String("typed rpc test")})
		if err != nil {
			t.Fatalf("Failed to call RPC.Ping: %v", err)
		}

		if result.Message != "pong: typed rpc test" {
			t.Errorf("Expected message 'pong: typed rpc test', got %q", result.Message)
		}

		if result.Timestamp < 0 {
			t.Errorf("Expected timestamp >= 0, got %f", result.Timestamp)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should call RPC.Models.List with typed result", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		authStatus, err := client.GetAuthStatus(t.Context())
		if err != nil {
			t.Fatalf("Failed to get auth status: %v", err)
		}

		if !authStatus.IsAuthenticated {
			t.Skip("Not authenticated - skipping models.list test")
		}

		result, err := client.RPC.Models.List(t.Context())
		if err != nil {
			t.Fatalf("Failed to call RPC.Models.List: %v", err)
		}

		if result.Models == nil {
			t.Error("Expected models to be defined")
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	// account.getQuota is defined in schema but not yet implemented in CLI
	t.Run("should call RPC.Account.GetQuota when authenticated", func(t *testing.T) {
		t.Skip("account.getQuota not yet implemented in CLI")

		client := copilot.NewClient(&copilot.ClientOptions{
			CLIPath:  cliPath,
			UseStdio: copilot.Bool(true),
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}

		authStatus, err := client.GetAuthStatus(t.Context())
		if err != nil {
			t.Fatalf("Failed to get auth status: %v", err)
		}

		if !authStatus.IsAuthenticated {
			t.Skip("Not authenticated - skipping account.getQuota test")
		}

		result, err := client.RPC.Account.GetQuota(t.Context())
		if err != nil {
			t.Fatalf("Failed to call RPC.Account.GetQuota: %v", err)
		}

		if result.QuotaSnapshots == nil {
			t.Error("Expected quotaSnapshots to be defined")
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})
}

func TestSessionRpc(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	// session.model.getCurrent is defined in schema but not yet implemented in CLI
	t.Run("should call session.RPC.Model.GetCurrent", func(t *testing.T) {
		t.Skip("session.model.getCurrent not yet implemented in CLI")

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model: "claude-sonnet-4.5",
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		result, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Failed to call session.RPC.Model.GetCurrent: %v", err)
		}

		if result.ModelID == nil || *result.ModelID == "" {
			t.Error("Expected modelId to be defined")
		}
	})

	// session.model.switchTo is defined in schema but not yet implemented in CLI
	t.Run("should call session.RPC.Model.SwitchTo", func(t *testing.T) {
		t.Skip("session.model.switchTo not yet implemented in CLI")

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model: "claude-sonnet-4.5",
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Get initial model
		before, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Failed to get current model: %v", err)
		}
		if before.ModelID == nil || *before.ModelID == "" {
			t.Error("Expected initial modelId to be defined")
		}

		// Switch to a different model
		result, err := session.RPC.Model.SwitchTo(t.Context(), &rpc.SessionModelSwitchToParams{
			ModelID: "gpt-4.1",
		})
		if err != nil {
			t.Fatalf("Failed to switch model: %v", err)
		}
		if result.ModelID == nil || *result.ModelID != "gpt-4.1" {
			t.Errorf("Expected modelId 'gpt-4.1', got %v", result.ModelID)
		}

		// Verify the switch persisted
		after, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Failed to get current model after switch: %v", err)
		}
		if after.ModelID == nil || *after.ModelID != "gpt-4.1" {
			t.Errorf("Expected modelId 'gpt-4.1' after switch, got %v", after.ModelID)
		}
	})
}
