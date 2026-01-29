// Package copilot provides a Go SDK for interacting with the GitHub Copilot CLI.
package copilot

import (
	"encoding/json"
	"fmt"
	"sync"
	"time"
)

type sessionHandler struct {
	id uint64
	fn SessionEventHandler
}

// Session represents a single conversation session with the Copilot CLI.
//
// A session maintains conversation state, handles events, and manages tool execution.
// Sessions are created via [Client.CreateSession] or resumed via [Client.ResumeSession].
//
// The session provides methods to send messages, subscribe to events, retrieve
// conversation history, and manage the session lifecycle. All methods are safe
// for concurrent use.
//
// Example usage:
//
//	session, err := client.CreateSession(copilot.SessionConfig{
//	    Model: "gpt-4",
//	})
//	if err != nil {
//	    log.Fatal(err)
//	}
//	defer session.Destroy()
//
//	// Subscribe to events
//	unsubscribe := session.On(func(event copilot.SessionEvent) {
//	    if event.Type == "assistant.message" {
//	        fmt.Println("Assistant:", event.Data.Content)
//	    }
//	})
//	defer unsubscribe()
//
//	// Send a message
//	messageID, err := session.Send(copilot.MessageOptions{
//	    Prompt: "Hello, world!",
//	})
type Session struct {
	// SessionID is the unique identifier for this session.
	SessionID         string
	workspacePath     string
	client            *JSONRPCClient
	handlers          []sessionHandler
	nextHandlerID     uint64
	handlerMutex      sync.RWMutex
	toolHandlers      map[string]ToolHandler
	toolHandlersM     sync.RWMutex
	permissionHandler PermissionHandler
	permissionMux     sync.RWMutex
	userInputHandler  UserInputHandler
	userInputMux      sync.RWMutex
	hooks             *SessionHooks
	hooksMux          sync.RWMutex
}

// WorkspacePath returns the path to the session workspace directory when infinite
// sessions are enabled. Contains checkpoints/, plan.md, and files/ subdirectories.
// Returns empty string if infinite sessions are disabled.
func (s *Session) WorkspacePath() string {
	return s.workspacePath
}

// NewSession creates a new session wrapper with the given session ID and client.
//
// Note: This function is primarily for internal use. Use [Client.CreateSession]
// to create sessions with proper initialization.
func NewSession(sessionID string, client *JSONRPCClient, workspacePath string) *Session {
	return &Session{
		SessionID:     sessionID,
		workspacePath: workspacePath,
		client:        client,
		handlers:      make([]sessionHandler, 0),
		toolHandlers:  make(map[string]ToolHandler),
	}
}

// Send sends a message to this session and waits for the response.
//
// The message is processed asynchronously. Subscribe to events via [Session.On]
// to receive streaming responses and other session events.
//
// Parameters:
//   - options: The message options including the prompt and optional attachments.
//
// Returns the message ID of the response, which can be used to correlate events,
// or an error if the session has been destroyed or the connection fails.
//
// Example:
//
//	messageID, err := session.Send(copilot.MessageOptions{
//	    Prompt: "Explain this code",
//	    Attachments: []copilot.Attachment{
//	        {Type: "file", Path: "./main.go"},
//	    },
//	})
//	if err != nil {
//	    log.Printf("Failed to send message: %v", err)
//	}
func (s *Session) Send(options MessageOptions) (string, error) {
	params := map[string]interface{}{
		"sessionId": s.SessionID,
		"prompt":    options.Prompt,
	}

	if options.Attachments != nil {
		params["attachments"] = options.Attachments
	}
	if options.Mode != "" {
		params["mode"] = options.Mode
	}

	result, err := s.client.Request("session.send", params)
	if err != nil {
		return "", fmt.Errorf("failed to send message: %w", err)
	}

	messageID, ok := result["messageId"].(string)
	if !ok {
		return "", fmt.Errorf("invalid response: missing messageId")
	}

	return messageID, nil
}

