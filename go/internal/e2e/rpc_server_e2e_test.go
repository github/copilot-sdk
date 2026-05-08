package e2e

import (
	"fmt"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcServerTests.cs (snapshot category "rpc_server").
// Tests server-scoped (non-session) RPCs.
func TestRpcServerE2E(t *testing.T) {
	t.Run("should call rpc ping with typed params and result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		message := "typed rpc test"
		result, err := client.RPC.Ping(t.Context(), &rpc.PingRequest{Message: &message})
		if err != nil {
			t.Fatalf("RPC.Ping failed: %v", err)
		}
		if !strings.Contains(result.Message, "typed rpc test") {
			t.Errorf("Expected ping response to contain 'typed rpc test', got %q", result.Message)
		}
		if result.Timestamp < 0 {
			t.Errorf("Expected non-negative Timestamp, got %d", result.Timestamp)
		}
	})

	t.Run("should call rpc models list with typed result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		const token = "rpc-models-token"
		registerProxyUser(t, ctx, token, "rpc-user", nil)
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		result, err := client.RPC.Models.List(t.Context(), &rpc.ModelsListRequest{})
		if err != nil {
			t.Fatalf("Models.List failed: %v", err)
		}
		if result.Models == nil {
			t.Fatal("Expected non-nil Models list")
		}
		var hasClaude bool
		for _, model := range result.Models {
			if strings.TrimSpace(model.Name) == "" {
				t.Errorf("Model %q has empty Name", model.ID)
			}
			if model.ID == "claude-sonnet-4.5" {
				hasClaude = true
			}
		}
		if !hasClaude {
			t.Errorf("Expected models list to contain 'claude-sonnet-4.5'")
		}
	})

	t.Run("should call rpc account get quota when authenticated", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		const token = "rpc-quota-token"
		registerProxyUser(t, ctx, token, "rpc-user", map[string]any{
			"chat": map[string]any{
				"entitlement":       100,
				"overage_count":     2,
				"overage_permitted": true,
				"percent_remaining": 75,
				"timestamp_utc":     "2026-04-30T00:00:00Z",
			},
		})
		client := newAuthenticatedClient(ctx, token)
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		tokenCopy := token
		result, err := client.RPC.Account.GetQuota(t.Context(), &rpc.AccountGetQuotaRequest{GitHubToken: &tokenCopy})
		if err != nil {
			t.Fatalf("Account.GetQuota failed: %v", err)
		}
		chat, present := result.QuotaSnapshots["chat"]
		if !present {
			t.Fatalf("Expected 'chat' quota in snapshots, got %+v", result.QuotaSnapshots)
		}
		if chat.EntitlementRequests != 100 {
			t.Errorf("Expected EntitlementRequests=100, got %d", chat.EntitlementRequests)
		}
		if chat.UsedRequests != 25 {
			t.Errorf("Expected UsedRequests=25, got %d", chat.UsedRequests)
		}
		if chat.RemainingPercentage != 75 {
			t.Errorf("Expected RemainingPercentage=75, got %v", chat.RemainingPercentage)
		}
		if chat.Overage != 2 {
			t.Errorf("Expected Overage=2, got %v", chat.Overage)
		}
		if !chat.UsageAllowedWithExhaustedQuota {
			t.Errorf("Expected UsageAllowedWithExhaustedQuota=true")
		}
		if !chat.OverageAllowedWithExhaustedQuota {
			t.Errorf("Expected OverageAllowedWithExhaustedQuota=true")
		}
		if chat.ResetDate == nil || *chat.ResetDate != "2026-04-30T00:00:00Z" {
			t.Errorf("Expected ResetDate='2026-04-30T00:00:00Z', got %v", chat.ResetDate)
		}
	})

	t.Run("should call rpc tools list with typed result", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		result, err := client.RPC.Tools.List(t.Context(), &rpc.ToolsListRequest{})
		if err != nil {
			t.Fatalf("Tools.List failed: %v", err)
		}
		if len(result.Tools) == 0 {
			t.Fatal("Expected non-empty Tools list")
		}
		for i, tool := range result.Tools {
			if strings.TrimSpace(tool.Name) == "" {
				t.Errorf("Tool[%d] has empty Name", i)
			}
		}
	})

	t.Run("should discover server mcp and skills", func(t *testing.T) {
		ctx := testharness.NewTestContext(t)
		ctx.ConfigureForTest(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Start failed: %v", err)
		}

		skillName := fmt.Sprintf("server-rpc-skill-%s", randomHex(t))
		skillsDir := createMcpSkillsRpcDirectory(t, ctx.WorkDir, "server-rpc-skills", skillName, "Skill discovered by server-scoped RPC tests.")

		workingDir := ctx.WorkDir
		mcp, err := client.RPC.Mcp.Discover(t.Context(), &rpc.McpDiscoverRequest{WorkingDirectory: &workingDir})
		if err != nil {
			t.Fatalf("Mcp.Discover failed: %v", err)
		}
		if mcp.Servers == nil {
			t.Errorf("Expected non-nil Servers")
		}

		skills, err := client.RPC.Skills.Discover(t.Context(), &rpc.SkillsDiscoverRequest{SkillDirectories: []string{skillsDir}})
		if err != nil {
			t.Fatalf("Skills.Discover failed: %v", err)
		}
		discovered := findServerSkill(skills.Skills, skillName)
		if discovered == nil {
			t.Fatalf("Expected to discover skill %q", skillName)
		}
		if discovered.Description != "Skill discovered by server-scoped RPC tests." {
			t.Errorf("Expected description to match, got %q", discovered.Description)
		}
		if !discovered.Enabled {
			t.Errorf("Expected discovered skill to be Enabled")
		}
		expectedSuffix := filepath.Join(skillName, "SKILL.md")
		if discovered.Path == nil || !strings.HasSuffix(filepath.ToSlash(*discovered.Path), filepath.ToSlash(expectedSuffix)) {
			t.Errorf("Expected skill path to end with %q, got %v", expectedSuffix, discovered.Path)
		}

		// Disable the skill globally and re-discover.
		if _, err := client.RPC.Skills.Config().SetDisabledSkills(t.Context(), &rpc.SkillsConfigSetDisabledSkillsRequest{
			DisabledSkills: []string{skillName},
		}); err != nil {
			t.Fatalf("Skills.Config.SetDisabledSkills failed: %v", err)
		}
		t.Cleanup(func() {
			_, _ = client.RPC.Skills.Config().SetDisabledSkills(t.Context(), &rpc.SkillsConfigSetDisabledSkillsRequest{
				DisabledSkills: []string{},
			})
		})

		disabled, err := client.RPC.Skills.Discover(t.Context(), &rpc.SkillsDiscoverRequest{SkillDirectories: []string{skillsDir}})
		if err != nil {
			t.Fatalf("Skills.Discover (after disable) failed: %v", err)
		}
		disabledSkill := findServerSkill(disabled.Skills, skillName)
		if disabledSkill == nil {
			t.Fatalf("Expected to find skill %q after disable", skillName)
		}
		if disabledSkill.Enabled {
			t.Errorf("Expected skill %q to be Enabled=false after global disable", skillName)
		}
	})
}

