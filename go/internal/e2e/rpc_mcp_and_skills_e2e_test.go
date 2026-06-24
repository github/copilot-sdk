package e2e

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

// Mirrors dotnet/test/RpcMcpAndSkillsTests.cs (snapshot category "rpc_mcp_and_skills").
// Tests session-scoped MCP, skills, plugins, and extensions RPCs.
func TestRPCMCPAndSkillsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	// --yolo auto-approves extension permission gates at the CLI level,
	// preventing breakage from new gates (e.g., extension-permission-access).
	client := ctx.NewClient(func(o *copilot.ClientOptions) {
		stdio := o.Connection.(copilot.StdioConnection)
		stdio.Args = []string{"--yolo"}
		o.Connection = stdio
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should list and toggle session skills", func(t *testing.T) {
		skillName := fmt.Sprintf("session-rpc-skill-%s", randomHex(t))
		skillsDir := createMCPSkillsRPCDirectory(t, ctx.WorkDir, "session-rpc-skills", skillName, "Session skill controlled by RPC.")

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
			DisabledSkills:      []string{skillName},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		disabled, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (initial) failed: %v", err)
		}
		assertSkillState(t, disabled, skillName, false)

		if _, err := session.RPC.Skills.Enable(t.Context(), &rpc.SkillsEnableRequest{Name: skillName}); err != nil {
			t.Fatalf("Skills.Enable failed: %v", err)
		}
		enabled, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (after enable) failed: %v", err)
		}
		assertSkillState(t, enabled, skillName, true)

		if _, err := session.RPC.Skills.Disable(t.Context(), &rpc.SkillsDisableRequest{Name: skillName}); err != nil {
			t.Fatalf("Skills.Disable failed: %v", err)
		}
		disabledAgain, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (after disable) failed: %v", err)
		}
		assertSkillState(t, disabledAgain, skillName, false)
	})

	t.Run("should ensure skills are loaded and list invoked skills", func(t *testing.T) {
		skillName := fmt.Sprintf("ensure-rpc-skill-%s", randomHex(t))
		skillsDir := createMCPSkillsRPCDirectory(t, ctx.WorkDir, "session-rpc-skills", skillName, "Skill loaded explicitly by RPC.")

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		if _, err := session.RPC.Skills.EnsureLoaded(t.Context()); err != nil {
			t.Fatalf("Skills.EnsureLoaded failed: %v", err)
		}
		loaded, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List failed: %v", err)
		}
		skill := assertSkillState(t, loaded, skillName, true)
		if skill.Description != "Skill loaded explicitly by RPC." {
			t.Errorf("Expected description to match, got %q", skill.Description)
		}

		invoked, err := session.RPC.Skills.GetInvoked(t.Context())
		if err != nil {
			t.Fatalf("Skills.GetInvoked failed: %v", err)
		}
		if invoked.Skills == nil {
			t.Fatal("Expected non-nil invoked skills list")
		}
		if len(invoked.Skills) != 0 {
			t.Fatalf("Expected no invoked skills in fresh session, got %+v", invoked.Skills)
		}
	})

	t.Run("should reload session skills", func(t *testing.T) {
		skillsDir := filepath.Join(ctx.WorkDir, "reloadable-rpc-skills", randomHex(t))
		if err := os.MkdirAll(skillsDir, 0755); err != nil {
			t.Fatalf("Failed to create skills directory: %v", err)
		}
		skillName := fmt.Sprintf("reload-rpc-skill-%s", randomHex(t))

		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			SkillDirectories:    []string{skillsDir},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		before, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (before) failed: %v", err)
		}
		for _, skill := range before.Skills {
			if skill.Name == skillName {
				t.Fatalf("Did not expect %q to be present before creation", skillName)
			}
		}

		writeSkillFile(t, skillsDir, skillName, "Skill added after session creation.")

		if _, err := session.RPC.Skills.Reload(t.Context()); err != nil {
			t.Fatalf("Skills.Reload failed: %v", err)
		}

		after, err := session.RPC.Skills.List(t.Context())
		if err != nil {
			t.Fatalf("Skills.List (after) failed: %v", err)
		}
		reloaded := assertSkillState(t, after, skillName, true)
		if reloaded != nil && reloaded.Description != "Skill added after session creation." {
			t.Errorf("Expected description %q, got %q", "Skill added after session creation.", reloaded.Description)
		}
	})

	t.Run("should list mcp servers with configured server", func(t *testing.T) {
		const serverName = "rpc-list-mcp-server"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          testMCPServers(t, serverName),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)
		result, err := session.RPC.MCP.List(t.Context())
		if err != nil {
			t.Fatalf("MCP.List failed: %v", err)
		}
		var found bool
		for _, server := range result.Servers {
			if server.Name == serverName {
				found = true
				if string(server.Status) == "" {
					t.Errorf("Expected non-empty MCP server status, got empty")
				}
				break
			}
		}
		if !found {
			t.Errorf("Expected MCP server %q in result, got %+v", serverName, result.Servers)
		}
	})

	t.Run("should set mcp env value mode and remove github server", func(t *testing.T) {
		const serverName = "github"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          testMCPServers(t, serverName),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)
		direct, err := session.RPC.MCP.SetEnvValueMode(t.Context(), &rpc.MCPSetEnvValueModeParams{Mode: rpc.MCPSetEnvValueModeDetailsDirect})
		if err != nil {
			t.Fatalf("MCP.SetEnvValueMode(direct) failed: %v", err)
		}
		if direct.Mode != rpc.MCPSetEnvValueModeDetailsDirect {
			t.Fatalf("Expected direct env value mode, got %+v", direct)
		}
		indirect, err := session.RPC.MCP.SetEnvValueMode(t.Context(), &rpc.MCPSetEnvValueModeParams{Mode: rpc.MCPSetEnvValueModeDetailsIndirect})
		if err != nil {
			t.Fatalf("MCP.SetEnvValueMode(indirect) failed: %v", err)
		}
		if indirect.Mode != rpc.MCPSetEnvValueModeDetailsIndirect {
			t.Fatalf("Expected indirect env value mode, got %+v", indirect)
		}

		removeGitHub, err := session.RPC.MCP.RemoveGitHub(t.Context())
		if err != nil {
			t.Fatalf("MCP.RemoveGitHub failed: %v", err)
		}
		if removeGitHub.Removed {
			t.Fatalf("Expected RemoveGitHub=false for explicitly configured server, got %+v", removeGitHub)
		}
		servers, err := session.RPC.MCP.List(t.Context())
		if err != nil {
			t.Fatalf("MCP.List failed: %v", err)
		}
		var stillConnected bool
		for _, server := range servers.Servers {
			if server.Name == serverName && server.Status == rpc.MCPServerStatusConnected {
				stillConnected = true
				break
			}
		}
		if !stillConnected {
			t.Fatalf("Expected %q MCP server to remain connected after RemoveGitHub, got %+v", serverName, servers.Servers)
		}
	})

	t.Run("should report mcp sampling failure and cancel missing sampling", func(t *testing.T) {
		const serverName = "rpc-sampling-server"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          testMCPServers(t, serverName),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)

		cancelMissing, err := session.RPC.MCP.CancelSamplingExecution(t.Context(), &rpc.MCPCancelSamplingExecutionParams{RequestID: "missing-" + randomHex(t)})
		if err != nil {
			t.Fatalf("MCP.CancelSamplingExecution failed: %v", err)
		}
		if cancelMissing.Cancelled {
			t.Fatal("Expected cancelling missing sampling execution to report Cancelled=false")
		}

		result, err := session.RPC.MCP.ExecuteSampling(t.Context(), &rpc.MCPExecuteSamplingParams{
			RequestID:    "sampling-" + randomHex(t),
			ServerName:   "missing-sampling-server",
			MCPRequestID: "mcp-request-" + randomHex(t),
			Request:      rpc.MCPExecuteSamplingRequest{},
		})
		if err != nil {
			assertRPCError(t, "MCP.ExecuteSampling", func() error { return err }, "sampling")
			return
		}
		if result.Action != rpc.MCPSamplingExecutionActionFailure {
			t.Fatalf("Expected sampling failure action, got %+v", result)
		}
		if result.Result != nil || result.Error == nil || strings.TrimSpace(*result.Error) == "" {
			t.Fatalf("Expected failure error without result, got %+v", result)
		}
		if strings.Contains(strings.ToLower(*result.Error), "unhandled method") {
			t.Fatalf("Expected implemented sampling error, got %+v", result)
		}
	})

	t.Run("should list plugins", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		result, err := session.RPC.Plugins.List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.List failed: %v", err)
		}
		if result.Plugins == nil {
			t.Error("Expected non-nil Plugins list")
		}
		for i, plugin := range result.Plugins {
			if strings.TrimSpace(plugin.Name) == "" {
				t.Errorf("Plugin[%d] has empty Name", i)
			}
		}
	})

	t.Run("should list extensions", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		result, err := session.RPC.Extensions.List(t.Context())
		if err != nil {
			t.Fatalf("Extensions.List failed: %v", err)
		}
		if result.Extensions == nil {
			t.Error("Expected non-nil Extensions list")
		}
		for i, ext := range result.Extensions {
			if strings.TrimSpace(ext.ID) == "" {
				t.Errorf("Extension[%d] has empty ID", i)
			}
			if strings.TrimSpace(ext.Name) == "" {
				t.Errorf("Extension[%d] has empty Name", i)
			}
		}
	})

	t.Run("should round trip MCP app host context", func(t *testing.T) {
		mcpAppsClient := createMCPAppsClient(ctx)
		t.Cleanup(func() { mcpAppsClient.ForceStop() })
		session, err := mcpAppsClient.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		displayMode := rpc.MCPAppsSetHostContextDetailsDisplayModeInline
		platform := rpc.MCPAppsSetHostContextDetailsPlatformDesktop
		theme := rpc.MCPAppsSetHostContextDetailsThemeDark
		if _, err := session.RPC.MCP.Apps().SetHostContext(t.Context(), &rpc.MCPAppsSetHostContextRequest{
			Context: rpc.MCPAppsSetHostContextDetails{
				AvailableDisplayModes: []rpc.MCPAppsSetHostContextDetailsAvailableDisplayMode{
					rpc.MCPAppsSetHostContextDetailsAvailableDisplayModeInline,
					rpc.MCPAppsSetHostContextDetailsAvailableDisplayModeFullscreen,
				},
				DisplayMode: &displayMode,
				Locale:      rpcPtr("en-GB"),
				Platform:    &platform,
				Theme:       &theme,
				TimeZone:    rpcPtr("Etc/UTC"),
				UserAgent:   rpcPtr("go-sdk-e2e"),
			},
		}); err != nil {
			t.Fatalf("MCP.Apps.SetHostContext failed: %v", err)
		}

		result, err := session.RPC.MCP.Apps().GetHostContext(t.Context())
		if err != nil {
			t.Fatalf("MCP.Apps.GetHostContext failed: %v", err)
		}
		if result.Context.DisplayMode == nil || string(*result.Context.DisplayMode) != "inline" ||
			result.Context.Locale == nil || *result.Context.Locale != "en-GB" ||
			result.Context.Platform == nil || string(*result.Context.Platform) != "desktop" ||
			result.Context.Theme == nil || string(*result.Context.Theme) != "dark" ||
			result.Context.TimeZone == nil || *result.Context.TimeZone != "Etc/UTC" ||
			result.Context.UserAgent == nil || *result.Context.UserAgent != "go-sdk-e2e" {
			t.Fatalf("Unexpected MCP app host context: %+v", result.Context)
		}
		if len(result.Context.AvailableDisplayModes) != 2 {
			t.Fatalf("Expected two available display modes, got %+v", result.Context.AvailableDisplayModes)
		}
	})

	t.Run("should diagnose and report mcp app capability errors", func(t *testing.T) {
		const serverName = "rpc-apps-server"
		const otherServerName = "rpc-apps-other-server"
		servers := testMCPServers(t, serverName, otherServerName)
		if stdio, ok := servers[serverName].(copilot.MCPStdioServerConfig); ok {
			stdio.Env = map[string]string{"MCP_APP_RPC_VALUE": "from-app-rpc"}
			servers[serverName] = stdio
		}

		mcpAppsClient := createMCPAppsClient(ctx)
		t.Cleanup(func() { mcpAppsClient.ForceStop() })
		session, err := mcpAppsClient.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          servers,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)
		waitForMCPServerStatus(t, session, otherServerName, rpc.MCPServerStatusConnected)

		diagnose, err := session.RPC.MCP.Apps().Diagnose(t.Context(), &rpc.MCPAppsDiagnoseRequest{ServerName: serverName})
		if err != nil {
			t.Fatalf("MCP.Apps.Diagnose failed: %v", err)
		}
		if !diagnose.Server.Connected || diagnose.Server.ToolCount < 1 {
			t.Fatalf("Expected connected MCP app diagnose result with tools, got %+v", diagnose)
		}

		assertMCPAppsResultOrImplementedError(t, "MCP.Apps.ListTools(self)", func() (any, error) {
			return session.RPC.MCP.Apps().ListTools(t.Context(), &rpc.MCPAppsListToolsRequest{
				ServerName:       serverName,
				OriginServerName: serverName,
			})
		})
		assertMCPAppsResultOrImplementedError(t, "MCP.Apps.ListTools(other)", func() (any, error) {
			return session.RPC.MCP.Apps().ListTools(t.Context(), &rpc.MCPAppsListToolsRequest{
				ServerName:       serverName,
				OriginServerName: otherServerName,
			})
		})
		assertMCPAppsResultOrImplementedError(t, "MCP.Apps.CallTool", func() (any, error) {
			return session.RPC.MCP.Apps().CallTool(t.Context(), &rpc.MCPAppsCallToolRequest{
				ServerName:       serverName,
				OriginServerName: serverName,
				ToolName:         "get_env",
				Arguments:        map[string]any{"name": "MCP_APP_RPC_VALUE"},
			})
		})
	})

	t.Run("should report error when mcp app resource is not available", func(t *testing.T) {
		const serverName = "rpc-apps-resource-server"
		mcpAppsClient := createMCPAppsClient(ctx)
		t.Cleanup(func() { mcpAppsClient.ForceStop() })
		session, err := mcpAppsClient.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          testMCPServers(t, serverName),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)

		_, err = session.RPC.MCP.Apps().ReadResource(t.Context(), &rpc.MCPAppsReadResourceRequest{
			ServerName: serverName,
			URI:        "ui://missing-resource",
		})
		if err == nil {
			t.Fatal("Expected missing MCP app resource to fail")
		}
		text := strings.ToLower(err.Error())
		if strings.Contains(text, "unhandled method") ||
			(!strings.Contains(text, "resource") && !strings.Contains(text, "not found") && !strings.Contains(text, "method not found")) {
			t.Fatalf("Expected implemented missing-resource error, got %v", err)
		}
	})

	t.Run("should report error when mcp host is not initialized", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		assertRPCError(t, "MCP.Enable", func() error {
			_, e := session.RPC.MCP.Enable(t.Context(), &rpc.MCPEnableRequest{ServerName: "missing-server"})
			return e
		}, "no mcp host initialized")
		assertRPCError(t, "MCP.Disable", func() error {
			_, e := session.RPC.MCP.Disable(t.Context(), &rpc.MCPDisableRequest{ServerName: "missing-server"})
			return e
		}, "no mcp host initialized")
		assertRPCError(t, "MCP.Reload", func() error {
			_, e := session.RPC.MCP.Reload(t.Context())
			return e
		}, "mcp config reload not available")
		assertRPCError(t, "MCP.Oauth.Login", func() error {
			_, e := session.RPC.MCP.Oauth().Login(t.Context(), &rpc.MCPOauthLoginRequest{ServerName: "missing-server"})
			return e
		}, "mcp host is not available")
	})

	t.Run("should report error when mcp oauth server is not configured", func(t *testing.T) {
		const serverName = "configured-stdio-server"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          testMCPServers(t, serverName),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)

		assertRPCError(t, "MCP.Oauth.Login", func() error {
			_, e := session.RPC.MCP.Oauth().Login(t.Context(), &rpc.MCPOauthLoginRequest{ServerName: "missing-server"})
			return e
		}, "is not configured")
	})

	t.Run("should report error when mcp oauth server is not remote", func(t *testing.T) {
		const serverName = "configured-stdio-server"
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			MCPServers:          testMCPServers(t, serverName),
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}
		waitForMCPServerStatus(t, session, serverName, rpc.MCPServerStatusConnected)

		force := true
		clientName := "SDK E2E"
		callback := "Done"
		assertRPCError(t, "MCP.Oauth.Login", func() error {
			_, e := session.RPC.MCP.Oauth().Login(t.Context(), &rpc.MCPOauthLoginRequest{
				ServerName:             serverName,
				ForceReauth:            &force,
				ClientName:             &clientName,
				CallbackSuccessMessage: &callback,
			})
			return e
		}, "not a remote server")
	})

	t.Run("should report error when extensions are not available", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		assertRPCError(t, "Extensions.Enable", func() error {
			_, e := session.RPC.Extensions.Enable(t.Context(), &rpc.ExtensionsEnableRequest{ID: "missing-extension"})
			return e
		}, "extensions not available")
		assertRPCError(t, "Extensions.Disable", func() error {
			_, e := session.RPC.Extensions.Disable(t.Context(), &rpc.ExtensionsDisableRequest{ID: "missing-extension"})
			return e
		}, "extensions not available")
		assertRPCError(t, "Extensions.Reload", func() error {
			_, e := session.RPC.Extensions.Reload(t.Context())
			return e
		}, "extensions not available")
	})
}

