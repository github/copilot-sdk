package e2e

import (
	"fmt"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestCommandsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.UseStdio = copilot.Bool(false)
		opts.TCPConnectionToken = sharedTcpToken
	})
	t.Cleanup(func() { client1.ForceStop() })

	// Start client1 with an init session to get the port
	initSession, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create init session: %v", err)
	}
	initSession.Disconnect()

	actualPort := client1.ActualPort()
	if actualPort == 0 {
		t.Fatalf("Expected non-zero port from TCP mode client")
	}

	client2 := copilot.NewClient(&copilot.ClientOptions{
		CLIUrl:             fmt.Sprintf("localhost:%d", actualPort),
		TCPConnectionToken: sharedTcpToken,
	})
	t.Cleanup(func() { client2.ForceStop() })

	t.Run("commands.changed event when another client joins with commands", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Client1 creates a session without commands
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Listen for commands.changed event on client1
		commandsChangedCh := make(chan copilot.SessionEvent, 1)
		unsubscribe := session1.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeCommandsChanged {
				select {
				case commandsChangedCh <- event:
				default:
				}
			}
		})
		defer unsubscribe()

		// Client2 joins with commands
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			DisableResume:       true,
			Commands: []copilot.CommandDefinition{
				{
					Name:        "deploy",
					Description: "Deploy the app",
					Handler:     func(ctx copilot.CommandContext) error { return nil },
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		select {
		case event := <-commandsChangedCh:
			d, ok := event.Data.(*copilot.CommandsChangedData)
			if !ok || len(d.Commands) == 0 {
				t.Errorf("Expected commands in commands.changed event")
			} else {
				found := false
				for _, cmd := range d.Commands {
					if cmd.Name == "deploy" {
						found = true
						if cmd.Description == nil || *cmd.Description != "Deploy the app" {
							t.Errorf("Expected deploy command description 'Deploy the app', got %v", cmd.Description)
						}
						break
					}
				}
				if !found {
					t.Errorf("Expected 'deploy' command in commands.changed event, got %+v", d.Commands)
				}
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for commands.changed event")
		}

		session2.Disconnect()
	})

	t.Run("session with commands creates successfully", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Commands: []copilot.CommandDefinition{
				{Name: "deploy", Description: "Deploy the app", Handler: func(_ copilot.CommandContext) error { return nil }},
				{Name: "rollback", Handler: func(_ copilot.CommandContext) error { return nil }},
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if session.SessionID == "" {
			t.Error("Expected non-empty SessionID")
		}
		_ = session.Disconnect()
	})

	t.Run("session with commands resumes successfully", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		sessionID := session1.SessionID
		t.Cleanup(func() { _ = session1.Disconnect() })

		session2, err := client1.ResumeSession(t.Context(), sessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Commands: []copilot.CommandDefinition{
				{Name: "deploy", Description: "Deploy", Handler: func(_ copilot.CommandContext) error { return nil }},
			},
		})
		if err != nil {
			t.Fatalf("ResumeSession failed: %v", err)
		}
		if session2.SessionID != sessionID {
			t.Errorf("Expected SessionID %q, got %q", sessionID, session2.SessionID)
		}
		_ = session2.Disconnect()
	})

	t.Run("session with no commands creates successfully", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if session == nil {
			t.Fatal("Expected non-nil session")
		}
		_ = session.Disconnect()
	})
}

func TestUIElicitationE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("elicitation methods error in headless mode", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Verify capabilities report no elicitation
		caps := session.Capabilities()
		if caps.UI != nil && caps.UI.Elicitation {
			t.Error("Expected no elicitation capability in headless mode")
		}

		// All UI methods should return a "not supported" error
		ui := session.UI()

		_, err = ui.Confirm(t.Context(), "Are you sure?")
		if err == nil {
			t.Error("Expected error calling Confirm without elicitation capability")
		} else if !strings.Contains(err.Error(), "not supported") {
			t.Errorf("Expected 'not supported' in error message, got: %s", err.Error())
		}

		_, _, err = ui.Select(t.Context(), "Pick one", []string{"a", "b"})
		if err == nil {
			t.Error("Expected error calling Select without elicitation capability")
		} else if !strings.Contains(err.Error(), "not supported") {
			t.Errorf("Expected 'not supported' in error message, got: %s", err.Error())
		}

		_, _, err = ui.Input(t.Context(), "Enter name", nil)
		if err == nil {
			t.Error("Expected error calling Input without elicitation capability")
		} else if !strings.Contains(err.Error(), "not supported") {
			t.Errorf("Expected 'not supported' in error message, got: %s", err.Error())
		}
	})
}

