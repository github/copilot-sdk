package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestMultiTurnE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should use tool results from previous turns", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "secret.txt"), []byte("The magic number is 42."), 0644); err != nil {
			t.Fatalf("Failed to write secret.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg1, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'secret.txt' and tell me what the magic number is.",
		})
		if err != nil {
			t.Fatalf("First SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg1); !strings.Contains(content, "42") {
			t.Fatalf("Expected first response to contain 42, got %q", content)
		}

		msg2, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "What is that magic number multiplied by 2?",
		})
		if err != nil {
			t.Fatalf("Second SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg2); !strings.Contains(content, "84") {
			t.Fatalf("Expected second response to contain 84, got %q", content)
		}
	})

	t.Run("should handle file creation then reading across turns", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'.",
		}); err != nil {
			t.Fatalf("First SendAndWait failed: %v", err)
		}

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'greeting.txt' and tell me its exact contents.",
		})
		if err != nil {
			t.Fatalf("Second SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg); !strings.Contains(content, "Hello from multi-turn test") {
			t.Fatalf("Expected response to contain created file contents, got %q", content)
		}
	})
}
