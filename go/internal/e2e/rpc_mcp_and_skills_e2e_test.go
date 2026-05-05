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
func TestRpcMcpAndSkillsE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)
	// --yolo auto-approves extension permission gates at the CLI level,
	// preventing breakage from new gates (e.g., extension-permission-access).
	client := ctx.NewClient(func(o *copilot.ClientOptions) {
		o.CLIArgs = []string{"--yolo"}
	})
	t.Cleanup(func() { client.ForceStop() })

	t.Run("should list and toggle session skills", func(t *testing.T) {
		skillName := fmt.Sprintf("session-rpc-skill-%s", randomHex(t))
		skillsDir := createMcpSkillsRpcDirectory(t, ctx.WorkDir, "session-rpc-skills", skillName, "Session skill controlled by RPC.")

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
			MCPServers: map[string]copilot.MCPServerConfig{
				serverName: copilot.MCPStdioServerConfig{
					Command: "echo",
					Args:    []string{"rpc-list-mcp-server"},
					Tools:   []string{"*"},
				},
			},
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		result, err := session.RPC.Mcp.List(t.Context())
		if err != nil {
			t.Fatalf("Mcp.List failed: %v", err)
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

	t.Run("should report error when mcp host is not initialized", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		assertRpcError(t, "Mcp.Enable", func() error {
			_, e := session.RPC.Mcp.Enable(t.Context(), &rpc.MCPEnableRequest{ServerName: "missing-server"})
			return e
		}, "no mcp host initialized")
		assertRpcError(t, "Mcp.Disable", func() error {
			_, e := session.RPC.Mcp.Disable(t.Context(), &rpc.MCPDisableRequest{ServerName: "missing-server"})
			return e
		}, "no mcp host initialized")
		assertRpcError(t, "Mcp.Reload", func() error {
			_, e := session.RPC.Mcp.Reload(t.Context())
			return e
		}, "mcp config reload not available")
	})

	t.Run("should report error when extensions are not available", func(t *testing.T) {
		session, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
			OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		})
		if err != nil {
			t.Fatalf("CreateSession failed: %v", err)
		}

		assertRpcError(t, "Extensions.Enable", func() error {
			_, e := session.RPC.Extensions.Enable(t.Context(), &rpc.ExtensionsEnableRequest{ID: "missing-extension"})
			return e
		}, "extensions not available")
		assertRpcError(t, "Extensions.Disable", func() error {
			_, e := session.RPC.Extensions.Disable(t.Context(), &rpc.ExtensionsDisableRequest{ID: "missing-extension"})
			return e
		}, "extensions not available")
		assertRpcError(t, "Extensions.Reload", func() error {
			_, e := session.RPC.Extensions.Reload(t.Context())
			return e
		}, "extensions not available")
	})
}

// createMcpSkillsRpcDirectory creates a unique skills directory containing a single
// SKILL.md and returns the parent directory suitable for SkillDirectories.
func createMcpSkillsRpcDirectory(t *testing.T, workDir, baseName, skillName, description string) string {
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

func assertRpcError(t *testing.T, name string, action func() error, expectedSubstring string) {
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
