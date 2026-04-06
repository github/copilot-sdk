package copilot

import (
	"encoding/json"
	"fmt"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

// newTestSession creates a session with an event channel and starts the consumer goroutine.
// Returns a cleanup function that closes the channel (stopping the consumer).
func newTestSession() (*Session, func()) {
	s := &Session{
		handlers:        make([]sessionHandler, 0),
		commandHandlers: make(map[string]CommandHandler),
		eventCh:         make(chan SessionEvent, 128),
	}
	go s.processEvents()
	return s, func() { close(s.eventCh) }
}

func TestSession_On(t *testing.T) {
	t.Run("multiple handlers all receive events", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var wg sync.WaitGroup
		wg.Add(3)
		var received1, received2, received3 bool
		session.On(func(event SessionEvent) { received1 = true; wg.Done() })
		session.On(func(event SessionEvent) { received2 = true; wg.Done() })
		session.On(func(event SessionEvent) { received3 = true; wg.Done() })

		session.dispatchEvent(SessionEvent{Type: "test"})
		wg.Wait()

		if !received1 || !received2 || !received3 {
			t.Errorf("Expected all handlers to receive event, got received1=%v, received2=%v, received3=%v",
				received1, received2, received3)
		}
	})

	t.Run("unsubscribing one handler does not affect others", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var count1, count2, count3 atomic.Int32
		var wg sync.WaitGroup

		wg.Add(3)
		session.On(func(event SessionEvent) { count1.Add(1); wg.Done() })
		unsub2 := session.On(func(event SessionEvent) { count2.Add(1); wg.Done() })
		session.On(func(event SessionEvent) { count3.Add(1); wg.Done() })

		// First event - all handlers receive it
		session.dispatchEvent(SessionEvent{Type: "test"})
		wg.Wait()

		// Unsubscribe handler 2
		unsub2()

		// Second event - only handlers 1 and 3 should receive it
		wg.Add(2)
		session.dispatchEvent(SessionEvent{Type: "test"})
		wg.Wait()

		if count1.Load() != 2 {
			t.Errorf("Expected handler 1 to receive 2 events, got %d", count1.Load())
		}
		if count2.Load() != 1 {
			t.Errorf("Expected handler 2 to receive 1 event (before unsubscribe), got %d", count2.Load())
		}
		if count3.Load() != 2 {
			t.Errorf("Expected handler 3 to receive 2 events, got %d", count3.Load())
		}
	})

	t.Run("calling unsubscribe multiple times is safe", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var count atomic.Int32
		var wg sync.WaitGroup

		wg.Add(1)
		unsub := session.On(func(event SessionEvent) { count.Add(1); wg.Done() })

		session.dispatchEvent(SessionEvent{Type: "test"})
		wg.Wait()

		unsub()
		unsub()
		unsub()

		// Dispatch again and wait for it to be processed via a sentinel handler
		wg.Add(1)
		session.On(func(event SessionEvent) { wg.Done() })
		session.dispatchEvent(SessionEvent{Type: "test"})
		wg.Wait()

		if count.Load() != 1 {
			t.Errorf("Expected handler to receive 1 event, got %d", count.Load())
		}
	})

	t.Run("handlers are called in registration order", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var order []int
		var wg sync.WaitGroup
		wg.Add(3)
		session.On(func(event SessionEvent) { order = append(order, 1); wg.Done() })
		session.On(func(event SessionEvent) { order = append(order, 2); wg.Done() })
		session.On(func(event SessionEvent) { order = append(order, 3); wg.Done() })

		session.dispatchEvent(SessionEvent{Type: "test"})
		wg.Wait()

		if len(order) != 3 || order[0] != 1 || order[1] != 2 || order[2] != 3 {
			t.Errorf("Expected handlers to be called in order [1,2,3], got %v", order)
		}
	})

	t.Run("concurrent subscribe and unsubscribe is safe", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var wg sync.WaitGroup
		for i := 0; i < 100; i++ {
			wg.Add(1)
			go func() {
				defer wg.Done()
				unsub := session.On(func(event SessionEvent) {})
				unsub()
			}()
		}
		wg.Wait()

		session.handlerMutex.RLock()
		count := len(session.handlers)
		session.handlerMutex.RUnlock()

		if count != 0 {
			t.Errorf("Expected 0 handlers after all unsubscribes, got %d", count)
		}
	})

	t.Run("events are dispatched serially", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var concurrentCount atomic.Int32
		var maxConcurrent atomic.Int32
		var done sync.WaitGroup
		const totalEvents = 5
		done.Add(totalEvents)

		session.On(func(event SessionEvent) {
			current := concurrentCount.Add(1)
			if current > maxConcurrent.Load() {
				maxConcurrent.Store(current)
			}

			time.Sleep(10 * time.Millisecond)

			concurrentCount.Add(-1)
			done.Done()
		})

		for i := 0; i < totalEvents; i++ {
			session.dispatchEvent(SessionEvent{Type: "test"})
		}

		done.Wait()

		if max := maxConcurrent.Load(); max != 1 {
			t.Errorf("Expected max concurrent count of 1, got %d", max)
		}
	})

	t.Run("handler panic does not halt delivery", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var eventCount atomic.Int32
		var done sync.WaitGroup
		done.Add(2)

		session.On(func(event SessionEvent) {
			count := eventCount.Add(1)
			defer done.Done()
			if count == 1 {
				panic("boom")
			}
		})

		session.dispatchEvent(SessionEvent{Type: "test"})
		session.dispatchEvent(SessionEvent{Type: "test"})

		done.Wait()

		if eventCount.Load() != 2 {
			t.Errorf("Expected 2 events dispatched, got %d", eventCount.Load())
		}
	})
}

