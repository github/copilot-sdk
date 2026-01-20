package copilot

import (
	"sync"
	"testing"
)

func TestSession_On(t *testing.T) {
	t.Run("multiple handlers all receive events", func(t *testing.T) {
		session := &Session{
			handlers: make([]sessionHandler, 0),
		}

		var received1, received2, received3 bool
		session.On(func(event SessionEvent) { received1 = true })
		session.On(func(event SessionEvent) { received2 = true })
		session.On(func(event SessionEvent) { received3 = true })

		session.dispatchEvent(SessionEvent{Type: "test"})

		if !received1 || !received2 || !received3 {
			t.Errorf("Expected all handlers to receive event, got received1=%v, received2=%v, received3=%v",
				received1, received2, received3)
		}
	})

	t.Run("unsubscribing one handler does not affect others", func(t *testing.T) {
		session := &Session{
			handlers: make([]sessionHandler, 0),
		}

		var count1, count2, count3 int
		session.On(func(event SessionEvent) { count1++ })
		unsub2 := session.On(func(event SessionEvent) { count2++ })
		session.On(func(event SessionEvent) { count3++ })

		// First event - all handlers receive it
		session.dispatchEvent(SessionEvent{Type: "test"})

		// Unsubscribe handler 2
		unsub2()

		// Second event - only handlers 1 and 3 should receive it
		session.dispatchEvent(SessionEvent{Type: "test"})

		if count1 != 2 {
			t.Errorf("Expected handler 1 to receive 2 events, got %d", count1)
		}
		if count2 != 1 {
			t.Errorf("Expected handler 2 to receive 1 event (before unsubscribe), got %d", count2)
		}
		if count3 != 2 {
			t.Errorf("Expected handler 3 to receive 2 events, got %d", count3)
		}
	})

	t.Run("calling unsubscribe multiple times is safe", func(t *testing.T) {
		session := &Session{
			handlers: make([]sessionHandler, 0),
		}

		var count int
		unsub := session.On(func(event SessionEvent) { count++ })

		session.dispatchEvent(SessionEvent{Type: "test"})

		// Call unsubscribe multiple times - should not panic
		unsub()
		unsub()
		unsub()

		session.dispatchEvent(SessionEvent{Type: "test"})

		if count != 1 {
			t.Errorf("Expected handler to receive 1 event, got %d", count)
		}
	})

	t.Run("handlers are called in registration order", func(t *testing.T) {
		session := &Session{
			handlers: make([]sessionHandler, 0),
		}

		var order []int
		session.On(func(event SessionEvent) { order = append(order, 1) })
		session.On(func(event SessionEvent) { order = append(order, 2) })
		session.On(func(event SessionEvent) { order = append(order, 3) })

		session.dispatchEvent(SessionEvent{Type: "test"})

		if len(order) != 3 || order[0] != 1 || order[1] != 2 || order[2] != 3 {
			t.Errorf("Expected handlers to be called in order [1,2,3], got %v", order)
		}
	})

	t.Run("concurrent subscribe and unsubscribe is safe", func(t *testing.T) {
		session := &Session{
			handlers: make([]sessionHandler, 0),
		}

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

		// Should not panic and handlers should be empty
		session.handlerMutex.RLock()
		count := len(session.handlers)
		session.handlerMutex.RUnlock()

		if count != 0 {
			t.Errorf("Expected 0 handlers after all unsubscribes, got %d", count)
		}
	})
}