// SendAndWait sends a message to this session and waits until the session becomes idle.
//
// This is a convenience method that combines [Session.Send] with waiting for
// the session.idle event. Use this when you want to block until the assistant
// has finished processing the message.
//
// Events are still delivered to handlers registered via [Session.On] while waiting.
//
// Parameters:
//   - options: The message options including the prompt and optional attachments.
//   - timeout: How long to wait for completion. Defaults to 60 seconds if zero.
//     Controls how long to wait; does not abort in-flight agent work.
//
// Returns the final assistant message event, or nil if none was received.
// Returns an error if the timeout is reached or the connection fails.
//
// Example:
//
//	response, err := session.SendAndWait(copilot.MessageOptions{
//	    Prompt: "What is 2+2?",
//	}, 0) // Use default 60s timeout
//	if err != nil {
//	    log.Printf("Failed: %v", err)
//	}
//	if response != nil {
//	    fmt.Println(*response.Data.Content)
//	}
func (s *Session) SendAndWait(options MessageOptions, timeout time.Duration) (*SessionEvent, error) {
	if timeout == 0 {
		timeout = 60 * time.Second
	}

	idleCh := make(chan struct{}, 1)
	errCh := make(chan error, 1)
	var lastAssistantMessage *SessionEvent
	var mu sync.Mutex

	unsubscribe := s.On(func(event SessionEvent) {
		switch event.Type {
		case AssistantMessage:
			mu.Lock()
			eventCopy := event
			lastAssistantMessage = &eventCopy
			mu.Unlock()
		case SessionIdle:
			select {
			case idleCh <- struct{}{}:
			default:
			}
		case SessionError:
			errMsg := "session error"
			if event.Data.Message != nil {
				errMsg = *event.Data.Message
			}
			select {
			case errCh <- fmt.Errorf("session error: %s", errMsg):
			default:
			}
		}
	})
	defer unsubscribe()

	_, err := s.Send(options)
	if err != nil {
		return nil, err
	}

	select {
	case <-idleCh:
		mu.Lock()
		result := lastAssistantMessage
		mu.Unlock()
		return result, nil
	case err := <-errCh:
		return nil, err
	case <-time.After(timeout):
		return nil, fmt.Errorf("timeout after %v waiting for session.idle", timeout)
	}
}

// On subscribes to events from this session.
//
// Events include assistant messages, tool executions, errors, and session state
// changes. Multiple handlers can be registered and will all receive events.
// Handlers are called synchronously in the order they were registered.
//
// The returned function can be called to unsubscribe the handler. It is safe
// to call the unsubscribe function multiple times.
//
// Example:
//
//	unsubscribe := session.On(func(event copilot.SessionEvent) {
//	    switch event.Type {
//	    case "assistant.message":
//	        fmt.Println("Assistant:", event.Data.Content)
//	    case "session.error":
//	        fmt.Println("Error:", event.Data.Message)
//	    }
//	})
//
//	// Later, to stop receiving events:
//	unsubscribe()
func (s *Session) On(handler SessionEventHandler) func() {
	s.handlerMutex.Lock()
	defer s.handlerMutex.Unlock()

	id := s.nextHandlerID
	s.nextHandlerID++
	s.handlers = append(s.handlers, sessionHandler{id: id, fn: handler})

	// Return unsubscribe function
	return func() {
		s.handlerMutex.Lock()
		defer s.handlerMutex.Unlock()

		for i, h := range s.handlers {
			if h.id == id {
				s.handlers = append(s.handlers[:i], s.handlers[i+1:]...)
				break
			}
		}
	}
}

// registerTools registers tool handlers for this session.
//
// Tools allow the assistant to execute custom functions. When the assistant
// invokes a tool, the corresponding handler is called with the tool arguments.
//
// This method is internal and typically called when creating a session with tools.
func (s *Session) registerTools(tools []Tool) {
	s.toolHandlersM.Lock()
	defer s.toolHandlersM.Unlock()

	s.toolHandlers = make(map[string]ToolHandler)
	for _, tool := range tools {
		if tool.Name == "" || tool.Handler == nil {
			continue
		}
		s.toolHandlers[tool.Name] = tool.Handler
	}
}

// getToolHandler retrieves a registered tool handler by name.
// Returns the handler and true if found, or nil and false if not registered.
func (s *Session) getToolHandler(name string) (ToolHandler, bool) {
	s.toolHandlersM.RLock()
	handler, ok := s.toolHandlers[name]
	s.toolHandlersM.RUnlock()
	return handler, ok
}

