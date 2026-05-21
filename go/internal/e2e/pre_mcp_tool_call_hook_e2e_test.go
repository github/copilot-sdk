package e2e

import (
	"path/filepath"
	"strings"
	"sync"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestPreMcpToolCallHookE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	testHarnessDir, _ := filepath.Abs("../../../test/harness")
	metaEchoServer := filepath.Join(testHarnessDir, "test-mcp-meta-echo-server.mjs")

	metaEchoConfig := func() map[string]copilot.MCPServerConfig {
		return map[string]copilot.MCPServerConfig{
			"meta-echo": copilot.MCPStdioServerConfig{
				Command:          "node",
				Args:             []string{metaEchoServer},
				WorkingDirectory: testHarnessDir,
				Tools:            []string{"*"},
			},
		}
	}

	t.Run("should set meta via preMcpToolCall hook", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.PreMcpToolCallHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          metaEchoConfig(),
			Hooks: &copilot.SessionHooks{
				OnPreMcpToolCall: func(input copilot.PreMcpToolCallHookInput, invocation copilot.HookInvocation) (*copilot.PreMcpToolCallHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					return &copilot.PreMcpToolCallHookOutput{
						MetaToUse: copilot.MetaToUseReplace(map[string]any{
							"injected": "by-hook",
							"source":   "test",
						}),
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the meta-echo/echo_meta tool with value 'test-set'. Reply with just the raw tool result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected assistant message data, got %T", response.Data)
		}
		if !strings.Contains(assistantMessage.Content, "injected") || !strings.Contains(assistantMessage.Content, "by-hook") {
			t.Errorf("Expected response to contain 'injected' and 'by-hook', got %q", assistantMessage.Content)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected at least one preMcpToolCall hook invocation")
		}
		if inputs[0].ServerName != "meta-echo" {
			t.Errorf("Expected serverName 'meta-echo', got %q", inputs[0].ServerName)
		}
		if inputs[0].ToolName != "echo_meta" {
			t.Errorf("Expected toolName 'echo_meta', got %q", inputs[0].ToolName)
		}
		if inputs[0].WorkingDirectory == "" {
			t.Error("Expected non-empty workingDirectory")
		}
		if inputs[0].Timestamp <= 0 {
			t.Error("Expected positive timestamp")
		}
	})

	t.Run("should replace meta via preMcpToolCall hook", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.PreMcpToolCallHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          metaEchoConfig(),
			Hooks: &copilot.SessionHooks{
				OnPreMcpToolCall: func(input copilot.PreMcpToolCallHookInput, invocation copilot.HookInvocation) (*copilot.PreMcpToolCallHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					return &copilot.PreMcpToolCallHookOutput{
						MetaToUse: copilot.MetaToUseReplace(map[string]any{
							"completely": "replaced",
						}),
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the meta-echo/echo_meta tool with value 'test-replace'. Reply with just the raw tool result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected assistant message data, got %T", response.Data)
		}
		if !strings.Contains(assistantMessage.Content, "completely") || !strings.Contains(assistantMessage.Content, "replaced") {
			t.Errorf("Expected response to contain 'completely' and 'replaced', got %q", assistantMessage.Content)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected at least one preMcpToolCall hook invocation")
		}
		if inputs[0].ServerName != "meta-echo" {
			t.Errorf("Expected serverName 'meta-echo', got %q", inputs[0].ServerName)
		}
		if inputs[0].ToolName != "echo_meta" {
			t.Errorf("Expected toolName 'echo_meta', got %q", inputs[0].ToolName)
		}
	})

	t.Run("should remove meta via preMcpToolCall hook", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		var (
			mu     sync.Mutex
			inputs []copilot.PreMcpToolCallHookInput
		)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          metaEchoConfig(),
			Hooks: &copilot.SessionHooks{
				OnPreMcpToolCall: func(input copilot.PreMcpToolCallHookInput, invocation copilot.HookInvocation) (*copilot.PreMcpToolCallHookOutput, error) {
					mu.Lock()
					inputs = append(inputs, input)
					mu.Unlock()
					return &copilot.PreMcpToolCallHookOutput{
						MetaToUse: copilot.MetaToUseRemove(),
					}, nil
				},
			},
		})
		if err != nil {
			t.Fatalf("Failed to create session: %v", err)
		}

		response, err := session.SendAndWait(t.Context(), copilot.MessageOptions{
			Prompt: "Use the meta-echo/echo_meta tool with value 'test-remove'. Reply with just the raw tool result.",
		})
		if err != nil {
			t.Fatalf("Failed to send message: %v", err)
		}

		assistantMessage, ok := response.Data.(*copilot.AssistantMessageData)
		if !ok {
			t.Fatalf("Expected assistant message data, got %T", response.Data)
		}
		if !strings.Contains(assistantMessage.Content, `"meta":null`) {
			t.Errorf("Expected response to contain '\"meta\":null', got %q", assistantMessage.Content)
		}
		if !strings.Contains(assistantMessage.Content, "test-remove") {
			t.Errorf("Expected response to contain 'test-remove', got %q", assistantMessage.Content)
		}

		mu.Lock()
		defer mu.Unlock()
		if len(inputs) == 0 {
			t.Fatal("Expected at least one preMcpToolCall hook invocation")
		}
		if inputs[0].ServerName != "meta-echo" {
			t.Errorf("Expected serverName 'meta-echo', got %q", inputs[0].ServerName)
		}
		if inputs[0].ToolName != "echo_meta" {
			t.Errorf("Expected toolName 'echo_meta', got %q", inputs[0].ToolName)
		}
	})
}
