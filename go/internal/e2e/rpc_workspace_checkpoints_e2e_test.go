package e2e

import (
	"os"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/E2E/RpcWorkspaceCheckpointsE2ETests.cs (snapshot category "rpc_workspace_checkpoints").
func TestRpcWorkspaceCheckpointsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should list no checkpoints for fresh session", func(t *testing.T) {
		session := createWorkspaceRPCSession(t, client)
		defer session.Disconnect()

		result, err := session.RPC.Workspaces.ListCheckpoints(t.Context())
		if err != nil {
			t.Fatalf("Workspaces.ListCheckpoints failed: %v", err)
		}
		if result.Checkpoints == nil {
			t.Fatal("Expected non-nil Checkpoints")
		}
		if len(result.Checkpoints) != 0 {
			t.Fatalf("Expected no checkpoints for fresh session, got %+v", result.Checkpoints)
		}
	})

	t.Run("should return nil or empty content for unknown checkpoint", func(t *testing.T) {
		session := createWorkspaceRPCSession(t, client)
		defer session.Disconnect()

		result, err := session.RPC.Workspaces.ReadCheckpoint(t.Context(), &rpc.WorkspacesReadCheckpointRequest{Number: 1<<62 - 1})
		if err != nil {
			t.Fatalf("Workspaces.ReadCheckpoint failed: %v", err)
		}
		if result.Content != nil && *result.Content != "" {
			t.Fatalf("Expected nil or empty content for unknown checkpoint, got %q", *result.Content)
		}
	})

	t.Run("should return typed workspace diff result", func(t *testing.T) {
		session := createWorkspaceRPCSession(t, client)
		defer session.Disconnect()

		result, err := session.RPC.Workspaces.Diff(t.Context(), &rpc.WorkspacesDiffRequest{Mode: rpc.WorkspaceDiffModeUnstaged})
		if err != nil {
			t.Fatalf("Workspaces.Diff failed: %v", err)
		}
		if result.RequestedMode != rpc.WorkspaceDiffModeUnstaged {
			t.Fatalf("Expected RequestedMode=unstaged, got %q", result.RequestedMode)
		}
		if result.Mode != rpc.WorkspaceDiffModeUnstaged && result.Mode != rpc.WorkspaceDiffModeBranch {
			t.Fatalf("Unexpected effective diff mode %q", result.Mode)
		}
		if result.Changes == nil {
			t.Fatal("Expected non-nil Changes")
		}
		for _, change := range result.Changes {
			if strings.TrimSpace(change.Path) == "" {
				t.Fatalf("Diff change has empty path: %+v", change)
			}
			switch change.ChangeType {
			case rpc.WorkspaceDiffFileChangeTypeAdded,
				rpc.WorkspaceDiffFileChangeTypeModified,
				rpc.WorkspaceDiffFileChangeTypeDeleted,
				rpc.WorkspaceDiffFileChangeTypeRenamed:
			default:
				t.Fatalf("Unexpected diff change type %q", change.ChangeType)
			}
			_ = change.Diff
		}
	})

	t.Run("should save large paste and expose readable content", func(t *testing.T) {
		session := createWorkspaceRPCSession(t, client)
		defer session.Disconnect()
		content := strings.Repeat("Large paste payload 🚀\n", 512)

		result, err := session.RPC.Workspaces.SaveLargePaste(t.Context(), &rpc.WorkspacesSaveLargePasteRequest{Content: content})
		if err != nil {
			t.Fatalf("Workspaces.SaveLargePaste failed: %v", err)
		}
		if result.Saved == nil {
			t.Fatal("Expected SaveLargePaste to return saved descriptor")
		}
		saved := result.Saved
		if strings.TrimSpace(saved.Filename) == "" || strings.TrimSpace(saved.FilePath) == "" {
			t.Fatalf("Expected saved filename and filepath, got %+v", saved)
		}
		if saved.SizeBytes != int64(len([]byte(content))) {
			t.Fatalf("Expected SizeBytes=%d, got %d", len([]byte(content)), saved.SizeBytes)
		}

		read, readErr := session.RPC.Workspaces.ReadFile(t.Context(), &rpc.WorkspacesReadFileRequest{Path: saved.Filename})
		if readErr == nil {
			if read.Content != content {
				t.Fatalf("Expected ReadFile content to match saved paste")
			}
			return
		}

		bytes, err := os.ReadFile(saved.FilePath)
		if err != nil {
			t.Fatalf("ReadFile failed (%v), and saved file %q was not readable: %v", readErr, saved.FilePath, err)
		}
		if string(bytes) != content {
			t.Fatalf("Expected saved file content to match large paste")
		}
	})
}

func createWorkspaceRPCSession(t *testing.T, client *copilot.Client) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return session
}
