package e2e

import (
	"strings"
	"testing"

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
		t.Skip("session.model.getCurrent not yet implemented in CLI")
	})

	t.Run("should call session rpc model switchTo", func(t *testing.T) {
		t.Skip("session.model.switchTo not yet implemented in CLI")
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

		sourceMessages, err := session.GetMessages(t.Context())
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

		forkedMessages, err := forkedSession.GetMessages(t.Context())
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

		sourceAfterFork, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to read source messages after fork: %v", err)
		}
		for _, m := range conversationMessages(sourceAfterFork) {
			if m.content == forkPrompt {
				t.Errorf("Source conversation should not contain fork prompt %q after fork", forkPrompt)
			}
		}

		forkAfterPrompt, err := forkedSession.GetMessages(t.Context())
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

	t.Run("should report error when forking session without persisted events", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = client.RPC.Sessions.Fork(t.Context(), &rpc.SessionsForkRequest{SessionID: session.SessionID})
		if err == nil {
			t.Fatal("Expected fork on empty session to fail")
		}
		if !strings.Contains(strings.ToLower(err.Error()), "not found or has no persisted events") {
			t.Errorf("Expected error mentioning 'not found or has no persisted events', got %v", err)
		}
		if strings.Contains(strings.ToLower(err.Error()), "unhandled method sessions.fork") {
			t.Errorf("sessions.fork should be implemented; error suggests it isn't: %v", err)
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

		sourceEvents, err := session.GetMessages(t.Context())
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

		forkedEvents, err := forkedSession.GetMessages(t.Context())
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
		if metrics.SessionStartTime <= 0 {
			t.Errorf("Expected positive sessionStartTime, got %d", metrics.SessionStartTime)
		}
		if metrics.TotalNanoAiu != nil && *metrics.TotalNanoAiu < 0 {
			t.Errorf("Expected non-negative totalNanoAiu, got %d", *metrics.TotalNanoAiu)
		}
		for k, detail := range metrics.TokenDetails {
			if detail.TokenCount < 0 {
				t.Errorf("Expected non-negative tokenCount for %q, got %d", k, detail.TokenCount)
			}
		}
		for modelName, modelMetric := range metrics.ModelMetrics {
			if modelMetric.TotalNanoAiu != nil && *modelMetric.TotalNanoAiu < 0 {
				t.Errorf("Expected non-negative totalNanoAiu for model %q, got %d", modelName, *modelMetric.TotalNanoAiu)
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

		_, err = session.RPC.Mcp.Oauth().Login(t.Context(), &rpc.MCPOauthLoginRequest{ServerName: "missing-server"})
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
