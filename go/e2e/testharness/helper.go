package testharness

import (
	"errors"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

// GetFinalAssistantMessage waits for and returns the final assistant message from a session turn.
func GetFinalAssistantMessage(session *copilot.Session, timeout time.Duration) (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)

	// Subscribe to future events
	var finalAssistantMessage *copilot.SessionEvent
	unsubscribe := session.On(func(event copilot.SessionEvent) {
		switch event.Type {
		case "assistant.message":
			finalAssistantMessage = &event
		case "session.idle":
			if finalAssistantMessage != nil {
				result <- finalAssistantMessage
			}
		case "session.error":
			msg := "session error"
			if event.Data.Message != nil {
				msg = *event.Data.Message
			}
			errCh <- errors.New(msg)
		}
	})
	defer unsubscribe()

	// Also check existing messages in case the response already arrived
	go func() {
		existing, err := getExistingFinalResponse(session)
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
	case <-time.After(timeout):
		return nil, errors.New("timeout waiting for assistant message")
	}
}

// GetNextEventOfType waits for and returns the next event of the specified type from a session.
func GetNextEventOfType(session *copilot.Session, eventType copilot.SessionEventType, timeout time.Duration) (*copilot.SessionEvent, error) {
	result := make(chan *copilot.SessionEvent, 1)
	errCh := make(chan error, 1)

	unsubscribe := session.On(func(event copilot.SessionEvent) {
		switch event.Type {
		case eventType:
			select {
			case result <- &event:
			default:
			}
		case copilot.SessionError:
			msg := "session error"
			if event.Data.Message != nil {
				msg = *event.Data.Message
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

func getExistingFinalResponse(session *copilot.Session) (*copilot.SessionEvent, error) {
	messages, err := session.GetMessages()
	if err != nil {
		return nil, err
	}

	// Find last user message
	finalUserMessageIndex := -1
	for i := len(messages) - 1; i >= 0; i-- {
		if messages[i].Type == "user.message" {
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
		if msg.Type == "session.error" {
			errMsg := "session error"
			if msg.Data.Message != nil {
				errMsg = *msg.Data.Message
			}
			return nil, errors.New(errMsg)
		}
	}

	// Find session.idle and get last assistant message before it
	sessionIdleIndex := -1
	for i, msg := range currentTurnMessages {
		if msg.Type == "session.idle" {
			sessionIdleIndex = i
			break
		}
	}

	if sessionIdleIndex != -1 {
		// Find last assistant.message before session.idle
		for i := sessionIdleIndex - 1; i >= 0; i-- {
			if currentTurnMessages[i].Type == "assistant.message" {
				return &currentTurnMessages[i], nil
			}
		}
	}

	return nil, nil
}
