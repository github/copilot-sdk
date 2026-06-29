package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

const unsupportedSDKHooksMessage = "SDK hook callbacks are no longer supported"

func assertUnsupportedHooks(t *testing.T, client *copilot.Client, hooks *copilot.SessionHooks) {
	t.Helper()

	session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		Hooks:               hooks,
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
}

func TestHooksE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	cases := map[string]*copilot.SessionHooks{
		"preToolUse": {
			OnPreToolUse: func(copilot.PreToolUseHookInput, copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
		},
		"postToolUse": {
			OnPostToolUse: func(copilot.PostToolUseHookInput, copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
				return nil, nil
			},
		},
		"preToolUse denial": {
			OnPreToolUse: func(copilot.PreToolUseHookInput, copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				return &copilot.PreToolUseHookOutput{PermissionDecision: "deny"}, nil
			},
		},
		"combined preToolUse and postToolUse": {
			OnPreToolUse: func(copilot.PreToolUseHookInput, copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
			OnPostToolUse: func(copilot.PostToolUseHookInput, copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
				return nil, nil
			},
		},
	}

	for name, hooks := range cases {
		t.Run("rejects SDK callback hook "+name, func(t *testing.T) {
			ctx.ConfigureForTest(t)
			assertUnsupportedHooks(t, client, hooks)
		})
	}
}
