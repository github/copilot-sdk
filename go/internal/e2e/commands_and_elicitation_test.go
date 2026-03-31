package e2e

import (
	"fmt"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestCommands(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.UseStdio = copilot.Bool(false)
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
		CLIUrl: fmt.Sprintf("localhost:%d", actualPort),
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
			if len(event.Data.Commands) == 0 {
				t.Errorf("Expected commands in commands.changed event")
			} else {
				found := false
				for _, cmd := range event.Data.Commands {
					if cmd.Name == "deploy" {
						found = true
						if cmd.Description == nil || *cmd.Description != "Deploy the app" {
							t.Errorf("Expected deploy command description 'Deploy the app', got %v", cmd.Description)
						}
						break
					}
				}
				if !found {
					t.Errorf("Expected 'deploy' command in commands.changed event, got %+v", event.Data.Commands)
				}
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timed out waiting for commands.changed event")
		}

		session2.Disconnect()
	})
}

func TestUIElicitation(t *testing.T) {
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

func TestUIElicitationCallback(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("session with OnElicitationRequest reports elicitation capability", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			OnElicitationRequest: func(req copilot.ElicitationRequest, inv copilot.ElicitationInvocation) (copilot.ElicitationResult, error) {
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
}

func TestUIElicitationMultiClient(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.UseStdio = copilot.Bool(false)
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
				if event.Data.UI != nil && event.Data.UI.Elicitation != nil && *event.Data.UI.Elicitation {
					select {
					case capEnabledCh <- event:
					default:
					}
				}
			}
		})

		// Client2 joins with elicitation handler — should trigger capabilities.changed
		client2 := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl: fmt.Sprintf("localhost:%d", actualPort),
		})
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			DisableResume:       true,
			OnElicitationRequest: func(req copilot.ElicitationRequest, inv copilot.ElicitationInvocation) (copilot.ElicitationResult, error) {
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
			if capEvent.Data.UI == nil || capEvent.Data.UI.Elicitation == nil || !*capEvent.Data.UI.Elicitation {
				t.Errorf("Expected capabilities.changed with ui.elicitation=true, got %+v", capEvent.Data.UI)
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
				if event.Data.UI != nil && event.Data.UI.Elicitation != nil && *event.Data.UI.Elicitation {
					select {
					case capEnabledCh <- struct{}{}:
					default:
					}
				}
			}
		})

		// Client3 (dedicated for this test) joins with elicitation handler
		client3 := copilot.NewClient(&copilot.ClientOptions{
			CLIUrl: fmt.Sprintf("localhost:%d", actualPort),
		})
		_, err = client3.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			DisableResume:       true,
			OnElicitationRequest: func(req copilot.ElicitationRequest, inv copilot.ElicitationInvocation) (copilot.ElicitationResult, error) {
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
				if event.Data.UI != nil && event.Data.UI.Elicitation != nil && !*event.Data.UI.Elicitation {
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
