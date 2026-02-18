package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestPermissions(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("permission handler for write operations", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var permissionRequests []copilot.PermissionRequest
		var mu sync.Mutex

		onPermissionRequest := func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
			mu.Lock()
			permissionRequests = append(permissionRequests, request)
			mu.Unlock()

			if invocation.SessionID == "" {
				t.Error("Expected non-empty session ID in invocation")
			}

			return copilot.PermissionRequestResult{Kind: "approved"}, nil
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: onPermissionRequest,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		testFile := filepath.Join(ctx.WorkDir, "test.txt")
		err = os.WriteFile(testFile, []byte("original content"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Edit test.txt and replace 'original' with 'modified'",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		if len(permissionRequests) == 0 {
			t.Error("Expected at least one permission request")
		}
		writeCount := 0
		for _, req := range permissionRequests {
			if req.Kind == "write" {
				writeCount++
			}
		}
		mu.Unlock()

		if writeCount == 0 {
			t.Error("Expected at least one write permission request")
		}
	})

	t.Run("permission handler for shell commands", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var permissionRequests []copilot.PermissionRequest
		var mu sync.Mutex

		onPermissionRequest := func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
			mu.Lock()
			permissionRequests = append(permissionRequests, request)
			mu.Unlock()

			return copilot.PermissionRequestResult{Kind: "approved"}, nil
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: onPermissionRequest,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo hello' and tell me the output",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		shellCount := 0
		for _, req := range permissionRequests {
			if req.Kind == "shell" {
				shellCount++
			}
		}
		mu.Unlock()

		if shellCount == 0 {
			t.Error("Expected at least one shell permission request")
		}
	})

	t.Run("deny permission", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		onPermissionRequest := func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
			return copilot.PermissionRequestResult{Kind: "denied-interactively-by-user"}, nil
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: onPermissionRequest,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		testFile := filepath.Join(ctx.WorkDir, "protected.txt")
		originalContent := []byte("protected content")
		err = os.WriteFile(testFile, originalContent, 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Edit protected.txt and replace 'protected' with 'hacked'.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		_, err = testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get final message: %v", err)
		}

		// Verify the file was NOT modified
		content, err := os.ReadFile(testFile)
		if err != nil {
			t.Fatalf("Failed to read test file: %v", err)
		}

		if string(content) != string(originalContent) {
			t.Errorf("Expected file to remain unchanged after denied permission, got: %s", string(content))
		}
	})

	t.Run("should deny tool operations by default when no handler is provided", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var mu sync.Mutex
		permissionDenied := false

		session.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.ToolExecutionComplete &&
				event.Data.Success != nil && !*event.Data.Success &&
				event.Data.Error != nil && event.Data.Error.ErrorClass != nil &&
				strings.Contains(event.Data.Error.ErrorClass.Message, "Permission denied") {
				mu.Lock()
				permissionDenied = true
				mu.Unlock()
			}
		})

		if _, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'node --version'",
		}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		if !permissionDenied {
			t.Error("Expected a tool.execution_complete event with Permission denied result")
		}
	})

	t.Run("should deny tool operations by default when no handler is provided after resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID
		if _, err = session1.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		session2, err := client.ResumeSession(t.Context(), sessionID)
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		var mu sync.Mutex
		permissionDenied := false

		session2.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.ToolExecutionComplete &&
				event.Data.Success != nil && !*event.Data.Success &&
				event.Data.Error != nil && event.Data.Error.ErrorClass != nil &&
				strings.Contains(event.Data.Error.ErrorClass.Message, "Permission denied") {
				mu.Lock()
				permissionDenied = true
				mu.Unlock()
			}
		})

		if _, err = session2.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'node --version'",
		}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		if !permissionDenied {
			t.Error("Expected a tool.execution_complete event with Permission denied result")
		}
	})

	t.Run("without permission handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		message, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get final message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "4") {
			t.Errorf("Expected message to contain '4', got: %v", message.Data.Content)
		}
	})
}
