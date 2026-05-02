package e2e

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// hasImageURLContent returns true if any user message in the given exchanges
// contains an image_url content part (multimodal vision content).
func hasImageURLContent(exchanges []testharness.ParsedHttpExchange) bool {
	for _, ex := range exchanges {
		for _, msg := range ex.Request.Messages {
			if msg.Role == "user" && len(msg.RawContent) > 0 {
				var content []interface{}
				if json.Unmarshal(msg.RawContent, &content) == nil {
					for _, part := range content {
						if m, ok := part.(map[string]interface{}); ok {
							if m["type"] == "image_url" {
								return true
							}
						}
					}
				}
			}
		}
	}
	return false
}

func TestSessionConfigE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	// Write 1x1 PNG to the work directory
	png1x1, err := base64.StdEncoding.DecodeString("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
	if err != nil {
		t.Fatalf("Failed to decode PNG: %v", err)
	}
	if err := os.WriteFile(filepath.Join(ctx.WorkDir, "test.png"), png1x1, 0644); err != nil {
		t.Fatalf("Failed to write test.png: %v", err)
	}

	viewImagePrompt := "Use the view tool to look at the file test.png and describe what you see"

	t.Run("vision disabled then enabled via setModel", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			ModelCapabilities: &copilot.ModelCapabilitiesOverride{
				Supports: &copilot.ModelCapabilitiesOverrideSupports{
					Vision: copilot.Bool(false),
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Turn 1: vision off — no image_url expected
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: viewImagePrompt}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		trafficAfterT1, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if hasImageURLContent(trafficAfterT1) {
			t.Error("Expected no image_url content parts when vision is disabled")
		}

		// Switch vision on
		if err := session.SetModel(t.Context(), "claude-sonnet-4.5", &copilot.SetModelOptions{
			ModelCapabilities: &copilot.ModelCapabilitiesOverride{
				Supports: &copilot.ModelCapabilitiesOverrideSupports{
					Vision: copilot.Bool(true),
				},
			},
		}); err != nil {
			t.Fatalf("SetModel returned error: %v", err)
		}

		// Turn 2: vision on — image_url expected in new exchanges
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: viewImagePrompt}); err != nil {
			t.Fatalf("Failed to send second message: %v", err)
		}

		trafficAfterT2, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges after turn 2: %v", err)
		}
		newExchanges := trafficAfterT2[len(trafficAfterT1):]
		if !hasImageURLContent(newExchanges) {
			t.Error("Expected image_url content parts when vision is enabled")
		}
	})

	t.Run("vision enabled then disabled via setModel", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			ModelCapabilities: &copilot.ModelCapabilitiesOverride{
				Supports: &copilot.ModelCapabilitiesOverrideSupports{
					Vision: copilot.Bool(true),
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Turn 1: vision on — image_url expected
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: viewImagePrompt}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		trafficAfterT1, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges: %v", err)
		}
		if !hasImageURLContent(trafficAfterT1) {
			t.Error("Expected image_url content parts when vision is enabled")
		}

		// Switch vision off
		if err := session.SetModel(t.Context(), "claude-sonnet-4.5", &copilot.SetModelOptions{
			ModelCapabilities: &copilot.ModelCapabilitiesOverride{
				Supports: &copilot.ModelCapabilitiesOverrideSupports{
					Vision: copilot.Bool(false),
				},
			},
		}); err != nil {
			t.Fatalf("SetModel returned error: %v", err)
		}

		// Turn 2: vision off — no image_url expected in new exchanges
		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: viewImagePrompt}); err != nil {
			t.Fatalf("Failed to send second message: %v", err)
		}

		trafficAfterT2, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("Failed to get exchanges after turn 2: %v", err)
		}
		newExchanges := trafficAfterT2[len(trafficAfterT1):]
		if hasImageURLContent(newExchanges) {
			t.Error("Expected no image_url content parts when vision is disabled")
		}
	})
}

