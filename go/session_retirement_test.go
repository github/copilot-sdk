// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"context"
	"fmt"
	"strings"
	"testing"
	"time"
)

func TestSession_RetirementClearsCallbackRoots(t *testing.T) {
	session, cleanup := newTestSession()
	t.Cleanup(cleanup)
	session.On(func(SessionEvent) {})
	entry := session.handlers[0]
	session.registerTools([]Tool{{Name: "tool", Handler: func(ToolInvocation) (ToolResult, error) { return ToolResult{}, nil }}})
	session.registerPermissionHandler(PermissionHandler.ApproveAll)
	session.registerMCPAuthHandler(func(MCPAuthRequest, MCPAuthInvocation) (*MCPAuthResult, error) { return nil, nil })
	session.registerUserInputHandler(func(UserInputRequest, UserInputInvocation) (UserInputResponse, error) {
		return UserInputResponse{}, nil
	})
	session.registerExitPlanModeHandler(func(ExitPlanModeRequest, ExitPlanModeInvocation) (ExitPlanModeResult, error) {
		return ExitPlanModeResult{}, nil
	})
	session.registerAutoModeSwitchHandler(func(AutoModeSwitchRequest, AutoModeSwitchInvocation) (AutoModeSwitchResponse, error) {
		return AutoModeSwitchResponseNo, nil
	})
	hooks := &SessionHooks{OnSessionEnd: func(SessionEndHookInput, HookInvocation) (*SessionEndHookOutput, error) {
		return nil, nil
	}}
	session.registerHooks(hooks)
	transforms := map[string]SectionTransformFn{"instructions": func(value string) (string, error) { return value, nil }}
	session.registerTransformCallbacks(transforms)
	session.registerCommands([]CommandDefinition{{Name: "command", Handler: func(CommandContext) error { return nil }}})
	session.registerElicitationHandler(func(ElicitationContext) (ElicitationResult, error) { return ElicitationResult{}, nil })
	session.registerCanvasHandler(&recordingCanvasHandler{})
	session.registerBearerTokenProviders(map[string]BearerTokenProvider{
		"provider": func(ProviderTokenArgs) (string, error) { return "token", nil },
	})
	releases := 0
	session.setGitHubTokenProviderRegistrationRelease(func() { releases++ })

	session.stopEventProcessing()
	session.stopEventProcessing()

	for name, retained := range map[string]bool{
		"event registrations":  len(session.handlers) != 0,
		"retained event entry": entry.fn != nil,
		"tools":                session.toolHandlers != nil,
		"permissions":          session.permissionHandler != nil,
		"MCP auth":             session.mcpAuthHandler != nil,
		"user input":           session.userInputHandler != nil,
		"exit plan mode":       session.exitPlanModeHandler != nil,
		"auto mode switch":     session.autoModeSwitchHandler != nil,
		"hooks":                session.hooks != nil,
		"transforms":           session.transformCallbacks != nil,
		"commands":             session.commandHandlers != nil,
		"elicitation":          session.elicitationHandler != nil,
		"canvas":               session.canvasHandler != nil,
		"bearer token":         session.bearerTokenProviders != nil,
		"provider release":     session.releaseGitHubTokenProvider != nil,
	} {
		if retained {
			t.Errorf("retired session retained %s", name)
		}
	}
	if releases != 1 {
		t.Errorf("provider releases = %d, want exactly one", releases)
	}
	if hooks.OnSessionEnd == nil || transforms["instructions"] == nil {
		t.Fatal("retirement mutated caller-owned hook or transform configuration")
	}
}

func TestSession_RetirementRejectsCallbackRebinding(t *testing.T) {
	session, cleanup := newTestSession()
	t.Cleanup(cleanup)
	session.stopEventProcessing()
	unsubscribe := session.On(func(SessionEvent) {})
	if len(session.handlers) != 0 {
		t.Fatal("retired session accepted a late event registration")
	}
	unsubscribe()
	unsubscribe()
	session.registerTools([]Tool{{Name: "late", Handler: func(ToolInvocation) (ToolResult, error) { return ToolResult{}, nil }}})
	if session.toolHandlers != nil {
		t.Fatal("retired session retained late tool handlers")
	}
}

