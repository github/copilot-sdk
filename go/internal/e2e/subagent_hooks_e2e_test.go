package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestSubagentHooksE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	t.Run("rejects SDK callback hooks for sub-agent hook propagation", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			Hooks: &copilot.SessionHooks{
				OnPreToolUse: func(copilot.PreToolUseHookInput, copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
					return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
				},
				OnPostToolUse: func(copilot.PostToolUseHookInput, copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
					return nil, nil
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
