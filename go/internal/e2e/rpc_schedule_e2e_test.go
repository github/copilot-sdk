package e2e

import (
	"math"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/E2E/RpcScheduleE2ETests.cs (snapshot category "rpc_schedule").
func TestRpcScheduleE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should list no schedules for fresh session", func(t *testing.T) {
		session := createScheduleSession(t, client)
		defer session.Disconnect()

		result, err := session.RPC.Schedule.List(t.Context())
		if err != nil {
			t.Fatalf("Schedule.List failed: %v", err)
		}
		if result.Entries == nil {
			t.Fatal("Expected non-nil schedule Entries")
		}
		if len(result.Entries) != 0 {
			t.Fatalf("Expected no schedules for a fresh session, got %+v", result.Entries)
		}
	})

	t.Run("should return nil entry when stopping unknown schedule", func(t *testing.T) {
		session := createScheduleSession(t, client)
		defer session.Disconnect()

		result, err := session.RPC.Schedule.Stop(t.Context(), &rpc.ScheduleStopRequest{ID: math.MaxInt64})
		if err != nil {
			t.Fatalf("Schedule.Stop failed: %v", err)
		}
		if result.Entry != nil {
			t.Fatalf("Expected nil entry for unknown schedule, got %+v", result.Entry)
		}
		list, err := session.RPC.Schedule.List(t.Context())
		if err != nil {
			t.Fatalf("Schedule.List after Stop failed: %v", err)
		}
		if len(list.Entries) != 0 {
			t.Fatalf("Expected no schedules after stopping unknown schedule, got %+v", list.Entries)
		}
	})
}

func createScheduleSession(t *testing.T, client *copilot.Client) *copilot.Session {
	t.Helper()
	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return session
}
