package e2e

import (
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const rpcEventLogTimeout = 30 * time.Second

// Mirrors dotnet/test/E2E/RpcEventLogE2ETests.cs (snapshot category "rpc_event_log").
func TestRpcEventLogE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should read persisted events from beginning", func(t *testing.T) {
		session := createEventLogSession(t, client)
		defer session.Disconnect()

		if _, err := session.RPC.Plan.Update(t.Context(), &rpc.PlanUpdateRequest{Content: "# Event log E2E plan\n- persisted event"}); err != nil {
			t.Fatalf("Plan.Update failed: %v", err)
		}

		var read *rpc.EventsReadResult
		waitForRPCCondition(t, rpcEventLogTimeout, "persisted session.plan_changed event", func() (bool, error) {
			var err error
			read, err = session.RPC.EventLog.Read(t.Context(), &rpc.EventLogReadRequest{
				Max:    rpcPtr(int64(100)),
				WaitMs: rpcPtr(int32(0)),
			})
			if err != nil {
				return false, err
			}
			for _, event := range read.Events {
				if data, ok := event.Data.(*copilot.SessionPlanChangedData); ok &&
					data.Operation == copilot.PlanChangedOperationCreate &&
					(event.Ephemeral == nil || !*event.Ephemeral) {
					return true, nil
				}
			}
			return false, nil
		})

		if read.CursorStatus != rpc.EventsCursorStatusOk {
			t.Fatalf("Expected cursor status ok, got %q", read.CursorStatus)
		}
		if read.Cursor == "" {
			t.Fatal("Expected non-empty cursor")
		}
	})

	t.Run("should return tail cursor and read empty when no new events", func(t *testing.T) {
		session := createEventLogSession(t, client)
		defer session.Disconnect()

		var tail *rpc.EventLogTailResult
		var read *rpc.EventsReadResult
		waitForRPCCondition(t, rpcEventLogTimeout, "stable empty event log tail", func() (bool, error) {
			var err error
			tail, err = session.RPC.EventLog.Tail(t.Context())
			if err != nil {
				return false, err
			}
			read, err = session.RPC.EventLog.Read(t.Context(), &rpc.EventLogReadRequest{
				Cursor: &tail.Cursor,
				Max:    rpcPtr(int64(10)),
				WaitMs: rpcPtr(int32(0)),
			})
			return err == nil && read.CursorStatus == rpc.EventsCursorStatusOk && len(read.Events) == 0, err
		})

		if tail.Cursor == "" {
			t.Fatal("Expected non-empty tail cursor")
		}
		if len(read.Events) != 0 {
			t.Fatalf("Expected no events after tail cursor, got %d", len(read.Events))
		}
		if read.HasMore {
			t.Fatal("Expected HasMore=false for empty read")
		}
	})

	t.Run("should register and release event interest idempotently", func(t *testing.T) {
		session := createEventLogSession(t, client)
		defer session.Disconnect()

		registered, err := session.RPC.EventLog.RegisterInterest(t.Context(), &rpc.RegisterEventInterestParams{
			EventType: string(copilot.SessionEventTypeSessionTitleChanged),
		})
		if err != nil {
			t.Fatalf("EventLog.RegisterInterest failed: %v", err)
		}
		if registered.Handle == "" {
			t.Fatal("Expected non-empty event interest handle")
		}

		released, err := session.RPC.EventLog.ReleaseInterest(t.Context(), &rpc.ReleaseEventInterestParams{Handle: registered.Handle})
		if err != nil {
			t.Fatalf("EventLog.ReleaseInterest failed: %v", err)
		}
		if !released.Success {
			t.Fatal("Expected first ReleaseInterest to succeed")
		}
		releasedAgain, err := session.RPC.EventLog.ReleaseInterest(t.Context(), &rpc.ReleaseEventInterestParams{Handle: registered.Handle})
		if err != nil {
			t.Fatalf("EventLog.ReleaseInterest second call failed: %v", err)
		}
		if !releasedAgain.Success {
			t.Fatal("Expected second ReleaseInterest to be idempotent")
		}
	})

	t.Run("should long poll with types filter for title changed event", func(t *testing.T) {
		session := createEventLogSession(t, client)
		defer session.Disconnect()

		var read *rpc.EventsReadResult
		var expectedTitle string
		waitForRPCCondition(t, rpcEventLogTimeout, "filtered session.title_changed event", func() (bool, error) {
			expectedTitle = "EventLogTitle-" + randomHex(t)
			tail, err := session.RPC.EventLog.Tail(t.Context())
			if err != nil {
				return false, err
			}
			resultCh := make(chan *rpc.EventsReadResult, 1)
			errCh := make(chan error, 1)
			go func() {
				result, err := session.RPC.EventLog.Read(t.Context(), &rpc.EventLogReadRequest{
					Cursor: &tail.Cursor,
					Max:    rpcPtr(int64(10)),
					WaitMs: rpcPtr(int32(5000)),
					Types:  &rpc.EventLogTypes{StringArray: []string{string(copilot.SessionEventTypeSessionTitleChanged)}},
				})
				if err != nil {
					errCh <- err
					return
				}
				resultCh <- result
			}()
			time.Sleep(100 * time.Millisecond)
			if _, err := session.RPC.Name.Set(t.Context(), &rpc.NameSetRequest{Name: expectedTitle}); err != nil {
				return false, err
			}
			select {
			case err := <-errCh:
				return false, err
			case read = <-resultCh:
			case <-time.After(6 * time.Second):
				return false, nil
			}
			for _, event := range read.Events {
				if event.Type() != copilot.SessionEventTypeSessionTitleChanged {
					return false, nil
				}
				if data, ok := event.Data.(*copilot.SessionTitleChangedData); ok && data.Title == expectedTitle {
					return true, nil
				}
			}
			return false, nil
		})

		if read.CursorStatus != rpc.EventsCursorStatusOk {
			t.Fatalf("Expected cursor status ok, got %q", read.CursorStatus)
		}
	})
}

func createEventLogSession(t *testing.T, client *copilot.Client) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return session
}
