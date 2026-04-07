package e2e

import (
	"encoding/base64"
	"encoding/json"
	"os"
	"path/filepath"
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

func TestSessionConfig(t *testing.T) {
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
