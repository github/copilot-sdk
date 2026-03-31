package copilot

import (
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
		if !containsString(err.Error(), expected) {
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

		session.registerElicitationHandler(func(req ElicitationRequest, inv ElicitationInvocation) (ElicitationResult, error) {
			return ElicitationResult{Action: "accept"}, nil
		})

		if session.getElicitationHandler() == nil {
			t.Error("Expected non-nil handler after registration")
		}
	})
}

func containsString(s, substr string) bool {
	return len(s) >= len(substr) && searchSubstring(s, substr)
}

func searchSubstring(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}
