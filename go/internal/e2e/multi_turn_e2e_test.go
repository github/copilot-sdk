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

func TestMultiTurnE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should use tool results from previous turns", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		if err := os.WriteFile(filepath.Join(ctx.WorkDir, "secret.txt"), []byte("The magic number is 42."), 0644); err != nil {
			t.Fatalf("Failed to write secret.txt: %v", err)
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

		msg1, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'secret.txt' and tell me what the magic number is.",
		})
		if err != nil {
			t.Fatalf("First SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg1); !strings.Contains(content, "42") {
			t.Fatalf("Expected first response to contain 42, got %q", content)
		}
		assertToolTurnOrdering(t, snapshotAndClearMultiTurnEvents(&mu, &events), "file read turn")

		msg2, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "What is that magic number multiplied by 2?",
		})
		if err != nil {
			t.Fatalf("Second SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg2); !strings.Contains(content, "84") {
			t.Fatalf("Expected second response to contain 84, got %q", content)
		}
	})

	t.Run("should handle file creation then reading across turns", func(t *testing.T) {
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
			Prompt: "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'.",
		}); err != nil {
			t.Fatalf("First SendAndWait failed: %v", err)
		}
		// File should have been created with the expected content
		greetingContent, err := os.ReadFile(filepath.Join(ctx.WorkDir, "greeting.txt"))
		if err != nil {
			t.Fatalf("Failed to read greeting.txt: %v", err)
		}
		if !strings.Contains(string(greetingContent), "Hello from multi-turn test") {
			t.Errorf("Expected greeting.txt to contain 'Hello from multi-turn test', got %q", string(greetingContent))
		}
		assertToolTurnOrdering(t, snapshotAndClearMultiTurnEvents(&mu, &events), "file creation turn")

		msg, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Read the file 'greeting.txt' and tell me its exact contents.",
		})
		if err != nil {
			t.Fatalf("Second SendAndWait failed: %v", err)
		}
		if content := assistantContent(t, msg); !strings.Contains(content, "Hello from multi-turn test") {
			t.Fatalf("Expected response to contain created file contents, got %q", content)
		}
		assertToolTurnOrdering(t, snapshotAndClearMultiTurnEvents(&mu, &events), "file read turn")
	})
}

func snapshotAndClearMultiTurnEvents(mu *sync.Mutex, events *[]copilot.SessionEvent) []copilot.SessionEvent {
	mu.Lock()
	defer mu.Unlock()
	snapshot := make([]copilot.SessionEvent, len(*events))
	copy(snapshot, *events)
	*events = (*events)[:0]
	return snapshot
}

// assertToolTurnOrdering verifies that for a turn with tool use the events arrive in the
// expected order: user.message → tool.execution_start(s) → tool.execution_complete(s)
// → assistant.message → session.idle.
func assertToolTurnOrdering(t *testing.T, events []copilot.SessionEvent, turnDescription string) {
	t.Helper()

	observedTypes := make([]copilot.SessionEventType, 0, len(events))
	for _, e := range events {
		observedTypes = append(observedTypes, e.Type())
	}

	userMessageIdx := indexOfEventType(events, copilot.SessionEventTypeUserMessage, 0)
	if userMessageIdx < 0 {
		// A turn without a tool call (e.g., pure text answer) may not need ordering.
		// Only assert if tool events are present.
		if !containsEventType(events, copilot.SessionEventTypeToolExecutionStart) {
			return
		}
		t.Errorf("Expected user.message in %s but none found; types=%v", turnDescription, observedTypes)
		return
	}

	firstToolStartIdx := indexOfEventType(events, copilot.SessionEventTypeToolExecutionStart, 0)
	if firstToolStartIdx < 0 {
		// No tool use in this turn — nothing to assert.
		return
	}
	lastToolCompleteIdx := lastIndexOfEventType(events, copilot.SessionEventTypeToolExecutionComplete)
	assistantAfterToolsIdx := indexOfEventType(events, copilot.SessionEventTypeAssistantMessage, lastToolCompleteIdx+1)
	sessionIdleIdx := indexOfEventType(events, copilot.SessionEventTypeSessionIdle, 0)

	if userMessageIdx >= firstToolStartIdx {
		t.Errorf("[%s] Expected user.message before first tool start; types=%v", turnDescription, observedTypes)
	}

	// Match each tool.execution_complete to a preceding tool.execution_start with the same ToolCallID.
	starts := make(map[string]int)
	for i, e := range events {
		if e.Type() == copilot.SessionEventTypeToolExecutionStart {
			if d, ok := e.Data.(*copilot.ToolExecutionStartData); ok {
				starts[d.ToolCallID] = i
			}
		}
	}
	for _, e := range events {
		if e.Type() == copilot.SessionEventTypeToolExecutionComplete {
			if d, ok := e.Data.(*copilot.ToolExecutionCompleteData); ok {
				if _, found := starts[d.ToolCallID]; !found {
					t.Errorf("[%s] tool.execution_complete for %q has no matching tool.execution_start; types=%v",
						turnDescription, d.ToolCallID, observedTypes)
				}
			}
		}
	}

	if assistantAfterToolsIdx < 0 {
		t.Errorf("[%s] Expected assistant.message after final tool completion; types=%v", turnDescription, observedTypes)
	}
	if sessionIdleIdx < 0 {
		t.Errorf("[%s] Expected session.idle; types=%v", turnDescription, observedTypes)
	}
	if assistantAfterToolsIdx >= 0 && lastToolCompleteIdx >= assistantAfterToolsIdx {
		t.Errorf("[%s] Expected final tool completion before final assistant.message; types=%v", turnDescription, observedTypes)
	}
	if assistantAfterToolsIdx >= 0 && sessionIdleIdx >= 0 && assistantAfterToolsIdx >= sessionIdleIdx {
		t.Errorf("[%s] Expected assistant.message before session.idle; types=%v", turnDescription, observedTypes)
	}
}

func indexOfEventType(events []copilot.SessionEvent, typ copilot.SessionEventType, startIdx int) int {
	for i := startIdx; i < len(events); i++ {
		if events[i].Type() == typ {
			return i
		}
	}
	return -1
}

func lastIndexOfEventType(events []copilot.SessionEvent, typ copilot.SessionEventType) int {
	for i := len(events) - 1; i >= 0; i-- {
		if events[i].Type() == typ {
			return i
		}
	}
	return -1
}

func containsEventType(events []copilot.SessionEvent, typ copilot.SessionEventType) bool {
	return indexOfEventType(events, typ, 0) >= 0
}