// registerPermissionHandler registers a permission handler for this session.
//
// When the assistant needs permission to perform certain actions (e.g., file
// operations), this handler is called to approve or deny the request.
//
// This method is internal and typically called when creating a session.
func (s *Session) registerPermissionHandler(handler PermissionHandler) {
	s.permissionMux.Lock()
	defer s.permissionMux.Unlock()
	s.permissionHandler = handler
}

// getPermissionHandler returns the currently registered permission handler, or nil.
func (s *Session) getPermissionHandler() PermissionHandler {
	s.permissionMux.RLock()
	defer s.permissionMux.RUnlock()
	return s.permissionHandler
}

// handlePermissionRequest handles a permission request from the Copilot CLI.
// This is an internal method called by the SDK when the CLI requests permission.
func (s *Session) handlePermissionRequest(requestData map[string]interface{}) (PermissionRequestResult, error) {
	handler := s.getPermissionHandler()

	if handler == nil {
		return PermissionRequestResult{
			Kind: "denied-no-approval-rule-and-could-not-request-from-user",
		}, nil
	}

	// Convert map to PermissionRequest struct
	kind, _ := requestData["kind"].(string)
	toolCallID, _ := requestData["toolCallId"].(string)

	request := PermissionRequest{
		Kind:       kind,
		ToolCallID: toolCallID,
		Extra:      requestData,
	}

	invocation := PermissionInvocation{
		SessionID: s.SessionID,
	}

	return handler(request, invocation)
}

// registerUserInputHandler registers a user input handler for this session.
//
// When the assistant needs to ask the user a question (e.g., via ask_user tool),
// this handler is called to get the user's response.
//
// This method is internal and typically called when creating a session.
func (s *Session) registerUserInputHandler(handler UserInputHandler) {
	s.userInputMux.Lock()
	defer s.userInputMux.Unlock()
	s.userInputHandler = handler
}

// getUserInputHandler returns the currently registered user input handler, or nil.
func (s *Session) getUserInputHandler() UserInputHandler {
	s.userInputMux.RLock()
	defer s.userInputMux.RUnlock()
	return s.userInputHandler
}

// handleUserInputRequest handles a user input request from the Copilot CLI.
// This is an internal method called by the SDK when the CLI requests user input.
func (s *Session) handleUserInputRequest(request UserInputRequest) (UserInputResponse, error) {
	handler := s.getUserInputHandler()

	if handler == nil {
		return UserInputResponse{}, fmt.Errorf("no user input handler registered")
	}

	invocation := UserInputInvocation{
		SessionID: s.SessionID,
	}

	return handler(request, invocation)
}

// registerHooks registers hook handlers for this session.
//
// Hooks are called at various points during session execution to allow
// customization and observation of the session lifecycle.
//
// This method is internal and typically called when creating a session.
func (s *Session) registerHooks(hooks *SessionHooks) {
	s.hooksMux.Lock()
	defer s.hooksMux.Unlock()
	s.hooks = hooks
}

// getHooks returns the currently registered hooks, or nil.
func (s *Session) getHooks() *SessionHooks {
	s.hooksMux.RLock()
	defer s.hooksMux.RUnlock()
	return s.hooks
}