// TestSessionConfigExtras mirrors the additional Should_* tests in dotnet/test/SessionConfigTests.cs:
//
//	Should_Use_Custom_SessionId
//	Should_Forward_ClientName_In_UserAgent
//	Should_Forward_Custom_Provider_Headers_On_Create
//	Should_Forward_Custom_Provider_Headers_On_Resume
//	Should_Use_WorkingDirectory_For_Tool_Execution
//	Should_Apply_WorkingDirectory_On_Session_Resume
//	Should_Apply_SystemMessage_On_Session_Resume
//	Should_Apply_AvailableTools_On_Session_Resume
func TestSessionConfigExtrasE2E(t *testing.T) {
	const providerHeaderName = "x-copilot-sdk-provider-header"
	const clientName = "go-public-surface-client"

	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Failed to start client: %v", err)
	}

	t.Run("should use custom sessionId", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		requestedSessionID := newUUID(t)
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SessionID:           requestedSessionID,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if session.SessionID != requestedSessionID {
			t.Errorf("Expected SessionID=%q, got %q", requestedSessionID, session.SessionID)
		}

		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("GetMessages failed: %v", err)
		}
		if len(messages) == 0 || messages[0].Type != copilot.SessionEventTypeSessionStart {
			t.Fatalf("Expected first event to be session.start, got %+v", messages)
		}
		startData := messages[0].Data.(*copilot.SessionStartData)
		if startData.SessionID != requestedSessionID {
			t.Errorf("Expected start.SessionID=%q, got %q", requestedSessionID, startData.SessionID)
		}
	})

	t.Run("should forward clientName in userAgent", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			ClientName:          clientName,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		if len(exchanges) != 1 {
			t.Fatalf("Expected exactly 1 exchange, got %d", len(exchanges))
		}
		if !exchangeHasHeader(exchanges[0], "user-agent", clientName) {
			t.Errorf("Expected user-agent to contain %q, got %v", clientName, exchanges[0].RequestHeaders)
		}
	})

	t.Run("should forward custom provider headers on create", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               "claude-sonnet-4.5",
			Provider:            createProxyProvider(ctx, providerHeaderName, "create-provider-header"),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !assistantMessageContains(message, "2") {
			t.Errorf("Expected response to contain '2', got %v", message)
		}

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		if len(exchanges) != 1 {
			t.Fatalf("Expected exactly 1 exchange, got %d", len(exchanges))
		}
		if !exchangeHasHeader(exchanges[0], "authorization", "Bearer test-provider-key") {
			t.Errorf("Expected authorization header to contain 'Bearer test-provider-key', got %v", exchanges[0].RequestHeaders)
		}
		if !exchangeHasHeader(exchanges[0], providerHeaderName, "create-provider-header") {
			t.Errorf("Expected %s header to contain 'create-provider-header', got %v", providerHeaderName, exchanges[0].RequestHeaders)
		}
	})

	t.Run("should forward custom provider headers on resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session1.SessionID
		t.Cleanup(func() { _ = session1.Disconnect() })

		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Model:               "claude-sonnet-4.5",
			Provider:            createProxyProvider(ctx, providerHeaderName, "resume-provider-header"),
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session2.Disconnect() })

		message, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !assistantMessageContains(message, "4") {
			t.Errorf("Expected response to contain '4', got %v", message)
		}

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		if len(exchanges) != 1 {
			t.Fatalf("Expected exactly 1 exchange, got %d", len(exchanges))
		}
		if !exchangeHasHeader(exchanges[0], "authorization", "Bearer test-provider-key") {
			t.Errorf("Expected authorization header to contain 'Bearer test-provider-key', got %v", exchanges[0].RequestHeaders)
		}
		if !exchangeHasHeader(exchanges[0], providerHeaderName, "resume-provider-header") {
			t.Errorf("Expected %s header to contain 'resume-provider-header', got %v", providerHeaderName, exchanges[0].RequestHeaders)
		}
	})

	t.Run("should use workingDirectory for tool execution", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		subDir := filepath.Join(ctx.WorkDir, "subproject")
		if err := os.MkdirAll(subDir, 0755); err != nil {
			t.Fatalf("MkdirAll failed: %v", err)
		}
		if err := os.WriteFile(filepath.Join(subDir, "marker.txt"), []byte("I am in the subdirectory"), 0644); err != nil {
			t.Fatalf("WriteFile failed: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			WorkingDirectory:    subDir,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		message, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file marker.txt and tell me what it says",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !assistantMessageContains(message, "subdirectory") {
			t.Errorf("Expected response to contain 'subdirectory', got %v", message)
		}
	})

	t.Run("should apply workingDirectory on session resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		subDir := filepath.Join(ctx.WorkDir, "resume-subproject")
		if err := os.MkdirAll(subDir, 0755); err != nil {
			t.Fatalf("MkdirAll failed: %v", err)
		}
		if err := os.WriteFile(filepath.Join(subDir, "resume-marker.txt"), []byte("I am in the resume working directory"), 0644); err != nil {
			t.Fatalf("WriteFile failed: %v", err)
		}

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session1.SessionID
		t.Cleanup(func() { _ = session1.Disconnect() })

		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			WorkingDirectory:    subDir,
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session2.Disconnect() })

		message, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file resume-marker.txt and tell me what it says",
		})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !assistantMessageContains(message, "resume working directory") {
			t.Errorf("Expected response to contain 'resume working directory', got %v", message)
		}
	})

	t.Run("should apply systemMessage on session resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session1.SessionID
		t.Cleanup(func() { _ = session1.Disconnect() })

		const resumeInstruction = "End the response with RESUME_SYSTEM_MESSAGE_SENTINEL."
		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "append",
				Content: resumeInstruction,
			},
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session2.Disconnect() })

		message, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if !assistantMessageContains(message, "RESUME_SYSTEM_MESSAGE_SENTINEL") {
			t.Errorf("Expected response to contain 'RESUME_SYSTEM_MESSAGE_SENTINEL', got %v", message)
		}

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		if len(exchanges) != 1 {
			t.Fatalf("Expected exactly 1 exchange, got %d", len(exchanges))
		}
		if !strings.Contains(getSystemMessage(exchanges[0]), resumeInstruction) {
			t.Errorf("Expected system message to contain %q", resumeInstruction)
		}
	})

	t.Run("should apply availableTools on session resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session1.SessionID
		t.Cleanup(func() { _ = session1.Disconnect() })

		session2, err := client.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      []string{"view"},
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		t.Cleanup(func() { _ = session2.Disconnect() })

		_, err = session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 1+1?"})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		exchanges, err := ctx.GetExchanges()
		if err != nil {
			t.Fatalf("GetExchanges failed: %v", err)
		}
		if len(exchanges) != 1 {
			t.Fatalf("Expected exactly 1 exchange, got %d", len(exchanges))
		}
		toolNames := getToolNames(exchanges[0])
		if len(toolNames) != 1 || toolNames[0] != "view" {
			t.Errorf("Expected toolNames=[view], got %v", toolNames)
		}
	})
}

// createProxyProvider returns a ProviderConfig that points at the test proxy and
// includes a custom header — used for the "should forward custom provider headers" tests.
func createProxyProvider(ctx *testharness.TestContext, headerName, headerValue string) *copilot.ProviderConfig {
	return &copilot.ProviderConfig{
		Type:    "openai",
		BaseURL: ctx.ProxyURL,
		APIKey:  "test-provider-key",
		Headers: map[string]string{
			headerName: headerValue,
		},
	}
}

// newUUID generates a v4 UUID string for tests that need a custom session ID.
func newUUID(t *testing.T) string {
	t.Helper()
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		t.Fatalf("rand.Read failed: %v", err)
	}
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}

// assistantMessageContains returns true when the SendAndWait return value is a
// non-nil assistant.message event whose content contains the given substring.
func assistantMessageContains(message *copilot.SessionEvent, substring string) bool {
	if message == nil {
		return false
	}
	data, ok := message.Data.(*copilot.AssistantMessageData)
	if !ok {
		return false
	}
	return strings.Contains(data.Content, substring)
}
