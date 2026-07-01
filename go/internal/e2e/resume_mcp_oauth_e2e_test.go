package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestResumeMCPOAuthE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should resume a persisted session with mcp auth handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		mcpAuthHandler := func(copilot.MCPAuthRequest, copilot.MCPAuthInvocation) (*copilot.MCPAuthResult, error) {
			return copilot.MCPAuthResultCancelled(), nil
		}

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnMCPAuthRequest:    mcpAuthHandler,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		_, err = session1.Send(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session1)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}
		if ad, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(ad.Content, "2") {
			t.Errorf("Expected answer to contain '2', got %v", answer.Data)
		}

		newClient := ctx.NewClient()
		t.Cleanup(func() { newClient.ForceStop() })

		session2, err := newClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnMCPAuthRequest:    mcpAuthHandler,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected resumed session ID to match, got %q vs %q", session2.SessionID, sessionID)
		}
	})
}
