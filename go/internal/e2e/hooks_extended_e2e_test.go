package e2e

import (
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// Mirrors dotnet/test/HookLifecycleAndOutputTests.cs (snapshot category "hooks_extended").
//
// Covers each handler exposed on copilot.SessionHooks: OnPreToolUse,
// OnPostToolUse, OnPostToolUseFailure, OnUserPromptSubmitted, OnSessionStart,
// OnSessionEnd, OnErrorOccurred. Output-shape behavior (modifiedPrompt /
// additionalContext / errorHandling / modifiedArgs / modifiedResult /
// sessionSummary) is asserted alongside hook invocation. If a new handler is
// added to SessionHooks, add a corresponding test here.
func TestHooksExtendedE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should invoke userPromptSubmitted hook and modify prompt", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.UserPromptSubmittedHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnUserPromptSubmitted: func(input copilot.UserPromptSubmittedHookInput, invocation copilot.HookInvocation) (*copilot.UserPromptSubmittedHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}
					return &copilot.UserPromptSubmittedHookOutput{
						ModifiedPrompt: "Reply with exactly: HOOKED_PROMPT",
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say something else"})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected at least one userPromptSubmitted hook invocation")
		}
		if !strings.Contains(inputs[0].Prompt, "Say something else") {
			t.Errorf("Expected hook input prompt to contain original prompt, got %q", inputs[0].Prompt)
		}

		assistantMessage, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok || !strings.Contains(assistantMessage.Content, "HOOKED_PROMPT") {
			t.Errorf("Expected response to contain 'HOOKED_PROMPT', got %v", response.Data)
		}
	})

	t.Run("should invoke sessionStart hook", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.SessionStartHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnSessionStart: func(input copilot.SessionStartHookInput, invocation copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}
					return &copilot.SessionStartHookOutput{
						AdditionalContext: "Session start hook context.",
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say hi"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected sessionStart hook to be invoked at least once")
		}
		if inputs[0].Source != "new" {
			t.Errorf("Expected source 'new', got %q", inputs[0].Source)
		}
		if inputs[0].WorkingDirectory == "" {
			t.Error("Expected non-empty cwd in sessionStart hook input")
		}
	})

	t.Run("should invoke sessionEnd hook", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu          sync.Mutex
			inputs      []copilot.SessionEndHookInput
			invocations = make(chan copilot.SessionEndHookInput, 4)
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnSessionEnd: func(input copilot.SessionEndHookInput, invocation copilot.HookInvocation) (*copilot.SessionEndHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}
					select {
					case invocations <- input:
					default:
					}
					return &copilot.SessionEndHookOutput{
						SessionSummary: "session ended",
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say bye"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}
		if err := session.Disconnect(); err != nil {
			t.Fatalf("Failed to disconnect session: %v", err)
		}

		select {
		case <-invocations:
		case <-time.After(10 * time.Second):
			t.Fatal("Timed out waiting for sessionEnd hook invocation")
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected sessionEnd hook to be invoked at least once")
		}
	})

	t.Run("should register errorOccurred hook", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.ErrorOccurredHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnErrorOccurred: func(input copilot.ErrorOccurredHookInput, invocation copilot.HookInvocation) (*copilot.ErrorOccurredHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}
					return &copilot.ErrorOccurredHookOutput{ErrorHandling: "skip"}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		if _, err := session.SendAndWait(t.Context(), copilot.MessageOptions{Prompt: "Say hi"}); err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		// OnErrorOccurred is dispatched only by genuine runtime errors (e.g. provider
		// failures, internal exceptions). A normal turn cannot deterministically trigger
		// one, so this is a registration-only test: the SDK must accept the hook and not
		// invoke it inappropriately during a healthy turn.
		mu.Lock()
		got := len(inputs)
		mu.Unlock()
		if got != 0 {
			t.Errorf("Expected errorOccurred hook to not fire on a healthy turn, got %d invocations", got)
		}
		if session.SessionID == "" {
			t.Error("Expected session id to be set")
		}
	})

	t.Run("should allow preToolUse to return modifiedArgs and suppressOutput", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		type EchoParams struct {
			Value string `json:"value" jsonschema:"Value to echo"`
		}
		echoTool := copilot.DefineTool("echo_value", "Echoes the supplied value",
			func(params EchoParams, inv copilot.ToolInvocation) (string, error) {
				return params.Value, nil
			})

		var (
			mu     sync.Mutex
			inputs []copilot.PreToolUseHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Tools:               []copilot.Tool{echoTool},
			Hooks: &copilot.SessionHooks{
				OnPreToolUse: func(input copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					if input.ToolName != "echo_value" {
						return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
					}
					return &copilot.PreToolUseHookOutput{
						PermissionDecision: "allow",
						ModifiedArgs:       map[string]any{"value": "modified by hook"},
						SuppressOutput:     false,
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Call echo_value with value 'original', then reply with the result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected preToolUse hook to be invoked at least once")
		}
		hadEchoInput := false
		for _, input := range inputs {
			if input.ToolName == "echo_value" {
				hadEchoInput = true
				break
			}
		}
		if !hadEchoInput {
			t.Errorf("Expected at least one preToolUse invocation for echo_value, got %+v", inputs)
		}

		assistantMessage, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok || !strings.Contains(assistantMessage.Content, "modified by hook") {
			t.Errorf("Expected response to contain 'modified by hook', got %v", response.Data)
		}
	})

	t.Run("should allow postToolUse to return modifiedResult", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.PostToolUseHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnPostToolUse: func(input copilot.PostToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					if input.ToolName != "view" {
						return nil, nil
					}
					return &copilot.PostToolUseHookOutput{
						ModifiedResult: copilot.ToolResult{
							TextResultForLLM: "modified by post hook",
							ResultType:       "success",
							ToolTelemetry:    map[string]any{},
						},
						SuppressOutput: false,
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Call the view tool to read the current directory, then reply done.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		hadView := false
		for _, input := range inputs {
			if input.ToolName == "view" {
				hadView = true
				break
			}
		}
		if !hadView {
			t.Errorf("Expected at least one postToolUse invocation for view, got %+v", inputs)
		}

		assistantMessage, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok || !strings.Contains(strings.ToLower(assistantMessage.Content), "done") {
			t.Errorf("Expected response content to contain 'done', got %v", response.Data)
		}
	})

	t.Run("should invoke postToolUseFailure hook for failed tool result", func(t *testing.T) {
		t.Skip("Fails with 1.0.64-0 runtime: built-in tools are not available when " +
			"hooks restrict availableTools, so the failure path cannot be exercised. " +
			"Follow up with runtime team.")
		ctx.ConfigureForTest(t)

		var (
			mu                sync.Mutex
			failureInputs     []copilot.PostToolUseFailureHookInput
			postToolUseInputs []copilot.PostToolUseHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      []string{"report_intent"},
			Hooks: &copilot.SessionHooks{
				OnPostToolUse: func(input copilot.PostToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
					mu.Lock()
					postToolUseInputs = append(postToolUseInputs, input)
					mu.Unlock()
					return nil, nil
				},
				OnPostToolUseFailure: func(input copilot.PostToolUseFailureHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseFailureHookOutput, error) {
					mu.Lock()
					failureInputs = append(failureInputs, input)
					mu.Unlock()
					if invocation.SessionID == "" {
						t.Error("Expected non-empty session ID in invocation")
					}
					return &copilot.PostToolUseFailureHookOutput{
						AdditionalContext: "HOOK_FAILURE_GUIDANCE_APPLIED",
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Call the view tool with path 'missing.txt'. If it fails, use the hook guidance to answer.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(postToolUseInputs) != 0 {
			t.Fatalf("Expected postToolUse not to fire for failed result, got %+v", postToolUseInputs)
		}
		if len(failureInputs) != 1 {
			t.Fatalf("Expected one postToolUseFailure input, got %+v", failureInputs)
		}
		input := failureInputs[0]
		if input.ToolName != "view" {
			t.Errorf("Expected tool name view, got %q", input.ToolName)
		}
		if !strings.Contains(input.Error, "does not exist") {
			t.Errorf("Expected missing-tool error, got %q", input.Error)
		}
		if !strings.Contains(fmt.Sprint(input.ToolArgs), "missing.txt") {
			t.Errorf("Expected tool args to contain missing.txt, got %+v", input.ToolArgs)
		}
		if input.WorkingDirectory == "" {
			t.Error("Expected working directory to be populated")
		}
		if input.Timestamp.IsZero() {
			t.Error("Expected timestamp to be populated")
		}
		if assistantMessage, ok := response.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(assistantMessage.Content, "HOOK_FAILURE_GUIDANCE_APPLIED") {
			t.Errorf("Expected response to contain hook guidance, got %v", response.Data)
		}
	})
}
