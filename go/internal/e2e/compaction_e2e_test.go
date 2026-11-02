package e2e

import (
	"errors"
	"os"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestCompactionE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should trigger compaction with low threshold and emit events", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		enabled := true
		backgroundThreshold := 0.005 // 0.5%
		bufferThreshold := 0.01      // 1%

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			InfiniteSessions: &copilot.InfiniteSessionConfig{
				Enabled:                       &enabled,
				BackgroundCompactionThreshold: &backgroundThreshold,
				BufferExhaustionThreshold:     &bufferThreshold,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// The first prompt leaves the session below the compaction processor's minimum
		// message count. The second prompt is therefore the first deterministic point
		// at which low thresholds can trigger compaction. Subscribe before any prompts
		// are sent so we never miss the events. The complete-event subscription filters
		// for Success==true so any transient failed compaction event the daemon may emit
		// before a successful retry is ignored (mirrors the dotnet/rust references).
		startCh := make(chan copilot.SessionEvent, 1)
		completeCh := make(chan copilot.SessionEvent, 1)
		errCh := make(chan error, 1)
		unsubscribe := session.On(func(event copilot.SessionEvent) {
			switch d := event.Data.(type) {
			case *copilot.SessionCompactionStartData:
				select {
				case startCh <- event:
				default:
				}
			case *copilot.SessionCompactionCompleteData:
				if !d.Success {
					return
				}
				select {
				case completeCh <- event:
				default:
				}
			case *copilot.SessionErrorData:
				msg := d.Message
				if msg == "" {
					msg = "session error"
				}
				select {
				case errCh <- errors.New(msg):
				default:
				}
			}
		})
		defer unsubscribe()

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Tell me a story about a dragon. Be detailed."})
		if err != nil {
			t.Fatalf("Failed to send first message: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Continue the story with more details about the dragon's castle."})
		if err != nil {
			t.Fatalf("Failed to send second message: %v", err)
		}

		const compactionTimeout = 60 * time.Second

		var startEvent copilot.SessionEvent
		select {
		case startEvent = <-startCh:
		case err := <-errCh:
			t.Fatalf("Session error waiting for session.compaction_start event: %v", err)
		case <-time.After(compactionTimeout):
			t.Fatalf("Timed out waiting for session.compaction_start event")
		}

		var completeEvent copilot.SessionEvent
		select {
		case completeEvent = <-completeCh:
		case err := <-errCh:
			t.Fatalf("Session error waiting for session.compaction_complete event: %v", err)
		case <-time.After(compactionTimeout):
			t.Fatalf("Timed out waiting for session.compaction_complete event")
		}

		startData, ok := startEvent.Data.(*copilot.SessionCompactionStartData)
		if !ok {
			t.Fatalf("Expected SessionCompactionStartData, got %T", startEvent.Data)
		}
		if startData.ConversationTokens == nil || *startData.ConversationTokens <= 0 {
			t.Errorf("Expected compaction to report conversation tokens at start, got %v", startData.ConversationTokens)
		}

		completeData, ok := completeEvent.Data.(*copilot.SessionCompactionCompleteData)
		if !ok {
			t.Fatalf("Expected SessionCompactionCompleteData, got %T", completeEvent.Data)
		}
		if !completeData.Success {
			t.Errorf("Expected compaction to succeed, error=%v", completeData.Error)
		}
		if completeData.CompactionTokensUsed == nil {
			t.Errorf("Expected compaction tokens-used data")
		} else {
			actualInputTokens := completeData.CompactionTokensUsed.InputTokens
			// Replay has no prompt usage; recording must preserve the provider's value or absence.
			if os.Getenv("GITHUB_ACTIONS") == "true" {
				if actualInputTokens != nil && *actualInputTokens != 0 {
					t.Errorf("Expected replay compaction input tokens to be zero or unavailable, got %d", *actualInputTokens)
				}
			} else {
				var expectedInputTokens *int64
				exchanges, err := ctx.GetExchanges()
				if err != nil {
					t.Fatalf("Failed to get provider responses: %v", err)
				}
				var compactions []*testharness.CompactionProviderUsage
				for _, exchange := range exchanges {
					if exchange.CompactionUsage != nil &&
						completeData.SummaryContent != nil && exchange.CompactionUsage.Summary == *completeData.SummaryContent {
						compactions = append(compactions, exchange.CompactionUsage)
					}
				}
				if len(compactions) != 1 {
					t.Fatalf("Expected one provider compaction chain for the completed summary, got %d", len(compactions))
				}
				expectedInputTokens = compactions[0].InputTokens
				if expectedInputTokens == nil {
					if actualInputTokens != nil {
						t.Errorf("Expected provider input tokens to be unavailable, got %d", *actualInputTokens)
					}
				} else if actualInputTokens == nil {
					t.Errorf("Expected provider input tokens %d, got unavailable", *expectedInputTokens)
				} else if *actualInputTokens != *expectedInputTokens {
					t.Errorf("Expected provider input tokens %d, got %d", *expectedInputTokens, *actualInputTokens)
				}
			}
		}
		summary := ""
		if completeData.SummaryContent != nil {
			summary = *completeData.SummaryContent
		}
		summary = strings.ToLower(summary)
		if !strings.Contains(summary, "<overview>") {
			t.Errorf("Expected summary to contain <overview>, got: %q", summary)
		}
		if !strings.Contains(summary, "<history>") {
			t.Errorf("Expected summary to contain <history>, got: %q", summary)
		}
		if !strings.Contains(summary, "<checkpoint_title>") {
			t.Errorf("Expected summary to contain <checkpoint_title>, got: %q", summary)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Now describe the dragon's treasure in great detail."})
		if err != nil {
			t.Fatalf("Failed to send third message: %v", err)
		}

		// Verify session still works after compaction
		answer, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What was the story about?"})
		if err != nil {
			t.Fatalf("Failed to send verification message: %v", err)
		}
		ad, ok := answer.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected assistant message data, got %T", answer.Data)
		}
		content := strings.ToLower(ad.Content)
		// Should remember it was about a dragon (context preserved via summary)
		if !strings.Contains(content, "kaedrith") {
			t.Errorf("Expected answer to mention 'Kaedrith', got: %q", ad.Content)
		}
		if !strings.Contains(content, "dragon") {
			t.Errorf("Expected answer to mention 'dragon', got: %q", ad.Content)
		}
	})

	t.Run("should not emit compaction events when infinite sessions disabled", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		enabled := false
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			InfiniteSessions: &copilot.InfiniteSessionConfig{
				Enabled: &enabled,
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var compactionEvents []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			switch event.Data.(type) {
			case *copilot.SessionCompactionStartData, *copilot.SessionCompactionCompleteData:
				compactionEvents = append(compactionEvents, event)
			}
		})

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 2+2?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Should not have any compaction events when disabled
		if len(compactionEvents) != 0 {
			t.Errorf("Expected 0 compaction events when disabled, got %d", len(compactionEvents))
		}
	})

}
