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
		handlers: make([]sessionHandler, 0),
		eventCh:  make(chan SessionEvent, 128),
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