func TestSession_RetirementRejectsEmptyCallbackSuccess(t *testing.T) {
	t.Run("hooks", func(t *testing.T) {
		session, cleanup := newTestSession()
		t.Cleanup(cleanup)
		session.SessionID = "retirement-session"
		client := &Client{sessions: map[string]*Session{session.SessionID: session}}
		request := hooksInvokeRequest{SessionID: session.SessionID, Type: "preToolUse", Input: []byte("{}")}
		if _, err := client.handleHooksInvoke(request); err != nil {
			t.Fatalf("live session without hooks returned an error: %v", err)
		}
		session.stopEventProcessing()
		_, err := client.handleHooksInvoke(request)
		if err == nil || err.Code != -32603 || !strings.Contains(err.Message, "session closed") {
			t.Fatalf("retired hook request error = %+v, want closed-session RPC error", err)
		}
	})
	t.Run("system message transforms", func(t *testing.T) {
		session, cleanup := newTestSession()
		t.Cleanup(cleanup)
		sections := map[string]systemMessageTransformSection{"instructions": {Content: "keep this policy"}}
		response, err := session.handleSystemMessageTransform(sections)
		if err != nil || response.Sections["instructions"].Content != "keep this policy" {
			t.Fatalf("live session without transforms changed content: %+v, %v", response, err)
		}
		session.stopEventProcessing()
		response, err = session.handleSystemMessageTransform(sections)
		if err == nil || !strings.Contains(err.Error(), "session closed") {
			t.Fatalf("retired transform response = %+v, %v; want closed error", response, err)
		}
	})
}

func TestSession_RetirementRejectsModeRequestDefaults(t *testing.T) {
	t.Run("exit plan mode", func(t *testing.T) {
		session, cleanup := newTestSession()
		t.Cleanup(cleanup)
		session.stopEventProcessing()
		response, err := session.handleExitPlanModeRequest(ExitPlanModeRequest{})
		if err == nil || !strings.Contains(err.Error(), "session closed") || response.Approved {
			t.Fatalf("retired exit-plan-mode response = %+v, %v; want closed error without approval", response, err)
		}
	})
	t.Run("auto mode switch", func(t *testing.T) {
		session, cleanup := newTestSession()
		t.Cleanup(cleanup)
		session.stopEventProcessing()
		response, err := session.handleAutoModeSwitchRequest(AutoModeSwitchRequest{})
		if err == nil || !strings.Contains(err.Error(), "session closed") {
			t.Fatalf("retired auto-mode-switch response = %+v, %v; want closed error", response, err)
		}
	})
}

func TestSession_SetToolsAcceptedAfterRetirementDoesNotRebind(t *testing.T) {
	session, server := newSetToolsTestSession(t)
	session.eventDone = make(chan struct{})
	session.eventCh = make(chan SessionEvent, 2)
	processingDone := make(chan struct{})
	go func() {
		defer close(processingDone)
		session.processEvents()
	}()
	t.Cleanup(func() {
		session.stopEventProcessing()
		select {
		case <-processingDone:
		case <-time.After(2 * time.Second):
			t.Error("event processing did not stop after retirement")
		}
	})
	result := make(chan error, 1)
	go func() {
		result <- session.SetTools(context.Background(), []Tool{{
			Name: "late", Handler: func(ToolInvocation) (ToolResult, error) { return ToolResult{}, nil },
		}})
	}()
	request := server.expectRequest(t, "session.tools.set")
	// Keep the transport alive so an accepted late ACK cannot mask local retirement.
	session.stopEventProcessing()
	server.respond(t, request.ID, map[string]any{})
	err := awaitSetTools(t, result)
	if err == nil || !strings.Contains(err.Error(), "session closed") {
		t.Fatalf("SetTools error = %v, want session closed", err)
	}

	if _, ok := session.getToolHandler("late"); ok {
		t.Fatal("accepted late replacement resurrected retired tool handlers")
	}
}

