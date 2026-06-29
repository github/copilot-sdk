package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestPreMCPToolCallHookE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("rejects SDK preMcpToolCall callback hooks", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnPreMCPToolCall: func(copilot.PreMCPToolCallHookInput, copilot.HookInvocation) (*copilot.PreMCPToolCallHookOutput, error) {
					return &copilot.PreMCPToolCallHookOutput{MetaToUse: map[string]any{"injected": "by-hook"}}, nil
				},
			},
		})
		if err == nil {
			if session != nil {
				_ = session.Disconnect()
			}
			t.Fatal("expected SDK callback hooks to be rejected")
		}
		if !strings.Contains(err.Error(), unsupportedSDKHooksMessage) {
			t.Fatalf("expected unsupported hooks error, got %v", err)
		}
	})
}
