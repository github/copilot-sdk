package e2e

import (
	"fmt"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const rpcEventSideEffectsTimeout = 30 * time.Second

// Mirrors dotnet/test/RpcEventSideEffectsE2ETests.cs (snapshot category "rpc_event_side_effects").
func TestRpcEventSideEffectsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should emit mode changed event when mode set", func(t *testing.T) {
		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		awaitModeChanged := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionModeChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionModeChangedData)
				return ok && data.NewMode == "plan" && data.PreviousMode == "interactive"
			},
			"session.mode_changed event for interactive to plan",
		)

		if _, err := session.RPC.Mode.Set(t.Context(), &rpc.ModeSetRequest{Mode: rpc.SessionModePlan}); err != nil {
			t.Fatalf("Failed to set mode to plan: %v", err)
		}

		evt := awaitEvent(t, awaitModeChanged)
		data := evt.Data.(*copilot.SessionModeChangedData)
		if data.NewMode != "plan" || data.PreviousMode != "interactive" {
			t.Fatalf("Unexpected mode change: %+v", data)
		}
	})

	t.Run("should emit plan changed event for update and delete", func(t *testing.T) {
		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		awaitCreate := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionPlanChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionPlanChangedData)
				return ok && data.Operation == copilot.PlanChangedOperationCreate
			},
			"session.plan_changed create event",
		)
		if _, err := session.RPC.Plan.Update(t.Context(), &rpc.PlanUpdateRequest{Content: "# Test plan\n- item"}); err != nil {
			t.Fatalf("Failed to update plan: %v", err)
		}
		if data := awaitEvent(t, awaitCreate).Data.(*copilot.SessionPlanChangedData); data.Operation != copilot.PlanChangedOperationCreate {
			t.Fatalf("Expected create operation, got %+v", data)
		}

		awaitDelete := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionPlanChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionPlanChangedData)
				return ok && data.Operation == copilot.PlanChangedOperationDelete
			},
			"session.plan_changed delete event",
		)
		if _, err := session.RPC.Plan.Delete(t.Context()); err != nil {
			t.Fatalf("Failed to delete plan: %v", err)
		}
		if data := awaitEvent(t, awaitDelete).Data.(*copilot.SessionPlanChangedData); data.Operation != copilot.PlanChangedOperationDelete {
			t.Fatalf("Expected delete operation, got %+v", data)
		}
	})

	t.Run("should emit plan changed update operation on second update", func(t *testing.T) {
		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		if _, err := session.RPC.Plan.Update(t.Context(), &rpc.PlanUpdateRequest{Content: "# initial"}); err != nil {
			t.Fatalf("Failed to create plan: %v", err)
		}

		awaitUpdate := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionPlanChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionPlanChangedData)
				return ok && data.Operation == copilot.PlanChangedOperationUpdate
			},
			"session.plan_changed update event",
		)
		if _, err := session.RPC.Plan.Update(t.Context(), &rpc.PlanUpdateRequest{Content: "# updated content"}); err != nil {
			t.Fatalf("Failed to update plan: %v", err)
		}
		if data := awaitEvent(t, awaitUpdate).Data.(*copilot.SessionPlanChangedData); data.Operation != copilot.PlanChangedOperationUpdate {
			t.Fatalf("Expected update operation, got %+v", data)
		}
	})

	t.Run("should emit workspace file changed event when file created", func(t *testing.T) {
		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		path := fmt.Sprintf("side-effect-%d.txt", time.Now().UnixNano())
		awaitChanged := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionWorkspaceFileChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionWorkspaceFileChangedData)
				return ok && data.Path == path
			},
			"session.workspace_file_changed event",
		)
		if _, err := session.RPC.Workspaces.CreateFile(t.Context(), &rpc.WorkspacesCreateFileRequest{Path: path, Content: "hello"}); err != nil {
			t.Fatalf("Failed to create workspace file: %v", err)
		}
		data := awaitEvent(t, awaitChanged).Data.(*copilot.SessionWorkspaceFileChangedData)
		if data.Path != path {
			t.Fatalf("Expected path %q, got %+v", path, data)
		}
		if data.Operation != copilot.WorkspaceFileChangedOperationCreate && data.Operation != copilot.WorkspaceFileChangedOperationUpdate {
			t.Fatalf("Unexpected workspace file operation: %+v", data)
		}
	})

	t.Run("should emit title changed event when name set", func(t *testing.T) {
		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		title := fmt.Sprintf("Renamed-%d", time.Now().UnixNano())
		awaitTitleChanged := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionTitleChanged,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionTitleChangedData)
				return ok && data.Title == title
			},
			"session.title_changed event",
		)
		if _, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: title}); err != nil {
			t.Fatalf("Failed to set session name: %v", err)
		}
		if data := awaitEvent(t, awaitTitleChanged).Data.(*copilot.SessionTitleChangedData); data.Title != title {
			t.Fatalf("Expected title %q, got %+v", title, data)
		}
	})

	t.Run("should emit snapshot rewind event and remove events on truncate", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say SNAPSHOT_REWIND_TARGET exactly."}); err != nil {
			t.Fatalf("Failed to create persisted message: %v", err)
		}

		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to read messages: %v", err)
		}
		userEvent := firstUserMessageEvent(messages)
		if userEvent == nil {
			t.Fatal("Expected at least one user.message in persisted history")
		}
		targetEventID := userEvent.ID

		awaitRewind := waitForMatchingEvent(
			session,
			copilot.SessionEventTypeSessionSnapshotRewind,
			func(event copilot.SessionEvent) bool {
				data, ok := event.Data.(*copilot.SessionSnapshotRewindData)
				return ok && strings.EqualFold(data.UpToEventID, targetEventID)
			},
			"session.snapshot_rewind event",
		)
		truncateResult, err := session.RPC.History.Truncate(t.Context(), &rpc.HistoryTruncateRequest{EventID: targetEventID})
		if err != nil {
			t.Fatalf("Failed to truncate history: %v", err)
		}
		if truncateResult.EventsRemoved < 1 {
			t.Fatalf("Expected truncate to remove at least one event, got %+v", truncateResult)
		}
		rewindData := awaitEvent(t, awaitRewind).Data.(*copilot.SessionSnapshotRewindData)
		if !strings.EqualFold(rewindData.UpToEventID, targetEventID) {
			t.Fatalf("Expected rewind to target %q, got %+v", targetEventID, rewindData)
		}
		if rewindData.EventsRemoved != float64(truncateResult.EventsRemoved) {
			t.Fatalf("Expected rewind count %d, got %+v", truncateResult.EventsRemoved, rewindData)
		}

		messagesAfter, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to read messages after truncate: %v", err)
		}
		for _, event := range messagesAfter {
			if event.ID == targetEventID {
				t.Fatalf("Expected truncated event %q to be removed", targetEventID)
			}
		}
	})

	t.Run("should allow session use after truncate", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session := createEventSideEffectsSession(t, client)
		defer session.Disconnect()

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say SNAPSHOT_REWIND_TARGET exactly."}); err != nil {
			t.Fatalf("Failed to create persisted message: %v", err)
		}

		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to read messages: %v", err)
		}
		userEvent := firstUserMessageEvent(messages)
		if userEvent == nil {
			t.Fatal("Expected at least one user.message in persisted history")
		}

		truncateResult, err := session.RPC.History.Truncate(t.Context(), &rpc.HistoryTruncateRequest{EventID: userEvent.ID})
		if err != nil {
			t.Fatalf("Failed to truncate history: %v", err)
		}
		if truncateResult.EventsRemoved < 1 {
			t.Fatalf("Expected truncate to remove at least one event, got %+v", truncateResult)
		}

		mode, err := session.RPC.Mode.Get(t.Context())
		if err != nil {
			t.Fatalf("Failed to get mode after truncate: %v", err)
		}
		if mode == nil || (*mode != rpc.SessionModeInteractive && *mode != rpc.SessionModePlan && *mode != rpc.SessionModeAutopilot) {
			t.Fatalf("Unexpected mode after truncate: %v", mode)
		}
		workspace, err := session.RPC.Workspaces.GetWorkspace(t.Context())
		if err != nil {
			t.Fatalf("Failed to get workspace after truncate: %v", err)
		}
		if workspace.Workspace == nil {
			t.Fatal("Expected workspace metadata after truncate")
		}
	})
}