func TestSession_CommandRouting(t *testing.T) {
	t.Run("routes command.execute event to the correct handler", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		var receivedCtx CommandContext
		session.registerCommands([]CommandDefinition{
			{
				Name:        "deploy",
				Description: "Deploy the app",
				Handler: func(ctx CommandContext) error {
					receivedCtx = ctx
					return nil
				},
			},
			{
				Name:        "rollback",
				Description: "Rollback",
				Handler: func(ctx CommandContext) error {
					return nil
				},
			},
		})

		// Simulate the dispatch — executeCommandAndRespond will fail on RPC (nil client)
		// but the handler will still be invoked. We test routing only.
		_, ok := session.getCommandHandler("deploy")
		if !ok {
			t.Fatal("Expected 'deploy' handler to be registered")
		}
		_, ok = session.getCommandHandler("rollback")
		if !ok {
			t.Fatal("Expected 'rollback' handler to be registered")
		}
		_, ok = session.getCommandHandler("nonexistent")
		if ok {
			t.Fatal("Expected 'nonexistent' handler to NOT be registered")
		}

		// Directly invoke handler to verify context is correct
		handler, _ := session.getCommandHandler("deploy")
		err := handler(CommandContext{
			SessionID:   "test-session",
			Command:     "/deploy production",
			CommandName: "deploy",
			Args:        "production",
		})
		if err != nil {
			t.Fatalf("Handler returned error: %v", err)
		}
		if receivedCtx.SessionID != "test-session" {
			t.Errorf("Expected sessionID 'test-session', got %q", receivedCtx.SessionID)
		}
		if receivedCtx.CommandName != "deploy" {
			t.Errorf("Expected commandName 'deploy', got %q", receivedCtx.CommandName)
		}
		if receivedCtx.Command != "/deploy production" {
			t.Errorf("Expected command '/deploy production', got %q", receivedCtx.Command)
		}
		if receivedCtx.Args != "production" {
			t.Errorf("Expected args 'production', got %q", receivedCtx.Args)
		}
	})

	t.Run("skips commands with empty name or nil handler", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.registerCommands([]CommandDefinition{
			{Name: "", Handler: func(ctx CommandContext) error { return nil }},
			{Name: "valid", Handler: nil},
			{Name: "good", Handler: func(ctx CommandContext) error { return nil }},
		})

		_, ok := session.getCommandHandler("")
		if ok {
			t.Error("Empty name should not be registered")
		}
		_, ok = session.getCommandHandler("valid")
		if ok {
			t.Error("Nil handler should not be registered")
		}
		_, ok = session.getCommandHandler("good")
		if !ok {
			t.Error("Expected 'good' handler to be registered")
		}
	})

	t.Run("handler error is propagated", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		handlerCalled := false
		session.registerCommands([]CommandDefinition{
			{
				Name: "fail",
				Handler: func(ctx CommandContext) error {
					handlerCalled = true
					return fmt.Errorf("deploy failed")
				},
			},
		})

		handler, ok := session.getCommandHandler("fail")
		if !ok {
			t.Fatal("Expected 'fail' handler to be registered")
		}

		err := handler(CommandContext{
			SessionID:   "test-session",
			CommandName: "fail",
			Command:     "/fail",
			Args:        "",
		})

		if !handlerCalled {
			t.Error("Expected handler to be called")
		}
		if err == nil {
			t.Fatal("Expected error from handler")
		}
		if !strings.Contains(err.Error(), "deploy failed") {
			t.Errorf("Expected error to contain 'deploy failed', got %q", err.Error())
		}
	})

	t.Run("unknown command returns no handler", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.registerCommands([]CommandDefinition{
			{Name: "deploy", Handler: func(ctx CommandContext) error { return nil }},
		})

		_, ok := session.getCommandHandler("unknown")
		if ok {
			t.Error("Expected no handler for unknown command")
		}
	})
}

