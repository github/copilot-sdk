package e2e

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestMultiClient(t *testing.T) {
	// Use TCP mode so a second client can connect to the same CLI process
	ctx := testharness.NewTestContext(t)
	client1 := ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.UseStdio = copilot.Bool(false)
	})
	t.Cleanup(func() { client1.ForceStop() })

	// Trigger connection so we can read the port
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

	t.Run("both clients see tool request and completion events", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type SeedParams struct {
			Seed string `json:"seed" jsonschema:"A seed value"`
		}

		tool := copilot.DefineTool("magic_number", "Returns a magic number",
			func(params SeedParams, inv copilot.ToolInvocation) (string, error) {
				return fmt.Sprintf("MAGIC_%s_42", params.Seed), nil
			})

		// Client 1 creates a session with a custom tool
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{tool},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Client 2 resumes with NO tools — should not overwrite client 1's tools
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Set up event waiters BEFORE sending the prompt to avoid race conditions
		client1Requested := make(chan struct{}, 1)
		client2Requested := make(chan struct{}, 1)
		client1Completed := make(chan struct{}, 1)
		client2Completed := make(chan struct{}, 1)

		session1.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeExternalToolRequested {
				select {
				case client1Requested <- struct{}{}:
				default:
				}
			}
			if event.Type == copilot.SessionEventTypeExternalToolCompleted {
				select {
				case client1Completed <- struct{}{}:
				default:
				}
			}
		})
		session2.On(func(event copilot.SessionEvent) {
			if event.Type == copilot.SessionEventTypeExternalToolRequested {
				select {
				case client2Requested <- struct{}{}:
				default:
				}
			}
			if event.Type == copilot.SessionEventTypeExternalToolCompleted {
				select {
				case client2Completed <- struct{}{}:
				default:
				}
			}
		})

		// Send a prompt that triggers the custom tool
		response, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the magic_number tool with seed 'hello' and tell me the result",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		if response == nil || response.Data.Content == nil || !strings.Contains(*response.Data.Content.String, "MAGIC_hello_42") {
			t.Errorf("Expected response to contain 'MAGIC_hello_42', got %v", response)
		}

		// Wait for all broadcast events to arrive on both clients
		timeout := time.After(30 * time.Second)
		for _, ch := range []chan struct{}{client1Requested, client2Requested, client1Completed, client2Completed} {
			select {
			case <-ch:
			case <-timeout:
				t.Fatal("Timed out waiting for broadcast events on both clients")
			}
		}

		session2.Disconnect()
	})

	t.Run("one client approves permission and both see the result", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var client1PermissionRequests []copilot.PermissionRequest
		var mu sync.Mutex

		// Client 1 creates a session and manually approves permission requests
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				mu.Lock()
				client1PermissionRequests = append(client1PermissionRequests, request)
				mu.Unlock()
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindApproved}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Client 2 resumes — its handler never resolves, so only client 1's approval takes effect
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				// Block forever so only client 1's handler responds
				select {}
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Track events
		var client1Events, client2Events []copilot.SessionEvent
		var mu1, mu2 sync.Mutex
		session1.On(func(event copilot.SessionEvent) {
			mu1.Lock()
			client1Events = append(client1Events, event)
			mu1.Unlock()
		})
		session2.On(func(event copilot.SessionEvent) {
			mu2.Lock()
			client2Events = append(client2Events, event)
			mu2.Unlock()
		})

		// Send a prompt that triggers a write operation (requires permission)
		response, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Create a file called hello.txt containing the text 'hello world'",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if response == nil || response.Data.Content == nil || *response.Data.Content.String == "" {
			t.Errorf("Expected non-empty response")
		}

		// Client 1 should have handled the permission request
		mu.Lock()
		permCount := len(client1PermissionRequests)
		mu.Unlock()
		if permCount == 0 {
			t.Errorf("Expected client 1 to handle at least one permission request")
		}

		// Both clients should have seen permission.requested events
		mu1.Lock()
		c1PermRequested := filterEventsByType(client1Events, copilot.SessionEventTypePermissionRequested)
		mu1.Unlock()
		mu2.Lock()
		c2PermRequested := filterEventsByType(client2Events, copilot.SessionEventTypePermissionRequested)
		mu2.Unlock()

		if len(c1PermRequested) == 0 {
			t.Errorf("Expected client 1 to see permission.requested events")
		}
		if len(c2PermRequested) == 0 {
			t.Errorf("Expected client 2 to see permission.requested events")
		}

		// Both clients should have seen permission.completed events with approved result
		mu1.Lock()
		c1PermCompleted := filterEventsByType(client1Events, copilot.SessionEventTypePermissionCompleted)
		mu1.Unlock()
		mu2.Lock()
		c2PermCompleted := filterEventsByType(client2Events, copilot.SessionEventTypePermissionCompleted)
		mu2.Unlock()

		if len(c1PermCompleted) == 0 {
			t.Errorf("Expected client 1 to see permission.completed events")
		}
		if len(c2PermCompleted) == 0 {
			t.Errorf("Expected client 2 to see permission.completed events")
		}
		for _, event := range append(c1PermCompleted, c2PermCompleted...) {
			if event.Data.Result == nil || event.Data.Result.Kind == nil || *event.Data.Result.Kind != "approved" {
				t.Errorf("Expected permission.completed result kind 'approved', got %v", event.Data.Result)
			}
		}

		session2.Disconnect()
	})

	t.Run("one client rejects permission and both see the result", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Client 1 creates a session and denies all permission requests
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				return copilot.PermissionRequestResult{Kind: copilot.PermissionRequestResultKindDeniedInteractivelyByUser}, nil
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Client 2 resumes — its handler never resolves so only client 1's denial takes effect
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: func(request copilot.PermissionRequest, invocation copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
				select {}
			},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		var client1Events, client2Events []copilot.SessionEvent
		var mu1, mu2 sync.Mutex
		session1.On(func(event copilot.SessionEvent) {
			mu1.Lock()
			client1Events = append(client1Events, event)
			mu1.Unlock()
		})
		session2.On(func(event copilot.SessionEvent) {
			mu2.Lock()
			client2Events = append(client2Events, event)
			mu2.Unlock()
		})

		// Write a test file and ask the agent to edit it
		testFile := filepath.Join(ctx.WorkDir, "protected.txt")
		if err := os.WriteFile(testFile, []byte("protected content"), 0644); err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Edit protected.txt and replace 'protected' with 'hacked'.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Verify the file was NOT modified (permission was denied)
		content, err := os.ReadFile(testFile)
		if err != nil {
			t.Fatalf("Failed to read test file: %v", err)
		}
		if string(content) != "protected content" {
			t.Errorf("Expected file content 'protected content', got '%s'", string(content))
		}

		// Both clients should have seen permission.requested events
		mu1.Lock()
		c1PermRequested := filterEventsByType(client1Events, copilot.SessionEventTypePermissionRequested)
		mu1.Unlock()
		mu2.Lock()
		c2PermRequested := filterEventsByType(client2Events, copilot.SessionEventTypePermissionRequested)
		mu2.Unlock()

		if len(c1PermRequested) == 0 {
			t.Errorf("Expected client 1 to see permission.requested events")
		}
		if len(c2PermRequested) == 0 {
			t.Errorf("Expected client 2 to see permission.requested events")
		}

		// Both clients should see the denial in the completed event
		mu1.Lock()
		c1PermCompleted := filterEventsByType(client1Events, copilot.SessionEventTypePermissionCompleted)
		mu1.Unlock()
		mu2.Lock()
		c2PermCompleted := filterEventsByType(client2Events, copilot.SessionEventTypePermissionCompleted)
		mu2.Unlock()

		if len(c1PermCompleted) == 0 {
			t.Errorf("Expected client 1 to see permission.completed events")
		}
		if len(c2PermCompleted) == 0 {
			t.Errorf("Expected client 2 to see permission.completed events")
		}
		for _, event := range append(c1PermCompleted, c2PermCompleted...) {
			if event.Data.Result == nil || event.Data.Result.Kind == nil || *event.Data.Result.Kind != "denied-interactively-by-user" {
				t.Errorf("Expected permission.completed result kind 'denied-interactively-by-user', got %v", event.Data.Result)
			}
		}

		session2.Disconnect()
	})

	t.Run("two clients register different tools and agent uses both", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type CountryCodeParams struct {
			CountryCode string `json:"countryCode" jsonschema:"A two-letter country code"`
		}

		toolA := copilot.DefineTool("city_lookup", "Returns a city name for a given country code",
			func(params CountryCodeParams, inv copilot.ToolInvocation) (string, error) {
				return fmt.Sprintf("CITY_FOR_%s", params.CountryCode), nil
			})

		toolB := copilot.DefineTool("currency_lookup", "Returns a currency for a given country code",
			func(params CountryCodeParams, inv copilot.ToolInvocation) (string, error) {
				return fmt.Sprintf("CURRENCY_FOR_%s", params.CountryCode), nil
			})

		// Client 1 creates a session with tool A
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{toolA},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Client 2 resumes with tool B (different tool, union should have both)
		session2, err := client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{toolB},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Send prompts sequentially to avoid nondeterministic tool_call ordering
		response1, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the city_lookup tool with countryCode 'US' and tell me the result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if response1 == nil || response1.Data.Content == nil {
			t.Fatalf("Expected response with content")
		}
		if !strings.Contains(*response1.Data.Content.String, "CITY_FOR_US") {
			t.Errorf("Expected response to contain 'CITY_FOR_US', got '%s'", *response1.Data.Content.String)
		}

		response2, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Now use the currency_lookup tool with countryCode 'US' and tell me the result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if response2 == nil || response2.Data.Content == nil {
			t.Fatalf("Expected response with content")
		}
		if !strings.Contains(*response2.Data.Content.String, "CURRENCY_FOR_US") {
			t.Errorf("Expected response to contain 'CURRENCY_FOR_US', got '%s'", *response2.Data.Content.String)
		}

		session2.Disconnect()
	})

	t.Run("disconnecting client removes its tools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type InputParams struct {
			Input string `json:"input" jsonschema:"Input string"`
		}

		toolA := copilot.DefineTool("stable_tool", "A tool that persists across disconnects",
			func(params InputParams, inv copilot.ToolInvocation) (string, error) {
				return fmt.Sprintf("STABLE_%s", params.Input), nil
			})

		toolB := copilot.DefineTool("ephemeral_tool", "A tool that will disappear when its client disconnects",
			func(params InputParams, inv copilot.ToolInvocation) (string, error) {
				return fmt.Sprintf("EPHEMERAL_%s", params.Input), nil
			})

		// Client 1 creates a session with stable_tool
		session1, err := client1.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{toolA},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Client 2 resumes with ephemeral_tool
		_, err = client2.ResumeSession(t.Context(), session1.SessionID, &copilot.ResumeSessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{toolB},
		})
		if err != nil {
			t.Fatalf("Failed to resume session: %v", err)
		}

		// Verify both tools work before disconnect (sequential to avoid nondeterministic tool_call ordering)
		stableResponse, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the stable_tool with input 'test1' and tell me the result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if stableResponse == nil || stableResponse.Data.Content == nil {
			t.Fatalf("Expected response with content")
		}
		if !strings.Contains(*stableResponse.Data.Content.String, "STABLE_test1") {
			t.Errorf("Expected response to contain 'STABLE_test1', got '%s'", *stableResponse.Data.Content.String)
		}

		ephemeralResponse, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the ephemeral_tool with input 'test2' and tell me the result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if ephemeralResponse == nil || ephemeralResponse.Data.Content == nil {
			t.Fatalf("Expected response with content")
		}
		if !strings.Contains(*ephemeralResponse.Data.Content.String, "EPHEMERAL_test2") {
			t.Errorf("Expected response to contain 'EPHEMERAL_test2', got '%s'", *ephemeralResponse.Data.Content.String)
		}

		// Disconnect client 2 without destroying the shared session
		client2.ForceStop()

		// Give the server time to process the connection close and remove tools
		time.Sleep(500 * time.Millisecond)

		// Recreate client2 for cleanup (but don't rejoin the session)
		client2 = copilot.NewClient(&copilot.ClientOptions{
			CLIUrl: fmt.Sprintf("localhost:%d", actualPort),
		})

		// Now only stable_tool should be available
		afterResponse, err := session1.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the stable_tool with input 'still_here'. Also try using ephemeral_tool if it is available.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if afterResponse == nil || afterResponse.Data.Content == nil {
			t.Fatalf("Expected response with content")
		}
		if !strings.Contains(*afterResponse.Data.Content.String, "STABLE_still_here") {
			t.Errorf("Expected response to contain 'STABLE_still_here', got '%s'", *afterResponse.Data.Content.String)
		}
		// ephemeral_tool should NOT have produced a result
		if strings.Contains(*afterResponse.Data.Content.String, "EPHEMERAL_") {
			t.Errorf("Expected response NOT to contain 'EPHEMERAL_', got '%s'", *afterResponse.Data.Content.String)
		}
	})
}

func filterEventsByType(events []copilot.SessionEvent, eventType copilot.SessionEventType) []copilot.SessionEvent {
	var filtered []copilot.SessionEvent
	for _, e := range events {
		if e.Type == eventType {
			filtered = append(filtered, e)
		}
	}
	return filtered
}