func createEventSideEffectsSession(t *testing.T, client *copilot.Client) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create session: %v", err)
	}
	return session
}

func waitForMatchingEvent(session *copilot.Session, eventType copilot.SessionEventType, predicate func(copilot.SessionEvent) bool, description string) func() (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		if event.Type == eventType && predicate(event) {
			select {
			case result <- &event:
			default:
			}
		} else if event.Type == copilot.SessionEventTypeSessionError {
			msg := "session error"
			if data, ok := event.Data.(*copilot.SessionErrorData); ok {
				msg = data.Message
			}
			select {
			case errCh <- fmt.Errorf("%s while waiting for %s", msg, description):
			default:
			}
		}
	})

	return func() (*copilot.SessionEvent, error) {
		defer unsubscribe()
		select {
		case event := <-result:
			return event, nil
		case err := <-errCh:
			return nil, err
		case <-time.After(rpcEventSideEffectsTimeout):
			return nil, fmt.Errorf("timed out waiting for %s", description)
		}
	}
}

func awaitEvent(t *testing.T, await func() (*copilot.SessionEvent, error)) *copilot.SessionEvent {
	t.Helper()
	event, err := await()
	if err != nil {
		t.Fatal(err)
	}
	return event
}

func firstUserMessageEvent(events []copilot.SessionEvent) *copilot.SessionEvent {
	for i := range events {
		if _, ok := events[i].Data.(*copilot.UserMessageData); ok {
			return &events[i]
		}
	}
	return nil
}
