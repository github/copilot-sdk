package e2e

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

const (
	portedMarketplaceName  = "go-e2e-marketplace"
	portedPluginName       = "go-e2e-plugin"
	portedDirectPluginName = "go-e2e-direct"
)

func TestRpcServerPlugins(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	t.Run("should_install_list_and_uninstall_plugin_from_local_marketplace", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		marketplaceDir := createPortedLocalMarketplaceFixture(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		if _, err := client.RPC.Plugins.Marketplaces().Add(t.Context(), &rpc.PluginsMarketplacesAddRequest{Source: marketplaceDir}); err != nil {
			t.Fatalf("Plugins.Marketplaces.Add failed: %v", err)
		}

		spec := portedPluginName + "@" + portedMarketplaceName
		install, err := client.RPC.Plugins.Install(t.Context(), &rpc.PluginsInstallRequest{Source: spec})
		if err != nil {
			t.Fatalf("Plugins.Install failed: %v", err)
		}
		if install.Plugin.Name != portedPluginName {
			t.Fatalf("Expected installed plugin name %q, got %q", portedPluginName, install.Plugin.Name)
		}
		if install.Plugin.Marketplace != portedMarketplaceName {
			t.Fatalf("Expected marketplace %q, got %q", portedMarketplaceName, install.Plugin.Marketplace)
		}
		if !install.Plugin.Enabled {
			t.Fatal("Expected installed marketplace plugin to be enabled")
		}
		if install.SkillsInstalled < 1 {
			t.Fatalf("Expected at least one skill, got %d", install.SkillsInstalled)
		}
		if install.DeprecationWarning != nil {
			t.Fatalf("Marketplace install should not return deprecation warning, got %q", *install.DeprecationWarning)
		}

		afterInstall, err := client.RPC.Plugins.List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.List after install failed: %v", err)
		}
		listed := findPortedInstalledPlugin(afterInstall.Plugins, portedPluginName, portedMarketplaceName)
		if listed == nil {
			t.Fatalf("Expected installed plugin %q in marketplace %q", portedPluginName, portedMarketplaceName)
		}
		if !listed.Enabled {
			t.Fatal("Expected listed marketplace plugin to be enabled")
		}

		if _, err := client.RPC.Plugins.Uninstall(t.Context(), &rpc.PluginsUninstallRequest{Name: spec}); err != nil {
			t.Fatalf("Plugins.Uninstall failed: %v", err)
		}

		afterUninstall, err := client.RPC.Plugins.List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.List after uninstall failed: %v", err)
		}
		if findPortedInstalledPlugin(afterUninstall.Plugins, portedPluginName, portedMarketplaceName) != nil {
			t.Fatalf("Expected plugin %q to be removed", spec)
		}
	})

	t.Run("should_enable_and_disable_marketplace_plugin", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		marketplaceDir := createPortedLocalMarketplaceFixture(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		spec := portedPluginName + "@" + portedMarketplaceName
		if _, err := client.RPC.Plugins.Marketplaces().Add(t.Context(), &rpc.PluginsMarketplacesAddRequest{Source: marketplaceDir}); err != nil {
			t.Fatalf("Plugins.Marketplaces.Add failed: %v", err)
		}
		if _, err := client.RPC.Plugins.Install(t.Context(), &rpc.PluginsInstallRequest{Source: spec}); err != nil {
			t.Fatalf("Plugins.Install failed: %v", err)
		}

		if _, err := client.RPC.Plugins.Disable(t.Context(), &rpc.PluginsDisableRequest{Names: []string{spec}}); err != nil {
			t.Fatalf("Plugins.Disable failed: %v", err)
		}
		if plugin := getPortedInstalledPlugin(t, client, portedPluginName, portedMarketplaceName); plugin.Enabled {
			t.Fatal("Expected plugin to be disabled")
		}

		if _, err := client.RPC.Plugins.Enable(t.Context(), &rpc.PluginsEnableRequest{Names: []string{spec}}); err != nil {
			t.Fatalf("Plugins.Enable failed: %v", err)
		}
		if plugin := getPortedInstalledPlugin(t, client, portedPluginName, portedMarketplaceName); !plugin.Enabled {
			t.Fatal("Expected plugin to be enabled")
		}
	})

	t.Run("should_update_single_marketplace_plugin", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		marketplaceDir := createPortedLocalMarketplaceFixture(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		spec := portedPluginName + "@" + portedMarketplaceName
		if _, err := client.RPC.Plugins.Marketplaces().Add(t.Context(), &rpc.PluginsMarketplacesAddRequest{Source: marketplaceDir}); err != nil {
			t.Fatalf("Plugins.Marketplaces.Add failed: %v", err)
		}
		if _, err := client.RPC.Plugins.Install(t.Context(), &rpc.PluginsInstallRequest{Source: spec}); err != nil {
			t.Fatalf("Plugins.Install failed: %v", err)
		}

		update, err := client.RPC.Plugins.Update(t.Context(), &rpc.PluginsUpdateRequest{Name: spec})
		if err != nil {
			t.Fatalf("Plugins.Update failed: %v", err)
		}
		if update.SkillsInstalled < 1 {
			t.Fatalf("Expected at least one skill, got %d", update.SkillsInstalled)
		}
		if update.PreviousVersion == nil || *update.PreviousVersion != "1.0.0" {
			t.Fatalf("Expected previous version 1.0.0, got %v", update.PreviousVersion)
		}
		if update.NewVersion == nil || *update.NewVersion != "1.0.0" {
			t.Fatalf("Expected new version 1.0.0, got %v", update.NewVersion)
		}
	})

	t.Run("should_update_all_installed_plugins", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		marketplaceDir := createPortedLocalMarketplaceFixture(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		spec := portedPluginName + "@" + portedMarketplaceName
		if _, err := client.RPC.Plugins.Marketplaces().Add(t.Context(), &rpc.PluginsMarketplacesAddRequest{Source: marketplaceDir}); err != nil {
			t.Fatalf("Plugins.Marketplaces.Add failed: %v", err)
		}
		if _, err := client.RPC.Plugins.Install(t.Context(), &rpc.PluginsInstallRequest{Source: spec}); err != nil {
			t.Fatalf("Plugins.Install failed: %v", err)
		}

		result, err := client.RPC.Plugins.UpdateAll(t.Context())
		if err != nil {
			t.Fatalf("Plugins.UpdateAll failed: %v", err)
		}
		var matches []rpc.PluginUpdateAllEntry
		for _, entry := range result.Results {
			if entry.Name == portedPluginName && entry.Marketplace == portedMarketplaceName {
				matches = append(matches, entry)
			}
		}
		if len(matches) != 1 {
			t.Fatalf("Expected exactly one update result for %q, got %d in %+v", spec, len(matches), result.Results)
		}
		entry := matches[0]
		if !entry.Success {
			t.Fatalf("Expected update all entry to succeed, got error %v", entry.Error)
		}
		if entry.SkillsInstalled == nil || *entry.SkillsInstalled < 1 {
			t.Fatalf("Expected at least one skill installed, got %v", entry.SkillsInstalled)
		}
	})

	t.Run("should_install_direct_local_plugin_with_deprecation_warning", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		pluginDir := createPortedDirectPluginFixture(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		install, err := client.RPC.Plugins.Install(t.Context(), &rpc.PluginsInstallRequest{Source: pluginDir})
		if err != nil {
			t.Fatalf("Plugins.Install direct failed: %v", err)
		}
		if install.Plugin.Name != portedDirectPluginName {
			t.Fatalf("Expected installed plugin name %q, got %q", portedDirectPluginName, install.Plugin.Name)
		}
		if install.Plugin.Marketplace != "" {
			t.Fatalf("Expected direct plugin marketplace to be empty, got %q", install.Plugin.Marketplace)
		}
		if install.DeprecationWarning == nil || !strings.Contains(strings.ToLower(*install.DeprecationWarning), "deprecated") {
			t.Fatalf("Expected deprecation warning containing deprecated, got %v", install.DeprecationWarning)
		}
		if install.SkillsInstalled < 1 {
			t.Fatalf("Expected at least one skill, got %d", install.SkillsInstalled)
		}

		afterInstall, err := client.RPC.Plugins.List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.List after direct install failed: %v", err)
		}
		if countPortedInstalledPluginByName(afterInstall.Plugins, portedDirectPluginName) != 1 {
			t.Fatalf("Expected exactly one direct plugin named %q, got %+v", portedDirectPluginName, afterInstall.Plugins)
		}

		if _, err := client.RPC.Plugins.Uninstall(t.Context(), &rpc.PluginsUninstallRequest{Name: portedDirectPluginName}); err != nil {
			t.Fatalf("Plugins.Uninstall direct failed: %v", err)
		}
		afterUninstall, err := client.RPC.Plugins.List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.List after direct uninstall failed: %v", err)
		}
		if countPortedInstalledPluginByName(afterUninstall.Plugins, portedDirectPluginName) != 0 {
			t.Fatalf("Expected direct plugin %q to be removed, got %+v", portedDirectPluginName, afterUninstall.Plugins)
		}
	})

	t.Run("should_list_browse_refresh_and_remove_local_marketplace", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		marketplaceDir := createPortedLocalMarketplaceFixture(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		add, err := client.RPC.Plugins.Marketplaces().Add(t.Context(), &rpc.PluginsMarketplacesAddRequest{Source: marketplaceDir})
		if err != nil {
			t.Fatalf("Plugins.Marketplaces.Add failed: %v", err)
		}
		if add.Name != portedMarketplaceName {
			t.Fatalf("Expected marketplace name %q, got %q", portedMarketplaceName, add.Name)
		}

		list, err := client.RPC.Plugins.Marketplaces().List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.Marketplaces.List failed: %v", err)
		}
		mine := findPortedMarketplace(list.Marketplaces, portedMarketplaceName)
		if mine == nil {
			t.Fatalf("Expected marketplace %q in list %+v", portedMarketplaceName, list.Marketplaces)
		}
		if mine.IsDefault != nil && *mine.IsDefault {
			t.Fatal("Expected local marketplace not to be marked default")
		}
		if !containsPortedDefaultMarketplace(list.Marketplaces) {
			t.Fatalf("Expected built-in default marketplace in %+v", list.Marketplaces)
		}

		browse, err := client.RPC.Plugins.Marketplaces().Browse(t.Context(), &rpc.PluginsMarketplacesBrowseRequest{Name: portedMarketplaceName})
		if err != nil {
			t.Fatalf("Plugins.Marketplaces.Browse failed: %v", err)
		}
		var advertised []rpc.MarketplacePluginInfo
		for _, plugin := range browse.Plugins {
			if plugin.Name == portedPluginName {
				advertised = append(advertised, plugin)
			}
		}
		if len(advertised) != 1 {
			t.Fatalf("Expected one advertised plugin %q, got %+v", portedPluginName, browse.Plugins)
		}
		if advertised[0].Description == nil || strings.TrimSpace(*advertised[0].Description) == "" {
			t.Fatalf("Expected advertised plugin description, got %+v", advertised[0])
		}

		refreshName := portedMarketplaceName
		refresh, err := client.RPC.Plugins.Marketplaces().Refresh(t.Context(), &rpc.PluginsMarketplacesRefreshRequest{Name: &refreshName})
		if err != nil {
			t.Fatalf("Plugins.Marketplaces.Refresh failed: %v", err)
		}
		var refreshMatches []rpc.MarketplaceRefreshEntry
		for _, entry := range refresh.Results {
			if entry.Name == portedMarketplaceName {
				refreshMatches = append(refreshMatches, entry)
			}
		}
		if len(refreshMatches) != 1 {
			t.Fatalf("Expected one refresh result for %q, got %+v", portedMarketplaceName, refresh.Results)
		}
		if !refreshMatches[0].Success {
			t.Fatalf("Expected refresh success, got error %v", refreshMatches[0].Error)
		}

		remove, err := client.RPC.Plugins.Marketplaces().Remove(t.Context(), &rpc.PluginsMarketplacesRemoveRequest{Name: portedMarketplaceName})
		if err != nil {
			t.Fatalf("Plugins.Marketplaces.Remove failed: %v", err)
		}
		if !remove.Removed {
			t.Fatalf("Expected marketplace removal, got %+v", remove)
		}

		afterRemove, err := client.RPC.Plugins.Marketplaces().List(t.Context())
		if err != nil {
			t.Fatalf("Plugins.Marketplaces.List after remove failed: %v", err)
		}
		if findPortedMarketplace(afterRemove.Marketplaces, portedMarketplaceName) != nil {
			t.Fatalf("Expected marketplace %q to be removed, got %+v", portedMarketplaceName, afterRemove.Marketplaces)
		}
	})

	t.Run("should_reload_mcp_config_cache", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedIsolatedPortedClient(t, ctx)
		defer client.ForceStop()

		if _, err := client.RPC.MCP.Config().Reload(t.Context()); err != nil {
			t.Fatalf("MCP.Config.Reload failed: %v", err)
		}
	})
}

