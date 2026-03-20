package e2e

import (
	"regexp"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestSession(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should create and disconnect sessions", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll, Model: "fake-test-model"})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		matched, _ := regexp.MatchString(`^[a-f0-9-]+$`, session.SessionID)
		if !matched {
			t.Errorf("Expected session ID to match UUID pattern, got %q", session.SessionID)
		}

		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to get messages: %v", err)
		}

		if len(messages) == 0 || messages[0].Type != "session.start" {
			t.Fatalf("Expected first message to be session.start, got %v", messages)
		}

		if messages[0].Data.SessionID == nil || *messages[0].Data.SessionID != session.SessionID {
			t.Errorf("Expected session.start sessionId to match")
		}

		if messages[0].Data.SelectedModel == nil || *messages[0].Data.SelectedModel != "fake-test-model" {
			t.Errorf("Expected selectedModel to be 'fake-test-model', got %v", messages[0].Data.SelectedModel)
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}

		_, err = session.GetMessages(t.Context())
		if err == nil || !strings.Contains(err.Error(), "not found") {
			t.Errorf("Expected GetMessages to fail with 'not found' after disconnect, got %v", err)
		}
	})

	t.Run("should have stateful conversation", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		assistantMessage, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if assistantMessage.Data.Content == nil || !strings.Contains(*assistantMessage.Data.Content, "2") {
			t.Errorf("Expected assistant message to contain '2', got %v", assistantMessage.Data.Content)
		}

		secondMessage, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Now if you double that, what do you get?"})
		if err != nil {
			t.Fatalf("Failed to send second message: %v", err)
		}

		if secondMessage.Data.Content == nil || !strings.Contains(*secondMessage.Data.Content, "4") {
			t.Errorf("Expected second message to contain '4', got %v", secondMessage.Data.Content)
		}
	})

	t.Run("should create a session with appended systemMessage config", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		systemMessageSuffix := "End each response with the phrase 'Have a nice day!'"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "append",
				Content: systemMessageSuffix,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		assistantMessage, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is your full name?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		content := ""
		if assistantMessage != nil && assistantMessage.Data.Content != nil {
			content = *assistantMessage.Data.Content
		}

		if !strings.Contains(content, "GitHub") {
			t.Errorf("Expected response to contain 'GitHub', got %q", content)
		}
		if !strings.Contains(content, "Have a nice day!") {
			t.Errorf("Expected response to contain 'Have a nice day!', got %q", content)
		}

		// Validate the underlying traffic
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if len(traffic) == 0 {
			t.Fatal("Expected at least one exchange")
		}
		systemMessage := getSystemMessage(traffic[0])
		if !strings.Contains(systemMessage, "GitHub") {
			t.Errorf("Expected system message to contain 'GitHub', got %q", systemMessage)
		}
		if !strings.Contains(systemMessage, systemMessageSuffix) {
			t.Errorf("Expected system message to contain suffix, got %q", systemMessage)
		}
	})

	t.Run("should create a session with replaced systemMessage config", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		testSystemMessage := "You are an assistant called Testy McTestface. Reply succinctly."
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "replace",
				Content: testSystemMessage,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is your full name?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		content := ""
		if assistantMessage.Data.Content != nil {
			content = *assistantMessage.Data.Content
		}

		if strings.Contains(content, "GitHub") {
			t.Errorf("Expected response to NOT contain 'GitHub', got %q", content)
		}
		if !strings.Contains(content, "Testy") {
			t.Errorf("Expected response to contain 'Testy', got %q", content)
		}

		// Validate the underlying traffic
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if len(traffic) == 0 {
			t.Fatal("Expected at least one exchange")
		}
		systemMessage := getSystemMessage(traffic[0])
		if systemMessage != testSystemMessage {
			t.Errorf("Expected system message to be exact match, got %q", systemMessage)
		}
	})

	t.Run("should create a session with availableTools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      []string{"view", "edit"},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		_, err = testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		// Validate that only the specified tools are present
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if len(traffic) == 0 {
			t.Fatal("Expected at least one exchange")
		}

		toolNames := getToolNames(traffic[0])
		if len(toolNames) != 2 {
			t.Errorf("Expected exactly 2 tools, got %d: %v", len(toolNames), toolNames)
		}
		if !contains(toolNames, "view") || !contains(toolNames, "edit") {
			t.Errorf("Expected tools to contain 'view' and 'edit', got %v", toolNames)
		}
	})

	t.Run("should create a session with excludedTools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			ExcludedTools:       []string{"view"},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		_, err = testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		// Validate that excluded tool is not present but others are
		traffic, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if len(traffic) == 0 {
			t.Fatal("Expected at least one exchange")
		}

		toolNames := getToolNames(traffic[0])
		if contains(toolNames, "view") {
			t.Errorf("Expected 'view' to be excluded, got %v", toolNames)
		}
		if !contains(toolNames, "edit") || !contains(toolNames, "grep") {
			t.Errorf("Expected 'edit' and 'grep' to be present, got %v", toolNames)
		}
	})

	t.Run("should create session with custom tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				{
					Name:        "get_secret_number",
					Description: "Gets the secret number",
					Parameters: map[string]any{
						"type": "object",
						"properties": map[string]any{
							"key": map[string]any{
								"type":        "string",
								"description": "Key",
							},
						},
						"required": []string{"key"},
					},
					Handler: func(invocation copilot.ToolInvocation) (copilot.ToolResult, error) {
						args, _ := invocation.Arguments.(map[string]any)
						key, _ := args["key"].(string)
						if key == "ALPHA" {
							return copilot.ToolResult{
								TextResultForLLM: "54321",
								ResultType:       "success",
							}, nil
						}
						return copilot.ToolResult{
							TextResultForLLM: "unknown",
							ResultType:       "success",
						}, nil
					},
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is the secret number for key ALPHA?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		content := ""
		if assistantMessage.Data.Content != nil {
			content = *assistantMessage.Data.Content
		}

		if !strings.Contains(content, "54321") {
			t.Errorf("Expected response to contain '54321', got %q", content)
		}
	})

	t.Run("should handle multiple concurrent sessions", func(t *testing.T) {
		t.Skip("Known race condition - see TypeScript test")
	})

	t.Run("should resume a session using the same client", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create initial session
		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
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

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "2") {
			t.Errorf("Expected answer to contain '2', got %v", answer.Data.Content)
		}

		// Resume using the same client
		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected resumed session ID to match, got %q vs %q", session2.SessionID, sessionID)
		}

		answer2, err := testharness.GetFinalAssistantMessage(t.Context(), session2)
		if err != nil {
			t.Fatalf("Failed to get assistant message from resumed session: %v", err)
		}

		if answer2.Data.Content == nil || !strings.Contains(*answer2.Data.Content, "2") {
			t.Errorf("Expected resumed session answer to contain '2', got %v", answer2.Data.Content)
		}

		// Can continue the conversation statefully
		answer3, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Now if you double that, what do you get?"})
		if err != nil {
			t.Fatalf("Failed to send follow-up message: %v", err)
		}
		if answer3 == nil || answer3.Data.Content == nil || !strings.Contains(*answer3.Data.Content, "4") {
			t.Errorf("Expected follow-up answer to contain '4', got %v", answer3)
		}
	})

	t.Run("should resume a session using a new client", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create initial session
		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
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

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "2") {
			t.Errorf("Expected answer to contain '2', got %v", answer.Data.Content)
		}

		// Resume using a new client
		newClient := ctx.NewClient()
		defer newClient.ForceStop()

		session2, err := newClient.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected resumed session ID to match, got %q vs %q", session2.SessionID, sessionID)
		}

		// When resuming with a new client, we check messages contain expected types
		messages, err := session2.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to get messages: %v", err)
		}

		hasUserMessage := false
		hasSessionResume := false
		for _, msg := range messages {
			if msg.Type == "user.message" {
				hasUserMessage = true
			}
			if msg.Type == "session.resume" {
				hasSessionResume = true
			}
		}

		if !hasUserMessage {
			t.Error("Expected messages to contain 'user.message'")
		}
		if !hasSessionResume {
			t.Error("Expected messages to contain 'session.resume'")
		}

		// Can continue the conversation statefully
		answer3, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Now if you double that, what do you get?"})
		if err != nil {
			t.Fatalf("Failed to send follow-up message: %v", err)
		}
		if answer3 == nil || answer3.Data.Content == nil || !strings.Contains(*answer3.Data.Content, "4") {
			t.Errorf("Expected follow-up answer to contain '4', got %v", answer3)
		}
	})

	t.Run("should throw error when resuming non-existent session", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, err := client.ResumeSession(t.Context(), "non-existent-session-id", &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err == nil {
			t.Error("Expected error when resuming non-existent session")
		}
	})

	t.Run("should resume session with a custom provider", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session.SessionID

		// Resume the session with a provider
		session2, err := client.ResumeSessionWithOptions(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Provider: &copilot.ProviderConfig{
				Type:    "openai",
				BaseURL: "https://api.openai.com/v1",
				APIKey:  "fake-key",
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session with provider: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected resumed session ID to match, got %q vs %q", session2.SessionID, sessionID)
		}
	})

	t.Run("should abort a session", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Set up event listeners BEFORE sending to avoid race conditions
		toolStartCh := make(chan *copilot.SessionEvent, 1)
		toolStartErrCh := make(chan error, 1)
		go func() {
			evt, err := testharness.GetNextEventOfType(session, copilot.SessionEventTypeToolExecutionStart, 60*time.Second)
			if err != nil {
				toolStartErrCh <- err
			} else {
				toolStartCh <- evt
			}
		}()

		sessionIdleCh := make(chan *copilot.SessionEvent, 1)
		sessionIdleErrCh := make(chan error, 1)
		go func() {
			evt, err := testharness.GetNextEventOfType(session, copilot.SessionEventTypeSessionIdle, 60*time.Second)
			if err != nil {
				sessionIdleErrCh <- err
			} else {
				sessionIdleCh <- evt
			}
		}()

		// Send a message that triggers a long-running shell command
		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "run the shell command 'sleep 100' (note this works on both bash and PowerShell)"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Wait for tool.execution_start
		select {
		case <-toolStartCh:
			// Tool execution has started
		case err := <-toolStartErrCh:
			t.Fatalf("Failed waiting for tool.execution_start: %v", err)
		}

		// Abort the session
		err = session.Abort(t.Context())
		if err != nil {
			t.Fatalf("Failed to abort session: %v", err)
		}

		// Wait for session.idle after abort
		select {
		case <-sessionIdleCh:
			// Session is idle
		case err := <-sessionIdleErrCh:
			t.Fatalf("Failed waiting for session.idle after abort: %v", err)
		}

		// The session should still be alive and usable after abort
		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("Failed to get messages after abort: %v", err)
		}
		if len(messages) == 0 {
			t.Error("Expected messages to exist after abort")
		}

		// Verify messages contain an abort event
		hasAbortEvent := false
		for _, msg := range messages {
			if msg.Type == copilot.SessionEventTypeAbort {
				hasAbortEvent = true
				break
			}
		}
		if !hasAbortEvent {
			t.Error("Expected messages to contain an 'abort' event")
		}

		// We should be able to send another message
		answer, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("Failed to send message after abort: %v", err)
		}

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "4") {
			t.Errorf("Expected answer to contain '4', got %v", answer.Data.Content)
		}
	})

	t.Run("should receive session events", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Use OnEvent to capture events dispatched during session creation.
		// session.start is emitted during the session.create RPC; with channel-based
		// dispatch it may not have been delivered by the time CreateSession returns.
		sessionStartCh := make(chan bool, 1)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnEvent: func(event copilot.SessionEvent) {
				if event.Type == "session.start" {
					select {
					case sessionStartCh <- true:
					default:
					}
				}
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		select {
		case <-sessionStartCh:
		case <-time.After(5 * time.Second):
			t.Error("Expected session.start event via OnEvent during creation")
		}

		var receivedEvents []copilot.SessionEvent
		idle := make(chan bool)

		session.On(func(event copilot.SessionEvent) {
			receivedEvents = append(receivedEvents, event)
			if event.Type == "session.idle" {
				select {
				case idle <- true:
				default:
				}
			}
		})

		// Send a message to trigger events
		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is 100+200?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Wait for session to become idle
		select {
		case <-idle:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for session.idle")
		}

		// Should have received multiple events
		if len(receivedEvents) == 0 {
			t.Error("Expected to receive events, got none")
		}

		hasUserMessage := false
		hasAssistantMessage := false
		hasSessionIdle := false
		for _, evt := range receivedEvents {
			switch evt.Type {
			case "user.message":
				hasUserMessage = true
			case "assistant.message":
				hasAssistantMessage = true
			case "session.idle":
				hasSessionIdle = true
			}
		}

		if !hasUserMessage {
			t.Error("Expected to receive user.message event")
		}
		if !hasAssistantMessage {
			t.Error("Expected to receive assistant.message event")
		}
		if !hasSessionIdle {
			t.Error("Expected to receive session.idle event")
		}

		// Verify the assistant response contains the expected answer
		assistantMessage, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}
		if assistantMessage.Data.Content == nil || !strings.Contains(*assistantMessage.Data.Content, "300") {
			t.Errorf("Expected assistant message to contain '300', got %v", assistantMessage.Data.Content)
		}
	})

	t.Run("should create session with custom config dir", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		customConfigDir := ctx.HomeDir + "/custom-config"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			ConfigDir:           customConfigDir,
		})
		if err != nil {
			t.Fatalf("Failed to create session with custom config dir: %v", err)
		}

		matched, _ := regexp.MatchString(`^[a-f0-9-]+$`, session.SessionID)
		if !matched {
			t.Errorf("Expected session ID to match UUID pattern, got %q", session.SessionID)
		}

		// Session should work normally with custom config dir
		_, err = session.Send(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if assistantMessage.Data.Content == nil || !strings.Contains(*assistantMessage.Data.Content, "2") {
			t.Errorf("Expected assistant message to contain '2', got %v", assistantMessage.Data.Content)
		}
	})

	t.Run("should list sessions", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create a couple of sessions and send messages to persist them
		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session1: %v", err)
		}

		_, err = session1.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say hello"})
		if err != nil {
			t.Fatalf("Failed to send message to session1: %v", err)
		}

		session2, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session2: %v", err)
		}

		_, err = session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say goodbye"})
		if err != nil {
			t.Fatalf("Failed to send message to session2: %v", err)
		}

		// Small delay to ensure session files are written to disk
		time.Sleep(200 * time.Millisecond)

		// List sessions and verify they're included
		sessions, err := client.ListSessions(t.Context(), nil)
		if err != nil {
			t.Fatalf("Failed to list sessions: %v", err)
		}

		// Verify it's a list
		if sessions == nil {
			t.Fatal("Expected sessions to be non-nil")
		}

		// Extract session IDs
		sessionIDs := make([]string, len(sessions))
		for i, s := range sessions {
			sessionIDs[i] = s.SessionID
		}

		// Verify both sessions are in the list
		if !contains(sessionIDs, session1.SessionID) {
			t.Errorf("Expected session1 ID %s to be in sessions list %v", session1.SessionID, sessionIDs)
		}
		if !contains(sessionIDs, session2.SessionID) {
			t.Errorf("Expected session2 ID %s to be in sessions list %v", session2.SessionID, sessionIDs)
		}

		// Verify session metadata structure
		for _, sessionData := range sessions {
			if sessionData.SessionID == "" {
				t.Error("Expected sessionId to be non-empty")
			}
			if sessionData.StartTime == "" {
				t.Error("Expected startTime to be non-empty")
			}
			if sessionData.ModifiedTime == "" {
				t.Error("Expected modifiedTime to be non-empty")
			}
			// isRemote is a boolean, so it's always set
		}

		// Verify context field is present on sessions
		for _, s := range sessions {
			if s.Context != nil {
				if s.Context.Cwd == "" {
					t.Error("Expected context.Cwd to be non-empty when context is present")
				}
			}
		}
	})

	t.Run("should delete session", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create a session and send a message to persist it
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Hello"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		sessionID := session.SessionID

		// Small delay to ensure session file is written to disk
		time.Sleep(200 * time.Millisecond)

		// Verify session exists in the list
		sessions, err := client.ListSessions(t.Context(), nil)
		if err != nil {
			t.Fatalf("Failed to list sessions: %v", err)
		}

		sessionIDs := make([]string, len(sessions))
		for i, s := range sessions {
			sessionIDs[i] = s.SessionID
		}

		if !contains(sessionIDs, sessionID) {
			t.Errorf("Expected session ID %s to be in sessions list before delete", sessionID)
		}

		// Delete the session
		err = client.DeleteSession(t.Context(), sessionID)
		if err != nil {
			t.Fatalf("Failed to delete session: %v", err)
		}

		// Verify session no longer exists in the list
		sessionsAfter, err := client.ListSessions(t.Context(), nil)
		if err != nil {
			t.Fatalf("Failed to list sessions after delete: %v", err)
		}

		sessionIDsAfter := make([]string, len(sessionsAfter))
		for i, s := range sessionsAfter {
			sessionIDsAfter[i] = s.SessionID
		}

		if contains(sessionIDsAfter, sessionID) {
			t.Errorf("Expected session ID %s to NOT be in sessions list after delete", sessionID)
		}

		// Verify we cannot resume the deleted session
		_, err = client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err == nil {
			t.Error("Expected error when resuming deleted session")
		}
	})
	t.Run("should get last session id", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create a session and send a message to persist it
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{OnPermissionRequest: copilot.PermissionHandler.ApproveAll})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say hello"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Small delay to ensure session data is flushed to disk
		time.Sleep(500 * time.Millisecond)

		lastSessionID, err := client.GetLastSessionID(t.Context())
		if err != nil {
			t.Fatalf("Failed to get last session ID: %v", err)
		}

		if lastSessionID == nil {
			t.Fatal("Expected last session ID to be non-nil")
		}

		if *lastSessionID != session.SessionID {
			t.Errorf("Expected last session ID to be %s, got %s", session.SessionID, *lastSessionID)
		}

		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to destroy session: %v", err)
		}
	})
}

