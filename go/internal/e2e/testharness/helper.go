package testharness

import (
	"errors"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

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