func newStartedPortedClient(t *testing.T, ctx *testharness.TestContext, opts ...func(*copilot.ClientOptions)) *copilot.Client {
	t.Helper()
	client := ctx.NewClient(opts...)
	if err := client.Start(t.Context()); err != nil {
		t.Fatalf("Start failed: %v", err)
	}
	return client
}

func newStartedIsolatedPortedClient(t *testing.T, ctx *testharness.TestContext) *copilot.Client {
	t.Helper()
	home := t.TempDir()
	return newStartedPortedClient(t, ctx, func(opts *copilot.ClientOptions) {
		opts.Env = append(opts.Env,
			"COPILOT_HOME="+home,
			"GH_CONFIG_DIR="+home,
			"XDG_CONFIG_HOME="+home,
			"XDG_STATE_HOME="+home,
		)
	})
}

func createPortedSession(t *testing.T, client *copilot.Client, config *copilot.SessionConfig) *copilot.Session {
	t.Helper()
	if config == nil {
		config = &copilot.SessionConfig{}
	}
	if config.OnPermissionRequest == nil {
		config.OnPermissionRequest = copilot.PermissionHandler.ApproveAll
	}
	session, err := client.CreateSession(t.Context(), config)
	if err != nil {
		t.Fatalf("CreateSession failed: %v", err)
	}
	return session
}

