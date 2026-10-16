// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

func assistantContent(t *testing.T, event *copilot.SessionEvent) string {
	t.Helper()

	if event == nil {
		t.Fatal("Expected assistant message, got nil")
		return ""
	}
	data, ok := event.Data.(*copilot.AssistantMessageData)
	if !ok {
		t.Fatalf("Expected AssistantMessageData, got %T", event.Data)
	}
	return data.Content
}

func containsString(slice []string, value string) bool {
	for _, entry := range slice {
		if entry == value {
			return true
		}
	}
	return false
}

func waitForMatchingEvent(session *copilot.Session, eventType copilot.SessionEventType, predicate func(copilot.SessionEvent) bool, description string) func() (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		if event.Type() == eventType && predicate(event) {
			select {
			case result <- &event:
			default:
			}
		} else if event.Type() == copilot.SessionEventTypeSessionError {
			msg := "session error"
			if data, ok := event.Data.(*copilot.SessionErrorData); ok {
				msg = data.Message
			}
			select {
			case errCh <- fmt.Errorf("%s while waiting for %s", msg, description):
			default:
			}
		}
	})

	return func() (*copilot.SessionEvent, error) {
		defer unsubscribe()
		select {
		case event := <-result:
			return event, nil
		case err := <-errCh:
			return nil, err
		case <-time.After(30 * time.Second):
			return nil, fmt.Errorf("timed out waiting for %s", description)
		}
	}
}

func awaitEvent(t *testing.T, await func() (*copilot.SessionEvent, error)) *copilot.SessionEvent {
	t.Helper()
	event, err := await()
	if err != nil {
		t.Fatal(err)
	}
	return event
}

func randomHex(t *testing.T) string {
	t.Helper()
	var buf [8]byte
	if _, err := rand.Read(buf[:]); err != nil {
		t.Fatalf("Failed to generate random bytes: %v", err)
	}
	return hex.EncodeToString(buf[:])
}

func rpcPtr[T any](value T) *T {
	return &value
}

func waitForRPCCondition(t *testing.T, timeout time.Duration, description string, condition func() (bool, error)) {
	t.Helper()
	deadline := time.Now().Add(timeout)
	var lastErr error
	for time.Now().Before(deadline) {
		ok, err := condition()
		if err == nil && ok {
			return
		}
		if err != nil {
			lastErr = err
		}
		time.Sleep(100 * time.Millisecond)
	}
	if lastErr != nil {
		t.Fatalf("Timed out waiting for %s: %v", description, lastErr)
	}
	t.Fatalf("Timed out waiting for %s", description)
}
