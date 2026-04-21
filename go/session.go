// Package copilot provides a Go SDK for interacting with the GitHub Copilot CLI.
package copilot

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
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
//	defer session.Disconnect()
//
//	// Subscribe to events
//	unsubscribe := session.On(func(event copilot.SessionEvent) {
//	    if d, ok := event.Data.(*copilot.AssistantMessageData); ok {
//	        fmt.Println("Assistant:", d.Content)
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
	SessionID          string
	workspacePath      string
	client             *jsonrpc2.Client
	clientSessionApis  *rpc.ClientSessionApiHandlers
	handlers           []sessionHandler
	nextHandlerID      uint64
	handlerMutex       sync.RWMutex
	toolHandlers       map[string]ToolHandler
	toolHandlersM      sync.RWMutex
	permissionHandler  PermissionHandlerFunc
	permissionMux      sync.RWMutex
	userInputHandler   UserInputHandler
	userInputMux       sync.RWMutex
	hooks              *SessionHooks
	hooksMux           sync.RWMutex
	transformCallbacks map[string]SectionTransformFn
	transformMu        sync.Mutex
	commandHandlers    map[string]CommandHandler
	commandHandlersMu  sync.RWMutex
	elicitationHandler ElicitationHandler
	elicitationMu      sync.RWMutex
	capabilities       SessionCapabilities
	capabilitiesMu     sync.RWMutex

	// eventCh serializes user event handler dispatch. dispatchEvent enqueues;
	// a single goroutine (processEvents) dequeues and invokes handlers in FIFO order.
	eventCh   chan SessionEvent
	closeOnce sync.Once // guards eventCh close so Disconnect is safe to call more than once

	// RPC provides typed session-scoped RPC methods.
	RPC *rpc.SessionRpc
}

// WorkspacePath returns the path to the session workspace directory when infinite
// sessions are enabled. Contains checkpoints/, plan.md, and files/ subdirectories.
// Returns empty string if infinite sessions are disabled.
func (s *Session) WorkspacePath() string {
	return s.workspacePath
}