func assertPortedNoUnhandledMethod(t *testing.T, message string) {
	t.Helper()
	if strings.Contains(strings.ToLower(message), "unhandled method") {
		t.Fatalf("Expected RPC to reach runtime, got %s", message)
	}
}

func assertPortedContainsFold(t *testing.T, message string, fragments ...string) {
	t.Helper()
	lower := strings.ToLower(message)
	for _, fragment := range fragments {
		if strings.Contains(lower, strings.ToLower(fragment)) {
			return
		}
	}
	t.Fatalf("Expected %q to contain one of %v", message, fragments)
}

func createPortedLocalMarketplaceFixture(t *testing.T) string {
	t.Helper()
	dir := t.TempDir()
	manifest := `{
  "name": "` + portedMarketplaceName + `",
  "owner": { "name": "Copilot SDK E2E" },
  "metadata": { "description": "Local marketplace fixture for SDK E2E tests." },
  "plugins": [
    {
      "name": "` + portedPluginName + `",
      "source": "./` + portedPluginName + `",
      "description": "E2E demo plugin advertised by the local marketplace.",
      "version": "1.0.0"
    }
  ]
}`
	if err := os.WriteFile(filepath.Join(dir, "marketplace.json"), []byte(manifest), 0644); err != nil {
		t.Fatalf("Failed to write marketplace manifest: %v", err)
	}
	pluginDir := filepath.Join(dir, portedPluginName)
	if err := os.MkdirAll(pluginDir, 0755); err != nil {
		t.Fatalf("Failed to create marketplace plugin directory: %v", err)
	}
	writePortedSkillFile(t, pluginDir)
	return dir
}