// newAuthenticatedClient builds a client that resolves auth through the test proxy.
func newAuthenticatedClient(ctx *testharness.TestContext, token string) *copilot.Client {
	return ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env, "COPILOT_DEBUG_GITHUB_API_URL="+ctx.ProxyURL)
		opts.GitHubToken = token
	})
}

// registerProxyUser configures the proxy with a fake CopilotUser response for the given token.
func registerProxyUser(t *testing.T, ctx *testharness.TestContext, token, login string, quotaSnapshots map[string]any) {
	t.Helper()
	user := map[string]any{
		"login":                 login,
		"copilot_plan":          "individual_pro",
		"endpoints":             map[string]any{"api": ctx.ProxyURL, "telemetry": "https://localhost:1/telemetry"},
		"analytics_tracking_id": login + "-tracking-id",
	}
	if quotaSnapshots != nil {
		user["quota_snapshots"] = quotaSnapshots
	}
	if err := ctx.SetCopilotUserByToken(token, user); err != nil {
		t.Fatalf("SetCopilotUserByToken failed: %v", err)
	}
}

func findServerSkill(skills []rpc.ServerSkill, name string) *rpc.ServerSkill {
	for i, skill := range skills {
		if skill.Name == name {
			return &skills[i]
		}
	}
	return nil
}