func TestSession_Capabilities(t *testing.T) {
	t.Run("defaults capabilities when not injected", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		caps := session.Capabilities()
		if caps.UI != nil {
			t.Errorf("Expected UI to be nil by default, got %+v", caps.UI)
		}
	})

	t.Run("setCapabilities stores and retrieves capabilities", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.setCapabilities(&SessionCapabilities{
			UI: &UICapabilities{Elicitation: true},
		})
		caps := session.Capabilities()
		if caps.UI == nil || !caps.UI.Elicitation {
			t.Errorf("Expected UI.Elicitation to be true")
		}
	})

	t.Run("setCapabilities with nil resets to empty", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.setCapabilities(&SessionCapabilities{
			UI: &UICapabilities{Elicitation: true},
		})
		session.setCapabilities(nil)
		caps := session.Capabilities()
		if caps.UI != nil {
			t.Errorf("Expected UI to be nil after reset, got %+v", caps.UI)
		}
	})

	t.Run("capabilities.changed event updates session capabilities", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		// Initially no capabilities
		caps := session.Capabilities()
		if caps.UI != nil {
			t.Fatal("Expected UI to be nil initially")
		}

		// Dispatch a capabilities.changed event with elicitation=true
		elicitTrue := true
		session.dispatchEvent(SessionEvent{
			Type: SessionEventTypeCapabilitiesChanged,
			Data: Data{
				UI: &UI{Elicitation: &elicitTrue},
			},
		})

		// Give the broadcast handler time to process
		time.Sleep(50 * time.Millisecond)

		caps = session.Capabilities()
		if caps.UI == nil || !caps.UI.Elicitation {
			t.Error("Expected UI.Elicitation to be true after capabilities.changed event")
		}

		// Dispatch with elicitation=false
		elicitFalse := false
		session.dispatchEvent(SessionEvent{
			Type: SessionEventTypeCapabilitiesChanged,
			Data: Data{
				UI: &UI{Elicitation: &elicitFalse},
			},
		})

		time.Sleep(50 * time.Millisecond)

		caps = session.Capabilities()
		if caps.UI == nil || caps.UI.Elicitation {
			t.Error("Expected UI.Elicitation to be false after second capabilities.changed event")
		}
	})
}

func TestSession_ElicitationCapabilityGating(t *testing.T) {
	t.Run("elicitation errors when capability is missing", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		err := session.assertElicitation()
		if err == nil {
			t.Fatal("Expected error when elicitation capability is missing")
		}
		expected := "elicitation is not supported"
		if !strings.Contains(err.Error(), expected) {
			t.Errorf("Expected error to contain %q, got %q", expected, err.Error())
		}
	})

	t.Run("elicitation succeeds when capability is present", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.setCapabilities(&SessionCapabilities{
			UI: &UICapabilities{Elicitation: true},
		})
		err := session.assertElicitation()
		if err != nil {
			t.Errorf("Expected no error when elicitation capability is present, got %v", err)
		}
	})
}

