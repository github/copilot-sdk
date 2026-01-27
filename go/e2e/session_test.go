package e2e

import (
	"regexp"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestSession(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should create and destroy sessions", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(&copilot.SessionConfig{Model: "fake-test-model"})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		matched, _ := regexp.MatchString(`^[a-f0-9-]+$`, session.SessionID)
		if !matched {
			t.Errorf("Expected session ID to match UUID pattern, got %q", session.SessionID)
		}

		messages, err := session.GetMessages()
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

		if err := session.Destroy(); err != nil {
			t.Fatalf("Failed to destroy session: %v", err)
		}

		_, err = session.GetMessages()
		if err == nil || !strings.Contains(err.Error(), "not found") {
			t.Errorf("Expected GetMessages to fail with 'not found' after destroy, got %v", err)
		}
	})

	t.Run("should have stateful conversation", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		assistantMessage, err := session.SendAndWait(copilot.MessageOptions{Prompt: "What is 1+1?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if assistantMessage.Data.Content == nil || !strings.Contains(*assistantMessage.Data.Content, "2") {
			t.Errorf("Expected assistant message to contain '2', got %v", assistantMessage.Data.Content)
		}

		secondMessage, err := session.SendAndWait(copilot.MessageOptions{Prompt: "Now if you double that, what do you get?"}, 60*time.Second)
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
		session, err := client.CreateSession(&copilot.SessionConfig{
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "append",
				Content: systemMessageSuffix,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		assistantMessage, err := session.SendAndWait(copilot.MessageOptions{Prompt: "What is your full name?"}, 60*time.Second)
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
		session, err := client.CreateSession(&copilot.SessionConfig{
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "replace",
				Content: testSystemMessage,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{Prompt: "What is your full name?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
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

		session, err := client.CreateSession(&copilot.SessionConfig{
			AvailableTools: []string{"view", "edit"},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		_, err = testharness.GetFinalAssistantMessage(session, 60*time.Second)
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

		session, err := client.CreateSession(&copilot.SessionConfig{
			ExcludedTools: []string{"view"},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		_, err = testharness.GetFinalAssistantMessage(session, 60*time.Second)
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

		session, err := client.CreateSession(&copilot.SessionConfig{
			Tools: []copilot.Tool{
				{
					Name:        "get_secret_number",
					Description: "Gets the secret number",
					Parameters: map[string]interface{}{
						"type": "object",
						"properties": map[string]interface{}{
							"key": map[string]interface{}{
								"type":        "string",
								"description": "Key",
							},
						},
						"required": []string{"key"},
					},
					Handler: func(invocation copilot.ToolInvocation) (copilot.ToolResult, error) {
						args, _ := invocation.Arguments.(map[string]interface{})
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

		_, err = session.Send(copilot.MessageOptions{Prompt: "What is the secret number for key ALPHA?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
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
		session1, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		_, err = session1.Send(copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(session1, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "2") {
			t.Errorf("Expected answer to contain '2', got %v", answer.Data.Content)
		}

		// Resume using the same client
		session2, err := client.ResumeSession(sessionID)
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected resumed session ID to match, got %q vs %q", session2.SessionID, sessionID)
		}

		answer2, err := testharness.GetFinalAssistantMessage(session2, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message from resumed session: %v", err)
		}

		if answer2.Data.Content == nil || !strings.Contains(*answer2.Data.Content, "2") {
			t.Errorf("Expected resumed session answer to contain '2', got %v", answer2.Data.Content)
		}
	})

	t.Run("should resume a session using a new client", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create initial session
		session1, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session1.SessionID

		_, err = session1.Send(copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(session1, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "2") {
			t.Errorf("Expected answer to contain '2', got %v", answer.Data.Content)
		}

		// Resume using a new client
		newClient := copilot.NewClient(&copilot.ClientOptions{
			CLIPath: ctx.CLIPath,
			Cwd:     ctx.WorkDir,
			Env:     ctx.Env(),
		})
		defer newClient.ForceStop()

		session2, err := newClient.ResumeSession(sessionID)
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		if session2.SessionID != sessionID {
			t.Errorf("Expected resumed session ID to match, got %q vs %q", session2.SessionID, sessionID)
		}

		// When resuming with a new client, we check messages contain expected types
		messages, err := session2.GetMessages()
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
	})

	t.Run("should throw error when resuming non-existent session", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		_, err := client.ResumeSession("non-existent-session-id")
		if err == nil {
			t.Error("Expected error when resuming non-existent session")
		}
	})

	t.Run("should resume session with a custom provider", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		sessionID := session.SessionID

		// Resume the session with a provider
		session2, err := client.ResumeSessionWithOptions(sessionID, &copilot.ResumeSessionConfig{
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

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Set up event listeners BEFORE sending to avoid race conditions
		toolStartCh := make(chan *copilot.SessionEvent, 1)
		toolStartErrCh := make(chan error, 1)
		go func() {
			evt, err := testharness.GetNextEventOfType(session, copilot.ToolExecutionStart, 60*time.Second)
			if err != nil {
				toolStartErrCh <- err
			} else {
				toolStartCh <- evt
			}
		}()

		sessionIdleCh := make(chan *copilot.SessionEvent, 1)
		sessionIdleErrCh := make(chan error, 1)
		go func() {
			evt, err := testharness.GetNextEventOfType(session, copilot.SessionIdle, 60*time.Second)
			if err != nil {
				sessionIdleErrCh <- err
			} else {
				sessionIdleCh <- evt
			}
		}()

		// Send a message that triggers a long-running shell command
		_, err = session.Send(copilot.MessageOptions{Prompt: "run the shell command 'sleep 100' (note this works on both bash and PowerShell)"})
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
		err = session.Abort()
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
		messages, err := session.GetMessages()
		if err != nil {
			t.Fatalf("Failed to get messages after abort: %v", err)
		}
		if len(messages) == 0 {
			t.Error("Expected messages to exist after abort")
		}

		// Verify messages contain an abort event
		hasAbortEvent := false
		for _, msg := range messages {
			if msg.Type == copilot.Abort {
				hasAbortEvent = true
				break
			}
		}
		if !hasAbortEvent {
			t.Error("Expected messages to contain an 'abort' event")
		}

		// We should be able to send another message
		answer, err := session.SendAndWait(copilot.MessageOptions{Prompt: "What is 2+2?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message after abort: %v", err)
		}

		if answer.Data.Content == nil || !strings.Contains(*answer.Data.Content, "4") {
			t.Errorf("Expected answer to contain '4', got %v", answer.Data.Content)
		}
	})

	t.Run("should receive streaming delta events when streaming is enabled", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(&copilot.SessionConfig{
			Streaming: true,
		})
		if err != nil {
			t.Fatalf("Failed to create session with streaming: %v", err)
		}

		var deltaContents []string
		done := make(chan bool)

		session.On(func(event copilot.SessionEvent) {
			switch event.Type {
			case "assistant.message_delta":
				if event.Data.DeltaContent != nil {
					deltaContents = append(deltaContents, *event.Data.DeltaContent)
				}
			case "session.idle":
				close(done)
			}
		})

		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Wait for completion
		select {
		case <-done:
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for session.idle")
		}

		// Should have received delta events
		if len(deltaContents) == 0 {
			t.Error("Expected to receive delta events, got none")
		}

		// Get the final message to compare
		assistantMessage, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		// Accumulated deltas should equal the final message
		accumulated := strings.Join(deltaContents, "")
		if assistantMessage.Data.Content != nil && accumulated != *assistantMessage.Data.Content {
			t.Errorf("Accumulated deltas don't match final message.\nAccumulated: %q\nFinal: %q", accumulated, *assistantMessage.Data.Content)
		}

		// Final message should contain the answer
		if assistantMessage.Data.Content == nil || !strings.Contains(*assistantMessage.Data.Content, "4") {
			t.Errorf("Expected assistant message to contain '4', got %v", assistantMessage.Data.Content)
		}
	})

	t.Run("should pass streaming option to session creation", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Verify that the streaming option is accepted without errors
		session, err := client.CreateSession(&copilot.SessionConfig{
			Streaming: true,
		})
		if err != nil {
			t.Fatalf("Failed to create session with streaming: %v", err)
		}

		matched, _ := regexp.MatchString(`^[a-f0-9-]+$`, session.SessionID)
		if !matched {
			t.Errorf("Expected session ID to match UUID pattern, got %q", session.SessionID)
		}

		// Session should still work normally
		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}

		if assistantMessage.Data.Content == nil || !strings.Contains(*assistantMessage.Data.Content, "2") {
			t.Errorf("Expected assistant message to contain '2', got %v", assistantMessage.Data.Content)
		}
	})

	t.Run("should receive session events", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
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
		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 100+200?"})
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
		assistantMessage, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
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
		session, err := client.CreateSession(&copilot.SessionConfig{
			ConfigDir: customConfigDir,
		})
		if err != nil {
			t.Fatalf("Failed to create session with custom config dir: %v", err)
		}

		matched, _ := regexp.MatchString(`^[a-f0-9-]+$`, session.SessionID)
		if !matched {
			t.Errorf("Expected session ID to match UUID pattern, got %q", session.SessionID)
		}

		// Session should work normally with custom config dir
		_, err = session.Send(copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, err := testharness.GetFinalAssistantMessage(session, 60*time.Second)
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
		session1, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session1: %v", err)
		}

		_, err = session1.SendAndWait(copilot.MessageOptions{Prompt: "Say hello"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message to session1: %v", err)
		}

		session2, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session2: %v", err)
		}

		_, err = session2.SendAndWait(copilot.MessageOptions{Prompt: "Say goodbye"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message to session2: %v", err)
		}

		// Small delay to ensure session files are written to disk
		time.Sleep(200 * time.Millisecond)

		// List sessions and verify they're included
		sessions, err := client.ListSessions()
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
			t.Errorf("Expected session1 ID %s to be in sessions list", session1.SessionID)
		}
		if !contains(sessionIDs, session2.SessionID) {
			t.Errorf("Expected session2 ID %s to be in sessions list", session2.SessionID)
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
	})

	t.Run("should delete session", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Create a session and send a message to persist it
		session, err := client.CreateSession(nil)
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{Prompt: "Hello"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		sessionID := session.SessionID

		// Small delay to ensure session file is written to disk
		time.Sleep(200 * time.Millisecond)

		// Verify session exists in the list
		sessions, err := client.ListSessions()
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
		err = client.DeleteSession(sessionID)
		if err != nil {
			t.Fatalf("Failed to delete session: %v", err)
		}

		// Verify session no longer exists in the list
		sessionsAfter, err := client.ListSessions()
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
		_, err = client.ResumeSession(sessionID)
		if err == nil {
			t.Error("Expected error when resuming deleted session")
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