func getSystemMessage(exchange testharness.ParsedHttpExchange) string {
	for _, msg := range exchange.Request.Messages {
		if msg.Role == "system" {
			return msg.Content
		}
	}
	return ""
}

func TestSetModelWithReasoningEffort(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create session: %v", err)
	}

	modelChanged := make(chan copilot.SessionEvent, 1)
	session.On(func(event copilot.SessionEvent) {
		if event.Type == copilot.SessionEventTypeSessionModelChange {
			select {
			case modelChanged <- event:
			default:
			}
		}
	})

	if err := session.SetModel(t.Context(), "gpt-4.1", copilot.SetModelOptions{ReasoningEffort: "high"}); err != nil {
		t.Fatalf("SetModel returned error: %v", err)
	}

	select {
	case evt := <-modelChanged:
		if evt.Data.NewModel == nil || *evt.Data.NewModel != "gpt-4.1" {
			t.Errorf("Expected newModel 'gpt-4.1', got %v", evt.Data.NewModel)
		}
		if evt.Data.ReasoningEffort == nil || *evt.Data.ReasoningEffort != "high" {
			t.Errorf("Expected reasoningEffort 'high', got %v", evt.Data.ReasoningEffort)
		}
	case <-time.After(30 * time.Second):
		t.Fatal("Timed out waiting for session.model_change event")
	}
}

