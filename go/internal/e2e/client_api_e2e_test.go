package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Mirrors dotnet/test/ClientSessionManagementTests.cs (snapshot category "client_api").
func TestClientApiE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should delete session by id", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session.SessionID

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}

		if err := client.DeleteSession(t.Context(), sessionID); err != nil {
			t.Fatalf("Failed to delete session: %v", err)
		}

		metadata, err := client.GetSessionMetadata(t.Context(), sessionID)
		if err != nil {
			t.Fatalf("Failed to query session metadata: %v", err)
		}
		if metadata != nil {
			t.Errorf("Expected metadata to be nil after delete, got %+v", metadata)
		}
	})

	t.Run("should report error when deleting unknown session id", func(t *testing.T) {
		sessionID := "00000000-0000-0000-0000-000000000000"
		err := client.DeleteSession(t.Context(), sessionID)
		if err == nil {
			t.Fatal("Expected DeleteSession to fail for unknown id")
		}
		expectedMessage := "failed to delete session " + sessionID
		if !strings.Contains(strings.ToLower(err.Error()), expectedMessage) {
			t.Errorf("Expected error mentioning %q, got %v", expectedMessage, err)
		}
	})

	t.Run("should get null last session id before any sessions exist", func(t *testing.T) {
		// Use a fresh client with isolated COPILOT_HOME so other subtests don't pollute state.
		freshCtx := testharness.NewTestContext(t)
		freshClient := freshCtx.NewClient()
		t.Cleanup(func() { freshClient.ForceStop() })

		if err := freshClient.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start fresh client: %v", err)
		}

		result, err := freshClient.GetLastSessionID(t.Context())
		if err != nil {
			t.Fatalf("Failed to get last session id: %v", err)
		}
		if result != nil {
			t.Errorf("Expected nil last session id on fresh client, got %q", *result)
		}
	})

	t.Run("should track last session id after session created", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session.SessionID

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say OK."}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}

		lastID, err := client.GetLastSessionID(t.Context())
		if err != nil {
			t.Fatalf("Failed to get last session id: %v", err)
		}
		if lastID == nil || *lastID != sessionID {
			got := "<nil>"
			if lastID != nil {
				got = *lastID
			}
			t.Errorf("Expected last session id %q, got %q", sessionID, got)
		}
	})

	t.Run("should get null foreground session id in headless mode", func(t *testing.T) {
		sessionID, err := client.GetForegroundSessionID(t.Context())
		if err != nil {
			t.Fatalf("Failed to get foreground session id: %v", err)
		}
		if sessionID != nil {
			t.Errorf("Expected nil foreground session id in headless mode, got %q", *sessionID)
		}
	})

	t.Run("should report error when setting foreground session in headless mode", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { session.Disconnect() })

		err = client.SetForegroundSessionID(t.Context(), session.SessionID)
		if err == nil {
			t.Fatal("Expected SetForegroundSessionID to fail in headless mode")
		}
		if !strings.Contains(err.Error(), "Not running in TUI+server mode") {
			t.Errorf("Expected error mentioning 'Not running in TUI+server mode', got %v", err)
		}
	})
}
