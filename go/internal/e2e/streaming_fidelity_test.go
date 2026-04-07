package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestStreamingFidelity(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should produce delta events when streaming is enabled", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Streaming:           true,
		})
		if err != nil {
			t.Fatalf("Failed to create session with streaming: %v", err)
		}

		var events []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			events = append(events, event)
		})

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Count from 1 to 5, separated by commas."})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Should have streaming deltas before the final message
		var deltaEvents []copilot.SessionEvent
		for _, e := range events {
			if e.Type == "assistant.message_delta" {
				deltaEvents = append(deltaEvents, e)
			}
		}
		if len(deltaEvents) < 1 {
			t.Error("Expected at least 1 delta event")
		}

		// Deltas should have content
		for _, delta := range deltaEvents {
			if delta.Data.DeltaContent == nil {
				t.Error("Expected delta to have content")
			}
		}

		// Should still have a final assistant.message
		hasAssistantMessage := false
		for _, e := range events {
			if e.Type == "assistant.message" {
				hasAssistantMessage = true
				break
			}
		}
		if !hasAssistantMessage {
			t.Error("Expected a final assistant.message event")
		}

		// Deltas should come before the final message
		firstDeltaIdx := -1
		lastAssistantIdx := -1
		for i, e := range events {
			if e.Type == "assistant.message_delta" && firstDeltaIdx == -1 {
				firstDeltaIdx = i
			}
			if e.Type == "assistant.message" {
				lastAssistantIdx = i
			}
		}
		if firstDeltaIdx >= lastAssistantIdx {
			t.Errorf("Expected deltas before final message, got delta at %d, message at %d", firstDeltaIdx, lastAssistantIdx)
		}
	})

	t.Run("should not produce deltas when streaming is disabled", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Streaming:           false,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		var events []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			events = append(events, event)
		})

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say 'hello world'."})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// No deltas when streaming is off
		var deltaEvents []copilot.SessionEvent
		for _, e := range events {
			if e.Type == "assistant.message_delta" {
				deltaEvents = append(deltaEvents, e)
			}
		}
		if len(deltaEvents) != 0 {
			t.Errorf("Expected no delta events, got %d", len(deltaEvents))
		}

		// But should still have a final assistant.message
		var assistantEvents []copilot.SessionEvent
		for _, e := range events {
			if e.Type == "assistant.message" {
				assistantEvents = append(assistantEvents, e)
			}
		}
		if len(assistantEvents) < 1 {
			t.Error("Expected at least 1 assistant.message event")
		}
	})

	t.Run("should produce deltas after session resume", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Streaming:           false,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "What is 3 + 6?"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Resume using a new client
		newClient := ctx.NewClient()
		defer newClient.ForceStop()

		session2, err := newClient.ResumeSession(t.Context(), session.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Streaming:           true,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		var events []copilot.SessionEvent
		session2.On(func(event copilot.SessionEvent) {
			events = append(events, event)
		})

		answer, err := session2.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Now if you double that, what do you get?"})
		if err != nil {
			t.Fatalf("Failed to send follow-up message: %v", err)
		}
		if answer == nil || answer.Data.Content == nil || !strings.Contains(*answer.Data.Content.String, "18") {
			t.Errorf("Expected answer to contain '18', got %v", answer)
		}

		// Should have streaming deltas before the final message
		var deltaEvents []copilot.SessionEvent
		for _, e := range events {
			if e.Type == "assistant.message_delta" {
				deltaEvents = append(deltaEvents, e)
			}
		}
		if len(deltaEvents) < 1 {
			t.Error("Expected at least 1 delta event")
		}

		// Deltas should have content
		for _, delta := range deltaEvents {
			if delta.Data.DeltaContent == nil {
				t.Error("Expected delta to have content")
			}
		}
	})
}
