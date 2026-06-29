package e2e

import (
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func assertUnsupportedExtendedHooks(t *testing.T, client *copilot.Client, hooks *copilot.SessionHooks) {
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

func TestHooksExtendedE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	client := ctx.NewClient()
	t.Cleanup(func() { client.ForceStop() })

	cases := map[string]*copilot.SessionHooks{
		"userPromptSubmitted": {
			OnUserPromptSubmitted: func(copilot.UserPromptSubmittedHookInput, copilot.HookInvocation) (*copilot.UserPromptSubmittedHookOutput, error) {
				return &copilot.UserPromptSubmittedHookOutput{ModifiedPrompt: "not used"}, nil
			},
		},
		"sessionStart": {
			OnSessionStart: func(copilot.SessionStartHookInput, copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
				return &copilot.SessionStartHookOutput{AdditionalContext: "not used"}, nil
			},
		},
		"sessionEnd": {
			OnSessionEnd: func(copilot.SessionEndHookInput, copilot.HookInvocation) (*copilot.SessionEndHookOutput, error) {
				return &copilot.SessionEndHookOutput{SessionSummary: "not used"}, nil
			},
		},
		"errorOccurred": {
			OnErrorOccurred: func(copilot.ErrorOccurredHookInput, copilot.HookInvocation) (*copilot.ErrorOccurredHookOutput, error) {
				return &copilot.ErrorOccurredHookOutput{ErrorHandling: "skip"}, nil
			},
		},
		"preToolUse output": {
			OnPreToolUse: func(copilot.PreToolUseHookInput, copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
				return &copilot.PreToolUseHookOutput{PermissionDecision: "allow"}, nil
			},
		},
		"postToolUse output": {
			OnPostToolUse: func(copilot.PostToolUseHookInput, copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
				return &copilot.PostToolUseHookOutput{SuppressOutput: false}, nil
			},
		},
		"postToolUseFailure output": {
			OnPostToolUse: func(copilot.PostToolUseHookInput, copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
				return nil, nil
			},
			OnPostToolUseFailure: func(copilot.PostToolUseFailureHookInput, copilot.HookInvocation) (*copilot.PostToolUseFailureHookOutput, error) {
				return &copilot.PostToolUseFailureHookOutput{AdditionalContext: "not used"}, nil
			},
		},
	}

	for name, hooks := range cases {
		t.Run("rejects SDK callback hook "+name, func(t *testing.T) {
			ctx.ConfigureForTest(t)
			assertUnsupportedExtendedHooks(t, client, hooks)
		})
	}
}