// createMCPSkillsRPCDirectory creates a unique skills directory containing a single
// SKILL.md and returns the parent directory suitable for SkillDirectories.
func createMCPSkillsRPCDirectory(t *testing.T, workDir, baseName, skillName, description string) string {
	t.Helper()
	skillsDir := filepath.Join(workDir, baseName, randomHex(t))
	if err := os.MkdirAll(skillsDir, 0755); err != nil {
		t.Fatalf("Failed to create skills directory: %v", err)
	}
	writeSkillFile(t, skillsDir, skillName, description)
	return skillsDir
}

func writeSkillFile(t *testing.T, skillsDir, skillName, description string) {
	t.Helper()
	skillSubdir := filepath.Join(skillsDir, skillName)
	if err := os.MkdirAll(skillSubdir, 0755); err != nil {
		t.Fatalf("Failed to create skill subdirectory: %v", err)
	}
	content := fmt.Sprintf("---\nname: %s\ndescription: %s\n---\n\n# %s\n\nThis skill is used by RPC E2E tests.\n", skillName, description, skillName)
	if err := os.WriteFile(filepath.Join(skillSubdir, "SKILL.md"), []byte(content), 0644); err != nil {
		t.Fatalf("Failed to write SKILL.md: %v", err)
	}
}

// assertSkillState finds a skill by name in the list and asserts it has the
// expected enabled state, returning the matched skill (or nil if not found).
func assertSkillState(t *testing.T, list *rpc.SkillList, name string, enabled bool) *rpc.Skill {
	t.Helper()
	var matched *rpc.Skill
	count := 0
	for i, skill := range list.Skills {
		if skill.Name == name {
			count++
			matched = &list.Skills[i]
		}
	}
	if count != 1 {
		t.Fatalf("Expected exactly 1 skill named %q, found %d", name, count)
	}
	if matched.Enabled != enabled {
		t.Errorf("Expected skill %q Enabled=%t, got %t", name, enabled, matched.Enabled)
	}
	if matched.Path == nil || !strings.HasSuffix(strings.ReplaceAll(*matched.Path, "\\", "/"), strings.Join([]string{name, "SKILL.md"}, "/")) {
		t.Errorf("Expected skill path to end with %s/SKILL.md, got %v", name, matched.Path)
	}
	return matched
}

