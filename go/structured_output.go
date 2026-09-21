package copilot

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"
	"sync"
	"time"

	"github.com/google/jsonschema-go/jsonschema"
)

// SendAndWait infers a JSON Schema for T using the same generator as DefineTool,
// sends a message, and unmarshals its final correlated root response at session idle.
// Go does not support generic methods, so this is a package-level function.
// Options must not specify ResponseSchema or immediate delivery. Streaming events
// remain text. Cancellation stops waiting, not the agent; errors are session-scoped.
// Provider schema restrictions apply. Unmarshaling is not full JSON Schema validation.
func SendAndWait[T any](ctx context.Context, session *Session, options MessageOptions) (T, error) {
	var result T
	if session == nil {
		return result, fmt.Errorf("session must not be nil")
	}
	if options.ResponseSchema != nil || options.Mode == "immediate" {
		return result, fmt.Errorf("typed structured output cannot specify ResponseSchema or immediate delivery")
	}
	schema, err := jsonschema.ForType(reflect.TypeFor[T](), nil)
	if err != nil {
		return result, fmt.Errorf("infer response schema: %w", err)
	}
	encoded, err := json.Marshal(schema)
	if err != nil {
		return result, fmt.Errorf("marshal response schema: %w", err)
	}
	if err := json.Unmarshal(encoded, &options.ResponseSchema); err != nil {
		return result, fmt.Errorf("decode response schema: %w", err)
	}
	event, err := session.SendAndWait(ctx, options)
	if err != nil {
		return result, err
	}
	content := event.Data.(*AssistantMessageData).Content
	if bytes.Equal(bytes.TrimSpace([]byte(content)), []byte("null")) {
		return result, fmt.Errorf("structured response was JSON null, not a result")
	}
	if err := json.Unmarshal([]byte(content), &result); err != nil {
		return result, fmt.Errorf("decode structured response: %w", err)
	}
	return result, nil
}

func (s *Session) sendAndWaitStructured(ctx context.Context, options MessageOptions) (*SessionEvent, error) {
	if _, ok := ctx.Deadline(); !ok {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, 60*time.Second)
		defer cancel()
	}
	ctx, cancelWait := context.WithCancelCause(ctx)
	defer cancelWait(nil)
	go func() {
		select {
		case <-s.eventDone:
			cancelWait(fmt.Errorf("session closed before structured output completed"))
		case <-ctx.Done():
		}
	}()
	type outcome struct {
		event *SessionEvent
		err   error
	}
	completion := make(chan outcome, 1)
	var mu sync.Mutex
	var messageID string
	var admitted, started, completed bool
	var pending []SessionEvent
	var final *SessionEvent
	finish := func(result outcome) {
		if !completed {
			completed = true
			completion <- result
		}
	}
	process := func(event SessionEvent) {
		if completed || (event.AgentID != nil && *event.AgentID != "") {
			return
		}
		switch data := event.Data.(type) {
		case *UserMessageData:
			if data.MessageID != nil && *data.MessageID == messageID {
				started = true
			}
		case *AssistantMessageData:
			if data.OriginatingMessageID != nil && *data.OriginatingMessageID == messageID {
				started = true
				if len(data.ToolRequests) > 0 {
					final = nil
				} else {
					copy := event
					final = &copy
				}
			}
		case *SessionIdleData:
			if !started || (data.Mode != nil && *data.Mode == SessionModeAutopilot) {
				return
			}
			if data.Aborted != nil && *data.Aborted {
				finish(outcome{err: fmt.Errorf("session aborted before structured output completed")})
			} else if final == nil || strings.TrimSpace(final.Data.(*AssistantMessageData).Content) == "" {
				finish(outcome{err: fmt.Errorf("run completed without a structured assistant response")})
			} else {
				finish(outcome{event: final})
			}
		case *SessionErrorData:
			if started {
				finish(outcome{err: fmt.Errorf("session error: %s", data.Message)})
			}
		}
	}
	unsubscribe := s.On(func(event SessionEvent) {
		switch event.Data.(type) {
		case *UserMessageData, *AssistantMessageData, *SessionIdleData, *SessionErrorData:
		default:
			return
		}
		mu.Lock()
		defer mu.Unlock()
		if !admitted {
			pending = append(pending, event)
		} else {
			process(event)
		}
	})
	defer unsubscribe()
	id, err := s.Send(ctx, options)
	if err != nil {
		if cause := context.Cause(ctx); cause != nil {
			return nil, fmt.Errorf("admitting structured output: %w", cause)
		}
		return nil, err
	}
	mu.Lock()
	messageID, admitted = id, true
	for _, event := range pending {
		process(event)
	}
	pending = nil
	mu.Unlock()
	select {
	case result := <-completion:
		return result.event, result.err
	case <-s.eventDone:
		return nil, fmt.Errorf("session closed before structured output completed")
	case <-ctx.Done():
		return nil, fmt.Errorf("waiting for structured output: %w", context.Cause(ctx))
	}
}
