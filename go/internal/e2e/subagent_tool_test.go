//go:build integration

package e2e

import (
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// TestSubagentCustomTools requires a real CLI to test the full round-trip of
// subagent child sessions invoking custom tools registered on parent sessions.
//
// Run with:
//
//	cd go && go test -tags integration -v ./internal/e2e -run TestSubagentCustomTools
//
// Prerequisites:
//   - Copilot CLI installed (or COPILOT_CLI_PATH set)
//   - Valid GitHub authentication configured
func TestSubagentCustomTools(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("subagent invokes parent custom tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Track tool invocations
		toolInvoked := make(chan string, 1)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				copilot.DefineTool("save_result", "Saves a result string",
					func(params struct {
						Result string `json:"result" jsonschema:"The result to save"`
					}, inv copilot.ToolInvocation) (string, error) {
						toolInvoked <- params.Result
						return "saved: " + params.Result, nil
					}),
			},
			CustomAgents: []copilot.CustomAgentConfig{
				{
					Name:        "helper-agent",
					DisplayName: "Helper Agent",
					Description: "A helper agent that can save results using the save_result tool",
					Tools:       []string{"save_result"},
					Prompt:      "You are a helper agent. When asked to save something, use the save_result tool.",
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Send a message that should trigger the subagent which invokes the custom tool
		_, err = session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Use the helper-agent to save the result 'hello world'",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// Wait for the tool to be invoked (with timeout)
		select {
		case result := <-toolInvoked:
			if !strings.Contains(strings.ToLower(result), "hello world") {
				t.Errorf("Expected tool to receive 'hello world', got %q", result)
			}
		case <-time.After(30 * time.Second):
			t.Fatal("Timeout waiting for save_result tool invocation from subagent")
		}

		// Get the final response
		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}
		if answer.Data.Content == nil {
			t.Fatal("Expected non-nil content in response")
		}
		t.Logf("Response: %s", *answer.Data.Content)
	})

	t.Run("subagent denied unlisted tool returns unsupported", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools: []copilot.Tool{
				copilot.DefineTool("allowed_tool", "An allowed tool",
					func(params struct{}, inv copilot.ToolInvocation) (string, error) {
						return "allowed", nil
					}),
				copilot.DefineTool("restricted_tool", "A restricted tool",
					func(params struct{}, inv copilot.ToolInvocation) (string, error) {
						t.Error("restricted_tool should not be invoked by subagent")
						return "should not reach here", nil
					}),
			},
			CustomAgents: []copilot.CustomAgentConfig{
				{
					Name:        "restricted-agent",
					DisplayName: "Restricted Agent",
					Description: "An agent with limited tool access",
					Tools:       []string{"allowed_tool"}, // restricted_tool NOT listed
					Prompt:      "You are a restricted agent. Try to use both allowed_tool and restricted_tool.",
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		_, err = session.Send(t.Context(), copilot.MessageOptions{
			Prompt: "Use the restricted-agent to invoke restricted_tool",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		answer, err := testharness.GetFinalAssistantMessage(t.Context(), session)
		if err != nil {
			t.Fatalf("Failed to get assistant message: %v", err)
		}
		if answer.Data.Content != nil {
			t.Logf("Response: %s", *answer.Data.Content)
		}
		// The restricted_tool handler should NOT have been called (assertion in handler above)
	})
}
