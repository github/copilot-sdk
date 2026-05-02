package e2e

import (
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Mirrors dotnet/test/ClientLifecycleTests.cs.
func TestClientLifecycleE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	t.Run("should receive session created lifecycle event", func(t *testing.T) {
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		created := make(chan copilot.SessionLifecycleEvent, 4)
		unsubscribe := client.On(func(event copilot.SessionLifecycleEvent) {
			if event.Type == copilot.SessionLifecycleCreated {
				select {
				case created <- event:
				default:
				}
			}
		})
		defer unsubscribe()

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		select {
		case evt := <-created:
			if evt.Type != copilot.SessionLifecycleCreated {
				t.Errorf("Expected event type %q, got %q", copilot.SessionLifecycleCreated, evt.Type)
			}
			if evt.SessionID != session.SessionID {
				t.Errorf("Expected session id %q, got %q", session.SessionID, evt.SessionID)
			}
		case <-time.After(10 * time.Second):
			t.Fatal("Timed out waiting for session.created lifecycle event")
		}
	})

	t.Run("should filter session lifecycle events by type", func(t *testing.T) {
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		created := make(chan copilot.SessionLifecycleEvent, 4)
		unsubscribe := client.OnEventType(copilot.SessionLifecycleCreated, func(event copilot.SessionLifecycleEvent) {
			select {
			case created <- event:
			default:
			}
		})
		defer unsubscribe()

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		select {
		case evt := <-created:
			if evt.Type != copilot.SessionLifecycleCreated {
				t.Errorf("Expected event type %q, got %q", copilot.SessionLifecycleCreated, evt.Type)
			}
			if evt.SessionID != session.SessionID {
				t.Errorf("Expected session id %q, got %q", session.SessionID, evt.SessionID)
			}
		case <-time.After(10 * time.Second):
			t.Fatal("Timed out waiting for filtered session.created lifecycle event")
		}
	})

	t.Run("disposing lifecycle subscription stops receiving events", func(t *testing.T) {
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		var disposedCount int64
		unsubscribeFirst := client.On(func(event copilot.SessionLifecycleEvent) {
			atomic.AddInt64(&disposedCount, 1)
		})
		// Dispose before any session is created — should never be invoked.
		unsubscribeFirst()

		created := make(chan copilot.SessionLifecycleEvent, 4)
		unsubscribeActive := client.OnEventType(copilot.SessionLifecycleCreated, func(event copilot.SessionLifecycleEvent) {
			select {
			case created <- event:
			default:
			}
		})
		defer unsubscribeActive()

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		select {
		case evt := <-created:
			if evt.SessionID != session.SessionID {
				t.Errorf("Expected session id %q, got %q", session.SessionID, evt.SessionID)
			}
		case <-time.After(10 * time.Second):
			t.Fatal("Timed out waiting for active subscription to receive event")
		}

		if got := atomic.LoadInt64(&disposedCount); got != 0 {
			t.Errorf("Expected disposed subscription to receive 0 events, got %d", got)
		}
	})

	t.Run("stop disconnects client", func(t *testing.T) {
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}
		if client.State() != copilot.StateConnected {
			t.Errorf("Expected state to be connected after Start, got %q", client.State())
		}

		if err := client.Stop(); err != nil {
			t.Fatalf("Failed to stop client: %v", err)
		}
		if client.State() != copilot.StateDisconnected {
			t.Errorf("Expected state to be disconnected after Stop, got %q", client.State())
		}
	})

	t.Run("force stop disconnects client", func(t *testing.T) {
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client: %v", err)
		}
		if client.State() != copilot.StateConnected {
			t.Errorf("Expected state to be connected after Start, got %q", client.State())
		}

		client.ForceStop()
		if client.State() != copilot.StateDisconnected {
			t.Errorf("Expected state to be disconnected after ForceStop, got %q", client.State())
		}
	})
}