func TestSession_SendAndWaitPreservesTerminalResultBeforeRetirement(t *testing.T) {
	for _, terminal := range []string{"idle", "error", "missing", "error-then-idle", "idle-then-error"} {
		for iteration := range 32 {
			t.Run(fmt.Sprintf("%s-%d", terminal, iteration), func(t *testing.T) {
				session, server := newSetToolsTestSession(t)
				session.eventDone = make(chan struct{})
				session.eventCh = make(chan SessionEvent, 2)
				processingDone := make(chan struct{})
				go func() {
					defer close(processingDone)
					session.processEvents()
				}()
				t.Cleanup(func() {
					session.stopEventProcessing()
					select {
					case <-processingDone:
					case <-time.After(2 * time.Second):
						t.Error("event processing did not stop after retirement")
					}
				})
				type response struct {
					event *SessionEvent
					err   error
				}
				result := make(chan response, 1)
				go func() {
					event, err := session.SendAndWait(context.Background(), MessageOptions{Prompt: "finish"})
					result <- response{event: event, err: err}
				}()
				request := server.expectRequest(t, "session.send")
				terminalObserved := make(chan struct{}, 2)
				unsubscribe := session.On(func(event SessionEvent) {
					switch event.Data.(type) {
					case *SessionIdleData, *SessionErrorData:
						terminalObserved <- struct{}{}
					}
				})
				defer unsubscribe()
				switch terminal {
				case "idle", "idle-then-error":
					session.dispatchEvent(SessionEvent{Data: &AssistantMessageData{Content: "finished"}})
					session.dispatchEvent(SessionEvent{Data: &SessionIdleData{}})
					if terminal == "idle-then-error" {
						session.dispatchEvent(SessionEvent{Data: &SessionErrorData{Message: "later provider failure"}})
					}
				case "error", "error-then-idle":
					session.dispatchEvent(SessionEvent{Data: &SessionErrorData{Message: "provider failed"}})
					if terminal == "error-then-idle" {
						session.dispatchEvent(SessionEvent{Data: &SessionIdleData{}})
					}
				}
				if terminal != "missing" {
					count := 1
					if terminal == "error-then-idle" || terminal == "idle-then-error" {
						count = 2
					}
					for range count {
						select {
						case <-terminalObserved:
						case <-time.After(2 * time.Second):
							t.Fatal("the wait did not receive its terminal event before retirement")
						}
					}
				}
				// Publish retirement before the send ACK so it can race the terminal result.
				session.stopEventProcessing()
				server.respond(t, request.ID, map[string]any{"messageId": "message-1"})
				select {
				case response := <-result:
					switch terminal {
					case "idle", "idle-then-error":
						if response.err != nil || response.event == nil {
							t.Fatalf("completed response = %+v, %v; want assistant message", response.event, response.err)
						}
						message, ok := response.event.Data.(*AssistantMessageData)
						if !ok || message.Content != "finished" {
							t.Fatalf("assistant response = %+v, want finished", response.event.Data)
						}
					case "error", "error-then-idle":
						if response.err == nil || response.err.Error() != "session error: provider failed" {
							t.Fatalf("terminal error = %v, want provider failure", response.err)
						}
					case "missing":
						if response.err == nil || response.err.Error() != "session closed before response completed" {
							t.Fatalf("incomplete response error = %v, want session closure", response.err)
						}
					}
				case <-time.After(2 * time.Second):
					t.Fatal("SendAndWait did not observe the acknowledged send and retirement")
				}
			})
		}
	}
}

func TestSession_SendAndWaitPreservesTerminalResultWhenHandlerDisconnects(t *testing.T) {
	for _, terminal := range []string{"idle", "error"} {
		t.Run(terminal, func(t *testing.T) {
			t.Parallel()
			session, server := newSetToolsTestSession(t)
			session.eventDone = make(chan struct{})
			session.eventCh = make(chan SessionEvent, 2)
			processingDone := make(chan struct{})
			go func() {
				defer close(processingDone)
				session.processEvents()
			}()
			t.Cleanup(func() {
				session.stopEventProcessing()
				select {
				case <-processingDone:
				case <-time.After(2 * time.Second):
					t.Error("event processing did not stop after retirement")
				}
			})

			disconnected := make(chan error, 1)
			session.On(func(event SessionEvent) {
				switch event.Data.(type) {
				case *SessionIdleData, *SessionErrorData:
					disconnected <- session.Disconnect()
				}
			})
			lateCallback := make(chan struct{}, 1)
			session.On(func(event SessionEvent) {
				switch event.Data.(type) {
				case *SessionIdleData, *SessionErrorData:
					lateCallback <- struct{}{}
				}
			})
			type response struct {
				event *SessionEvent
				err   error
			}
			result := make(chan response, 1)
			ctx, cancel := context.WithTimeout(t.Context(), 5*time.Second)
			defer cancel()
			go func() {
				event, err := session.SendAndWait(ctx, MessageOptions{Prompt: "finish"})
				result <- response{event: event, err: err}
			}()
			send := server.expectRequest(t, "session.send")
			session.dispatchEvent(SessionEvent{Data: &AssistantMessageData{Content: "finished"}})
			if terminal == "idle" {
				session.dispatchEvent(SessionEvent{Data: &SessionIdleData{}})
			} else {
				session.dispatchEvent(SessionEvent{Data: &SessionErrorData{Message: "provider failed"}})
			}
			detach := server.expectRequest(t, "session.detach")
			server.respond(t, detach.ID, map[string]any{"success": true})
			select {
			case err := <-disconnected:
				if err != nil {
					t.Fatal("disconnect:", err)
				}
			case <-ctx.Done():
				t.Fatal("terminal handler did not disconnect")
			}
			select {
			case <-lateCallback:
				t.Fatal("retirement admitted a later user callback from the same event snapshot")
			default:
			}
			// Acknowledge only after the earlier handler has retired the session.
			server.respond(t, send.ID, map[string]any{"messageId": "message-1"})
			select {
			case response := <-result:
				if terminal == "error" {
					if response.err == nil || response.err.Error() != "session error: provider failed" {
						t.Fatalf("terminal error = %v, want provider failure", response.err)
					}
				} else if response.err != nil || response.event == nil {
					t.Fatalf("completed response = %+v, %v; want assistant message", response.event, response.err)
				} else if message, ok := response.event.Data.(*AssistantMessageData); !ok || message.Content != "finished" {
					t.Fatalf("assistant response = %+v, want finished", response.event.Data)
				}
			case <-ctx.Done():
				t.Fatal("SendAndWait did not preserve the terminal result")
			}
		})
	}
}
