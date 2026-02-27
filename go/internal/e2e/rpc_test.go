package e2e

import (
	"strings"
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
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               "claude-sonnet-4.5",
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
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               "claude-sonnet-4.5",
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

	t.Run("should get and set session mode", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Get initial mode (default should be interactive)
		initial, err := session.RPC.Mode.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get mode: %v", err)
		}
		if initial.Mode != rpc.Interactive {
			t.Errorf("Expected initial mode 'interactive', got %q", initial.Mode)
		}

		// Switch to plan mode
		planResult, err := session.RPC.Mode.Set(t.Context(), &rpc.SessionModeSetParams{Mode: rpc.Plan})
		if err != nil {
			t.Fatalf("Failed to set mode to plan: %v", err)
		}
		if planResult.Mode != rpc.Plan {
			t.Errorf("Expected mode 'plan', got %q", planResult.Mode)
		}

		// Verify mode persisted
		afterPlan, err := session.RPC.Mode.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get mode after plan: %v", err)
		}
		if afterPlan.Mode != rpc.Plan {
			t.Errorf("Expected mode 'plan' after set, got %q", afterPlan.Mode)
		}

		// Switch back to interactive
		interactiveResult, err := session.RPC.Mode.Set(t.Context(), &rpc.SessionModeSetParams{Mode: rpc.Interactive})
		if err != nil {
			t.Fatalf("Failed to set mode to interactive: %v", err)
		}
		if interactiveResult.Mode != rpc.Interactive {
			t.Errorf("Expected mode 'interactive', got %q", interactiveResult.Mode)
		}
	})

	t.Run("should read, update, and delete plan", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Initially plan should not exist
		initial, err := session.RPC.Plan.Read(t.Context())
		if err != nil {
			t.Fatalf("Failed to read plan: %v", err)
		}
		if initial.Exists {
			t.Error("Expected plan to not exist initially")
		}
		if initial.Content != nil {
			t.Error("Expected content to be nil initially")
		}

		// Create/update plan
		planContent := "# Test Plan\n\n- Step 1\n- Step 2"
		_, err = session.RPC.Plan.Update(t.Context(), &rpc.SessionPlanUpdateParams{Content: planContent})
		if err != nil {
			t.Fatalf("Failed to update plan: %v", err)
		}

		// Verify plan exists and has correct content
		afterUpdate, err := session.RPC.Plan.Read(t.Context())
		if err != nil {
			t.Fatalf("Failed to read plan after update: %v", err)
		}
		if !afterUpdate.Exists {
			t.Error("Expected plan to exist after update")
		}
		if afterUpdate.Content == nil || *afterUpdate.Content != planContent {
			t.Errorf("Expected content %q, got %v", planContent, afterUpdate.Content)
		}

		// Delete plan
		_, err = session.RPC.Plan.Delete(t.Context())
		if err != nil {
			t.Fatalf("Failed to delete plan: %v", err)
		}

		// Verify plan is deleted
		afterDelete, err := session.RPC.Plan.Read(t.Context())
		if err != nil {
			t.Fatalf("Failed to read plan after delete: %v", err)
		}
		if afterDelete.Exists {
			t.Error("Expected plan to not exist after delete")
		}
		if afterDelete.Content != nil {
			t.Error("Expected content to be nil after delete")
		}
	})

	t.Run("should create, list, and read workspace files", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Initially no files
		initialFiles, err := session.RPC.Workspace.ListFiles(t.Context())
		if err != nil {
			t.Fatalf("Failed to list files: %v", err)
		}
		if len(initialFiles.Files) != 0 {
			t.Errorf("Expected no files initially, got %v", initialFiles.Files)
		}

		// Create a file
		fileContent := "Hello, workspace!"
		_, err = session.RPC.Workspace.CreateFile(t.Context(), &rpc.SessionWorkspaceCreateFileParams{
			Path:    "test.txt",
			Content: fileContent,
		})
		if err != nil {
			t.Fatalf("Failed to create file: %v", err)
		}

		// List files
		afterCreate, err := session.RPC.Workspace.ListFiles(t.Context())
		if err != nil {
			t.Fatalf("Failed to list files after create: %v", err)
		}
		if !containsString(afterCreate.Files, "test.txt") {
			t.Errorf("Expected files to contain 'test.txt', got %v", afterCreate.Files)
		}

		// Read file
		readResult, err := session.RPC.Workspace.ReadFile(t.Context(), &rpc.SessionWorkspaceReadFileParams{
			Path: "test.txt",
		})
		if err != nil {
			t.Fatalf("Failed to read file: %v", err)
		}
		if readResult.Content != fileContent {
			t.Errorf("Expected content %q, got %q", fileContent, readResult.Content)
		}

		// Create nested file
		_, err = session.RPC.Workspace.CreateFile(t.Context(), &rpc.SessionWorkspaceCreateFileParams{
			Path:    "subdir/nested.txt",
			Content: "Nested content",
		})
		if err != nil {
			t.Fatalf("Failed to create nested file: %v", err)
		}

		afterNested, err := session.RPC.Workspace.ListFiles(t.Context())
		if err != nil {
			t.Fatalf("Failed to list files after nested: %v", err)
		}
		if !containsString(afterNested.Files, "test.txt") {
			t.Errorf("Expected files to contain 'test.txt', got %v", afterNested.Files)
		}
		hasNested := false
		for _, f := range afterNested.Files {
			if strings.Contains(f, "nested.txt") {
				hasNested = true
				break
			}
		}
		if !hasNested {
			t.Errorf("Expected files to contain 'nested.txt', got %v", afterNested.Files)
		}
	})
}

func containsString(slice []string, str string) bool {
	for _, s := range slice {
		if s == str {
			return true
		}
	}
	return false
}