// handleHooksInvoke handles a hook invocation from the Copilot CLI.
// This is an internal method called by the SDK when the CLI invokes a hook.
func (s *Session) handleHooksInvoke(hookType string, input map[string]interface{}) (interface{}, error) {
	hooks := s.getHooks()

	if hooks == nil {
		return nil, nil
	}

	invocation := HookInvocation{
		SessionID: s.SessionID,
	}

	switch hookType {
	case "preToolUse":
		if hooks.OnPreToolUse == nil {
			return nil, nil
		}
		hookInput := parsePreToolUseInput(input)
		return hooks.OnPreToolUse(hookInput, invocation)

	case "postToolUse":
		if hooks.OnPostToolUse == nil {
			return nil, nil
		}
		hookInput := parsePostToolUseInput(input)
		return hooks.OnPostToolUse(hookInput, invocation)

	case "userPromptSubmitted":
		if hooks.OnUserPromptSubmitted == nil {
			return nil, nil
		}
		hookInput := parseUserPromptSubmittedInput(input)
		return hooks.OnUserPromptSubmitted(hookInput, invocation)

	case "sessionStart":
		if hooks.OnSessionStart == nil {
			return nil, nil
		}
		hookInput := parseSessionStartInput(input)
		return hooks.OnSessionStart(hookInput, invocation)

	case "sessionEnd":
		if hooks.OnSessionEnd == nil {
			return nil, nil
		}
		hookInput := parseSessionEndInput(input)
		return hooks.OnSessionEnd(hookInput, invocation)

	case "errorOccurred":
		if hooks.OnErrorOccurred == nil {
			return nil, nil
		}
		hookInput := parseErrorOccurredInput(input)
		return hooks.OnErrorOccurred(hookInput, invocation)

	default:
		return nil, fmt.Errorf("unknown hook type: %s", hookType)
	}
}

// Helper functions to parse hook inputs

func parsePreToolUseInput(input map[string]interface{}) PreToolUseHookInput {
	result := PreToolUseHookInput{}
	if ts, ok := input["timestamp"].(float64); ok {
		result.Timestamp = int64(ts)
	}
	if cwd, ok := input["cwd"].(string); ok {
		result.Cwd = cwd
	}
	if name, ok := input["toolName"].(string); ok {
		result.ToolName = name
	}
	result.ToolArgs = input["toolArgs"]
	return result
}

func parsePostToolUseInput(input map[string]interface{}) PostToolUseHookInput {
	result := PostToolUseHookInput{}
	if ts, ok := input["timestamp"].(float64); ok {
		result.Timestamp = int64(ts)
	}
	if cwd, ok := input["cwd"].(string); ok {
		result.Cwd = cwd
	}
	if name, ok := input["toolName"].(string); ok {
		result.ToolName = name
	}
	result.ToolArgs = input["toolArgs"]
	result.ToolResult = input["toolResult"]
	return result
}

func parseUserPromptSubmittedInput(input map[string]interface{}) UserPromptSubmittedHookInput {
	result := UserPromptSubmittedHookInput{}
	if ts, ok := input["timestamp"].(float64); ok {
		result.Timestamp = int64(ts)
	}
	if cwd, ok := input["cwd"].(string); ok {
		result.Cwd = cwd
	}
	if prompt, ok := input["prompt"].(string); ok {
		result.Prompt = prompt
	}
	return result
}

func parseSessionStartInput(input map[string]interface{}) SessionStartHookInput {
	result := SessionStartHookInput{}
	if ts, ok := input["timestamp"].(float64); ok {
		result.Timestamp = int64(ts)
	}
	if cwd, ok := input["cwd"].(string); ok {
		result.Cwd = cwd
	}
	if source, ok := input["source"].(string); ok {
		result.Source = source
	}
	if prompt, ok := input["initialPrompt"].(string); ok {
		result.InitialPrompt = prompt
	}
	return result
}

func parseSessionEndInput(input map[string]interface{}) SessionEndHookInput {
	result := SessionEndHookInput{}
	if ts, ok := input["timestamp"].(float64); ok {
		result.Timestamp = int64(ts)
	}
	if cwd, ok := input["cwd"].(string); ok {
		result.Cwd = cwd
	}
	if reason, ok := input["reason"].(string); ok {
		result.Reason = reason
	}
	if msg, ok := input["finalMessage"].(string); ok {
		result.FinalMessage = msg
	}
	if errStr, ok := input["error"].(string); ok {
		result.Error = errStr
	}
	return result
}

func parseErrorOccurredInput(input map[string]interface{}) ErrorOccurredHookInput {
	result := ErrorOccurredHookInput{}
	if ts, ok := input["timestamp"].(float64); ok {
		result.Timestamp = int64(ts)
	}
	if cwd, ok := input["cwd"].(string); ok {
		result.Cwd = cwd
	}
	if errMsg, ok := input["error"].(string); ok {
		result.Error = errMsg
	}
	if ctx, ok := input["errorContext"].(string); ok {
		result.ErrorContext = ctx
	}
	if rec, ok := input["recoverable"].(bool); ok {
		result.Recoverable = rec
	}
	return result
}

