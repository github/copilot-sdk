package e2e

import (
	"os"
	"path/filepath"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/e2e/testharness"
)

func TestHooks(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should invoke preToolUse hook when model runs a tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var preToolUseInputs []copilot.PreToolUseHookInput
		var mu sync.Mutex

		session, err := client.CreateSession(&copilot.SessionConfig{
			Hooks: &copilot.SessionHooks{
				OnPreToolUse: func(input copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
					mu.Lock()
					preToolUseInputs = append(preToolUseInputs, input)
					mu.Unlock()

					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}

					return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Create a file for the model to read
		testFile := filepath.Join(ctx.WorkDir, "hello.txt")
		err = os.WriteFile(testFile, []byte("Hello from the test!"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Read the contents of hello.txt and tell me what it says",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(preToolUseInputs) == 0 {
			t.Error("Expected at least one preToolUse hook call")
		}

		hasToolName := false
		for _, input := range preToolUseInputs {
			if input.ToolName != "" {
				hasToolName = true
				break
			}
		}
		if !hasToolName {
			t.Error("Expected at least one input with a tool name")
		}
	})

	t.Run("should invoke postToolUse hook after model runs a tool", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var postToolUseInputs []copilot.PostToolUseHookInput
		var mu sync.Mutex

		session, err := client.CreateSession(&copilot.SessionConfig{
			Hooks: &copilot.SessionHooks{
				OnPostToolUse: func(input copilot.PostToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
					mu.Lock()
					postToolUseInputs = append(postToolUseInputs, input)
					mu.Unlock()

					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}

					return nil, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Create a file for the model to read
		testFile := filepath.Join(ctx.WorkDir, "world.txt")
		err = os.WriteFile(testFile, []byte("World from the test!"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Read the contents of world.txt and tell me what it says",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(postToolUseInputs) == 0 {
			t.Error("Expected at least one postToolUse hook call")
		}

		hasToolName := false
		hasResult := false
		for _, input := range postToolUseInputs {
			if input.ToolName != "" {
				hasToolName = true
			}
			if input.ToolResult != nil {
				hasResult = true
			}
		}
		if !hasToolName {
			t.Error("Expected at least one input with a tool name")
		}
		if !hasResult {
			t.Error("Expected at least one input with a tool result")
		}
	})

	t.Run("should invoke both preToolUse and postToolUse hooks for a single tool call", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var preToolUseInputs []copilot.PreToolUseHookInput
		var postToolUseInputs []copilot.PostToolUseHookInput
		var mu sync.Mutex

		session, err := client.CreateSession(&copilot.SessionConfig{
			Hooks: &copilot.SessionHooks{
				OnPreToolUse: func(input copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
					mu.Lock()
					preToolUseInputs = append(preToolUseInputs, input)
					mu.Unlock()
					return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
				},
				OnPostToolUse: func(input copilot.PostToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
					mu.Lock()
					postToolUseInputs = append(postToolUseInputs, input)
					mu.Unlock()
					return nil, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		testFile := filepath.Join(ctx.WorkDir, "both.txt")
		err = os.WriteFile(testFile, []byte("Testing both hooks!"), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		_, err = session.SendAndWait(copilot.MessageOptions{
			Prompt: "Read the contents of both.txt",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(preToolUseInputs) == 0 {
			t.Error("Expected at least one preToolUse hook call")
		}
		if len(postToolUseInputs) == 0 {
			t.Error("Expected at least one postToolUse hook call")
		}

		// Check that the same tool appears in both
		preToolNames := make(map[string]bool)
		for _, input := range preToolUseInputs {
			if input.ToolName != "" {
				preToolNames[input.ToolName] = true
			}
		}

		foundCommon := false
		for _, input := range postToolUseInputs {
			if preToolNames[input.ToolName] {
				foundCommon = true
				break
			}
		}
		if !foundCommon {
			t.Error("Expected the same tool to appear in both pre and post hooks")
		}
	})

	t.Run("should deny tool execution when preToolUse returns deny", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var preToolUseInputs []copilot.PreToolUseHookInput
		var mu sync.Mutex

		session, err := client.CreateSession(&copilot.SessionConfig{
			Hooks: &copilot.SessionHooks{
				OnPreToolUse: func(input copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
					mu.Lock()
					preToolUseInputs = append(preToolUseInputs, input)
					mu.Unlock()
					// Deny all tool calls
					return &copilot.PreToolUseHookOutput{PermissionDecision: "deny"}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		// Create a file
		originalContent := "Original content that should not be modified"
		testFile := filepath.Join(ctx.WorkDir, "protected.txt")
		err = os.WriteFile(testFile, []byte(originalContent), 0644)
		if err != nil {
			t.Fatalf("Failed to write test file: %v", err)
		}

		response, err := session.SendAndWait(copilot.MessageOptions{
			Prompt: "Edit protected.txt and replace 'Original' with 'Modified'",
		}, 60*time.Second)
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()

		if len(preToolUseInputs) == 0 {
			t.Error("Expected at least one preToolUse hook call")
		}

		// The response should be defined
		if response == nil {
			t.Error("Expected non-nil response")
		}
	})
}
