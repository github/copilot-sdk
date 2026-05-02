package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestEventFidelityE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should emit events in correct order for tool-using conversation", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "hello.txt"), []byte("Hello World"), 0644); err != nil {
			t.Fatalf("Failed to write hello.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		var mu sync.Mutex
		var events []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			mu.Lock()
			events = append(events, event)
			mu.Unlock()
		})

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'hello.txt' and tell me its contents.",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		snapshot := snapshotEventFidelityEvents(&mu, &events)
		types := make([]copilot.SessionEventType, 0, len(snapshot))
		for _, event := range snapshot {
			types = append(types, event.Type)
		}

		if !containsEventFidelityType(types, copilot.SessionEventTypeUserMessage) {
			t.Fatalf("Expected user.message event, got %v", types)
		}
		if !containsEventFidelityType(types, copilot.SessionEventTypeAssistantMessage) {
			t.Fatalf("Expected assistant.message event, got %v", types)
		}

		userIdx := firstEventFidelityTypeIndex(types, copilot.SessionEventTypeUserMessage)
		assistantIdx := lastEventFidelityTypeIndex(types, copilot.SessionEventTypeAssistantMessage)
		if userIdx < 0 || assistantIdx < 0 || userIdx >= assistantIdx {
			t.Fatalf("Expected user.message before last assistant.message; types=%v", types)
		}

		idleIdx := lastEventFidelityTypeIndex(types, copilot.SessionEventTypeSessionIdle)
		if idleIdx != len(types)-1 {
			t.Fatalf("Expected session.idle to be last event; idleIdx=%d len=%d types=%v", idleIdx, len(types), types)
		}
	})

	t.Run("should include valid fields on all events", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		var mu sync.Mutex
		var events []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			mu.Lock()
			events = append(events, event)
			mu.Unlock()
		})

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "What is 5+5? Reply with just the number.",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		snapshot := snapshotEventFidelityEvents(&mu, &events)
		for _, event := range snapshot {
			if event.ID == "" {
				t.Fatalf("Expected event id to be populated for %q", event.Type)
			}
			if event.Timestamp.IsZero() {
				t.Fatalf("Expected event timestamp to be populated for %q", event.Type)
			}
		}

		userEvent := firstUserMessageEventFidelityData(snapshot)
		if userEvent == nil || userEvent.Content == "" {
			t.Fatalf("Expected user.message content, got %#v", userEvent)
		}

		assistantEvent := firstAssistantMessageEventFidelityData(snapshot)
		if assistantEvent == nil || assistantEvent.MessageID == "" || assistantEvent.Content == "" {
			t.Fatalf("Expected assistant.message messageId and content, got %#v", assistantEvent)
		}
	})

	t.Run("should emit tool execution events with correct fields", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "data.txt"), []byte("test data"), 0644); err != nil {
			t.Fatalf("Failed to write data.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		var mu sync.Mutex
		var events []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			mu.Lock()
			events = append(events, event)
			mu.Unlock()
		})

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'data.txt'.",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		snapshot := snapshotEventFidelityEvents(&mu, &events)
		var toolStarts []*copilot.ToolExecutionStartData
		var toolCompletes []*copilot.ToolExecutionCompleteData
		for _, event := range snapshot {
			switch data := event.Data.(type) {
			case *copilot.ToolExecutionStartData:
				toolStarts = append(toolStarts, data)
			case *copilot.ToolExecutionCompleteData:
				toolCompletes = append(toolCompletes, data)
			}
		}

		if len(toolStarts) == 0 {
			t.Fatalf("Expected at least one tool.execution_start event; events=%v", eventFidelityTypes(snapshot))
		}
		if len(toolCompletes) == 0 {
			t.Fatalf("Expected at least one tool.execution_complete event; events=%v", eventFidelityTypes(snapshot))
		}
		if toolStarts[0].ToolCallID == "" || toolStarts[0].ToolName == "" {
			t.Fatalf("Expected tool.execution_start toolCallId and toolName, got %#v", toolStarts[0])
		}
		if toolCompletes[0].ToolCallID == "" {
			t.Fatalf("Expected tool.execution_complete toolCallId, got %#v", toolCompletes[0])
		}
	})

	t.Run("should emit assistant.message with messageId", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		var mu sync.Mutex
		var events []copilot.SessionEvent
		session.On(func(event copilot.SessionEvent) {
			mu.Lock()
			events = append(events, event)
			mu.Unlock()
		})

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Say 'pong'.",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		snapshot := snapshotEventFidelityEvents(&mu, &events)
		assistantEvent := firstAssistantMessageEventFidelityData(snapshot)
		if assistantEvent == nil {
			t.Fatalf("Expected at least one assistant.message event; events=%v", eventFidelityTypes(snapshot))
		}
		if assistantEvent.MessageID == "" {
			t.Fatalf("Expected assistant.message messageId, got %#v", assistantEvent)
		}
		if !strings.Contains(assistantEvent.Content, "pong") {
			t.Fatalf("Expected assistant.message content to contain pong, got %q", assistantEvent.Content)
		}
	})
}

func snapshotEventFidelityEvents(mu *sync.Mutex, events *[]copilot.SessionEvent) []copilot.SessionEvent {
	mu.Lock()
	defer mu.Unlock()

	snapshot := make([]copilot.SessionEvent, len(*events))
	copy(snapshot, *events)
	return snapshot
}

func eventFidelityTypes(events []copilot.SessionEvent) []copilot.SessionEventType {
	types := make([]copilot.SessionEventType, 0, len(events))
	for _, event := range events {
		types = append(types, event.Type)
	}
	return types
}

func containsEventFidelityType(types []copilot.SessionEventType, eventType copilot.SessionEventType) bool {
	return firstEventFidelityTypeIndex(types, eventType) >= 0
}

func firstEventFidelityTypeIndex(types []copilot.SessionEventType, eventType copilot.SessionEventType) int {
	for i, typ := range types {
		if typ == eventType {
			return i
		}
	}
	return -1
}

func lastEventFidelityTypeIndex(types []copilot.SessionEventType, eventType copilot.SessionEventType) int {
	for i := len(types) - 1; i >= 0; i-- {
		if types[i] == eventType {
			return i
		}
	}
	return -1
}

func firstUserMessageEventFidelityData(events []copilot.SessionEvent) *copilot.UserMessageData {
	for _, event := range events {
		if data, ok := event.Data.(*copilot.UserMessageData); ok {
			return data
		}
	}
	return nil
}

func firstAssistantMessageEventFidelityData(events []copilot.SessionEvent) *copilot.AssistantMessageData {
	for _, event := range events {
		if data, ok := event.Data.(*copilot.AssistantMessageData); ok {
			return data
		}
	}
	return nil
}