func createMCPAppsClient(ctx *testharness.TestContext) *copilot.Client {
	return ctx.NewClient(func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env, "COPILOT_MCP_APPS=true", "MCP_APPS=true")
	})
}

func assertMCPAppsResultOrImplementedError(t *testing.T, name string, action func() (any, error)) {
	t.Helper()
	result, err := action()
	if err == nil {
		if result == nil {
			t.Fatalf("%s returned nil result", name)
		}
		switch value := result.(type) {
		case *rpc.MCPAppsListToolsResult:
			if value.Tools == nil {
				t.Fatalf("%s returned nil Tools", name)
			}
		case *rpc.SessionMCPAppsCallToolResult:
			if value == nil {
				t.Fatalf("%s returned nil CallTool result", name)
			}
		}
		return
	}

	text := strings.ToLower(err.Error())
	if strings.Contains(text, "unhandled method") ||
		(!strings.Contains(text, "mcp-apps") && !strings.Contains(text, "capability") && !strings.Contains(text, "visibility")) {
		t.Fatalf("Expected %s to return an implemented MCP apps error, got %v", name, err)
	}
}

func assertRPCError(t *testing.T, name string, action func() error, expectedSubstring string) {
	t.Helper()
	err := action()
	if err == nil {
		t.Errorf("Expected %s to fail with error containing %q, got nil", name, expectedSubstring)
		return
	}
	if !strings.Contains(strings.ToLower(err.Error()), strings.ToLower(expectedSubstring)) {
		t.Errorf("Expected %s error to contain %q, got %v", name, expectedSubstring, err)
	}
}
