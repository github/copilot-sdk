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
	"github.com/github/copilot-sdk/go/rpc"
)

func TestPermissionsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("permission handler for write operations", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var permissionRequests []copilot.PermissionRequest
		var mu sync.Mutex

		onPermissionRequest := func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
			mu.Lock()
			permissionRequests = append(permissionRequests, request)
			mu.Unlock()

			if invocation.SessionID == "" {
				t.Error("Expected non-empty session ID in invocation")
			}

			return &rpc.PermissionDecisionApproveOnce{}, nil
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
			if _, ok := req.(*copilot.PermissionRequestWrite); ok {
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

		onPermissionRequest := func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
			mu.Lock()
			permissionRequests = append(permissionRequests, request)
			mu.Unlock()

			return &rpc.PermissionDecisionApproveOnce{}, nil
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
			if _, ok := req.(*copilot.PermissionRequestShell); ok {
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

		onPermissionRequest := func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
			return &rpc.PermissionDecisionReject{}, nil
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: onPermissionRequest,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Regression check for https://github.com/github/copilot-sdk/issues/1194:
		// the reject decision must round-trip through the CLI with its discriminator
		// intact so the agent surfaces the user-rejected error to the model. The
		// CLI emits a kind-specific error message ("The user rejected this tool call.")
		// for the reject decision, which lets us assert the decision was honored
		// — not merely that the operation didn't happen.
		var mu sync.Mutex
		userRejectedToolCall := false

		session.On(func(event copilot.SessionEvent) {
			if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok &&
				!d.Success &&
				d.Error != nil &&
				strings.Contains(strings.ToLower(d.Error.Message), "user rejected") {
				mu.Lock()
				userRejectedToolCall = true
				mu.Unlock()
			}
		})

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

		mu.Lock()
		if !userRejectedToolCall {
			t.Error("Expected a tool.execution_complete event whose error indicates the user rejected the call.")
		}
		mu.Unlock()

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
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				return &rpc.PermissionDecisionUserNotAvailable{}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var mu sync.Mutex
		permissionDenied := false

		session.On(func(event copilot.SessionEvent) {
			if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok &&
				!d.Success &&
				d.Error != nil &&
				strings.Contains(d.Error.Message, "Permission denied") {
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
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				return &rpc.PermissionDecisionUserNotAvailable{}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		var mu sync.Mutex
		permissionDenied := false

		session2.On(func(event copilot.SessionEvent) {
			if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok &&
				!d.Success &&
				d.Error != nil &&
				strings.Contains(d.Error.Message, "Permission denied") {
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
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				permissionRequestReceived.Set(true)
				return &rpc.PermissionDecisionApproveOnce{}, nil
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
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				permissionRequestReceived.Set(true)
				return &rpc.PermissionDecisionApproveOnce{}, nil
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
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				return nil, fmt.Errorf("handler error")
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
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				if shellReq, ok := req.(*copilot.PermissionRequestShell); ok && shellReq.ToolCallID != nil && *shellReq.ToolCallID != "" {
					receivedToolCallID.Set(true)
				}
				return &rpc.PermissionDecisionApproveOnce{}, nil
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

	t.Run("should wait for slow permission handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type lifecycleEvent struct {
			Phase      string
			ToolCallID string
		}

		handlerEntered := make(chan struct{}, 1)
		releaseHandler := make(chan struct{})
		targetToolCallID := make(chan string, 1)
		var lifecycleMu sync.Mutex
		var lifecycle []lifecycleEvent

		addLifecycle := func(phase, toolCallID string) {
			lifecycleMu.Lock()
			lifecycle = append(lifecycle, lifecycleEvent{phase, toolCallID})
			lifecycleMu.Unlock()
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				shellReq, ok := req.(*copilot.PermissionRequestShell)
				if !ok {
					return &rpc.PermissionDecisionApproveOnce{}, nil
				}
				toolCallID := ""
				if shellReq.ToolCallID != nil {
					toolCallID = *shellReq.ToolCallID
				}
				addLifecycle("permission-start", toolCallID)
				select {
				case targetToolCallID <- toolCallID:
				default:
				}
				select {
				case handlerEntered <- struct{}{}:
				default:
				}
				<-releaseHandler
				addLifecycle("permission-complete", toolCallID)
				return &rpc.PermissionDecisionApproveOnce{}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		session.On(func(event copilot.SessionEvent) {
			switch d := event.Data.(type) {
			case *copilot.ToolExecutionStartData:
				addLifecycle("tool-start", d.ToolCallID)
			case *copilot.ToolExecutionCompleteData:
				addLifecycle("tool-complete", d.ToolCallID)
			}
		})

		go func() {
			_, _ = session.Send(t.Context(), copilot.MessageOptions{
				Prompt: "Run 'echo slow_handler_test'",
			})
		}()

		select {
		case <-handlerEntered:
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for permission handler to be entered")
		}
		var targetID string
		select {
		case targetID = <-targetToolCallID:
		default:
		}

		// Verify tool-complete has not yet happened while handler is still running
		lifecycleMu.Lock()
		for _, evt := range lifecycle {
			if evt.Phase == "tool-complete" && evt.ToolCallID == targetID {
				t.Error("tool-complete should not have occurred before permission handler completed")
			}
		}
		lifecycleMu.Unlock()

		close(releaseHandler)

		message, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("GetFinalAssistantMessage failed: %v", err)
		}

		lifecycleMu.Lock()
		orderedLifecycle := make([]lifecycleEvent, len(lifecycle))
		copy(orderedLifecycle, lifecycle)
		lifecycleMu.Unlock()

		permStartIdx, permCompleteIdx, toolStartIdx, toolCompleteIdx := -1, -1, -1, -1
		for i, evt := range orderedLifecycle {
			if evt.ToolCallID != targetID && targetID != "" {
				continue
			}
			switch evt.Phase {
			case "permission-start":
				if permStartIdx < 0 {
					permStartIdx = i
				}
			case "permission-complete":
				if permCompleteIdx < 0 {
					permCompleteIdx = i
				}
			case "tool-start":
				if toolStartIdx < 0 {
					toolStartIdx = i
				}
			case "tool-complete":
				if toolCompleteIdx < 0 {
					toolCompleteIdx = i
				}
			}
		}

		if permStartIdx < 0 || permCompleteIdx < 0 || toolCompleteIdx < 0 {
			t.Errorf("Expected permission-start, permission-complete, and tool-complete in lifecycle; got %v", orderedLifecycle)
		}
		if permCompleteIdx >= 0 && toolCompleteIdx >= 0 && permCompleteIdx >= toolCompleteIdx {
			t.Errorf("Expected permission completion before tool completion; lifecycle=%v", orderedLifecycle)
		}
		if toolStartIdx >= 0 && toolCompleteIdx >= 0 && toolStartIdx >= toolCompleteIdx {
			t.Errorf("Expected tool start before tool completion; lifecycle=%v", orderedLifecycle)
		}

		if md, ok := message.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(md.Content, "slow_handler_test") {
			t.Errorf("Expected assistant message to reference 'slow_handler_test', got %v", message.Data)
		}
	})

	t.Run("should handle concurrent permission requests from parallel tools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type EmptyParams struct{}

		var permissionRequestCount int
		var permissionRequestsMu sync.Mutex
		var permissionRequests []copilot.PermissionRequest
		bothStarted := make(chan struct{})
		var bothStartedOnce sync.Once

		firstToolCalled := make(chan struct{}, 1)
		secondToolCalled := make(chan struct{}, 1)
		firstToolCompleted := make(chan *copilot.ToolExecutionCompleteData, 1)
		secondToolCompleted := make(chan *copilot.ToolExecutionCompleteData, 1)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			Tools: []copilot.Tool{
				copilot.DefineTool("first_permission_tool", "First concurrent permission test tool",
					func(_ EmptyParams, inv copilot.ToolInvocation) (copilot.ToolResult, error) {
						select {
						case firstToolCalled <- struct{}{}:
						default:
						}
						return copilot.ToolResult{
							TextResultForLLM: "first_permission_tool completed after permission approval",
							ResultType:       "rejected",
						}, nil
					}),
				copilot.DefineTool("second_permission_tool", "Second concurrent permission test tool",
					func(_ EmptyParams, inv copilot.ToolInvocation) (copilot.ToolResult, error) {
						select {
						case secondToolCalled <- struct{}{}:
						default:
						}
						return copilot.ToolResult{
							TextResultForLLM: "second_permission_tool completed after permission approval",
							ResultType:       "rejected",
						}, nil
					}),
			},
			AvailableTools: []string{"first_permission_tool", "second_permission_tool"},
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				permissionRequestsMu.Lock()
				permissionRequestCount++
				permissionRequests = append(permissionRequests, req)
				count := permissionRequestCount
				permissionRequestsMu.Unlock()
				if count >= 2 {
					bothStartedOnce.Do(func() { close(bothStarted) })
				}
				select {
				case <-bothStarted:
				case <-time.After(30 * time.Second):
				}
				return &rpc.PermissionDecisionApproveOnce{}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		session.On(func(event copilot.SessionEvent) {
			if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok {
				var errMsg string
				if d.Error != nil {
					errMsg = d.Error.Message
				}
				switch {
				case strings.Contains(errMsg, "first_permission_tool"):
					select {
					case firstToolCompleted <- d:
					default:
					}
				case strings.Contains(errMsg, "second_permission_tool"):
					select {
					case secondToolCompleted <- d:
					default:
					}
				}
			}
		})

		if _, err := session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Call both first_permission_tool and second_permission_tool in the same turn. Do not call any other tools.",
		}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}

		select {
		case <-firstToolCalled:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for first_permission_tool to be called")
		}
		select {
		case <-secondToolCalled:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for second_permission_tool to be called")
		}

		permissionRequestsMu.Lock()
		reqCount := permissionRequestCount
		reqs := make([]copilot.PermissionRequest, len(permissionRequests))
		copy(reqs, permissionRequests)
		permissionRequestsMu.Unlock()

		if reqCount < 2 {
			t.Errorf("Expected at least 2 permission requests, got %d", reqCount)
		}
		hasFirst := false
		hasSecond := false
		for _, req := range reqs {
			if customReq, ok := req.(*copilot.PermissionRequestCustomTool); ok {
				if customReq.ToolName == "first_permission_tool" {
					hasFirst = true
				}
				if customReq.ToolName == "second_permission_tool" {
					hasSecond = true
				}
			}
		}
		if !hasFirst {
			t.Error("Expected permission request for first_permission_tool")
		}
		if !hasSecond {
			t.Error("Expected permission request for second_permission_tool")
		}

		assertRejectedToolComplete := func(name string, ch <-chan *copilot.ToolExecutionCompleteData, expectedMessage string) {
			t.Helper()
			select {
			case d := <-ch:
				if d.Success {
					t.Errorf("Expected %s tool execution to complete with Success=false", name)
				}
				if d.Error == nil {
					t.Errorf("Expected %s tool execution to include an error", name)
					return
				}
				if d.Error.Code == nil || *d.Error.Code != "rejected" {
					t.Errorf("Expected %s tool execution error code 'rejected', got %v", name, d.Error.Code)
				}
				if !strings.Contains(d.Error.Message, expectedMessage) {
					t.Errorf("Expected %s tool execution error message to contain %q, got %q", name, expectedMessage, d.Error.Message)
				}
			case <-time.After(60 * time.Second):
				t.Fatalf("Timed out waiting for %s tool.execution_complete", name)
			}
		}
		assertRejectedToolComplete("first_permission_tool", firstToolCompleted, "first_permission_tool completed after permission approval")
		assertRejectedToolComplete("second_permission_tool", secondToolCompleted, "second_permission_tool completed after permission approval")
	})

	t.Run("should deny permission with noresult kind", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		permissionCalled := make(chan struct{}, 1)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				select {
				case permissionCalled <- struct{}{}:
				default:
				}
				return &rpc.PermissionDecisionNoResult{}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		if _, err := session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'node --version'",
		}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}

		select {
		case <-permissionCalled:
			// Expected: legacy no-result does not send a permission decision.
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for permission handler to be called")
		}

		_ = session.Abort(t.Context())
	})

	t.Run("should short circuit permission handler when set approve all enabled", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var handlerCallCount int
		var handlerCallCountMu sync.Mutex

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
				handlerCallCountMu.Lock()
				handlerCallCount++
				handlerCallCountMu.Unlock()
				return &rpc.PermissionDecisionApproveOnce{}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		// Runtime contract: when approveAllToolPermissionRequests is true the runtime
		// short-circuits the permission flow before invoking the SDK-supplied handler.
		setResult, err := session.RPC.Permissions.SetApproveAll(t.Context(), &rpc.PermissionsSetApproveAllRequest{Enabled: true})
		if err != nil {
			t.Fatalf("SetApproveAll failed: %v", err)
		}
		if !setResult.Success {
			t.Fatalf("SetApproveAll returned success=false")
		}
		defer func() {
			_, _ = session.RPC.Permissions.SetApproveAll(t.Context(), &rpc.PermissionsSetApproveAllRequest{Enabled: false})
		}()

		toolCompleted := make(chan struct{}, 1)
		session.On(func(event copilot.SessionEvent) {
			if d, ok := event.Data.(*copilot.ToolExecutionCompleteData); ok && d.Success {
				select {
				case toolCompleted <- struct{}{}:
				default:
				}
			}
		})

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Run 'echo test' and tell me what happens",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		select {
		case <-toolCompleted:
			// A real shell tool completed successfully under runtime-level approval.
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for successful tool.execution_complete")
		}

		handlerCallCountMu.Lock()
		count := handlerCallCount
		handlerCallCountMu.Unlock()
		if count != 0 {
			t.Errorf("Expected permission handler to NOT be called when SetApproveAll is enabled, got %d calls", count)
		}
	})

	t.Run("should configure and update permission paths", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		configuredAllowed := createUniqueRPCWorkDirectory(t, ctx, "configured-allowed")
		addedAllowed := createUniqueRPCWorkDirectory(t, ctx, "added-allowed")
		newPrimary := createUniqueRPCWorkDirectory(t, ctx, "new-primary")
		includeTemp := false
		unrestricted := false
		configure, err := session.RPC.Permissions.Configure(t.Context(), &rpc.PermissionsConfigureParams{
			ApproveAllToolPermissionRequests: rpcPtr(false),
			ApproveAllReadPermissionRequests: rpcPtr(true),
			Rules: &rpc.PermissionRulesSet{
				Approved: []rpc.PermissionRule{{Kind: "read", Argument: nil}},
				Denied:   []rpc.PermissionRule{{Kind: "write", Argument: nil}},
			},
			Paths: &rpc.PermissionPathsConfig{
				WorkspacePath:         &ctx.WorkDir,
				AdditionalDirectories: []string{configuredAllowed},
				IncludeTempDirectory:  &includeTemp,
				Unrestricted:          &unrestricted,
			},
			Urls: &rpc.PermissionUrlsConfig{
				InitialAllowed: []string{"https://example.invalid/permissions-configure"},
				Unrestricted:   &unrestricted,
			},
		})
		if err != nil {
			t.Fatalf("Permissions.Configure failed: %v", err)
		}
		if !configure.Success {
			t.Fatalf("Expected Configure Success=true, got %+v", configure)
		}

		configuredList, err := session.RPC.Permissions.Paths().List(t.Context())
		if err != nil {
			t.Fatalf("Permissions.Paths.List failed: %v", err)
		}
		assertRPCPathEqual(t, ctx.WorkDir, configuredList.Primary)
		assertRPCContainsPath(t, configuredList.Directories, ctx.WorkDir)
		assertRPCContainsPath(t, configuredList.Directories, configuredAllowed)

		add, err := session.RPC.Permissions.Paths().Add(t.Context(), &rpc.PermissionPathsAddParams{Path: addedAllowed})
		if err != nil {
			t.Fatalf("Permissions.Paths.Add failed: %v", err)
		}
		if !add.Success {
			t.Fatalf("Expected Paths.Add Success=true, got %+v", add)
		}

		allowed, err := session.RPC.Permissions.Paths().IsPathWithinAllowedDirectories(t.Context(), &rpc.PermissionPathsAllowedCheckParams{
			Path: filepath.Join(addedAllowed, "child.txt"),
		})
		if err != nil {
			t.Fatalf("Permissions.Paths.IsPathWithinAllowedDirectories failed: %v", err)
		}
		if !allowed.Allowed {
			t.Fatalf("Expected path within added allowed directory to be allowed")
		}

		updatePrimary, err := session.RPC.Permissions.Paths().UpdatePrimary(t.Context(), &rpc.PermissionPathsUpdatePrimaryParams{Path: newPrimary})
		if err != nil {
			t.Fatalf("Permissions.Paths.UpdatePrimary failed: %v", err)
		}
		if !updatePrimary.Success {
			t.Fatalf("Expected UpdatePrimary Success=true, got %+v", updatePrimary)
		}

		updatedList, err := session.RPC.Permissions.Paths().List(t.Context())
		if err != nil {
			t.Fatalf("Permissions.Paths.List after update failed: %v", err)
		}
		assertRPCPathEqual(t, newPrimary, updatedList.Primary)
		assertRPCContainsPath(t, updatedList.Directories, newPrimary)

		workspaceCheck, err := session.RPC.Permissions.Paths().IsPathWithinWorkspace(t.Context(), &rpc.PermissionPathsWorkspaceCheckParams{
			Path: filepath.Join(newPrimary, "child.txt"),
		})
		if err != nil {
			t.Fatalf("Permissions.Paths.IsPathWithinWorkspace failed: %v", err)
		}
		if !workspaceCheck.Allowed {
			t.Fatalf("Expected path within new primary workspace to be allowed")
		}
	})

	t.Run("should invoke permission state rpc apis", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		pending, err := session.RPC.Permissions.PendingRequests(t.Context())
		if err != nil {
			t.Fatalf("Permissions.PendingRequests failed: %v", err)
		}
		if len(pending.Items) != 0 {
			t.Fatalf("Expected no pending permission requests, got %+v", pending.Items)
		}

		setRequired, err := session.RPC.Permissions.SetRequired(t.Context(), &rpc.PermissionsSetRequiredRequest{Required: true})
		if err != nil {
			t.Fatalf("Permissions.SetRequired(true) failed: %v", err)
		}
		if !setRequired.Success {
			t.Fatalf("Expected SetRequired(true) Success=true")
		}
		clearRequired, err := session.RPC.Permissions.SetRequired(t.Context(), &rpc.PermissionsSetRequiredRequest{Required: false})
		if err != nil {
			t.Fatalf("Permissions.SetRequired(false) failed: %v", err)
		}
		if !clearRequired.Success {
			t.Fatalf("Expected SetRequired(false) Success=true")
		}

		promptShown, err := session.RPC.Permissions.NotifyPromptShown(t.Context(), &rpc.PermissionPromptShownNotification{
			Message: "Permission prompt shown from Go SDK E2E",
		})
		if err != nil {
			t.Fatalf("Permissions.NotifyPromptShown failed: %v", err)
		}
		if !promptShown.Success {
			t.Fatalf("Expected NotifyPromptShown Success=true")
		}

		ruleArg := "go-permission-e2e-" + randomHex(t)
		rule := rpc.PermissionRule{Kind: "commands", Argument: &ruleArg}
		addRule, err := session.RPC.Permissions.ModifyRules(t.Context(), &rpc.PermissionsModifyRulesParams{
			Scope: rpc.PermissionsModifyRulesScopeSession,
			Add:   []rpc.PermissionRule{rule},
		})
		if err != nil {
			t.Fatalf("Permissions.ModifyRules(add) failed: %v", err)
		}
		if !addRule.Success {
			t.Fatalf("Expected ModifyRules(add) Success=true")
		}
		removeRule, err := session.RPC.Permissions.ModifyRules(t.Context(), &rpc.PermissionsModifyRulesParams{
			Scope:  rpc.PermissionsModifyRulesScopeSession,
			Remove: []rpc.PermissionRule{rule},
		})
		if err != nil {
			t.Fatalf("Permissions.ModifyRules(remove) failed: %v", err)
		}
		if !removeRule.Success {
			t.Fatalf("Expected ModifyRules(remove) Success=true")
		}

		enableUrls, err := session.RPC.Permissions.Urls().SetUnrestrictedMode(t.Context(), &rpc.PermissionUrlsSetUnrestrictedModeParams{Enabled: true})
		if err != nil {
			t.Fatalf("Permissions.Urls.SetUnrestrictedMode(true) failed: %v", err)
		}
		if !enableUrls.Success {
			t.Fatalf("Expected SetUnrestrictedMode(true) Success=true")
		}
		disableUrls, err := session.RPC.Permissions.Urls().SetUnrestrictedMode(t.Context(), &rpc.PermissionUrlsSetUnrestrictedModeParams{Enabled: false})
		if err != nil {
			t.Fatalf("Permissions.Urls.SetUnrestrictedMode(false) failed: %v", err)
		}
		if !disableUrls.Success {
			t.Fatalf("Expected SetUnrestrictedMode(false) Success=true")
		}
	})

	t.Run("should invoke permission location and folder trust rpc apis", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		locationDirectory := createUniqueRPCWorkDirectory(t, ctx, "permission-location")
		trustedDirectory := createUniqueRPCWorkDirectory(t, ctx, "folder-trust")
		commandIdentifier := "go-permission-location-" + randomHex(t)

		resolved, err := session.RPC.Permissions.Locations().Resolve(t.Context(), &rpc.PermissionLocationResolveParams{WorkingDirectory: locationDirectory})
		if err != nil {
			t.Fatalf("Permissions.Locations.Resolve failed: %v", err)
		}
		if resolved.LocationType != rpc.PermissionLocationTypeDir {
			t.Fatalf("Expected dir location type, got %+v", resolved)
		}
		assertRPCPathEqual(t, locationDirectory, resolved.LocationKey)

		addToolApproval, err := session.RPC.Permissions.Locations().AddToolApproval(t.Context(), &rpc.PermissionLocationAddToolApprovalParams{
			LocationKey: resolved.LocationKey,
			Approval:    &rpc.PermissionsLocationsAddToolApprovalDetailsCommands{CommandIdentifiers: []string{commandIdentifier}},
		})
		if err != nil {
			t.Fatalf("Permissions.Locations.AddToolApproval failed: %v", err)
		}
		if !addToolApproval.Success {
			t.Fatalf("Expected AddToolApproval Success=true")
		}

		applied, err := session.RPC.Permissions.Locations().Apply(t.Context(), &rpc.PermissionLocationApplyParams{WorkingDirectory: locationDirectory})
		if err != nil {
			t.Fatalf("Permissions.Locations.Apply failed: %v", err)
		}
		if applied.LocationType != resolved.LocationType {
			t.Fatalf("Expected applied location type %q, got %+v", resolved.LocationType, applied)
		}
		assertRPCPathEqual(t, resolved.LocationKey, applied.LocationKey)
		if applied.AppliedRuleCount < 1 {
			t.Fatalf("Expected at least one applied rule, got %+v", applied)
		}
		var foundRule bool
		for _, rule := range applied.AppliedRules {
			if rule.Kind == "shell" && rule.Argument != nil && *rule.Argument == commandIdentifier {
				foundRule = true
				break
			}
		}
		if !foundRule {
			t.Fatalf("Expected applied shell rule for %q, got %+v", commandIdentifier, applied.AppliedRules)
		}

		initialTrust, err := session.RPC.Permissions.FolderTrust().IsTrusted(t.Context(), &rpc.FolderTrustCheckParams{Path: trustedDirectory})
		if err != nil {
			t.Fatalf("Permissions.FolderTrust.IsTrusted(initial) failed: %v", err)
		}
		if initialTrust.Trusted {
			t.Fatalf("Expected new trusted directory to start untrusted")
		}

		addTrusted, err := session.RPC.Permissions.FolderTrust().AddTrusted(t.Context(), &rpc.FolderTrustAddParams{Path: trustedDirectory})
		if err != nil {
			t.Fatalf("Permissions.FolderTrust.AddTrusted failed: %v", err)
		}
		if !addTrusted.Success {
			t.Fatalf("Expected AddTrusted Success=true")
		}
		updatedTrust, err := session.RPC.Permissions.FolderTrust().IsTrusted(t.Context(), &rpc.FolderTrustCheckParams{Path: trustedDirectory})
		if err != nil {
			t.Fatalf("Permissions.FolderTrust.IsTrusted(updated) failed: %v", err)
		}
		if !updatedTrust.Trusted {
			t.Fatalf("Expected trusted directory to be trusted after AddTrusted")
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
