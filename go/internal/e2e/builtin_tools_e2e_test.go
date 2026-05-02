package e2e

import (
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestBuiltinToolsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should capture exit code in output", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo hello && echo world'. Tell me the exact output.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		content := assistantContent(t, msg)
		if !strings.Contains(content, "hello") || !strings.Contains(content, "world") {
			t.Fatalf("Expected output to contain hello and world, got %q", content)
		}
	})

	t.Run("should capture stderr output", func(t *testing.T) {
		if runtime.GOOS == "windows" {
			t.Skip("stderr prompt uses bash syntax")
		}

		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo error_msg >&2; echo ok' and tell me what stderr said. Reply with just the stderr content.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		if content := assistantContent(t, msg); !strings.Contains(content, "error_msg") {
			t.Fatalf("Expected stderr response to contain error_msg, got %q", content)
		}
	})

	t.Run("should read file with line range", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "lines.txt"), []byte("line1\nline2\nline3\nline4\nline5\n"), 0644); err != nil {
			t.Fatalf("Failed to write lines.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read lines 2 through 4 of the file 'lines.txt' in this directory. Tell me what those lines contain.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		content := assistantContent(t, msg)
		if !strings.Contains(content, "line2") || !strings.Contains(content, "line4") {
			t.Fatalf("Expected response to contain line2 and line4, got %q", content)
		}
	})

	t.Run("should handle nonexistent file gracefully", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Try to read the file 'does_not_exist.txt'. If it doesn't exist, say 'FILE_NOT_FOUND'.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		content := strings.ToUpper(assistantContent(t, msg))
		if !strings.Contains(content, "NOT FOUND") &&
			!strings.Contains(content, "NOT EXIST") &&
			!strings.Contains(content, "NO SUCH") &&
			!strings.Contains(content, "FILE_NOT_FOUND") &&
			!strings.Contains(content, "DOES NOT EXIST") &&
			!strings.Contains(content, "ERROR") {
			t.Fatalf("Expected a not-found style response, got %q", content)
		}
	})

	t.Run("should edit a file successfully", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "edit_me.txt"), []byte("Hello World\nGoodbye World\n"), 0644); err != nil {
			t.Fatalf("Failed to write edit_me.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Edit the file 'edit_me.txt': replace 'Hello World' with 'Hi Universe'. Then read it back and tell me its contents.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		if content := assistantContent(t, msg); !strings.Contains(content, "Hi Universe") {
			t.Fatalf("Expected response to contain Hi Universe, got %q", content)
		}
	})

	t.Run("should create a new file", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Create a file called 'new_file.txt' with the content 'Created by test'. Then read it back to confirm.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		if content := assistantContent(t, msg); !strings.Contains(content, "Created by test") {
			t.Fatalf("Expected response to contain Created by test, got %q", content)
		}
	})

	t.Run("should search for patterns in files", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "data.txt"), []byte("apple\nbanana\napricot\ncherry\n"), 0644); err != nil {
			t.Fatalf("Failed to write data.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Search for lines starting with 'ap' in the file 'data.txt'. Tell me which lines matched.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		content := assistantContent(t, msg)
		if !strings.Contains(content, "apple") || !strings.Contains(content, "apricot") {
			t.Fatalf("Expected response to contain apple and apricot, got %q", content)
		}
	})

	t.Run("should find files by pattern", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.MkdirAll(filepath.Join(ctx.WorkDir, "src"), 0755); err != nil {
			t.Fatalf("Failed to create src directory: %v", err)
		}
		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "src", "index.ts"), []byte("export const index = 1;"), 0644); err != nil {
			t.Fatalf("Failed to write index.ts: %v", err)
		}
		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "README.md"), []byte("# Readme"), 0644); err != nil {
			t.Fatalf("Failed to write README.md: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Find all .ts files in this directory (recursively). List the filenames you found.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		if content := assistantContent(t, msg); !strings.Contains(content, "index.ts") {
			t.Fatalf("Expected response to contain index.ts, got %q", content)
		}
	})
}

func assistantContent(t *testing.T, event *copilot.SessionEvent) string {
	t.Helper()

	if event == nil {
		t.Fatal("Expected assistant message, got nil")
	}
	data, ok := event.Data.(*copilot.AssistantMessageData)
	if !ok {
		t.Fatalf("Expected AssistantMessageData, got %T", event.Data)
	}
	return data.Content
}