// dispatchEvent dispatches an event to all registered handlers.
// This is an internal method; handlers are called synchronously and any panics
// are recovered to prevent crashing the event dispatcher.
func (s *Session) dispatchEvent(event SessionEvent) {
	s.handlerMutex.RLock()
	handlers := make([]SessionEventHandler, 0, len(s.handlers))
	for _, h := range s.handlers {
		handlers = append(handlers, h.fn)
	}
	s.handlerMutex.RUnlock()

	for _, handler := range handlers {
		// Call handler - don't let panics crash the dispatcher
		func() {
			defer func() {
				if r := recover(); r != nil {
					fmt.Printf("Error in session event handler: %v\n", r)
				}
			}()
			handler(event)
		}()
	}
}

// GetMessages retrieves all events and messages from this session's history.
//
// This returns the complete conversation history including user messages,
// assistant responses, tool executions, and other session events in
// chronological order.
//
// Returns an error if the session has been destroyed or the connection fails.
//
// Example:
//
//	events, err := session.GetMessages()
//	if err != nil {
//	    log.Printf("Failed to get messages: %v", err)
//	    return
//	}
//	for _, event := range events {
//	    if event.Type == "assistant.message" {
//	        fmt.Println("Assistant:", event.Data.Content)
//	    }
//	}
func (s *Session) GetMessages() ([]SessionEvent, error) {
	params := map[string]interface{}{
		"sessionId": s.SessionID,
	}

	result, err := s.client.Request("session.getMessages", params)
	if err != nil {
		return nil, fmt.Errorf("failed to get messages: %w", err)
	}

	eventsRaw, ok := result["events"].([]interface{})
	if !ok {
		return nil, fmt.Errorf("invalid response: missing events")
	}

	// Convert to SessionEvent structs
	events := make([]SessionEvent, 0, len(eventsRaw))
	for _, eventRaw := range eventsRaw {
		// Marshal back to JSON and unmarshal into typed struct
		eventJSON, err := json.Marshal(eventRaw)
		if err != nil {
			continue
		}

		event, err := UnmarshalSessionEvent(eventJSON)
		if err != nil {
			continue
		}

		events = append(events, event)
	}

	return events, nil
}

// Destroy destroys this session and releases all associated resources.
//
// After calling this method, the session can no longer be used. All event
// handlers and tool handlers are cleared. To continue the conversation,
// use [Client.ResumeSession] with the session ID.
//
// Returns an error if the connection fails.
//
// Example:
//
//	// Clean up when done
//	if err := session.Destroy(); err != nil {
//	    log.Printf("Failed to destroy session: %v", err)
//	}
func (s *Session) Destroy() error {
	params := map[string]interface{}{
		"sessionId": s.SessionID,
	}

	_, err := s.client.Request("session.destroy", params)
	if err != nil {
		return fmt.Errorf("failed to destroy session: %w", err)
	}

	// Clear handlers
	s.handlerMutex.Lock()
	s.handlers = nil
	s.handlerMutex.Unlock()

	s.toolHandlersM.Lock()
	s.toolHandlers = nil
	s.toolHandlersM.Unlock()

	s.permissionMux.Lock()
	s.permissionHandler = nil
	s.permissionMux.Unlock()

	return nil
}

// Abort aborts the currently processing message in this session.
//
// Use this to cancel a long-running request. The session remains valid
// and can continue to be used for new messages.
//
// Returns an error if the session has been destroyed or the connection fails.
//
// Example:
//
//	// Start a long-running request in a goroutine
//	go func() {
//	    session.Send(copilot.MessageOptions{
//	        Prompt: "Write a very long story...",
//	    })
//	}()
//
//	// Abort after 5 seconds
//	time.Sleep(5 * time.Second)
//	if err := session.Abort(); err != nil {
//	    log.Printf("Failed to abort: %v", err)
//	}
func (s *Session) Abort() error {
	params := map[string]interface{}{
		"sessionId": s.SessionID,
	}

	_, err := s.client.Request("session.abort", params)
	if err != nil {
		return fmt.Errorf("failed to abort session: %w", err)
	}

	return nil
}
