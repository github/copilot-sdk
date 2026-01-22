package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
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

		session, err := client.CreateSession(&copilot.SessionConfig{
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

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Edit test.txt and replace 'original' with 'modified'",
		}, 60*time.Second)
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

		session, err := client.CreateSession(&copilot.SessionConfig{
			OnPermissionRequest: onPermissionRequest,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Run 'echo hello' and tell me the output",
		}, 60*time.Second)
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

		session, err := client.CreateSession(&copilot.SessionConfig{
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

		_, err = session.Send(copilot.MessageOptions{
			Prompt: "Edit protected.txt and replace 'protected' with 'hacked'.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		_, err = testharness.GetFinalAssistantMessage(session, 60*time.Second)
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

	t.Run("without permission handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		message, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get final message: %v", err)
		}

		if message.Data.Content == nil || !strings.Contains(*message.Data.Content, "4") {
			t.Errorf("Expected message to contain '4', got: %v", message.Data.Content)
		}
	})
}
