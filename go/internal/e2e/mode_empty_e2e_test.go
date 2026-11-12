/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package e2e

import (
	"context"
	"regexp"
	"runtime"
	"slices"
	"strings"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// E2E coverage for Mode = ModeEmpty + ToolSet patterns. The runtime is
// mode-agnostic — these tests verify the SDK's translation reaches the
// runtime by inspecting captured chat-completion requests via the proxy.
func TestModeEmptyE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient(func(o *copilot.ClientOptions) {
		o.Mode = copilot.ModeEmpty
		o.BaseDirectory = ctx.HomeDir
	})
	t.Cleanup(func() { client.ForceStop() })

	getToolsExposedToLLM := func(t *testing.T) []string {
		t.Helper()
		exchanges := ctx.WaitForExchanges(t, 1)
		last := exchanges[len(exchanges)-1]
		names := make([]string, 0, len(last.Request.Tools))
		for _, tool := range last.Request.Tools {
			if tool.Type == "function" && tool.Function.Name != "" {
				names = append(names, tool.Function.Name)
			}
		}
		return names
	}

	getSystemMessageSentToLLM := func(t *testing.T) string {
		t.Helper()
		exchanges := ctx.WaitForExchanges(t, 1)
		last := exchanges[len(exchanges)-1]
		for _, m := range last.Request.Messages {
			if m.Role == "system" {
				return m.Content
			}
		}
		return ""
	}

	shellToolName := "bash"
	if runtime.GOOS == "windows" {
		shellToolName = "powershell"
	}

	t.Run("empty mode isolated set shell tool is not exposed", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      copilot.NewToolSet().AddBuiltIn(copilot.BuiltInToolsIsolated...).ToSlice(),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer func() { _ = session.Disconnect() }()

		sendCtx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		_, _ = session.SendAndWait(sendCtx, copilot.MessageOptions{Prompt: "Say hi."})

		toolNames := getToolsExposedToLLM(t)
		for _, banned := range []string{"bash", "powershell", "edit", "grep", "web_fetch"} {
			if slices.Contains(toolNames, banned) {
				t.Errorf("isolated set must not expose %q, got tools %v", banned, toolNames)
			}
		}
		anyIsolated := false
		for _, name := range copilot.BuiltInToolsIsolated {
			if slices.Contains(toolNames, name) {
				anyIsolated = true
				break
			}
		}
		if !anyIsolated {
			t.Errorf("expected at least one isolated tool to be registered, got %v", toolNames)
		}
	})

	t.Run("empty mode builtin star exposes all built in tools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      copilot.NewToolSet().AddBuiltIn("*").ToSlice(),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer func() { _ = session.Disconnect() }()

		sendCtx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		_, _ = session.SendAndWait(sendCtx, copilot.MessageOptions{Prompt: "Say hi."})

		toolNames := getToolsExposedToLLM(t)
		if !slices.Contains(toolNames, shellToolName) {
			t.Errorf("builtin:* should expose %q, got %v", shellToolName, toolNames)
		}
	})

	t.Run("empty mode excluded tools subtracts from available tools", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      copilot.NewToolSet().AddBuiltIn("*").ToSlice(),
			ExcludedTools:       []string{"builtin:" + shellToolName},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer func() { _ = session.Disconnect() }()

		sendCtx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		_, _ = session.SendAndWait(sendCtx, copilot.MessageOptions{Prompt: "Say hi."})

		toolNames := getToolsExposedToLLM(t)
		if slices.Contains(toolNames, shellToolName) {
			t.Errorf("excluded shell tool %q leaked through builtin:*, got %v", shellToolName, toolNames)
		}
		if len(toolNames) == 0 {
			t.Errorf("expected other built-ins to remain after subtraction, got empty list")
		}
	})

	t.Run("empty mode strips environment context from the system message by default", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      copilot.NewToolSet().AddBuiltIn(copilot.BuiltInToolsIsolated...).ToSlice(),
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "customize",
				Content: "If the user asks you to name an element, reply with exactly the single word ARGON in all caps and nothing else.",
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer func() { _ = session.Disconnect() }()

		sendCtx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		reply, err := session.SendAndWait(sendCtx, copilot.MessageOptions{Prompt: "Name an element."})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if data, ok := reply.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(data.Content, "ARGON") {
			t.Errorf("expected response to contain ARGON, got %+v", reply.Data)
		}

		sys := getSystemMessageSentToLLM(t)
		if regexp.MustCompile(`(?i)current working directory:`).MatchString(sys) {
			t.Errorf("system message should not contain 'Current working directory:': %q", sys)
		}
		if regexp.MustCompile(`(?i)operating system:`).MatchString(sys) {
			t.Errorf("system message should not contain 'Operating System:': %q", sys)
		}
	})

	t.Run("empty mode system message replace llm follows caller content verbatim", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      copilot.NewToolSet().AddBuiltIn(copilot.BuiltInToolsIsolated...).ToSlice(),
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "replace",
				Content: "You are a test fixture. Whenever the user asks anything, reply with exactly the single word KRYPTON in all caps and nothing else.",
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer func() { _ = session.Disconnect() }()

		sendCtx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		reply, err := session.SendAndWait(sendCtx, copilot.MessageOptions{Prompt: "Hello."})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if data, ok := reply.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(data.Content, "KRYPTON") {
			t.Errorf("expected response to contain KRYPTON, got %+v", reply.Data)
		}
	})

	t.Run("empty mode append caller instruction takes effect and env context stripped", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			AvailableTools:      copilot.NewToolSet().AddBuiltIn(copilot.BuiltInToolsIsolated...).ToSlice(),
			SystemMessage: &copilot.SystemMessageConfig{
				Mode:    "append",
				Content: "If the user asks you to name a noble gas, reply with exactly the single word XENON in all caps and nothing else.",
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		defer func() { _ = session.Disconnect() }()

		sendCtx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
		defer cancel()
		reply, err := session.SendAndWait(sendCtx, copilot.MessageOptions{Prompt: "Name a noble gas."})
		if err != nil {
			t.Fatalf("SendAndWait failed: %v", err)
		}
		if data, ok := reply.Data.(*copilot.AssistantMessageData); !ok || !strings.Contains(data.Content, "XENON") {
			t.Errorf("expected response to contain XENON, got %+v", reply.Data)
		}

		sys := getSystemMessageSentToLLM(t)
		if regexp.MustCompile(`(?i)current working directory:`).MatchString(sys) {
			t.Errorf("system message should not contain 'Current working directory:': %q", sys)
		}
		if regexp.MustCompile(`(?i)operating system:`).MatchString(sys) {
			t.Errorf("system message should not contain 'Operating System:': %q", sys)
		}
	})
}