func TestSessionBlobAttachment(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should accept blob attachments", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		data := "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
		mimeType := "image/png"
		displayName := "test-pixel.png"
		_, err = session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Describe this image",
			Attachments: []copilot.Attachment{
				{
					Type:        copilot.AttachmentTypeBlob,
					Data:        &data,
					MIMEType:    &mimeType,
					DisplayName: &displayName,
				},
			},
		})
		if err != nil {
			t.Fatalf("Send with blob attachment failed: %v", err)
		}

		// Just verify send doesn't error — blob attachment support varies by runtime
		session.Disconnect()
	})
}

func getToolNames(exchange testharness.ParsedHttpExchange) []string {
	var names []string
	for _, tool := range exchange.Request.Tools {
		names = append(names, tool.Function.Name)
	}
	return names
}

func contains(slice []string, item string) bool {
	for _, s := range slice {
		if s == item {
			return true
		}
	}
	return false
}

func TestSessionLog(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create session: %v", err)
	}

	// Collect events
	var events []copilot.SessionEvent
	var mu sync.Mutex
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		mu.Lock()
		defer mu.Unlock()
		events = append(events, event)
	})
	defer unsubscribe()

	t.Run("should log info message (default level)", func(t *testing.T) {
		if err := session.Log(t.Context(), "Info message", nil); err != nil {
			t.Fatalf("Log failed: %v", err)
		}

		evt := waitForEvent(t, &mu, &events, copilot.SessionEventTypeSessionInfo, "Info message", 5*time.Second)
		if evt.Data.InfoType == nil || *evt.Data.InfoType != "notification" {
			t.Errorf("Expected infoType 'notification', got %v", evt.Data.InfoType)
		}
		if evt.Data.Message == nil || *evt.Data.Message != "Info message" {
			t.Errorf("Expected message 'Info message', got %v", evt.Data.Message)
		}
	})

	t.Run("should log warning message", func(t *testing.T) {
		if err := session.Log(t.Context(), "Warning message", &copilot.LogOptions{Level: rpc.LevelWarning}); err != nil {
			t.Fatalf("Log failed: %v", err)
		}

		evt := waitForEvent(t, &mu, &events, copilot.SessionEventTypeSessionWarning, "Warning message", 5*time.Second)
		if evt.Data.WarningType == nil || *evt.Data.WarningType != "notification" {
			t.Errorf("Expected warningType 'notification', got %v", evt.Data.WarningType)
		}
		if evt.Data.Message == nil || *evt.Data.Message != "Warning message" {
			t.Errorf("Expected message 'Warning message', got %v", evt.Data.Message)
		}
	})

	t.Run("should log error message", func(t *testing.T) {
		if err := session.Log(t.Context(), "Error message", &copilot.LogOptions{Level: rpc.LevelError}); err != nil {
			t.Fatalf("Log failed: %v", err)
		}

		evt := waitForEvent(t, &mu, &events, copilot.SessionEventTypeSessionError, "Error message", 5*time.Second)
		if evt.Data.ErrorType == nil || *evt.Data.ErrorType != "notification" {
			t.Errorf("Expected errorType 'notification', got %v", evt.Data.ErrorType)
		}
		if evt.Data.Message == nil || *evt.Data.Message != "Error message" {
			t.Errorf("Expected message 'Error message', got %v", evt.Data.Message)
		}
	})

	t.Run("should log ephemeral message", func(t *testing.T) {
		if err := session.Log(t.Context(), "Ephemeral message", &copilot.LogOptions{Ephemeral: copilot.Bool(true)}); err != nil {
			t.Fatalf("Log failed: %v", err)
		}

		evt := waitForEvent(t, &mu, &events, copilot.SessionEventTypeSessionInfo, "Ephemeral message", 5*time.Second)
		if evt.Data.InfoType == nil || *evt.Data.InfoType != "notification" {
			t.Errorf("Expected infoType 'notification', got %v", evt.Data.InfoType)
		}
		if evt.Data.Message == nil || *evt.Data.Message != "Ephemeral message" {
			t.Errorf("Expected message 'Ephemeral message', got %v", evt.Data.Message)
		}
	})
}

// waitForEvent polls the collected events for a matching event type and message.
func waitForEvent(t *testing.T, mu *sync.Mutex, events *[]copilot.SessionEvent, eventType copilot.SessionEventType, message string, timeout time.Duration) copilot.SessionEvent {
	t.Helper()
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		mu.Lock()
		for _, evt := range *events {
			if evt.Type == eventType && evt.Data.Message != nil && *evt.Data.Message == message {
				mu.Unlock()
				return evt
			}
		}
		mu.Unlock()
		time.Sleep(50 * time.Millisecond)
	}
	t.Fatalf("Timed out waiting for %s event with message %q", eventType, message)
	return copilot.SessionEvent{} // unreachable
}
