package testharness

import (
	"context"
	"errors"
	"path/filepath"
	"runtime"
	"time"

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

// GetFinalAssistantMessage waits for and returns the final assistant message from a session turn.
// If alreadyIdle is true, skip waiting for session.idle (useful for resumed sessions where the
// idle event was ephemeral and not persisted in the event history).
func GetFinalAssistantMessage(ctx context.Context, session *copilot.Session, alreadyIdle ...bool) (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)

	// Subscribe to future events
	var finalAssistantMessage *copilot.SessionEvent
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		switch d := event.Data.(type) {
		case *copilot.AssistantMessageData:
			finalAssistantMessage = &event
		case *copilot.SessionIdleData:
			if finalAssistantMessage != nil {
				result <- finalAssistantMessage
			}
		case *copilot.SessionErrorData:
			errCh <- errors.New(d.Message)
		}
	})
	defer unsubscribe()

	// Also check existing messages in case the response already arrived
	isAlreadyIdle := len(alreadyIdle) > 0 && alreadyIdle[0]
	go func() {
		existing, err := getExistingFinalResponse(ctx, session, isAlreadyIdle)
		if err != nil {
			errCh <- err
			return
		}
		if existing != nil {
			result <- existing
		}
	}()

	select {
	case msg := <-result:
		return msg, nil
	case err := <-errCh:
		return nil, err
	case <-ctx.Done():
		return nil, errors.New("timeout waiting for assistant message")
	}
}

// GetNextEventOfType waits for and returns the next event of the specified type from a session.
func GetNextEventOfType(session *copilot.Session, eventType copilot.SessionEventType, timeout time.Duration) (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)

	unsubscribe := session.On(func(event copilot.SessionEvent) {
		switch event.Type() {
		case eventType:
			select {
			case result <- &event:
			default:
			}
		case copilot.SessionEventTypeSessionError:
			msg := "session error"
			if d, ok := event.Data.(*copilot.SessionErrorData); ok {
				msg = d.Message
			}
			select {
			case errCh <- errors.New(msg):
			default:
			}
		}
	})
	defer unsubscribe()

	select {
	case evt := <-result:
		return evt, nil
	case err := <-errCh:
		return nil, err
	case <-time.After(timeout):
		return nil, errors.New("timeout waiting for event: " + string(eventType))
	}
}

func getExistingFinalResponse(ctx context.Context, session *copilot.Session, alreadyIdle bool) (*copilot.SessionEvent, error) {
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

	// Find session.idle and get last assistant message before it
	sessionIdleIndex := -1
	if alreadyIdle {
		sessionIdleIndex = len(currentTurnMessages)
	} else {
		for i, msg := range currentTurnMessages {
			if msg.Type() == "session.idle" {
				sessionIdleIndex = i
				break
			}
		}
	}

	if sessionIdleIndex != -1 {
		// Find last assistant.message before session.idle
		for i := sessionIdleIndex - 1; i >= 0; i-- {
			if currentTurnMessages[i].Type() == "assistant.message" {
				return &currentTurnMessages[i], nil
			}
		}
	}

	return nil, nil
}
