package e2e

import (
	"path/filepath"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcSessionStateTests.cs (snapshot category "rpc_session_state").
//
// Reuses snapshot files in test/snapshots/rpc_session_state/. Tests that don't issue
// LLM calls don't need snapshots.
func TestRpcSessionStateE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should call session rpc model getCurrent", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "claude-sonnet-4.5",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		result, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Model.GetCurrent failed: %v", err)
		}
		if result.ModelID == nil || *result.ModelID != "claude-sonnet-4.5" {
			t.Fatalf("Expected current model claude-sonnet-4.5, got %+v", result)
		}
	})

	t.Run("should call session rpc model switchTo", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "claude-sonnet-4.5",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if before, err := session.RPC.Model.GetCurrent(t.Context()); err != nil {
			t.Fatalf("Model.GetCurrent before switch failed: %v", err)
		} else if before.ModelID == nil {
			t.Fatalf("Expected non-empty model before switch, got %+v", before)
		}

		reasoningEffort := "high"
		result, err := session.RPC.Model.SwitchTo(t.Context(), &rpc.ModelSwitchToRequest{
			ModelID:         "gpt-4.1",
			ReasoningEffort: &reasoningEffort,
		})
		if err != nil {
			t.Fatalf("Model.SwitchTo failed: %v", err)
		}
		if result.ModelID == nil || *result.ModelID != "gpt-4.1" {
			t.Fatalf("Expected switch result model gpt-4.1, got %+v", result)
		}
		after, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Model.GetCurrent after switch failed: %v", err)
		}
		if after.ModelID == nil || *after.ModelID != "gpt-4.1" || after.ReasoningEffort == nil || *after.ReasoningEffort != "high" {
			t.Fatalf("Expected current model gpt-4.1/high after switch, got %+v", after)
		}
	})

	t.Run("should get and set session mode", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		initial, err := session.RPC.Mode.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get mode: %v", err)
		}
		if initial == nil || *initial != rpc.SessionModeInteractive {
			t.Errorf("Expected initial mode 'interactive', got %v", initial)
		}

		if _, err := session.RPC.Mode.Set(t.Context(), &rpc.ModeSetRequest{Mode: rpc.SessionModePlan}); err != nil {
			t.Fatalf("Failed to set mode to plan: %v", err)
		}
		afterPlan, err := session.RPC.Mode.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get mode after plan: %v", err)
		}
		if afterPlan == nil || *afterPlan != rpc.SessionModePlan {
			t.Errorf("Expected mode 'plan' after set, got %v", afterPlan)
		}

		if _, err := session.RPC.Mode.Set(t.Context(), &rpc.ModeSetRequest{Mode: rpc.SessionModeInteractive}); err != nil {
			t.Fatalf("Failed to set mode to interactive: %v", err)
		}
		final, err := session.RPC.Mode.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get mode after revert: %v", err)
		}
		if final == nil || *final != rpc.SessionModeInteractive {
			t.Errorf("Expected mode 'interactive' after revert, got %v", final)
		}
	})

	t.Run("should shutdown session with routine type", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		awaitShutdown := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionShutdown,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionShutdownData)
				return ok && data.ShutdownType == copilot.ShutdownTypeRoutine
			},
			"session.shutdown routine event",
		)

		reason := "Go SDK E2E shutdown coverage"
		shutdownType := rpc.ShutdownTypeRoutine
		if _, err := session.RPC.Shutdown(t.Context(), &rpc.ShutdownRequest{Type: &shutdownType, Reason: &reason}); err != nil {
			t.Fatalf("Shutdown failed: %v", err)
		}
		event := awaitEvent(t, awaitShutdown)
		if data := event.Data.(*copilot.SessionShutdownData); data.ShutdownType != copilot.ShutdownTypeRoutine {
			t.Fatalf("Expected routine shutdown event, got %+v", data)
		}
	})

	t.Run("should set and get each session mode value", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		for _, mode := range []rpc.SessionMode{rpc.SessionModeInteractive, rpc.SessionModePlan, rpc.SessionModeAutopilot} {
			if _, err := session.RPC.Mode.Set(t.Context(), &rpc.ModeSetRequest{Mode: mode}); err != nil {
				t.Fatalf("Failed to set mode %q: %v", mode, err)
			}
			got, err := session.RPC.Mode.Get(t.Context())
			if err != nil {
				t.Fatalf("Failed to get mode %q: %v", mode, err)
			}
			if got == nil || *got != mode {
				t.Fatalf("Expected mode %q, got %v", mode, got)
			}
		}
	})

	t.Run("should read update and delete plan", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		initial, err := session.RPC.Plan.Read(t.Context())
		if err != nil {
			t.Fatalf("Failed to read plan: %v", err)
		}
		if initial.Exists {
			t.Error("Expected plan to not exist initially")
		}
		if initial.Content != nil {
			t.Error("Expected plan content to be nil initially")
		}

		const planContent = "# Test Plan\n\n- Step 1\n- Step 2"
		if _, err := session.RPC.Plan.Update(t.Context(), &rpc.PlanUpdateRequest{Content: planContent}); err != nil {
			t.Fatalf("Failed to update plan: %v", err)
		}

		afterUpdate, err := session.RPC.Plan.Read(t.Context())
		if err != nil {
			t.Fatalf("Failed to read plan after update: %v", err)
		}
		if !afterUpdate.Exists {
			t.Error("Expected plan to exist after update")
		}
		if afterUpdate.Content == nil || *afterUpdate.Content != planContent {
			t.Errorf("Expected plan content %q, got %v", planContent, afterUpdate.Content)
		}

		if _, err := session.RPC.Plan.Delete(t.Context()); err != nil {
			t.Fatalf("Failed to delete plan: %v", err)
		}

		afterDelete, err := session.RPC.Plan.Read(t.Context())
		if err != nil {
			t.Fatalf("Failed to read plan after delete: %v", err)
		}
		if afterDelete.Exists {
			t.Error("Expected plan to not exist after delete")
		}
		if afterDelete.Content != nil {
			t.Error("Expected plan content to be nil after delete")
		}
	})

	t.Run("should call workspace file rpc methods", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		initial, err := session.RPC.Workspaces.ListFiles(t.Context())
		if err != nil {
			t.Fatalf("Failed to list workspace files: %v", err)
		}
		if initial.Files == nil {
			t.Error("Expected workspace files slice to be non-nil")
		}

		if _, err := session.RPC.Workspaces.CreateFile(t.Context(), &rpc.WorkspacesCreateFileRequest{
			Path:    "test.txt",
			Content: "Hello, workspace!",
		}); err != nil {
			t.Fatalf("Failed to create workspace file: %v", err)
		}

		afterCreate, err := session.RPC.Workspaces.ListFiles(t.Context())
		if err != nil {
			t.Fatalf("Failed to list workspace files after create: %v", err)
		}
		if !containsString(afterCreate.Files, "test.txt") {
			t.Errorf("Expected workspace files to contain 'test.txt', got %v", afterCreate.Files)
		}

		file, err := session.RPC.Workspaces.ReadFile(t.Context(), &rpc.WorkspacesReadFileRequest{Path: "test.txt"})
		if err != nil {
			t.Fatalf("Failed to read workspace file: %v", err)
		}
		if file.Content != "Hello, workspace!" {
			t.Errorf("Expected file content 'Hello, workspace!', got %q", file.Content)
		}

		workspace, err := session.RPC.Workspaces.GetWorkspace(t.Context())
		if err != nil {
			t.Fatalf("Failed to get workspace: %v", err)
		}
		if workspace.Workspace == nil {
			t.Fatal("Expected non-nil workspace metadata")
		}
		if workspace.Workspace.ID == "" {
			t.Error("Expected workspace.ID to be non-empty")
		}
	})

	t.Run("should reject workspace file path traversal", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		for _, path := range []string{"../escaped.txt", "../../escaped.txt", "nested/../../../escaped.txt"} {
			_, err := session.RPC.Workspaces.CreateFile(t.Context(), &rpc.WorkspacesCreateFileRequest{
				Path:    path,
				Content: "should not land outside workspace",
			})
			if err == nil || !strings.Contains(strings.ToLower(err.Error()), "workspace files directory") {
				t.Fatalf("Expected CreateFile(%q) to reject traversal, got %v", path, err)
			}
			_, err = session.RPC.Workspaces.ReadFile(t.Context(), &rpc.WorkspacesReadFileRequest{Path: path})
			if err == nil || !strings.Contains(strings.ToLower(err.Error()), "workspace files directory") {
				t.Fatalf("Expected ReadFile(%q) to reject traversal, got %v", path, err)
			}
		}
	})

	t.Run("should create workspace file with nested path auto creating dirs", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		nestedPath := "nested-" + randomHex(t) + "/subdir/file.txt"
		if _, err := session.RPC.Workspaces.CreateFile(t.Context(), &rpc.WorkspacesCreateFileRequest{Path: nestedPath, Content: "nested content"}); err != nil {
			t.Fatalf("Failed to create nested workspace file: %v", err)
		}
		read, err := session.RPC.Workspaces.ReadFile(t.Context(), &rpc.WorkspacesReadFileRequest{Path: nestedPath})
		if err != nil {
			t.Fatalf("Failed to read nested workspace file: %v", err)
		}
		if read.Content != "nested content" {
			t.Fatalf("Expected nested content, got %q", read.Content)
		}
		list, err := session.RPC.Workspaces.ListFiles(t.Context())
		if err != nil {
			t.Fatalf("Failed to list files: %v", err)
		}
		found := false
		for _, file := range list.Files {
			if filepath.ToSlash(file) == nestedPath {
				found = true
				break
			}
		}
		if !found {
			t.Fatalf("Expected list to contain nested file %q, got %v", nestedPath, list.Files)
		}
	})

	t.Run("should report error reading nonexistent workspace file", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.RPC.Workspaces.ReadFile(t.Context(), &rpc.WorkspacesReadFileRequest{Path: "never-exists-" + randomHex(t) + ".txt"})
		if err == nil {
			t.Fatal("Expected reading nonexistent workspace file to fail")
		}
	})

	t.Run("should update existing workspace file with update operation", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		path := "reused-" + randomHex(t) + ".txt"
		if _, err := session.RPC.Workspaces.CreateFile(t.Context(), &rpc.WorkspacesCreateFileRequest{Path: path, Content: "v1"}); err != nil {
			t.Fatalf("Failed to create workspace file: %v", err)
		}
		awaitUpdate := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionWorkspaceFileChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionWorkspaceFileChangedData)
				return ok && data.Path == path && data.Operation == copilot.WorkspaceFileChangedOperationUpdate
			},
			"workspace_file_changed update event",
		)
		if _, err := session.RPC.Workspaces.CreateFile(t.Context(), &rpc.WorkspacesCreateFileRequest{Path: path, Content: "v2"}); err != nil {
			t.Fatalf("Failed to update workspace file: %v", err)
		}
		event := awaitEvent(t, awaitUpdate)
		if data := event.Data.(*copilot.SessionWorkspaceFileChangedData); data.Operation != copilot.WorkspaceFileChangedOperationUpdate {
			t.Fatalf("Expected update operation, got %+v", data)
		}
		read, err := session.RPC.Workspaces.ReadFile(t.Context(), &rpc.WorkspacesReadFileRequest{Path: path})
		if err != nil {
			t.Fatalf("Failed to read updated workspace file: %v", err)
		}
		if read.Content != "v2" {
			t.Fatalf("Expected updated content v2, got %q", read.Content)
		}
	})

	t.Run("should get and set session metadata", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: "SDK test session"}); err != nil {
			t.Fatalf("Failed to set session name: %v", err)
		}
		name, err := session.RPC.Name.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get session name: %v", err)
		}
		if name.Name == nil || *name.Name != "SDK test session" {
			t.Errorf("Expected session name 'SDK test session', got %v", name.Name)
		}

		sources, err := session.RPC.Instructions.GetSources(t.Context())
		if err != nil {
			t.Fatalf("Failed to get instruction sources: %v", err)
		}
		if sources.Sources == nil {
			t.Error("Expected instructions.Sources to be non-nil")
		}
	})

	t.Run("should reject empty or whitespace session name", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		for _, name := range []string{"", "   ", "\t\n  \r"} {
			_, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: name})
			if err == nil || !strings.Contains(strings.ToLower(err.Error()), "empty") {
				t.Fatalf("Expected setting whitespace name %q to fail with empty-name error, got %v", name, err)
			}
		}
	})

	t.Run("should emit title changed event each time name set is called", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		titleA := "Title-A-" + randomHex(t)
		awaitFirst := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionTitleChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionTitleChangedData)
				return ok && data.Title == titleA
			},
			"first title_changed event",
		)
		if _, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: titleA}); err != nil {
			t.Fatalf("Failed to set first session name: %v", err)
		}
		awaitEvent(t, awaitFirst)

		titleB := "Title-B-" + randomHex(t)
		awaitSecond := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionTitleChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionTitleChangedData)
				return ok && data.Title == titleB
			},
			"second title_changed event",
		)
		if _, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: titleB}); err != nil {
			t.Fatalf("Failed to set second session name: %v", err)
		}
		event := awaitEvent(t, awaitSecond)
		if data := event.Data.(*copilot.SessionTitleChangedData); data.Title != titleB {
			t.Fatalf("Expected title %q, got %+v", titleB, data)
		}
	})

	t.Run("should call metadata snapshot set working directory and record context change", func(t *testing.T) {
		firstDirectory := createUniqueRPCWorkDirectory(t, ctx, "rpc-session-state-first")
		secondDirectory := createUniqueRPCWorkDirectory(t, ctx, "rpc-session-state-second")
		contextDirectory := createUniqueRPCWorkDirectory(t, ctx, "rpc-session-state-context")
		branch := "rpc-context-" + randomHex(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "claude-sonnet-4.5",
			WorkingDirectory:    firstDirectory,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		initial, err := session.RPC.Metadata.Snapshot(t.Context())
		if err != nil {
			t.Fatalf("Metadata.Snapshot failed: %v", err)
		}
		if initial.SessionID != session.SessionID || initial.CurrentMode != rpc.MetadataSnapshotCurrentModeInteractive ||
			initial.SelectedModel == nil || *initial.SelectedModel != "claude-sonnet-4.5" ||
			initial.IsRemote || initial.AlreadyInUse || initial.StartTime.IsZero() || initial.ModifiedTime.IsZero() ||
			initial.Workspace == nil || initial.WorkspacePath == nil || *initial.WorkspacePath == "" {
			t.Fatalf("Unexpected initial metadata snapshot: %+v", initial)
		}
		assertRPCPathEqual(t, firstDirectory, initial.WorkingDirectory)

		setWorkingDirectory, err := session.RPC.Metadata.SetWorkingDirectory(t.Context(), &rpc.MetadataSetWorkingDirectoryRequest{WorkingDirectory: secondDirectory})
		if err != nil {
			t.Fatalf("Metadata.SetWorkingDirectory failed: %v", err)
		}
		assertRPCPathEqual(t, secondDirectory, setWorkingDirectory.WorkingDirectory)

		waitForRPCCondition(t, 15*time.Second, "metadata snapshot working directory update", func() (bool, error) {
			snapshot, err := session.RPC.Metadata.Snapshot(t.Context())
			return err == nil && rpcPathsEqual(secondDirectory, snapshot.WorkingDirectory), err
		})

		awaitContextChanged := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionContextChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionContextChangedData)
				return ok && data.Branch != nil && *data.Branch == branch
			},
			"session.context_changed event",
		)

		repo := "github/copilot-sdk-e2e"
		repoHost := "github.com"
		hostType := rpc.SessionWorkingDirectoryContextHostTypeGithub
		baseCommit := "0000000000000000000000000000000000000000"
		headCommit := "1111111111111111111111111111111111111111"
		if _, err := session.RPC.Metadata.RecordContextChange(t.Context(), &rpc.MetadataRecordContextChangeRequest{
			Context: rpc.SessionWorkingDirectoryContext{
				Cwd:            contextDirectory,
				GitRoot:        &firstDirectory,
				Branch:         &branch,
				Repository:     &repo,
				RepositoryHost: &repoHost,
				HostType:       &hostType,
				BaseCommit:     &baseCommit,
				HeadCommit:     &headCommit,
			},
		}); err != nil {
			t.Fatalf("Metadata.RecordContextChange failed: %v", err)
		}
		contextChanged := awaitEvent(t, awaitContextChanged)
		data := contextChanged.Data.(*copilot.SessionContextChangedData)
		assertRPCPathEqual(t, contextDirectory, data.Cwd)
		if data.GitRoot == nil {
			t.Fatal("Expected context changed git root")
		}
		assertRPCPathEqual(t, firstDirectory, *data.GitRoot)
		if data.Branch == nil || *data.Branch != branch ||
			data.Repository == nil || *data.Repository != repo ||
			data.RepositoryHost == nil || *data.RepositoryHost != repoHost ||
			data.HostType == nil || string(*data.HostType) != "github" ||
			data.BaseCommit == nil || *data.BaseCommit != baseCommit ||
			data.HeadCommit == nil || *data.HeadCommit != headCommit {
			t.Fatalf("Unexpected context changed payload: %+v", data)
		}
	})

	t.Run("should update options and initialize session services", func(t *testing.T) {
		initialDirectory := createUniqueRPCWorkDirectory(t, ctx, "rpc-session-state-initial")
		optionsDirectory := createUniqueRPCWorkDirectory(t, ctx, "rpc-session-state-options")
		featureName := "rpc-session-state-" + randomHex(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			WorkingDirectory:    initialDirectory,
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		update, err := session.RPC.Options.Update(t.Context(), &rpc.SessionUpdateOptionsParams{
			ClientName:       rpcPtr("go-sdk-rpc-session-state-e2e"),
			LspClientName:    rpcPtr("go-sdk-rpc-session-state-lsp"),
			IntegrationID:    rpcPtr("go-sdk-" + randomHex(t)),
			FeatureFlags:     map[string]bool{featureName: true},
			WorkingDirectory: &optionsDirectory,
			CoauthorEnabled:  rpcPtr(false),
			EnableStreaming:  rpcPtr(false),
			AskUserDisabled:  rpcPtr(true),
		})
		if err != nil {
			t.Fatalf("Options.Update failed: %v", err)
		}
		if !update.Success {
			t.Fatalf("Expected Options.Update Success=true, got %+v", update)
		}

		waitForRPCCondition(t, 15*time.Second, "options working directory to reach metadata snapshot", func() (bool, error) {
			snapshot, err := session.RPC.Metadata.Snapshot(t.Context())
			return err == nil && rpcPathsEqual(optionsDirectory, snapshot.WorkingDirectory), err
		})

		if _, err := session.RPC.Lsp.Initialize(t.Context(), &rpc.LspInitializeRequest{
			WorkingDirectory: &optionsDirectory,
			GitRoot:          &initialDirectory,
			Force:            rpcPtr(true),
		}); err != nil {
			t.Fatalf("Lsp.Initialize failed: %v", err)
		}
		if _, err := session.RPC.Telemetry.SetFeatureOverrides(t.Context(), &rpc.TelemetrySetFeatureOverridesRequest{
			Features: map[string]string{
				"rpc_session_state_feature": featureName,
				"rpc_session_state_value":   "enabled",
			},
		}); err != nil {
			t.Fatalf("Telemetry.SetFeatureOverrides failed: %v", err)
		}
		if _, err := session.RPC.Tools.InitializeAndValidate(t.Context()); err != nil {
			t.Fatalf("Tools.InitializeAndValidate failed: %v", err)
		}
		snapshot, err := session.RPC.Metadata.Snapshot(t.Context())
		if err != nil {
			t.Fatalf("Metadata.Snapshot after options update failed: %v", err)
		}
		assertRPCPathEqual(t, optionsDirectory, snapshot.WorkingDirectory)
	})

	t.Run("should set reasoning effort and auto name", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Model:               "claude-sonnet-4.5",
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		reasoning, err := session.RPC.Model.SetReasoningEffort(t.Context(), &rpc.ModelSetReasoningEffortRequest{ReasoningEffort: "high"})
		if err != nil {
			t.Fatalf("Model.SetReasoningEffort failed: %v", err)
		}
		if reasoning.ReasoningEffort != "high" {
			t.Fatalf("Expected reasoning effort high, got %+v", reasoning)
		}
		current, err := session.RPC.Model.GetCurrent(t.Context())
		if err != nil {
			t.Fatalf("Model.GetCurrent failed: %v", err)
		}
		if current.ModelID == nil || *current.ModelID != "claude-sonnet-4.5" ||
			current.ReasoningEffort == nil || *current.ReasoningEffort != "high" {
			t.Fatalf("Expected current model claude-sonnet-4.5/high, got %+v", current)
		}

		autoName := "Auto Session " + randomHex(t)
		awaitAutoTitle := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionTitleChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionTitleChangedData)
				return ok && data.Title == autoName
			},
			"session.title_changed after name.setAuto",
		)
		autoResult, err := session.RPC.Name.SetAuto(t.Context(), &rpc.NameSetAutoRequest{Summary: "  " + autoName + "  "})
		if err != nil {
			t.Fatalf("Name.SetAuto failed: %v", err)
		}
		if !autoResult.Applied {
			t.Fatalf("Expected first Name.SetAuto to apply, got %+v", autoResult)
		}
		awaitEvent(t, awaitAutoTitle)
		name, err := session.RPC.Name.Get(t.Context())
		if err != nil {
			t.Fatalf("Name.Get failed: %v", err)
		}
		if name.Name == nil || *name.Name != autoName {
			t.Fatalf("Expected auto name %q, got %+v", autoName, name)
		}

		explicitName := "Explicit Session " + randomHex(t)
		if _, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: explicitName}); err != nil {
			t.Fatalf("Name.Set explicit failed: %v", err)
		}
		ignoredAuto, err := session.RPC.Name.SetAuto(t.Context(), &rpc.NameSetAutoRequest{Summary: "Ignored " + randomHex(t)})
		if err != nil {
			t.Fatalf("Name.SetAuto after explicit name failed: %v", err)
		}
		if ignoredAuto.Applied {
			t.Fatal("Expected SetAuto to be ignored after explicit name")
		}
		name, err = session.RPC.Name.Get(t.Context())
		if err != nil {
			t.Fatalf("Name.Get explicit failed: %v", err)
		}
		if name.Name == nil || *name.Name != explicitName {
			t.Fatalf("Expected explicit name %q to remain, got %+v", explicitName, name)
		}
	})

	t.Run("should set auth credentials", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		login := "sdk-rpc-" + randomHex(t)

		api := ctx.ProxyURL
		telemetry := "https://localhost:1/telemetry"
		setCredentials, err := session.RPC.Auth.SetCredentials(t.Context(), &rpc.SessionSetCredentialsParams{
			Credentials: &rpc.UserAuthInfo{
				CopilotUser: &rpc.CopilotUserResponse{
					AnalyticsTrackingID: rpcPtr("rpc-session-state-tracking-id"),
					ChatEnabled:         rpcPtr(true),
					CopilotPlan:         rpcPtr("individual_pro"),
					Endpoints: &rpc.CopilotUserResponseEndpoints{
						API:       &api,
						Telemetry: &telemetry,
					},
					Login: &login,
				},
				Host:  "https://github.com",
				Login: login,
			},
		})
		if err != nil {
			t.Fatalf("Auth.SetCredentials failed: %v", err)
		}
		if !setCredentials.Success {
			t.Fatalf("Expected Auth.SetCredentials Success=true, got %+v", setCredentials)
		}

		status, err := session.RPC.Auth.GetStatus(t.Context())
		if err != nil {
			t.Fatalf("Auth.GetStatus failed: %v", err)
		}
		if !status.IsAuthenticated || status.AuthType == nil || *status.AuthType != rpc.AuthInfoTypeUser ||
			status.Host == nil || *status.Host != "https://github.com" ||
			status.Login == nil || *status.Login != login {
			t.Fatalf("Unexpected auth status after SetCredentials: %+v", status)
		}
	})

	t.Run("should report idle processing and context token shapes", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		processing, err := session.RPC.Metadata.IsProcessing(t.Context())
		if err != nil {
			t.Fatalf("Metadata.IsProcessing failed: %v", err)
		}
		if processing.Processing {
			t.Fatal("Expected fresh session to be idle")
		}

		model := "claude-sonnet-4.5"
		contextInfo, err := session.RPC.Metadata.ContextInfo(t.Context(), &rpc.MetadataContextInfoRequest{
			PromptTokenLimit: 128000,
			OutputTokenLimit: 4096,
			SelectedModel:    &model,
		})
		if err != nil {
			t.Fatalf("Metadata.ContextInfo failed: %v", err)
		}
		if contextInfo.ContextInfo != nil {
			info := contextInfo.ContextInfo
			if info.ModelName != model || info.PromptTokenLimit != 128000 || info.TotalTokens < 0 ||
				info.SystemTokens < 0 || info.ConversationTokens < 0 || info.ToolDefinitionsTokens < 0 {
				t.Fatalf("Unexpected context info: %+v", info)
			}
		}

		recomputed, err := session.RPC.Metadata.RecomputeContextTokens(t.Context(), &rpc.MetadataRecomputeContextTokensRequest{ModelID: model})
		if err != nil {
			t.Fatalf("Metadata.RecomputeContextTokens failed: %v", err)
		}
		if recomputed.SystemTokenCount < 0 || recomputed.MessagesTokenCount < 0 ||
			recomputed.TotalTokens != recomputed.SystemTokenCount+recomputed.MessagesTokenCount {
			t.Fatalf("Unexpected recomputed context tokens: %+v", recomputed)
		}
	})

	t.Run("should fork session with persisted messages", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		const sourcePrompt = "Say FORK_SOURCE_ALPHA exactly."
		const forkPrompt = "Now say FORK_CHILD_BETA exactly."

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		initialAnswer, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: sourcePrompt})
		if err != nil {
			t.Fatalf("Failed to send sourcePrompt: %v", err)
		}
		if assistant, ok := initialAnswer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "FORK_SOURCE_ALPHA") {
			t.Errorf("Expected initial answer to contain FORK_SOURCE_ALPHA, got %v", initialAnswer.Data)
		}

		sourceMessages, err := session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read source messages: %v", err)
		}
		sourceConversation := conversationMessages(sourceMessages)
		if !containsConversation(sourceConversation, "user", sourcePrompt, false) {
			t.Errorf("Expected source conversation to contain user message %q, got %v", sourcePrompt, sourceConversation)
		}
		if !containsConversation(sourceConversation, "assistant", "FORK_SOURCE_ALPHA", true) {
			t.Errorf("Expected source conversation to contain assistant text 'FORK_SOURCE_ALPHA', got %v", sourceConversation)
		}

		fork, err := client.RPC.Sessions.Fork(t.Context(), &rpc.SessionsForkRequest{SessionID: session.SessionID})
		if err != nil {
			t.Fatalf("Failed to fork session: %v", err)
		}
		if strings.TrimSpace(fork.SessionID) == "" {
			t.Fatal("Expected non-empty fork session id")
		}
		if fork.SessionID == session.SessionID {
			t.Errorf("Expected fork session id to differ from source %q", session.SessionID)
		}

		forkedSession, err := client.ResumeSession(t.Context(), fork.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume forked session: %v", err)
		}

		forkedMessages, err := forkedSession.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read forked messages: %v", err)
		}
		forkedConversation := conversationMessages(forkedMessages)
		if len(forkedConversation) < len(sourceConversation) {
			t.Fatalf("Expected forked conversation to include source conversation, got source=%v fork=%v", sourceConversation, forkedConversation)
		}
		for i := range sourceConversation {
			if forkedConversation[i] != sourceConversation[i] {
				t.Errorf("Forked conversation diverges at index %d: got %+v, expected %+v", i, forkedConversation[i], sourceConversation[i])
			}
		}

		forkAnswer, err := forkedSession.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: forkPrompt})
		if err != nil {
			t.Fatalf("Failed to send forkPrompt to fork: %v", err)
		}
		if assistant, ok := forkAnswer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistant.Content, "FORK_CHILD_BETA") {
			t.Errorf("Expected forked answer to contain FORK_CHILD_BETA, got %v", forkAnswer.Data)
		}

		sourceAfterFork, err := session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read source messages after fork: %v", err)
		}
		for _, m := range conversationMessages(sourceAfterFork) {
			if m.content == forkPrompt {
				t.Errorf("Source conversation should not contain fork prompt %q after fork", forkPrompt)
			}
		}

		forkAfterPrompt, err := forkedSession.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read forked messages after prompt: %v", err)
		}
		forkConv := conversationMessages(forkAfterPrompt)
		if !containsConversation(forkConv, "user", forkPrompt, false) {
			t.Errorf("Expected fork conversation to contain user prompt %q, got %v", forkPrompt, forkConv)
		}
		if !containsConversation(forkConv, "assistant", "FORK_CHILD_BETA", true) {
			t.Errorf("Expected fork conversation to contain assistant text 'FORK_CHILD_BETA', got %v", forkConv)
		}

		forkedSession.Disconnect()
	})

	t.Run("should handle forking session without persisted events", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer session.Disconnect()

		fork, err := client.RPC.Sessions.Fork(t.Context(), &rpc.SessionsForkRequest{SessionID: session.SessionID})
		if err != nil {
			errText := strings.ToLower(err.Error())
			if !strings.Contains(errText, "not found or has no persisted events") {
				t.Errorf("Expected error mentioning 'not found or has no persisted events', got %v", err)
			}
			if strings.Contains(errText, "unhandled method sessions.fork") {
				t.Errorf("sessions.fork should be implemented; error suggests it isn't: %v", err)
			}
			return
		}
		if fork == nil {
			t.Fatal("Expected non-nil fork result")
			return
		}
		if strings.TrimSpace(fork.SessionID) == "" {
			t.Fatal("Expected non-empty fork session id")
		}
		if fork.SessionID == session.SessionID {
			t.Errorf("Expected fork session id to differ from source %q", session.SessionID)
		}

		forkedSession, err := client.ResumeSession(t.Context(), fork.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume forked session: %v", err)
		}
		defer forkedSession.Disconnect()

		forkedMessages, err := forkedSession.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read forked messages: %v", err)
		}
		if forkedConversation := conversationMessages(forkedMessages); len(forkedConversation) != 0 {
			t.Errorf("Expected empty forked conversation, got %v", forkedConversation)
		}
	})

	t.Run("should fork session to event id excluding boundary event", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		const firstPrompt = "Say FORK_BOUNDARY_FIRST exactly."
		const secondPrompt = "Say FORK_BOUNDARY_SECOND exactly."

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer session.Disconnect()

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: firstPrompt}); err != nil {
			t.Fatalf("Failed to send first prompt: %v", err)
		}
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: secondPrompt}); err != nil {
			t.Fatalf("Failed to send second prompt: %v", err)
		}

		sourceEvents, err := session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read source messages: %v", err)
		}
		var secondUserEvent *copilot.SessionEvent
		for i := range sourceEvents {
			data, ok := sourceEvents[i].Data.(*copilot.UserMessageData)
			if ok && data.Content == secondPrompt {
				secondUserEvent = &sourceEvents[i]
				break
			}
		}
		if secondUserEvent == nil {
			t.Fatal("Expected the second user.message in persisted history")
			return
		}
		boundaryEventID := secondUserEvent.ID

		fork, err := client.RPC.Sessions.Fork(t.Context(), &rpc.SessionsForkRequest{
			SessionID: session.SessionID,
			ToEventID: &boundaryEventID,
		})
		if err != nil {
			t.Fatalf("Failed to fork session to event id: %v", err)
		}
		if strings.TrimSpace(fork.SessionID) == "" {
			t.Fatal("Expected non-empty fork session id")
		}
		if fork.SessionID == session.SessionID {
			t.Errorf("Expected fork session id to differ from source %q", session.SessionID)
		}

		forkedSession, err := client.ResumeSession(t.Context(), fork.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume forked session: %v", err)
		}
		defer forkedSession.Disconnect()

		forkedEvents, err := forkedSession.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("Failed to read forked messages: %v", err)
		}
		for _, event := range forkedEvents {
			if event.ID == boundaryEventID {
				t.Fatalf("toEventId is exclusive; boundary event %q must not be in forked session", boundaryEventID)
			}
		}
		forkedConversation := conversationMessages(forkedEvents)
		if !containsConversation(forkedConversation, "user", firstPrompt, false) {
			t.Errorf("Expected forked conversation to contain first prompt %q, got %v", firstPrompt, forkedConversation)
		}
		if containsConversation(forkedConversation, "user", secondPrompt, false) {
			t.Errorf("Expected forked conversation to exclude second prompt %q, got %v", secondPrompt, forkedConversation)
		}
	})

	t.Run("should report error when forking session to unknown event id", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		const sourcePrompt = "Say FORK_UNKNOWN_EVENT_OK exactly."

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		defer session.Disconnect()

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: sourcePrompt}); err != nil {
			t.Fatalf("Failed to send source prompt: %v", err)
		}

		bogusEventID := "00000000-0000-4000-8000-000000000000"
		_, err = client.RPC.Sessions.Fork(t.Context(), &rpc.SessionsForkRequest{
			SessionID: session.SessionID,
			ToEventID: &bogusEventID,
		})
		if err == nil {
			t.Fatal("Expected sessions.fork to fail for unknown event id")
		}
		if !strings.Contains(strings.ToLower(err.Error()), strings.ToLower("Event "+bogusEventID+" not found")) {
			t.Errorf("Expected error mentioning unknown event %q, got %v", bogusEventID, err)
		}
		if strings.Contains(strings.ToLower(err.Error()), "unhandled method sessions.fork") {
			t.Errorf("sessions.fork should be implemented; error suggests it isn't: %v", err)
		}
	})

	t.Run("should call session usage and permission rpcs", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		metrics, err := session.RPC.Usage.GetMetrics(t.Context())
		if err != nil {
			t.Fatalf("Failed to get usage metrics: %v", err)
		}
		if metrics.SessionStartTime.IsZero() {
			t.Errorf("Expected non-zero sessionStartTime, got %s", metrics.SessionStartTime)
		}
		if metrics.TotalNanoAiu != nil && *metrics.TotalNanoAiu < 0 {
			t.Errorf("Expected non-negative totalNanoAiu, got %f", *metrics.TotalNanoAiu)
		}
		for k, detail := range metrics.TokenDetails {
			if detail.TokenCount < 0 {
				t.Errorf("Expected non-negative tokenCount for %q, got %d", k, detail.TokenCount)
			}
		}
		for modelName, modelMetric := range metrics.ModelMetrics {
			if modelMetric.TotalNanoAiu != nil && *modelMetric.TotalNanoAiu < 0 {
				t.Errorf("Expected non-negative totalNanoAiu for model %q, got %f", modelName, *modelMetric.TotalNanoAiu)
			}
			for tokenType, detail := range modelMetric.TokenDetails {
				if detail.TokenCount < 0 {
					t.Errorf("Expected non-negative tokenCount for model %q type %q, got %d", modelName, tokenType, detail.TokenCount)
				}
			}
		}

		approve, err := session.RPC.Permissions.SetApproveAll(t.Context(), &rpc.PermissionsSetApproveAllRequest{Enabled: true})
		if err != nil {
			t.Fatalf("Failed to call SetApproveAll(true): %v", err)
		}
		if !approve.Success {
			t.Errorf("Expected SetApproveAll(true) to succeed, got %+v", approve)
		}

		reset, err := session.RPC.Permissions.ResetSessionApprovals(t.Context())
		if err != nil {
			t.Fatalf("Failed to call ResetSessionApprovals: %v", err)
		}
		if !reset.Success {
			t.Errorf("Expected ResetSessionApprovals to succeed, got %+v", reset)
		}

		// Restore.
		if _, err := session.RPC.Permissions.SetApproveAll(t.Context(), &rpc.PermissionsSetApproveAllRequest{Enabled: false}); err != nil {
			t.Errorf("Failed to restore SetApproveAll(false): %v", err)
		}
	})

	t.Run("should report implemented errors for unsupported session rpc paths", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.RPC.History.Truncate(t.Context(), &rpc.HistoryTruncateRequest{EventID: "missing-event"})
		if err == nil {
			t.Fatal("Expected History.Truncate with unknown event id to fail")
		}
		if strings.Contains(strings.ToLower(err.Error()), "unhandled method session.history.truncate") {
			t.Errorf("session.history.truncate should be implemented; error suggests it isn't: %v", err)
		}

		_, err = session.RPC.Mcp.Oauth().Login(t.Context(), &rpc.McpOauthLoginRequest{ServerName: "missing-server"})
		if err == nil {
			t.Fatal("Expected Mcp.Oauth.Login with unknown server to fail")
		}
		if strings.Contains(strings.ToLower(err.Error()), "unhandled method session.mcp.oauth.login") {
			t.Errorf("session.mcp.oauth.login should be implemented; error suggests it isn't: %v", err)
		}
	})

	t.Run("should compact session history after messages", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		result, err := session.RPC.History.Compact(t.Context())
		if err != nil {
			t.Fatalf("Failed to compact session: %v", err)
		}
		if result == nil {
			t.Fatal("Expected non-nil compaction result")
		}
	})
}

type roleContent struct {
	role    string
	content string
}

func conversationMessages(events []copilot.SessionEvent) []roleContent {
	var msgs []roleContent
	for _, evt := range events {
		switch d := evt.Data.(type) {
		case *copilot.UserMessageData:
			msgs = append(msgs, roleContent{role: "user", content: d.Content})
		case *copilot.AssistantMessageData:
			msgs = append(msgs, roleContent{role: "assistant", content: d.Content})
		}
	}
	return msgs
}

func containsConversation(msgs []roleContent, role, contentNeedle string, contains bool) bool {
	for _, m := range msgs {
		if m.role != role {
			continue
		}
		if contains {
			if strings.Contains(m.content, contentNeedle) {
				return true
			}
		} else if m.content == contentNeedle {
			return true
		}
	}
	return false
}
