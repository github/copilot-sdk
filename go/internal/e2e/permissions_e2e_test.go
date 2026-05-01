package e2e

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestPermissionsE2E(t *testing.T) {
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

			return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindApproved}, nil
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

			return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindApproved}, nil
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
			return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindRejected}, nil
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

	t.Run("should deny tool operations when handler explicitly denies", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindUserNotAvailable}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var mu sync.Mutex
		permissionDenied := false

		session.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeToolExecutionComplete {
				if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok &&
					!d.Success &&
					d.Error != nil &&
					strings.Contains(d.Error.Message, "Permission denied") {
					mu.Lock()
					permissionDenied = true
					mu.Unlock()
				}
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

	t.Run("should deny tool operations when handler explicitly denies after resume", func(t *testing.T) {
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

		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindUserNotAvailable}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		var mu sync.Mutex
		permissionDenied := false

		session2.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeToolExecutionComplete {
				if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok &&
					!d.Success &&
					d.Error != nil &&
					strings.Contains(d.Error.Message, "Permission denied") {
					mu.Lock()
					permissionDenied = true
					mu.Unlock()
				}
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

	t.Run("should work with approve-all permission handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
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

		if md, ok := message.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(md.Content, "4") {
			var content string
			if ok {
				content = md.Content
			}
			t.Errorf("Expected message to contain '4', got: %v", content)
		}
	})

	t.Run("should handle async permission handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var permissionRequestReceived atomicBool
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				permissionRequestReceived.Set(true)
				// Simulate async work.
				time.Sleep(20 * time.Millisecond)
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindApproved}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo test' and tell me what happens",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !permissionRequestReceived.Get() {
			t.Error("Expected permission handler to have been invoked")
		}
	})

	t.Run("should resume session with permission handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session1.SessionID
		if _, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"}); err != nil {
			t.Fatalf("Initial SendAndWait failed: %v", err)
		}
		if err := session1.Disconnect(); err != nil {
			t.Fatalf("Disconnect failed: %v", err)
		}

		var permissionRequestReceived atomicBool
		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				permissionRequestReceived.Set(true)
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindApproved}, nil
			},
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}

		_, err = session2.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo resumed' for me",
		})
		if err != nil {
			t.Fatalf("SendAndWait (after resume) failed: %v", err)
		}
		if !permissionRequestReceived.Get() {
			t.Error("Expected permission handler from ResumeSessionConfig to have been invoked")
		}
	})

	t.Run("should handle permission handler errors gracefully", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				return copilot.PermissionRequestResult{}, fmt.Errorf("handler error")
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo test'. If you can't, say 'failed'.",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		ad, ok := message.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected *AssistantMessageData, got %T", message.Data)
		}
		content := strings.ToLower(ad.Content)
		matched := false
		for _, keyword := range []string{"fail", "cannot", "unable", "permission"} {
			if strings.Contains(content, keyword) {
				matched = true
				break
			}
		}
		if !matched {
			t.Errorf("Expected response to indicate failure (fail/cannot/unable/permission), got %q", ad.Content)
		}
	})

	t.Run("should receive toolCallId in permission requests", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var receivedToolCallID atomicBool
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				if req.Kind == copilot.PermissionRequestKindShell && req.ToolCallID != nil && *req.ToolCallID != "" {
					receivedToolCallID.Set(true)
				}
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindApproved}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Run 'echo test'"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !receivedToolCallID.Get() {
			t.Error("Expected ToolCallID to be populated on shell permission request")
		}
	})
}

// atomicBool is a tiny helper for concurrent flag updates in handler callbacks.
type atomicBool struct {
	mu sync.Mutex
	v  bool
}

func (a *atomicBool) Set(v bool) {
	a.mu.Lock()
	a.v = v
	a.mu.Unlock()
}

func (a *atomicBool) Get() bool {
	a.mu.Lock()
	defer a.mu.Unlock()
	return a.v
}
