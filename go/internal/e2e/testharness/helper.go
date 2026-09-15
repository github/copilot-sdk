package testharness

import (
	"context"
	"errors"
	"path/filepath"
	"runtime"
	"sync"

	copilot "github.com/github/copilot-sdk/go"
)

// RepoPath resolves a path relative to the repository root, anchored to this
// source file's directory rather than the process working directory. The
// in-process (FFI) transport os.Chdir's the whole test process into a per-test
// temp workdir (the shared runtime host inherits the process cwd), so any
// cwd-relative resolution (e.g. filepath.Abs("../../../test/...")) would break
// for every test after the first in-process one. This helper stays correct
// regardless of the current working directory.
func RepoPath(elem ...string) string {
	_, callerFile, _, ok := runtime.Caller(0)
	if !ok {
		// Fall back to a cwd-relative join; only correct before any chdir.
		return filepath.Join(append([]string{"..", "..", ".."}, elem...)...)
	}
	// This file lives at go/internal/e2e/testharness/, so the repo root is four
	// levels up from its directory.
	repoRoot := filepath.Join(filepath.Dir(callerFile), "..", "..", "..", "..")
	return filepath.Join(append([]string{repoRoot}, elem...)...)
}

type eventResult struct {
	event *copilot.SessionEvent
	err   error
}

// EventWaiter is a synchronously installed subscription. Call Close even if the
// operation that should produce the event fails before Wait is called.
type EventWaiter struct {
	result      chan eventResult
	once        sync.Once
	unsubscribe func()
}

func (w *EventWaiter) complete(event *copilot.SessionEvent, err error) {
	w.once.Do(func() { w.result <- eventResult{event: event, err: err} })
}

// Wait waits using only the caller's context, without imposing a default timeout.
// Events received between subscription and Wait are retained.
func (w *EventWaiter) Wait(ctx context.Context) (*copilot.SessionEvent, error) {
	defer w.Close()
	select {
	case result := <-w.result:
		return result.event, result.err
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

// Close removes the subscription and is safe to call more than once.
func (w *EventWaiter) Close() {
	w.unsubscribe()
}

// SubscribeToFinalAssistantMessage subscribes before returning. Call it before
// Send, releasing a blocked handler, or any other operation that can finish a
// turn. Unlike durable assistant messages, session.idle is ephemeral: GetEvents
// cannot recover a missed completion. A successful wait always includes a message.
func SubscribeToFinalAssistantMessage(session *copilot.Session) *EventWaiter {
	w := &EventWaiter{result: make(chan eventResult, 1)}
	var finalAssistantMessage *copilot.SessionEvent
	w.unsubscribe = session.On(func(event copilot.SessionEvent) {
		switch d := event.Data.(type) {
		case *copilot.AssistantMessageData:
			finalAssistantMessage = &event
		case *copilot.SessionIdleData:
			if finalAssistantMessage == nil {
				w.complete(nil, errors.New("session became idle without an assistant message"))
			} else {
				w.complete(finalAssistantMessage, nil)
			}
		case *copilot.SessionErrorData:
			w.complete(nil, errors.New(d.Message))
		}
	})
	return w
}

// SubscribeToEvent subscribes before returning, so the triggering operation can
// run before Wait without losing events. Call Close if the operation fails.
func SubscribeToEvent(session *copilot.Session, eventType copilot.SessionEventType) *EventWaiter {
	w := &EventWaiter{result: make(chan eventResult, 1)}
	w.unsubscribe = session.On(func(event copilot.SessionEvent) {
		switch event.Type() {
		case eventType:
			w.complete(&event, nil)
		case copilot.SessionEventTypeSessionError:
			msg := "session error"
			if d, ok := event.Data.(*copilot.SessionErrorData); ok {
				msg = d.Message
			}
			w.complete(nil, errors.New(msg))
		}
	})
	return w
}

// GetFinalAssistantMessageFromHistory reads a turn whose completion has already
// been observed independently, including after resuming an idle session. It does
// not wait for completion and must not be used as a substitute for a live waiter.
func GetFinalAssistantMessageFromHistory(ctx context.Context, session *copilot.Session) (*copilot.SessionEvent, error) {
	messages, err := session.GetEvents(ctx)
	if err != nil {
		return nil, err
	}

	// Find last user message
	finalUserMessageIndex := -1
	for i := len(messages) - 1; i >= 0; i-- {
		if messages[i].Type() == "user.message" {
			finalUserMessageIndex = i
			break
		}
	}

	var currentTurnMessages []copilot.SessionEvent
	if finalUserMessageIndex < 0 {
		currentTurnMessages = messages
	} else {
		currentTurnMessages = messages[finalUserMessageIndex:]
	}

	// Check for errors
	for _, msg := range currentTurnMessages {
		if msg.Type() == "session.error" {
			errMsg := "session error"
			if d, ok := msg.Data.(*copilot.SessionErrorData); ok {
				errMsg = d.Message
			}
			return nil, errors.New(errMsg)
		}
	}

	for i := len(currentTurnMessages) - 1; i >= 0; i-- {
		if currentTurnMessages[i].Type() == "assistant.message" {
			return &currentTurnMessages[i], nil
		}
	}

	return nil, errors.New("no assistant message in the completed turn")
}