// newSession creates a new session wrapper with the given session ID and client.
func newSession(sessionID string, client *jsonrpc2.Client, workspacePath string) *Session {
	s := &Session{
		SessionID:         sessionID,
		workspacePath:     workspacePath,
		client:            client,
		clientSessionApis: &rpc.ClientSessionApiHandlers{},
		handlers:          make([]sessionHandler, 0),
		toolHandlers:      make(map[string]ToolHandler),
		commandHandlers:   make(map[string]CommandHandler),
		eventCh:           make(chan SessionEvent, 128),
		RPC:               rpc.NewSessionRpc(client, sessionID),
	}
	go s.processEvents()
	return s
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
// or an error if the session has been disconnected or the connection fails.
//
// Example:
//
//	messageID, err := session.Send(context.Background(), copilot.MessageOptions{
//	    Prompt: "Explain this code",
//	    Attachments: []copilot.Attachment{
//	        {Type: "file", Path: "./main.go"},
//	    },
//	})
//	if err != nil {
//	    log.Printf("Failed to send message: %v", err)
//	}
func (s *Session) Send(ctx context.Context, options MessageOptions) (string, error) {
	traceparent, tracestate := getTraceContext(ctx)
	req := sessionSendRequest{
		SessionID:      s.SessionID,
		Prompt:         options.Prompt,
		Attachments:    options.Attachments,
		Mode:           options.Mode,
		Traceparent:    traceparent,
		Tracestate:     tracestate,
		RequestHeaders: options.RequestHeaders,
	}

	result, err := s.client.Request("session.send", req)
	if err != nil {
		return "", fmt.Errorf("failed to send message: %w", err)
	}

	var response sessionSendResponse
	if err := json.Unmarshal(result, &response); err != nil {
		return "", fmt.Errorf("failed to unmarshal send response: %w", err)
	}
	return response.MessageID, nil
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
//	response, err := session.SendAndWait(context.Background(), copilot.MessageOptions{
//	    Prompt: "What is 2+2?",
//	}) // Use default 60s timeout
//	if err != nil {
//	    log.Printf("Failed: %v", err)
//	}
//	if response != nil {
//	    if d, ok := response.Data.(*AssistantMessageData); ok {
//	        fmt.Println(d.Content)
//	    }
//	}
func (s *Session) SendAndWait(ctx context.Context, options MessageOptions) (*SessionEvent, error) {
	if _, ok := ctx.Deadline(); !ok {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, 60*time.Second)
		defer cancel()
	}

	idleCh := make(chan struct{}, 1)
	errCh := make(chan error, 1)
	var lastAssistantMessage *SessionEvent
	var mu sync.Mutex

	unsubscribe := s.On(func(event SessionEvent) {
		switch d := event.Data.(type) {
		case *AssistantMessageData:
			mu.Lock()
			eventCopy := event
			lastAssistantMessage = &eventCopy
			mu.Unlock()
		case *SessionIdleData:
			select {
			case idleCh <- struct{}{}:
			default:
			}
		case *SessionErrorData:
			select {
			case errCh <- fmt.Errorf("session error: %s", d.Message):
			default:
			}
		}
	})
	defer unsubscribe()

	_, err := s.Send(ctx, options)
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
	case <-ctx.Done(): // TODO: remove once session.Send honors the context
		return nil, fmt.Errorf("waiting for session.idle: %w", ctx.Err())
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
//	    switch d := event.Data.(type) {
//	    case *copilot.AssistantMessageData:
//	        fmt.Println("Assistant:", d.Content)
//	    case *copilot.SessionErrorData:
//	        fmt.Println("Error:", d.Message)
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
func (s *Session) registerPermissionHandler(handler PermissionHandlerFunc) {
	s.permissionMux.Lock()
	defer s.permissionMux.Unlock()
	s.permissionHandler = handler
}

// getPermissionHandler returns the currently registered permission handler, or nil.
func (s *Session) getPermissionHandler() PermissionHandlerFunc {
	s.permissionMux.RLock()
	defer s.permissionMux.RUnlock()
	return s.permissionHandler
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
func (s *Session) handleHooksInvoke(hookType string, rawInput json.RawMessage) (any, error) {
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
		var input PreToolUseHookInput
		if err := json.Unmarshal(rawInput, &input); err != nil {
			return nil, fmt.Errorf("invalid hook input: %w", err)
		}
		return hooks.OnPreToolUse(input, invocation)

	case "postToolUse":
		if hooks.OnPostToolUse == nil {
			return nil, nil
		}
		var input PostToolUseHookInput
		if err := json.Unmarshal(rawInput, &input); err != nil {
			return nil, fmt.Errorf("invalid hook input: %w", err)
		}
		return hooks.OnPostToolUse(input, invocation)

	case "userPromptSubmitted":
		if hooks.OnUserPromptSubmitted == nil {
			return nil, nil
		}
		var input UserPromptSubmittedHookInput
		if err := json.Unmarshal(rawInput, &input); err != nil {
			return nil, fmt.Errorf("invalid hook input: %w", err)
		}
		return hooks.OnUserPromptSubmitted(input, invocation)

	case "sessionStart":
		if hooks.OnSessionStart == nil {
			return nil, nil
		}
		var input SessionStartHookInput
		if err := json.Unmarshal(rawInput, &input); err != nil {
			return nil, fmt.Errorf("invalid hook input: %w", err)
		}
		return hooks.OnSessionStart(input, invocation)

	case "sessionEnd":
		if hooks.OnSessionEnd == nil {
			return nil, nil
		}
		var input SessionEndHookInput
		if err := json.Unmarshal(rawInput, &input); err != nil {
			return nil, fmt.Errorf("invalid hook input: %w", err)
		}
		return hooks.OnSessionEnd(input, invocation)

	case "errorOccurred":
		if hooks.OnErrorOccurred == nil {
			return nil, nil
		}
		var input ErrorOccurredHookInput
		if err := json.Unmarshal(rawInput, &input); err != nil {
			return nil, fmt.Errorf("invalid hook input: %w", err)
		}
		return hooks.OnErrorOccurred(input, invocation)
	default:
		return nil, nil
	}
}

// registerTransformCallbacks registers transform callbacks for this session.
//
// Transform callbacks are invoked when the CLI requests system message section
// transforms. This method is internal and typically called when creating a session.
func (s *Session) registerTransformCallbacks(callbacks map[string]SectionTransformFn) {
	s.transformMu.Lock()
	defer s.transformMu.Unlock()
	s.transformCallbacks = callbacks
}

type systemMessageTransformSection struct {
	Content string `json:"content"`
}

type systemMessageTransformRequest struct {
	SessionID string                                   `json:"sessionId"`
	Sections  map[string]systemMessageTransformSection `json:"sections"`
}

type systemMessageTransformResponse struct {
	Sections map[string]systemMessageTransformSection `json:"sections"`
}

// handleSystemMessageTransform handles a system message transform request from the Copilot CLI.
// This is an internal method called by the SDK when the CLI requests section transforms.
func (s *Session) handleSystemMessageTransform(sections map[string]systemMessageTransformSection) (systemMessageTransformResponse, error) {
	s.transformMu.Lock()
	callbacks := s.transformCallbacks
	s.transformMu.Unlock()

	result := make(map[string]systemMessageTransformSection)
	for sectionID, data := range sections {
		var callback SectionTransformFn
		if callbacks != nil {
			callback = callbacks[sectionID]
		}
		if callback != nil {
			transformed, err := callback(data.Content)
			if err != nil {
				result[sectionID] = systemMessageTransformSection{Content: data.Content}
			} else {
				result[sectionID] = systemMessageTransformSection{Content: transformed}
			}
		} else {
			result[sectionID] = systemMessageTransformSection{Content: data.Content}
		}
	}
	return systemMessageTransformResponse{Sections: result}, nil
}

// registerCommands registers command handlers for this session.
func (s *Session) registerCommands(commands []CommandDefinition) {
	s.commandHandlersMu.Lock()
	defer s.commandHandlersMu.Unlock()
	s.commandHandlers = make(map[string]CommandHandler)
	for _, cmd := range commands {
		if cmd.Name == "" || cmd.Handler == nil {
			continue
		}
		s.commandHandlers[cmd.Name] = cmd.Handler
	}
}

// getCommandHandler retrieves a registered command handler by name.
func (s *Session) getCommandHandler(name string) (CommandHandler, bool) {
	s.commandHandlersMu.RLock()
	handler, ok := s.commandHandlers[name]
	s.commandHandlersMu.RUnlock()
	return handler, ok
}

// executeCommandAndRespond dispatches a command.execute event to the registered handler
// and sends the result (or error) back via the RPC layer.
func (s *Session) executeCommandAndRespond(requestID, commandName, command, args string) {
	ctx := context.Background()
	handler, ok := s.getCommandHandler(commandName)
	if !ok {
		errMsg := fmt.Sprintf("Unknown command: %s", commandName)
		s.RPC.Commands.HandlePendingCommand(ctx, &rpc.CommandsHandlePendingCommandRequest{
			RequestID: requestID,
			Error:     &errMsg,
		})
		return
	}

	cmdCtx := CommandContext{
		SessionID:   s.SessionID,
		Command:     command,
		CommandName: commandName,
		Args:        args,
	}

	if err := handler(cmdCtx); err != nil {
		errMsg := err.Error()
		s.RPC.Commands.HandlePendingCommand(ctx, &rpc.CommandsHandlePendingCommandRequest{
			RequestID: requestID,
			Error:     &errMsg,
		})
		return
	}

	s.RPC.Commands.HandlePendingCommand(ctx, &rpc.CommandsHandlePendingCommandRequest{
		RequestID: requestID,
	})
}

// registerElicitationHandler registers an elicitation handler for this session.
func (s *Session) registerElicitationHandler(handler ElicitationHandler) {
	s.elicitationMu.Lock()
	defer s.elicitationMu.Unlock()
	s.elicitationHandler = handler
}

// getElicitationHandler returns the currently registered elicitation handler, or nil.
func (s *Session) getElicitationHandler() ElicitationHandler {
	s.elicitationMu.RLock()
	defer s.elicitationMu.RUnlock()
	return s.elicitationHandler
}

// handleElicitationRequest dispatches an elicitation.requested event to the registered handler
// and sends the result back via the RPC layer. Auto-cancels on error.
func (s *Session) handleElicitationRequest(elicitCtx ElicitationContext, requestID string) {
	handler := s.getElicitationHandler()
	if handler == nil {
		return
	}

	ctx := context.Background()

	result, err := handler(elicitCtx)
	if err != nil {
		// Handler failed — attempt to cancel so the request doesn't hang.
		s.RPC.UI.HandlePendingElicitation(ctx, &rpc.UIHandlePendingElicitationRequest{
			RequestID: requestID,
			Result: rpc.UIElicitationResponse{
				Action: rpc.UIElicitationResponseActionCancel,
			},
		})
		return
	}

	rpcContent := make(map[string]*rpc.UIElicitationFieldValue)
	for k, v := range result.Content {
		rpcContent[k] = toRPCContent(v)
	}

	s.RPC.UI.HandlePendingElicitation(ctx, &rpc.UIHandlePendingElicitationRequest{
		RequestID: requestID,
		Result: rpc.UIElicitationResponse{
			Action:  rpc.UIElicitationResponseAction(result.Action),
			Content: rpcContent,
		},
	})
}

// toRPCContent converts an arbitrary value to a *rpc.UIElicitationFieldValue for elicitation responses.
func toRPCContent(v any) *rpc.UIElicitationFieldValue {
	if v == nil {
		return nil
	}
	c := &rpc.UIElicitationFieldValue{}
	switch val := v.(type) {
	case bool:
		c.Bool = &val
	case float64:
		c.Double = &val
	case int:
		f := float64(val)
		c.Double = &f
	case string:
		c.String = &val
	case []string:
		c.StringArray = val
	case []any:
		strs := make([]string, 0, len(val))
		for _, item := range val {
			if s, ok := item.(string); ok {
				strs = append(strs, s)
			}
		}
		c.StringArray = strs
	default:
		s := fmt.Sprintf("%v", val)
		c.String = &s
	}
	return c
}

// Capabilities returns the session capabilities reported by the server.
func (s *Session) Capabilities() SessionCapabilities {
	s.capabilitiesMu.RLock()
	defer s.capabilitiesMu.RUnlock()
	return s.capabilities
}

// setCapabilities updates the session capabilities.
func (s *Session) setCapabilities(caps *SessionCapabilities) {
	s.capabilitiesMu.Lock()
	defer s.capabilitiesMu.Unlock()
	if caps != nil {
		s.capabilities = *caps
	} else {
		s.capabilities = SessionCapabilities{}
	}
}

// UI returns the interactive UI API for showing elicitation dialogs.
// Methods on the returned SessionUI will error if the host does not support
// elicitation (check Capabilities().UI.Elicitation first).
func (s *Session) UI() *SessionUI {
	return &SessionUI{session: s}
}

// assertElicitation checks that the host supports elicitation and returns an error if not.
func (s *Session) assertElicitation() error {
	caps := s.Capabilities()
	if caps.UI == nil || !caps.UI.Elicitation {
		return fmt.Errorf("elicitation is not supported by the host; check session.Capabilities().UI.Elicitation before calling UI methods")
	}
	return nil
}

// Elicitation shows a generic elicitation dialog with a custom schema.
func (ui *SessionUI) Elicitation(ctx context.Context, message string, requestedSchema rpc.UIElicitationSchema) (*ElicitationResult, error) {
	if err := ui.session.assertElicitation(); err != nil {
		return nil, err
	}
	rpcResult, err := ui.session.RPC.UI.Elicitation(ctx, &rpc.UIElicitationRequest{
		Message:         message,
		RequestedSchema: requestedSchema,
	})
	if err != nil {
		return nil, err
	}
	return fromRPCElicitationResult(rpcResult), nil
}

// Confirm shows a confirmation dialog and returns the user's boolean answer.
// Returns false if the user declines or cancels.
func (ui *SessionUI) Confirm(ctx context.Context, message string) (bool, error) {
	if err := ui.session.assertElicitation(); err != nil {
		return false, err
	}
	defaultTrue := &rpc.UIElicitationFieldValue{Bool: Bool(true)}
	rpcResult, err := ui.session.RPC.UI.Elicitation(ctx, &rpc.UIElicitationRequest{
		Message: message,
		RequestedSchema: rpc.UIElicitationSchema{
			Type: rpc.UIElicitationSchemaTypeObject,
			Properties: map[string]rpc.UIElicitationSchemaProperty{
				"confirmed": {
					Type:    rpc.UIElicitationSchemaPropertyTypeBoolean,
					Default: defaultTrue,
				},
			},
			Required: []string{"confirmed"},
		},
	})
	if err != nil {
		return false, err
	}
	if rpcResult.Action == rpc.UIElicitationResponseActionAccept {
		if c, ok := rpcResult.Content["confirmed"]; ok && c != nil && c.Bool != nil {
			return *c.Bool, nil
		}
	}
	return false, nil
}

// Select shows a selection dialog with the given options.
// Returns the selected string, or empty string and false if the user declines/cancels.
func (ui *SessionUI) Select(ctx context.Context, message string, options []string) (string, bool, error) {
	if err := ui.session.assertElicitation(); err != nil {
		return "", false, err
	}
	rpcResult, err := ui.session.RPC.UI.Elicitation(ctx, &rpc.UIElicitationRequest{
		Message: message,
		RequestedSchema: rpc.UIElicitationSchema{
			Type: rpc.UIElicitationSchemaTypeObject,
			Properties: map[string]rpc.UIElicitationSchemaProperty{
				"selection": {
					Type: rpc.UIElicitationSchemaPropertyTypeString,
					Enum: options,
				},
			},
			Required: []string{"selection"},
		},
	})
	if err != nil {
		return "", false, err
	}
	if rpcResult.Action == rpc.UIElicitationResponseActionAccept {
		if c, ok := rpcResult.Content["selection"]; ok && c != nil && c.String != nil {
			return *c.String, true, nil
		}
	}
	return "", false, nil
}

// Input shows a text input dialog. Returns the entered text, or empty string and
// false if the user declines/cancels.
func (ui *SessionUI) Input(ctx context.Context, message string, opts *InputOptions) (string, bool, error) {
	if err := ui.session.assertElicitation(); err != nil {
		return "", false, err
	}
	prop := rpc.UIElicitationSchemaProperty{Type: rpc.UIElicitationSchemaPropertyTypeString}
	if opts != nil {
		if opts.Title != "" {
			prop.Title = &opts.Title
		}
		if opts.Description != "" {
			prop.Description = &opts.Description
		}
		if opts.MinLength != nil {
			f := float64(*opts.MinLength)
			prop.MinLength = &f
		}
		if opts.MaxLength != nil {
			f := float64(*opts.MaxLength)
			prop.MaxLength = &f
		}
		if opts.Format != "" {
			format := rpc.UIElicitationSchemaPropertyStringFormat(opts.Format)
			prop.Format = &format
		}
		if opts.Default != "" {
			prop.Default = &rpc.UIElicitationFieldValue{String: &opts.Default}
		}
	}
	rpcResult, err := ui.session.RPC.UI.Elicitation(ctx, &rpc.UIElicitationRequest{
		Message: message,
		RequestedSchema: rpc.UIElicitationSchema{
			Type: rpc.UIElicitationSchemaTypeObject,
			Properties: map[string]rpc.UIElicitationSchemaProperty{
				"value": prop,
			},
			Required: []string{"value"},
		},
	})
	if err != nil {
		return "", false, err
	}
	if rpcResult.Action == rpc.UIElicitationResponseActionAccept {
		if c, ok := rpcResult.Content["value"]; ok && c != nil && c.String != nil {
			return *c.String, true, nil
		}
	}
	return "", false, nil
}

// fromRPCElicitationResult converts the RPC result to the SDK ElicitationResult.
func fromRPCElicitationResult(r *rpc.UIElicitationResponse) *ElicitationResult {
	if r == nil {
		return nil
	}
	content := make(map[string]any)
	for k, v := range r.Content {
		if v == nil {
			content[k] = nil
			continue
		}
		if v.Bool != nil {
			content[k] = *v.Bool
		} else if v.Double != nil {
			content[k] = *v.Double
		} else if v.String != nil {
			content[k] = *v.String
		} else if v.StringArray != nil {
			content[k] = v.StringArray
		}
	}
	return &ElicitationResult{
		Action:  string(r.Action),
		Content: content,
	}
}

// dispatchEvent enqueues an event for delivery to user handlers and fires
// broadcast handlers concurrently.
//
// Broadcast work (tool calls, permission requests) is fired in a separate
// goroutine so it does not block the JSON-RPC read loop. User event handlers
// are delivered by a single consumer goroutine (processEvents), guaranteeing
// serial, FIFO dispatch without blocking the read loop.
func (s *Session) dispatchEvent(event SessionEvent) {
	go s.handleBroadcastEvent(event)

	// Send to the event channel in a closure with a recover guard.
	// Disconnect closes eventCh, and in Go sending on a closed channel
	// panics — there is no non-panicking send primitive. We only want
	// to suppress that specific panic; other panics are not expected here.
	func() {
		defer func() { recover() }()
		s.eventCh <- event
	}()
}

// processEvents is the single consumer goroutine for the event channel.
// It invokes user handlers serially, in arrival order. Panics in individual
// handlers are recovered so that one misbehaving handler does not prevent
// others from receiving the event.
func (s *Session) processEvents() {
	for event := range s.eventCh {
		s.handlerMutex.RLock()
		handlers := make([]SessionEventHandler, 0, len(s.handlers))
		for _, h := range s.handlers {
			handlers = append(handlers, h.fn)
		}
		s.handlerMutex.RUnlock()

		for _, handler := range handlers {
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
}

// handleBroadcastEvent handles broadcast request events by executing local handlers
// and responding via RPC. This implements the protocol v3 broadcast model where tool
// calls and permission requests are broadcast as session events to all clients.
//
// Handlers are executed in their own goroutine (not the JSON-RPC read loop or the
// event consumer loop) so that a stalled handler does not block event delivery or
// cause RPC deadlocks.
func (s *Session) handleBroadcastEvent(event SessionEvent) {
	switch d := event.Data.(type) {
	case *ExternalToolRequestedData:
		handler, ok := s.getToolHandler(d.ToolName)
		if !ok {
			return
		}
		var tp, ts string
		if d.Traceparent != nil {
			tp = *d.Traceparent
		}
		if d.Tracestate != nil {
			ts = *d.Tracestate
		}
		s.executeToolAndRespond(d.RequestID, d.ToolName, d.ToolCallID, d.Arguments, handler, tp, ts)

	case *PermissionRequestedData:
		if d.ResolvedByHook != nil && *d.ResolvedByHook {
			return // Already resolved by a permissionRequest hook; no client action needed.
		}
		handler := s.getPermissionHandler()
		if handler == nil {
			return
		}
		s.executePermissionAndRespond(d.RequestID, d.PermissionRequest, handler)

	case *CommandExecuteData:
		s.executeCommandAndRespond(d.RequestID, d.CommandName, d.Command, d.Args)

	case *ElicitationRequestedData:
		handler := s.getElicitationHandler()
		if handler == nil {
			return
		}
		var requestedSchema map[string]any
		if d.RequestedSchema != nil {
			requestedSchema = map[string]any{
				"type":       string(d.RequestedSchema.Type),
				"properties": d.RequestedSchema.Properties,
			}
			if len(d.RequestedSchema.Required) > 0 {
				requestedSchema["required"] = d.RequestedSchema.Required
			}
		}
		mode := ""
		if d.Mode != nil {
			mode = string(*d.Mode)
		}
		elicitationSource := ""
		if d.ElicitationSource != nil {
			elicitationSource = *d.ElicitationSource
		}
		url := ""
		if d.URL != nil {
			url = *d.URL
		}
		s.handleElicitationRequest(ElicitationContext{
			SessionID:         s.SessionID,
			Message:           d.Message,
			RequestedSchema:   requestedSchema,
			Mode:              mode,
			ElicitationSource: elicitationSource,
			URL:               url,
		}, d.RequestID)

	case *CapabilitiesChangedData:
		if d.UI != nil && d.UI.Elicitation != nil {
			s.setCapabilities(&SessionCapabilities{
				UI: &UICapabilities{Elicitation: *d.UI.Elicitation},
			})
		}
	}
}

// executeToolAndRespond executes a tool handler and sends the result back via RPC.
func (s *Session) executeToolAndRespond(requestID, toolName, toolCallID string, arguments any, handler ToolHandler, traceparent, tracestate string) {
	ctx := contextWithTraceParent(context.Background(), traceparent, tracestate)
	defer func() {
		if r := recover(); r != nil {
			errMsg := fmt.Sprintf("tool panic: %v", r)
			s.RPC.Tools.HandlePendingToolCall(ctx, &rpc.ToolsHandlePendingToolCallRequest{
				RequestID: requestID,
				Error:     &errMsg,
			})
		}
	}()

	invocation := ToolInvocation{
		SessionID:    s.SessionID,
		ToolCallID:   toolCallID,
		ToolName:     toolName,
		Arguments:    arguments,
		TraceContext: ctx,
	}

	result, err := handler(invocation)
	if err != nil {
		errMsg := err.Error()
		s.RPC.Tools.HandlePendingToolCall(ctx, &rpc.ToolsHandlePendingToolCallRequest{
			RequestID: requestID,
			Error:     &errMsg,
		})
		return
	}

	textResultForLLM := result.TextResultForLLM
	if textResultForLLM == "" {
		textResultForLLM = fmt.Sprintf("%v", result)
	}

	// Default ResultType to "success" when unset, or "failure" when there's an error.
	effectiveResultType := result.ResultType
	if effectiveResultType == "" {
		if result.Error != "" {
			effectiveResultType = "failure"
		} else {
			effectiveResultType = "success"
		}
	}

	rpcResult := rpc.ToolsHandlePendingToolCall{
		ToolCallResult: &rpc.ToolCallResult{
			TextResultForLlm: textResultForLLM,
			ToolTelemetry:    result.ToolTelemetry,
			ResultType:       &effectiveResultType,
		},
	}
	if result.Error != "" {
		rpcResult.ToolCallResult.Error = &result.Error
	}
	s.RPC.Tools.HandlePendingToolCall(ctx, &rpc.ToolsHandlePendingToolCallRequest{
		RequestID: requestID,
		Result:    &rpcResult,
	})
}

// executePermissionAndRespond executes a permission handler and sends the result back via RPC.
func (s *Session) executePermissionAndRespond(requestID string, permissionRequest PermissionRequest, handler PermissionHandlerFunc) {
	defer func() {
		if r := recover(); r != nil {
			s.RPC.Permissions.HandlePendingPermissionRequest(context.Background(), &rpc.PermissionDecisionRequest{
				RequestID: requestID,
				Result: rpc.PermissionDecision{
					Kind: rpc.PermissionDecisionKindDeniedNoApprovalRuleAndCouldNotRequestFromUser,
				},
			})
		}
	}()

	invocation := PermissionInvocation{
		SessionID: s.SessionID,
	}

	result, err := handler(permissionRequest, invocation)
	if err != nil {
		s.RPC.Permissions.HandlePendingPermissionRequest(context.Background(), &rpc.PermissionDecisionRequest{
			RequestID: requestID,
			Result: rpc.PermissionDecision{
				Kind: rpc.PermissionDecisionKindDeniedNoApprovalRuleAndCouldNotRequestFromUser,
			},
		})
		return
	}
	if result.Kind == "no-result" {
		return
	}

	s.RPC.Permissions.HandlePendingPermissionRequest(context.Background(), &rpc.PermissionDecisionRequest{
		RequestID: requestID,
		Result: rpc.PermissionDecision{
			Kind:     rpc.PermissionDecisionKind(result.Kind),
			Rules:    result.Rules,
			Feedback: nil,
		},
	})
}

// GetMessages retrieves all events and messages from this session's history.
//
// This returns the complete conversation history including user messages,
// assistant responses, tool executions, and other session events in
// chronological order.
//
// Returns an error if the session has been disconnected or the connection fails.
//
// Example:
//
//	events, err := session.GetMessages(context.Background())
//	if err != nil {
//	    log.Printf("Failed to get messages: %v", err)
//	    return
//	}
//	for _, event := range events {
//	    if d, ok := event.Data.(*copilot.AssistantMessageData); ok {
//	        fmt.Println("Assistant:", d.Content)
//	    }
//	}
func (s *Session) GetMessages(ctx context.Context) ([]SessionEvent, error) {

	result, err := s.client.Request("session.getMessages", sessionGetMessagesRequest{SessionID: s.SessionID})
	if err != nil {
		return nil, fmt.Errorf("failed to get messages: %w", err)
	}

	var response sessionGetMessagesResponse
	if err := json.Unmarshal(result, &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal get messages response: %w", err)
	}
	return response.Events, nil
}

// Disconnect closes this session and releases all in-memory resources (event
// handlers, tool handlers, permission handlers).
//
// The caller should ensure the session is idle (e.g., [Session.SendAndWait] has
// returned) before disconnecting. If the session is not idle, in-flight event
// handlers or tool handlers may observe failures.
//
// Session state on disk (conversation history, planning state, artifacts) is
// preserved, so the conversation can be resumed later by calling
// [Client.ResumeSession] with the session ID. To permanently remove all
// session data including files on disk, use [Client.DeleteSession] instead.
//
// After calling this method, the session object can no longer be used.
//
// Returns an error if the connection fails.
//
// Example:
//
//	// Clean up when done — session can still be resumed later
//	if err := session.Disconnect(); err != nil {
//	    log.Printf("Failed to disconnect session: %v", err)
//	}
func (s *Session) Disconnect() error {
	_, err := s.client.Request("session.destroy", sessionDestroyRequest{SessionID: s.SessionID})
	if err != nil {
		return fmt.Errorf("failed to disconnect session: %w", err)
	}

	s.closeOnce.Do(func() { close(s.eventCh) })

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

	s.commandHandlersMu.Lock()
	s.commandHandlers = nil
	s.commandHandlersMu.Unlock()

	s.elicitationMu.Lock()
	s.elicitationHandler = nil
	s.elicitationMu.Unlock()

	return nil
}

// Deprecated: Use [Session.Disconnect] instead. Destroy will be removed in a future release.
//
// Destroy closes this session and releases all in-memory resources.
// Session data on disk is preserved for later resumption.
func (s *Session) Destroy() error {
	return s.Disconnect()
}

// Abort aborts the currently processing message in this session.
//
// Use this to cancel a long-running request. The session remains valid
// and can continue to be used for new messages.
//
// Returns an error if the session has been disconnected or the connection fails.
//
// Example:
//
//	// Start a long-running request in a goroutine
//	go func() {
//	    session.Send(context.Background(), copilot.MessageOptions{
//	        Prompt: "Write a very long story...",
//	    })
//	}()
//
//	// Abort after 5 seconds
//	time.Sleep(5 * time.Second)
//	if err := session.Abort(context.Background()); err != nil {
//	    log.Printf("Failed to abort: %v", err)
//	}
func (s *Session) Abort(ctx context.Context) error {
	_, err := s.client.Request("session.abort", sessionAbortRequest{SessionID: s.SessionID})
	if err != nil {
		return fmt.Errorf("failed to abort session: %w", err)
	}

	return nil
}

// SetModelOptions configures optional parameters for SetModel.
type SetModelOptions struct {
	// ReasoningEffort sets the reasoning effort level for the new model (e.g., "low", "medium", "high", "xhigh").
	ReasoningEffort *string
	// ModelCapabilities overrides individual model capabilities resolved by the runtime.
	// Only non-nil fields are applied over the runtime-resolved capabilities.
	ModelCapabilities *rpc.ModelCapabilitiesOverride
}

// SetModel changes the model for this session.
// The new model takes effect for the next message. Conversation history is preserved.
//
// Example:
//
//	if err := session.SetModel(context.Background(), "gpt-4.1", nil); err != nil {
//	    log.Printf("Failed to set model: %v", err)
//	}
//	if err := session.SetModel(context.Background(), "claude-sonnet-4.6", &SetModelOptions{ReasoningEffort: new("high")}); err != nil {
//	    log.Printf("Failed to set model: %v", err)
//	}
func (s *Session) SetModel(ctx context.Context, model string, opts *SetModelOptions) error {
	params := &rpc.ModelSwitchToRequest{ModelID: model}
	if opts != nil {
		params.ReasoningEffort = opts.ReasoningEffort
		params.ModelCapabilities = opts.ModelCapabilities
	}
	_, err := s.RPC.Model.SwitchTo(ctx, params)
	if err != nil {
		return fmt.Errorf("failed to set model: %w", err)
	}

	return nil
}

type LogOptions struct {
	// Level sets the log severity. Valid values are [rpc.SessionLogLevelInfo] (default),
	// [rpc.SessionLogLevelWarning], and [rpc.SessionLogLevelError].
	Level rpc.SessionLogLevel
	// Ephemeral marks the message as transient so it is not persisted
	// to the session event log on disk. When nil the server decides the
	// default; set to a non-nil value to explicitly control persistence.
	Ephemeral *bool
}

// Log sends a log message to the session timeline.
// The message appears in the session event stream and is visible to SDK consumers
// and (for non-ephemeral messages) persisted to the session event log on disk.
//
// Pass nil for opts to use defaults (info level, non-ephemeral).
//
// Example:
//
//	// Simple info message
//	session.Log(ctx, "Processing started")
//
//	// Warning with options
//	session.Log(ctx, "Rate limit approaching", &copilot.LogOptions{Level: rpc.SessionLogLevelWarning})
//
//	// Ephemeral message (not persisted)
//	session.Log(ctx, "Working...", &copilot.LogOptions{Ephemeral: copilot.Bool(true)})
func (s *Session) Log(ctx context.Context, message string, opts *LogOptions) error {
	params := &rpc.LogRequest{Message: message}

	if opts != nil {
		if opts.Level != "" {
			params.Level = &opts.Level
		}
		if opts.Ephemeral != nil {
			params.Ephemeral = opts.Ephemeral
		}
	}

	_, err := s.RPC.Log(ctx, params)
	if err != nil {
		return fmt.Errorf("failed to log message: %w", err)
	}

	return nil
}
