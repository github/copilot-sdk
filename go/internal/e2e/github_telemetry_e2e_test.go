package e2e

import (
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestGitHubTelemetryE2E(t *testing.T) {
	t.Run("should forward github telemetry for a live session", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)

		var mu sync.Mutex
		var notifications []*rpc.GitHubTelemetryNotification
		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			opts.OnGitHubTelemetry = func(notification *rpc.GitHubTelemetryNotification) {
				mu.Lock()
				notifications = append(notifications, notification)
				mu.Unlock()
			}
		})
		t.Cleanup(func() { client.ForceStop() })

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		notification := waitForGitHubTelemetryNotification(t, &mu, &notifications, 30*time.Second)
		if notification.SessionID == "" {
			t.Fatal("Expected a non-empty SessionID")
		}
		if notification.Event.Kind == "" {
			t.Fatal("Expected a non-empty Event.Kind")
		}
	})
}

func waitForGitHubTelemetryNotification(t *testing.T, mu *sync.Mutex, notifications *[]*rpc.GitHubTelemetryNotification, timeout time.Duration) *rpc.GitHubTelemetryNotification {
	t.Helper()

	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		mu.Lock()
		if len(*notifications) > 0 {
			notification := (*notifications)[0]
			mu.Unlock()
			if notification != nil {
				return notification
			}
			t.Fatal("Received nil GitHub telemetry notification")
		}
		mu.Unlock()

		time.Sleep(50 * time.Millisecond)
	}

	t.Fatalf("Timed out waiting for GitHub telemetry notification after %s", timeout)
	return nil
}
