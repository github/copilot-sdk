package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestCompaction(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should trigger compaction with low threshold and emit events", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		enabled := true
		backgroundThreshold := 0.005 // 0.5%
		bufferThreshold := 0.01      // 1%

		session, err := client.CreateSession(&copilot.SessionConfig{
			InfiniteSessions: &copilot.InfiniteSessionConfig{
				Enabled:                       &enabled,
				BackgroundCompactionThreshold: &backgroundThreshold,
				BufferExhaustionThreshold:     &bufferThreshold,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var compactionStartEvents []copilot.SessionEvent
		var compactionCompleteEvents []copilot.SessionEvent

		session.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionCompactionStart {
				compactionStartEvents = append(compactionStartEvents, event)
			}
			if event.Type == copilot.SessionCompactionComplete {
				compactionCompleteEvents = append(compactionCompleteEvents, event)
			}
		})

		// Send multiple messages to fill up the context window
		_, err = session.SendAndWait(copilot.MessageOptions{Prompt: "Tell me a long story about a dragon. Be very detailed."}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send first message: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{Prompt: "Continue the story with more details about the dragon's castle."}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send second message: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{Prompt: "Now describe the dragon's treasure in great detail."}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send third message: %v", err)
		}

		// Should have triggered compaction at least once
		if len(compactionStartEvents) < 1 {
			t.Errorf("Expected at least 1 compaction_start event, got %d", len(compactionStartEvents))
		}
		if len(compactionCompleteEvents) < 1 {
			t.Errorf("Expected at least 1 compaction_complete event, got %d", len(compactionCompleteEvents))
		}

		// Compaction should have succeeded
		if len(compactionCompleteEvents) > 0 {
			lastComplete := compactionCompleteEvents[len(compactionCompleteEvents)-1]
			if lastComplete.Data.Success == nil || !*lastComplete.Data.Success {
				t.Errorf("Expected compaction to succeed")
			}
			if lastComplete.Data.TokensRemoved != nil && *lastComplete.Data.TokensRemoved <= 0 {
				t.Errorf("Expected tokensRemoved > 0, got %v", *lastComplete.Data.TokensRemoved)
			}
		}

		// Verify session still works after compaction
		answer, err := session.SendAndWait(copilot.MessageOptions{Prompt: "What was the story about?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send verification message: %v", err)
		}
		if answer.Data.Content == nil || !strings.Contains(strings.ToLower(*answer.Data.Content), "dragon") {
			t.Errorf("Expected answer to contain 'dragon', got %v", answer.Data.Content)
		}
	})

	t.Run("should not emit compaction events when infinite sessions disabled", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		enabled := false
		session, err := client.CreateSession(&copilot.SessionConfig{
			InfiniteSessions: &copilot.InfiniteSessionConfig{
				Enabled: &enabled,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var compactionEvents []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionCompactionStart || event.Type == copilot.SessionCompactionComplete {
				compactionEvents = append(compactionEvents, event)
			}
		})

		_, err = session.SendAndWait(copilot.MessageOptions{Prompt: "What is 2+2?"}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Should not have any compaction events when disabled
		if len(compactionEvents) != 0 {
			t.Errorf("Expected 0 compaction events when disabled, got %d", len(compactionEvents))
		}
	})
}