func createPortedDirectPluginFixture(t *testing.T) string {
	t.Helper()
	dir := t.TempDir()
	manifest := `{
  "name": "` + portedDirectPluginName + `",
  "description": "E2E demo plugin installed directly from a local path.",
  "version": "1.0.0"
}`
	if err := os.WriteFile(filepath.Join(dir, "plugin.json"), []byte(manifest), 0644); err != nil {
		t.Fatalf("Failed to write direct plugin manifest: %v", err)
	}
	writePortedSkillFile(t, dir)
	return dir
}

func writePortedSkillFile(t *testing.T, pluginDir string) {
	t.Helper()
	const skill = `---
name: go-e2e-skill
description: A demo skill contributed by the E2E test plugin.
---
# Demo Skill

This skill exists so the plugin reports at least one installed skill.
`
	if err := os.WriteFile(filepath.Join(pluginDir, "SKILL.md"), []byte(skill), 0644); err != nil {
		t.Fatalf("Failed to write skill file: %v", err)
	}
}

func getPortedInstalledPlugin(t *testing.T, client *copilot.Client, name, marketplace string) *rpc.InstalledPluginInfo {
	t.Helper()
	list, err := client.RPC.Plugins.List(t.Context())
	if err != nil {
		t.Fatalf("Plugins.List failed: %v", err)
	}
	plugin := findPortedInstalledPlugin(list.Plugins, name, marketplace)
	if plugin == nil {
		t.Fatalf("Expected installed plugin %q in marketplace %q, got %+v", name, marketplace, list.Plugins)
	}
	return plugin
}

func findPortedInstalledPlugin(plugins []rpc.InstalledPluginInfo, name, marketplace string) *rpc.InstalledPluginInfo {
	for i := range plugins {
		if plugins[i].Name == name && plugins[i].Marketplace == marketplace {
			return &plugins[i]
		}
	}
	return nil
}

func countPortedInstalledPluginByName(plugins []rpc.InstalledPluginInfo, name string) int {
	count := 0
	for _, plugin := range plugins {
		if plugin.Name == name {
			count++
		}
	}
	return count
}

func findPortedMarketplace(marketplaces []rpc.MarketplaceInfo, name string) *rpc.MarketplaceInfo {
	for i := range marketplaces {
		if marketplaces[i].Name == name {
			return &marketplaces[i]
		}
	}
	return nil
}

func containsPortedDefaultMarketplace(marketplaces []rpc.MarketplaceInfo) bool {
	for _, marketplace := range marketplaces {
		if marketplace.IsDefault != nil && *marketplace.IsDefault {
			return true
		}
	}
	return false
}
