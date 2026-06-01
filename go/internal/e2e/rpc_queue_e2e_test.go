package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/E2E/RpcQueueE2ETests.cs (snapshot category "rpc_queue").
func TestRPCQueueE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("fresh queue is empty and empty mutations are noops", func(t *testing.T) {
		session := createQueueSession(t, client)
		defer session.Disconnect()

		assertQueueEmpty(t, session)

		remove, err := session.RPC.Queue.RemoveMostRecent(t.Context())
		if err != nil {
			t.Fatalf("Queue.RemoveMostRecent failed: %v", err)
		}
		if remove.Removed {
			t.Fatal("Expected RemoveMostRecent Removed=false on empty queue")
		}
		assertQueueEmpty(t, session)

		if _, err := session.RPC.Queue.Clear(t.Context()); err != nil {
			t.Fatalf("Queue.Clear failed: %v", err)
		}
		assertQueueEmpty(t, session)
	})

	t.Run("pending items reports queued command and remove and clear update queue", func(t *testing.T) {
		session := createQueueSession(t, client)
		defer session.Disconnect()

		interest, err := session.RPC.EventLog.RegisterInterest(t.Context(), &rpc.RegisterEventInterestParams{EventType: string(copilot.SessionEventTypeCommandQueued)})
		if err != nil {
			t.Fatalf("EventLog.RegisterInterest failed: %v", err)
		}
		defer func() {
			_, _ = session.RPC.EventLog.ReleaseInterest(t.Context(), &rpc.ReleaseEventInterestParams{Handle: interest.Handle})
			_, _ = session.RPC.Queue.Clear(t.Context())
		}()

		firstCommand := "/sdk-queue-first-" + randomHex(t)
		secondCommand := "/sdk-queue-second-" + randomHex(t)
		thirdCommand := "/sdk-queue-third-" + randomHex(t)
		firstQueued := make(chan *copilot.CommandQueuedData, 1)
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			data, ok := event.Data.(*copilot.CommandQueuedData)
			if ok && data.Command == firstCommand {
				select {
				case firstQueued <- data:
				default:
				}
			}
		})
		defer unsubscribe()

		first, err := session.RPC.Commands.Enqueue(t.Context(), &rpc.EnqueueCommandParams{Command: firstCommand})
		if err != nil {
			t.Fatalf("Commands.Enqueue(first) failed: %v", err)
		}
		if !first.Queued {
			t.Fatal("Expected first command to be queued")
		}

		var firstEvent *copilot.CommandQueuedData
		select {
		case firstEvent = <-firstQueued:
		case <-time.After(30 * time.Second):
			t.Fatalf("Timed out waiting for first command.queued event")
		}

		second, err := session.RPC.Commands.Enqueue(t.Context(), &rpc.EnqueueCommandParams{Command: secondCommand})
		if err != nil {
			t.Fatalf("Commands.Enqueue(second) failed: %v", err)
		}
		if !second.Queued {
			t.Fatal("Expected second command to be queued")
		}
		waitForCommandInPendingItems(t, session, secondCommand)

		remove, err := session.RPC.Queue.RemoveMostRecent(t.Context())
		if err != nil {
			t.Fatalf("Queue.RemoveMostRecent failed: %v", err)
		}
		if !remove.Removed {
			t.Fatal("Expected RemoveMostRecent to remove second queued command")
		}
		waitForCommandNotInPendingItems(t, session, secondCommand)

		third, err := session.RPC.Commands.Enqueue(t.Context(), &rpc.EnqueueCommandParams{Command: thirdCommand})
		if err != nil {
			t.Fatalf("Commands.Enqueue(third) failed: %v", err)
		}
		if !third.Queued {
			t.Fatal("Expected third command to be queued")
		}
		waitForCommandInPendingItems(t, session, thirdCommand)

		if _, err := session.RPC.Queue.Clear(t.Context()); err != nil {
			t.Fatalf("Queue.Clear failed: %v", err)
		}
		waitForCommandNotInPendingItems(t, session, thirdCommand)

		stop := true
		completed, err := session.RPC.Commands.RespondToQueuedCommand(t.Context(), &rpc.CommandsRespondToQueuedCommandRequest{
			RequestID: firstEvent.RequestID,
			Result:    rpc.QueuedCommandHandled{StopProcessingQueue: &stop},
		})
		if err != nil {
			t.Fatalf("Commands.RespondToQueuedCommand failed: %v", err)
		}
		if !completed.Success {
			t.Fatal("Expected response to first queued command to succeed")
		}
		waitForQueueEmpty(t, session)
	})
}

func createQueueSession(t *testing.T, client *copilot.Client) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return session
}

func assertQueueEmpty(t *testing.T, session *copilot.Session) {
	t.Helper()
	pending, err := session.RPC.Queue.PendingItems(t.Context())
	if err != nil {
		t.Fatalf("Queue.PendingItems failed: %v", err)
	}
	if len(pending.Items) != 0 || len(pending.SteeringMessages) != 0 {
		t.Fatalf("Expected empty queue, got %+v", pending)
	}
}

func waitForCommandInPendingItems(t *testing.T, session *copilot.Session, command string) {
	t.Helper()
	var matched *rpc.QueuePendingItems
	waitForRPCCondition(t, 30*time.Second, "queued command "+command+" to appear", func() (bool, error) {
		pending, err := session.RPC.Queue.PendingItems(t.Context())
		if err != nil {
			return false, err
		}
		for i := range pending.Items {
			if isPendingCommand(pending.Items[i], command) {
				matched = &pending.Items[i]
				return true, nil
			}
		}
		return false, nil
	})
	if matched.Kind != rpc.QueuePendingItemsKindCommand {
		t.Fatalf("Expected command pending item, got %+v", matched)
	}
	if !strings.Contains(matched.DisplayText, strings.TrimPrefix(command, "/")) && matched.DisplayText != command {
		t.Fatalf("Expected pending item display text to include %q, got %q", command, matched.DisplayText)
	}
}

func waitForCommandNotInPendingItems(t *testing.T, session *copilot.Session, command string) {
	t.Helper()
	waitForRPCCondition(t, 30*time.Second, "queued command "+command+" to leave queue", func() (bool, error) {
		pending, err := session.RPC.Queue.PendingItems(t.Context())
		if err != nil {
			return false, err
		}
		for _, item := range pending.Items {
			if isPendingCommand(item, command) {
				return false, nil
			}
		}
		return true, nil
	})
}

func waitForQueueEmpty(t *testing.T, session *copilot.Session) {
	t.Helper()
	waitForRPCCondition(t, 30*time.Second, "queue to empty", func() (bool, error) {
		pending, err := session.RPC.Queue.PendingItems(t.Context())
		return err == nil && len(pending.Items) == 0 && len(pending.SteeringMessages) == 0, err
	})
	assertQueueEmpty(t, session)
}

func isPendingCommand(item rpc.QueuePendingItems, command string) bool {
	return item.Kind == rpc.QueuePendingItemsKindCommand &&
		(item.DisplayText == command || strings.Contains(item.DisplayText, strings.TrimPrefix(command, "/")))
}
