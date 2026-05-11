package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestEventFidelityE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should emit assistant usage event after model call", func(t *testing.T) {
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

		var usageEvent *copilot.AssistantUsageData
		for i := len(snapshot) - 1; i >= 0; i-- {
			if d, ok := snapshot[i].Data.(*copilot.AssistantUsageData); ok {
				usageEvent = d
				break
			}
		}

		if usageEvent == nil {
			t.Fatalf("Expected at least one assistant.usage event; events=%v", eventFidelityTypes(snapshot))
		}
		if usageEvent.Model == "" {
			t.Errorf("Expected assistant.usage event to have a non-empty model field, got %#v", usageEvent)
		}

		// Verify the event itself has a valid ID and timestamp
		for _, evt := range snapshot {
			if _, ok := evt.Data.(*copilot.AssistantUsageData); ok {
				if evt.ID == "" {
					t.Error("Expected assistant.usage event to have a non-empty ID")
				}
				if evt.Timestamp.IsZero() {
					t.Error("Expected assistant.usage event to have a non-zero timestamp")
				}
				break
			}
		}
	})

	t.Run("should emit session usage info event after model call", func(t *testing.T) {
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

		var usageInfo *copilot.SessionUsageInfoData
		for i := len(snapshot) - 1; i >= 0; i-- {
			if d, ok := snapshot[i].Data.(*copilot.SessionUsageInfoData); ok {
				usageInfo = d
				break
			}
		}

		if usageInfo == nil {
			t.Fatalf("Expected at least one session.usage_info event; events=%v", eventFidelityTypes(snapshot))
		}
		if usageInfo.CurrentTokens <= 0 {
			t.Errorf("Expected session.usage_info.currentTokens > 0, got %v", usageInfo.CurrentTokens)
		}
		if usageInfo.MessagesLength <= 0 {
			t.Errorf("Expected session.usage_info.messagesLength > 0, got %v", usageInfo.MessagesLength)
		}
		if usageInfo.TokenLimit <= 0 {
			t.Errorf("Expected session.usage_info.tokenLimit > 0, got %v", usageInfo.TokenLimit)
		}
	})

	t.Run("should emit pending messages modified event when message queue changes", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		pendingModified := make(chan *copilot.SessionEvent, 1)
		session.On(func(event copilot.SessionEvent) {
			if _, ok := event.Data.(*copilot.PendingMessagesModifiedData); ok {
				select {
				case pendingModified <- &event:
				default:
				}
			}
		})

		if _, err := session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "What is 9+9? Reply with just the number.",
		}); err != nil {
			t.Fatalf("Send failed: %v", err)
		}

		select {
		case evt := <-pendingModified:
			if evt == nil {
				t.Error("Expected a non-nil pending_messages.modified event")
			}
		case <-time.After(60 * time.Second):
			t.Fatal("Timed out waiting for pending_messages.modified event")
		}

		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get final assistant message: %v", err)
		}
		if ad, ok := answer.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(ad.Content, "18") {
			t.Errorf("Expected answer to contain '18', got %v", answer.Data)
		}
	})

	t.Run("should preserve message order in getmessages after tool use", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "order.txt"), []byte("ORDER_CONTENT_42"), 0644); err != nil {
			t.Fatalf("Failed to write order.txt: %v", err)
		}

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}
		t.Cleanup(func() { _ = session.Disconnect() })

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'order.txt' and tell me what the number is.",
		}); err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}

		messages, err := session.GetMessages(t.Context())
		if err != nil {
			t.Fatalf("GetMessages failed: %v", err)
		}

		types := make([]copilot.SessionEventType, 0, len(messages))
		for _, m := range messages {
			types = append(types, m.Type())
		}

		sessionStartIdx := -1
		userMsgIdx := -1
		toolStartIdx := -1
		toolCompleteIdx := -1
		assistantMsgIdx := -1

		for i, typ := range types {
			if typ == copilot.SessionEventTypeSessionStart && sessionStartIdx < 0 {
				sessionStartIdx = i
			}
			if typ == copilot.SessionEventTypeUserMessage && userMsgIdx < 0 {
				userMsgIdx = i
			}
			if typ == copilot.SessionEventTypeToolExecutionStart && toolStartIdx < 0 {
				toolStartIdx = i
			}
			if typ == copilot.SessionEventTypeToolExecutionComplete && toolCompleteIdx < 0 {
				toolCompleteIdx = i
			}
			if typ == copilot.SessionEventTypeAssistantMessage {
				assistantMsgIdx = i
			}
		}

		if sessionStartIdx < 0 {
			t.Fatalf("Expected session.start event in GetMessages; types=%v", types)
		}
		if userMsgIdx < 0 {
			t.Fatalf("Expected user.message event in GetMessages; types=%v", types)
		}
		if toolStartIdx < 0 {
			t.Fatalf("Expected tool.execution_start event in GetMessages; types=%v", types)
		}
		if toolCompleteIdx < 0 {
			t.Fatalf("Expected tool.execution_complete event in GetMessages; types=%v", types)
		}
		if assistantMsgIdx < 0 {
			t.Fatalf("Expected assistant.message event in GetMessages; types=%v", types)
		}

		if sessionStartIdx >= userMsgIdx {
			t.Errorf("Expected session.start (%d) before user.message (%d); types=%v", sessionStartIdx, userMsgIdx, types)
		}
		if userMsgIdx >= toolStartIdx {
			t.Errorf("Expected user.message (%d) before tool.execution_start (%d); types=%v", userMsgIdx, toolStartIdx, types)
		}
		if toolStartIdx >= toolCompleteIdx {
			t.Errorf("Expected tool.execution_start (%d) before tool.execution_complete (%d); types=%v", toolStartIdx, toolCompleteIdx, types)
		}
		if toolCompleteIdx >= assistantMsgIdx {
			t.Errorf("Expected tool.execution_complete (%d) before final assistant.message (%d); types=%v", toolCompleteIdx, assistantMsgIdx, types)
		}

		// Verify user.message mentions the file
		for _, msg := range messages {
			if msg.Type() == copilot.SessionEventTypeUserMessage {
				if d, ok := msg.Data.(*copilot.UserMessageData); ok {
					if !strings.Contains(d.Content, "order.txt") {
						t.Errorf("Expected user.message to mention 'order.txt', got %q", d.Content)
					}
				}
				break
			}
		}

		// Verify assistant.message references the number
		for i := len(messages) - 1; i >= 0; i-- {
			if messages[i].Type() == copilot.SessionEventTypeAssistantMessage {
				if d, ok := messages[i].Data.(*copilot.AssistantMessageData); ok {
					if !strings.Contains(d.Content, "42") {
						t.Errorf("Expected assistant.message to contain '42', got %q", d.Content)
					}
				}
				break
			}
		}
	})

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
			types = append(types, event.Type())
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
				t.Fatalf("Expected event id to be populated for %q", event.Type())
			}
			if event.Timestamp.IsZero() {
				t.Fatalf("Expected event timestamp to be populated for %q", event.Type())
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
		types = append(types, event.Type())
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