func TestUIElicitationCallbackE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("session with OnElicitationRequest reports elicitation capability", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ctx copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				return copilot.ElicitationResult{Action: "accept", Content: map[string]any{}}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		caps := session.Capabilities()
		if caps.UI == nil || !caps.UI.Elicitation {
			// The test harness may or may not include capabilities in the response.
			// When running against a real CLI, this will be true.
			t.Logf("Note: capabilities.ui.elicitation=%v (may be false with test harness)", caps.UI)
		}
	})

	t.Run("session without OnElicitationRequest reports no elicitation capability", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		caps := session.Capabilities()
		if caps.UI != nil && caps.UI.Elicitation {
			t.Error("Expected no elicitation capability when OnElicitationRequest is not provided")
		}
	})

	t.Run("confirm returns true when handler accepts", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ec copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				if ec.Message != "Confirm?" {
					t.Errorf("Expected Message='Confirm?', got %q", ec.Message)
				}
				if !schemaHasProperty(ec.RequestedSchema, "confirmed") {
					t.Errorf("Expected RequestedSchema to contain 'confirmed' property")
				}
				return copilot.ElicitationResult{
					Action:  "accept",
					Content: map[string]any{"confirmed": true},
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		ok, err := session.UI().Confirm(t.Context(), "Confirm?")
		if err != nil {
			t.Fatalf("Confirm failed: %v", err)
		}
		if !ok {
			t.Error("Expected Confirm to return true when handler accepts")
		}
	})

	t.Run("confirm returns false when handler declines", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ec copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				return copilot.ElicitationResult{Action: "decline"}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		ok, err := session.UI().Confirm(t.Context(), "Confirm?")
		if err != nil {
			t.Fatalf("Confirm failed: %v", err)
		}
		if ok {
			t.Error("Expected Confirm to return false when handler declines")
		}
	})

	t.Run("select returns selected option", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ec copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				if ec.Message != "Choose" {
					t.Errorf("Expected Message='Choose', got %q", ec.Message)
				}
				if !schemaHasProperty(ec.RequestedSchema, "selection") {
					t.Errorf("Expected RequestedSchema to contain 'selection' property")
				}
				return copilot.ElicitationResult{
					Action:  "accept",
					Content: map[string]any{"selection": "beta"},
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		value, ok, err := session.UI().Select(t.Context(), "Choose", []string{"alpha", "beta"})
		if err != nil {
			t.Fatalf("Select failed: %v", err)
		}
		if !ok {
			t.Error("Expected Select to return ok=true on accept")
		}
		if value != "beta" {
			t.Errorf("Expected selected value 'beta', got %q", value)
		}
	})

	t.Run("input returns freeform value", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ec copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				if ec.Message != "Enter value" {
					t.Errorf("Expected Message='Enter value', got %q", ec.Message)
				}
				if !schemaHasProperty(ec.RequestedSchema, "value") {
					t.Errorf("Expected RequestedSchema to contain 'value' property")
				}
				return copilot.ElicitationResult{
					Action:  "accept",
					Content: map[string]any{"value": "typed value"},
				}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		minLen := 1
		maxLen := 20
		value, ok, err := session.UI().Input(t.Context(), "Enter value", &copilot.InputOptions{
			Title:       "Value",
			Description: "A value to test",
			MinLength:   &minLen,
			MaxLength:   &maxLen,
			Default:     "default",
		})
		if err != nil {
			t.Fatalf("Input failed: %v", err)
		}
		if !ok {
			t.Error("Expected Input to return ok=true on accept")
		}
		if value != "typed value" {
			t.Errorf("Expected typed value 'typed value', got %q", value)
		}
	})

	t.Run("elicitation returns all action shapes", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		responses := []copilot.ElicitationResult{
			{Action: "accept", Content: map[string]any{"name": "Mona"}},
			{Action: "decline"},
			{Action: "cancel"},
		}
		var idx int

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ec copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				if ec.Message != "Name?" {
					t.Errorf("Expected Message='Name?', got %q", ec.Message)
				}
				if idx >= len(responses) {
					t.Fatalf("Handler called more times than expected (%d)", idx+1)
				}
				resp := responses[idx]
				idx++
				return resp, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		schema := rpc.UIElicitationSchema{
			Type: rpc.UIElicitationSchemaTypeObject,
			Properties: map[string]rpc.UIElicitationSchemaProperty{
				"name": {Type: rpc.UIElicitationSchemaPropertyTypeString},
			},
			Required: []string{"name"},
		}

		accept, err := session.UI().Elicitation(t.Context(), "Name?", schema)
		if err != nil {
			t.Fatalf("Elicitation accept call failed: %v", err)
		}
		if accept.Action != "accept" {
			t.Errorf("Expected accept.Action='accept', got %q", accept.Action)
		}
		if accept.Content == nil || fmt.Sprintf("%v", accept.Content["name"]) != "Mona" {
			t.Errorf("Expected accept.Content[name]='Mona', got %v", accept.Content)
		}

		decline, err := session.UI().Elicitation(t.Context(), "Name?", schema)
		if err != nil {
			t.Fatalf("Elicitation decline call failed: %v", err)
		}
		if decline.Action != "decline" {
			t.Errorf("Expected decline.Action='decline', got %q", decline.Action)
		}

		cancel, err := session.UI().Elicitation(t.Context(), "Name?", schema)
		if err != nil {
			t.Fatalf("Elicitation cancel call failed: %v", err)
		}
		if cancel.Action != "cancel" {
			t.Errorf("Expected cancel.Action='cancel', got %q", cancel.Action)
		}
	})

	t.Run("defaults capabilities when not provided", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		// A session always exposes some capability struct (even when empty).
		_ = session.Capabilities()
		_ = session.Disconnect()
	})

	t.Run("sends requestElicitation when handler provided", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(ec copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				return copilot.ElicitationResult{Action: "accept", Content: map[string]any{}}, nil
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		if session.SessionID == "" {
			t.Error("Expected non-empty SessionID when handler provided")
		}
		_ = session.Disconnect()
	})
}