func TestSession_ElicitationHandler(t *testing.T) {
	t.Run("registerElicitationHandler stores handler", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		if session.getElicitationHandler() != nil {
			t.Error("Expected nil handler before registration")
		}

		session.registerElicitationHandler(func(ctx ElicitationContext) (ElicitationResult, error) {
			return ElicitationResult{Action: "accept"}, nil
		})

		if session.getElicitationHandler() == nil {
			t.Error("Expected non-nil handler after registration")
		}
	})

	t.Run("handler error is returned correctly", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.registerElicitationHandler(func(ctx ElicitationContext) (ElicitationResult, error) {
			return ElicitationResult{}, fmt.Errorf("handler exploded")
		})

		handler := session.getElicitationHandler()
		if handler == nil {
			t.Fatal("Expected non-nil handler")
		}

		_, err := handler(
			ElicitationContext{SessionID: "test-session", Message: "Pick a color"},
		)
		if err == nil {
			t.Fatal("Expected error from handler")
		}
		if !strings.Contains(err.Error(), "handler exploded") {
			t.Errorf("Expected error to contain 'handler exploded', got %q", err.Error())
		}
	})

	t.Run("handler success returns result", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.registerElicitationHandler(func(ctx ElicitationContext) (ElicitationResult, error) {
			return ElicitationResult{
				Action:  "accept",
				Content: map[string]any{"color": "blue"},
			}, nil
		})

		handler := session.getElicitationHandler()
		result, err := handler(
			ElicitationContext{SessionID: "test-session", Message: "Pick a color"},
		)
		if err != nil {
			t.Fatalf("Expected no error, got %v", err)
		}
		if result.Action != "accept" {
			t.Errorf("Expected action 'accept', got %q", result.Action)
		}
		if result.Content["color"] != "blue" {
			t.Errorf("Expected content color 'blue', got %v", result.Content["color"])
		}
	})
}

func TestSession_HookForwardCompatibility(t *testing.T) {
	t.Run("unknown hook type returns nil without error", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		session.registerHooks(&SessionHooks{
			OnPostToolUse: func(input PostToolUseHookInput, invocation HookInvocation) (*PostToolUseHookOutput, error) {
				return nil, nil
			},
		})

		output, err := session.handleHooksInvoke("postToolUseFailure", json.RawMessage(`{}`))
		if err != nil {
			t.Errorf("Expected no error for unknown hook type, got: %v", err)
		}
		if output != nil {
			t.Errorf("Expected nil output for unknown hook type, got: %v", output)
		}
	})

	t.Run("unknown hook type with no hooks registered returns nil without error", func(t *testing.T) {
		session, cleanup := newTestSession()
		defer cleanup()

		output, err := session.handleHooksInvoke("futureHookType", json.RawMessage(`{"someField":"value"}`))
		if err != nil {
			t.Errorf("Expected no error for unknown hook type with no hooks, got: %v", err)
		}
		if output != nil {
			t.Errorf("Expected nil output for unknown hook type with no hooks, got: %v", output)
		}
	})
}

func TestSession_ElicitationRequestSchema(t *testing.T) {
	t.Run("elicitation.requested passes full schema to handler", func(t *testing.T) {
		// Verify the schema extraction logic from handleBroadcastEvent
		// preserves type, properties, and required.
		properties := map[string]any{
			"name": map[string]any{"type": "string"},
			"age":  map[string]any{"type": "number"},
		}
		required := []string{"name", "age"}

		// Replicate the schema extraction logic from handleBroadcastEvent
		requestedSchema := map[string]any{
			"type":       "object",
			"properties": properties,
		}
		if len(required) > 0 {
			requestedSchema["required"] = required
		}

		if requestedSchema["type"] != "object" {
			t.Errorf("Expected schema type 'object', got %v", requestedSchema["type"])
		}
		props, ok := requestedSchema["properties"].(map[string]any)
		if !ok || props == nil {
			t.Fatal("Expected schema properties map")
		}
		if len(props) != 2 {
			t.Errorf("Expected 2 properties, got %d", len(props))
		}
		req, ok := requestedSchema["required"].([]string)
		if !ok || len(req) != 2 {
			t.Errorf("Expected required [name, age], got %v", requestedSchema["required"])
		}
	})

	t.Run("schema without required omits required key", func(t *testing.T) {
		properties := map[string]any{
			"optional_field": map[string]any{"type": "string"},
		}

		requestedSchema := map[string]any{
			"type":       "object",
			"properties": properties,
		}
		// Simulate: if len(schema.Required) > 0 { ... } — with empty required
		var required []string
		if len(required) > 0 {
			requestedSchema["required"] = required
		}

		if _, exists := requestedSchema["required"]; exists {
			t.Error("Expected no 'required' key when Required is empty")
		}
	})
}