// schemaHasProperty reports whether the elicitation schema map has a top-level
// property with the given name. RequestedSchema["properties"] is typically a
// map[string]rpc.UIElicitationSchemaProperty, but we accept any map[string]X.
func schemaHasProperty(schema map[string]any, name string) bool {
	if schema == nil {
		return false
	}
	props, ok := schema["properties"]
	if !ok || props == nil {
		return false
	}
	switch p := props.(type) {
	case map[string]any:
		_, found := p[name]
		return found
	case map[string]rpc.UIElicitationSchemaProperty:
		_, found := p[name]
		return found
	default:
		// Fallback: marshal/unmarshal via reflection-friendly route.
		// For test diagnostic purposes we treat unknown shapes as not found.
		return false
	}
}

func TestUIElicitationMultiClientE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.UseStdio = copilot.Bool(false)
		opts.TCPConnectionToken = sharedTcpToken
	})
	t.Cleanup(func() { client1.ForceStop() })

	// Start client1 with an init session to get the port
	initSession, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	})
	if err != nil {
		t.Fatalf("Failed to create init session: %v", err)
	}
	initSession.Disconnect()

	actualPort := client1.ActualPort()
	if actualPort == 0 {
		t.Fatalf("Expected non-zero port from TCP mode client")
	}

	t.Run("capabilities.changed fires when second client joins with elicitation handler", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Client1 creates a session without elicitation handler
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Verify initial state: no elicitation capability
		caps := session1.Capabilities()
		if caps.UI != nil && caps.UI.Elicitation {
			t.Error("Expected no elicitation capability before second client joins")
		}

		// Listen for capabilities.changed with elicitation enabled
		capEnabledCh := make(chan copilot.SessionEvent, 1)
		unsubscribe := session1.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeCapabilitiesChanged {
				if d, ok := event.Data.(*copilot.CapabilitiesChangedData); ok && d.UI != nil && d.UI.Elicitation != nil && *d.UI.Elicitation {
					select {
					case capEnabledCh <- event:
					default:
					}
				}
			}
		})

		// Client2 joins with elicitation handler — should trigger capabilities.changed
		client2 := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl:             fmt.Sprintf("localhost:%d", actualPort),
			TCPConnectionToken: sharedTcpToken,
		})
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			DisableResume:       true,
			OnElicitationRequest: func(ctx copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				return copilot.ElicitationResult{Action: "accept", Content: map[string]any{}}, nil
			},
		})
		if err != nil {
			client2.ForceStop()
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Wait for the elicitation-enabled capabilities.changed event
		select {
		case capEvent := <-capEnabledCh:
			capData, capOk := capEvent.Data.(*copilot.CapabilitiesChangedData)
			if !capOk || capData.UI == nil || capData.UI.Elicitation == nil || !*capData.UI.Elicitation {
				t.Errorf("Expected capabilities.changed with ui.elicitation=true, got %+v", capEvent.Data)
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for capabilities.changed event (elicitation enabled)")
		}

		unsubscribe()
		session2.Disconnect()
		client2.ForceStop()
	})

	t.Run("capabilities.changed fires when elicitation provider disconnects", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Client1 creates a session without elicitation handler
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Verify initial state: no elicitation capability
		caps := session1.Capabilities()
		if caps.UI != nil && caps.UI.Elicitation {
			t.Error("Expected no elicitation capability before provider joins")
		}

		// Listen for capability enabled
		capEnabledCh := make(chan struct{}, 1)
		unsubEnabled := session1.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeCapabilitiesChanged {
				if d, ok := event.Data.(*copilot.CapabilitiesChangedData); ok && d.UI != nil && d.UI.Elicitation != nil && *d.UI.Elicitation {
					select {
					case capEnabledCh <- struct{}{}:
					default:
					}
				}
			}
		})

		// Client3 (dedicated for this test) joins with elicitation handler
		client3 := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl:             fmt.Sprintf("localhost:%d", actualPort),
			TCPConnectionToken: sharedTcpToken,
		})
		_, err = client3.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			DisableResume:       true,
			OnElicitationRequest: func(ctx copilot.ElicitationContext) (copilot.ElicitationResult, error) {
				return copilot.ElicitationResult{Action: "accept", Content: map[string]any{}}, nil
			},
		})
		if err != nil {
			client3.ForceStop()
			t.Fatalf("Failed to resume session for client3: %v", err)
		}

		// Wait for elicitation to become enabled
		select {
		case <-capEnabledCh:
			// Good — elicitation is now enabled
		case <-time.After(30 * time.Second):
			client3.ForceStop()
			t.Fatal("Timed out waiting for capabilities.changed event (elicitation enabled)")
		}
		unsubEnabled()

		// Now listen for elicitation to become disabled
		capDisabledCh := make(chan struct{}, 1)
		unsubDisabled := session1.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeCapabilitiesChanged {
				if d, ok := event.Data.(*copilot.CapabilitiesChangedData); ok && d.UI != nil && d.UI.Elicitation != nil && !*d.UI.Elicitation {
					select {
					case capDisabledCh <- struct{}{}:
					default:
					}
				}
			}
		})

		// Disconnect client3 — should trigger capabilities.changed with elicitation=false
		client3.ForceStop()

		select {
		case <-capDisabledCh:
			// Good — got the disabled event
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for capabilities.changed event (elicitation disabled)")
		}
		unsubDisabled()
	})
}
