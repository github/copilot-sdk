/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot;
using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the server-scoped plugin and marketplace RPC methods that were previously
/// untested: plugins.install/list/uninstall/update/updateAll/enable/disable,
/// plugins.marketplaces.add/list/browse/refresh/remove, and mcp.config.reload.
///
/// All fixtures are self-contained local directories so the tests run fully offline (the E2E
/// proxy blocks github.com). A local marketplace directory with the plugin nested inside it
/// (a "monorepo" marketplace) lets the runtime install a real marketplace-scoped plugin without
/// any network access, which in turn makes enable/disable/update meaningful rather than no-ops.
/// Each test runs against its own client with a fresh COPILOT_HOME so installed-plugin and
/// marketplace state never leaks between tests.
/// </summary>
public class RpcServerPluginsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_server_plugins", output)
{
    private const string MarketplaceName = "csharp-e2e-marketplace";
    private const string PluginName = "csharp-e2e-plugin";
    private const string DirectPluginName = "csharp-e2e-direct";

    [Fact]
    public async Task Should_Install_List_And_Uninstall_Plugin_From_Local_Marketplace()
    {
        var marketplaceDir = CreateLocalMarketplaceFixture();
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            await client.Rpc.Plugins.Marketplaces.AddAsync(marketplaceDir);

            var spec = $"{PluginName}@{MarketplaceName}";
            var install = await client.Rpc.Plugins.InstallAsync(spec);

            Assert.Equal(PluginName, install.Plugin.Name);
            Assert.Equal(MarketplaceName, install.Plugin.Marketplace);
            Assert.True(install.Plugin.Enabled);
            Assert.True(install.SkillsInstalled >= 1, $"expected at least one skill, got {install.SkillsInstalled}");
            // Marketplace installs are the supported path and must NOT carry the deprecation warning.
            Assert.Null(install.DeprecationWarning);

            var afterInstall = await client.Rpc.Plugins.ListAsync();
            var listed = Assert.Single(
                afterInstall.Plugins,
                p => p.Name == PluginName && p.Marketplace == MarketplaceName);
            Assert.True(listed.Enabled);

            await client.Rpc.Plugins.UninstallAsync(spec);

            var afterUninstall = await client.Rpc.Plugins.ListAsync();
            Assert.DoesNotContain(afterUninstall.Plugins, p => p.Name == PluginName && p.Marketplace == MarketplaceName);
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, marketplaceDir);
        }
    }

    [Fact]
    public async Task Should_Enable_And_Disable_Marketplace_Plugin()
    {
        var marketplaceDir = CreateLocalMarketplaceFixture();
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            var spec = $"{PluginName}@{MarketplaceName}";
            await client.Rpc.Plugins.Marketplaces.AddAsync(marketplaceDir);
            await client.Rpc.Plugins.InstallAsync(spec);

            await client.Rpc.Plugins.DisableAsync([spec]);
            Assert.False(GetPlugin(await client.Rpc.Plugins.ListAsync()).Enabled);

            await client.Rpc.Plugins.EnableAsync([spec]);
            Assert.True(GetPlugin(await client.Rpc.Plugins.ListAsync()).Enabled);
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, marketplaceDir);
        }

        static InstalledPluginInfo GetPlugin(PluginListResult list) =>
            Assert.Single(list.Plugins, p => p.Name == PluginName && p.Marketplace == MarketplaceName);
    }

    [Fact]
    public async Task Should_Update_Single_Marketplace_Plugin()
    {
        var marketplaceDir = CreateLocalMarketplaceFixture();
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            var spec = $"{PluginName}@{MarketplaceName}";
            await client.Rpc.Plugins.Marketplaces.AddAsync(marketplaceDir);
            await client.Rpc.Plugins.InstallAsync(spec);

            // Re-installs from the (local) marketplace catalog and re-counts skills.
            var update = await client.Rpc.Plugins.UpdateAsync(spec);

            Assert.True(update.SkillsInstalled >= 1, $"expected at least one skill, got {update.SkillsInstalled}");
            Assert.Equal("1.0.0", update.PreviousVersion);
            Assert.Equal("1.0.0", update.NewVersion);
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, marketplaceDir);
        }
    }

    [Fact]
    public async Task Should_Update_All_Installed_Plugins()
    {
        var marketplaceDir = CreateLocalMarketplaceFixture();
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            var spec = $"{PluginName}@{MarketplaceName}";
            await client.Rpc.Plugins.Marketplaces.AddAsync(marketplaceDir);
            await client.Rpc.Plugins.InstallAsync(spec);

            var result = await client.Rpc.Plugins.UpdateAllAsync();

            var entry = Assert.Single(
                result.Results,
                r => r.Name == PluginName && r.Marketplace == MarketplaceName);
            Assert.True(entry.Success, entry.Error);
            Assert.True(entry.SkillsInstalled >= 1);
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, marketplaceDir);
        }
    }

    [Fact]
    public async Task Should_Install_Direct_Local_Plugin_With_Deprecation_Warning()
    {
        var pluginDir = CreateDirectPluginFixture();
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            var install = await client.Rpc.Plugins.InstallAsync(pluginDir);

            Assert.Equal(DirectPluginName, install.Plugin.Name);
            // Direct (local path) installs have no originating marketplace and are deprecated.
            Assert.Equal(string.Empty, install.Plugin.Marketplace);
            Assert.NotNull(install.DeprecationWarning);
            Assert.Contains("deprecated", install.DeprecationWarning, StringComparison.OrdinalIgnoreCase);
            Assert.True(install.SkillsInstalled >= 1, $"expected at least one skill, got {install.SkillsInstalled}");

            var afterInstall = await client.Rpc.Plugins.ListAsync();
            Assert.Single(afterInstall.Plugins, p => p.Name == DirectPluginName);

            await client.Rpc.Plugins.UninstallAsync(DirectPluginName);

            var afterUninstall = await client.Rpc.Plugins.ListAsync();
            Assert.DoesNotContain(afterUninstall.Plugins, p => p.Name == DirectPluginName);
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, pluginDir);
        }
    }

    [Fact]
    public async Task Should_List_Browse_Refresh_And_Remove_Local_Marketplace()
    {
        var marketplaceDir = CreateLocalMarketplaceFixture();
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            var add = await client.Rpc.Plugins.Marketplaces.AddAsync(marketplaceDir);
            Assert.Equal(MarketplaceName, add.Name);

            var list = await client.Rpc.Plugins.Marketplaces.ListAsync();
            var mine = Assert.Single(list.Marketplaces, m => m.Name == MarketplaceName);
            Assert.NotEqual(true, mine.IsDefault);
            // The runtime always ships built-in default marketplaces alongside user-added ones.
            Assert.Contains(list.Marketplaces, m => m.IsDefault == true);

            var browse = await client.Rpc.Plugins.Marketplaces.BrowseAsync(MarketplaceName);
            var advertised = Assert.Single(browse.Plugins, p => p.Name == PluginName);
            Assert.False(string.IsNullOrEmpty(advertised.Description));

            var refresh = await client.Rpc.Plugins.Marketplaces.RefreshAsync(MarketplaceName);
            var refreshed = Assert.Single(refresh.Results, r => r.Name == MarketplaceName);
            Assert.True(refreshed.Success, refreshed.Error);

            var remove = await client.Rpc.Plugins.Marketplaces.RemoveAsync(MarketplaceName);
            Assert.True(remove.Removed);

            var afterRemove = await client.Rpc.Plugins.Marketplaces.ListAsync();
            Assert.DoesNotContain(afterRemove.Marketplaces, m => m.Name == MarketplaceName);
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, marketplaceDir);
        }
    }

    [Fact]
    public async Task Should_Reload_Mcp_Config_Cache()
    {
        var (client, home) = await CreateIsolatedClientAsync();
        try
        {
            // Drops the runtime's in-memory MCP server-definition cache; succeeds with no return value.
            await client.Rpc.Mcp.Config.ReloadAsync();
        }
        finally
        {
            await DisposeIsolatedAsync(client, home, null);
        }
    }

    /// <summary>
    /// Creates a self-contained local marketplace directory: a marketplace.json catalog plus the
    /// plugin it advertises nested inside as a subdirectory. The plugin's catalog source is a
    /// relative path, so the runtime resolves and installs it purely from the local filesystem.
    /// </summary>
    private static string CreateLocalMarketplaceFixture()
    {
        var dir = Path.Combine(Path.GetTempPath(), "copilot-e2e-mp-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);

        var manifest = $$"""
        {
          "name": "{{MarketplaceName}}",
          "owner": { "name": "Copilot SDK E2E" },
          "metadata": { "description": "Local marketplace fixture for SDK E2E tests." },
          "plugins": [
            {
              "name": "{{PluginName}}",
              "source": "./{{PluginName}}",
              "description": "E2E demo plugin advertised by the local marketplace.",
              "version": "1.0.0"
            }
          ]
        }
        """;
        File.WriteAllText(Path.Combine(dir, "marketplace.json"), manifest);

        var pluginDir = Path.Combine(dir, PluginName);
        Directory.CreateDirectory(pluginDir);
        WriteSkillFile(pluginDir);

        return dir;
    }

    /// <summary>
    /// Creates a directory installable as a direct (deprecated) local plugin: a minimal plugin.json
    /// manifest plus a single skill.
    /// </summary>
    private static string CreateDirectPluginFixture()
    {
        var dir = Path.Combine(Path.GetTempPath(), "copilot-e2e-plugin-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);

        var manifest = $$"""
        {
          "name": "{{DirectPluginName}}",
          "description": "E2E demo plugin installed directly from a local path.",
          "version": "1.0.0"
        }
        """;
        File.WriteAllText(Path.Combine(dir, "plugin.json"), manifest);
        WriteSkillFile(dir);

        return dir;
    }

    private static void WriteSkillFile(string pluginDir)
    {
        const string skill = """
        ---
        name: csharp-e2e-skill
        description: A demo skill contributed by the E2E test plugin.
        ---
        # Demo Skill

        This skill exists so the plugin reports at least one installed skill.
        """;
        File.WriteAllText(Path.Combine(pluginDir, "SKILL.md"), skill);
    }

    /// <summary>
    /// Creates a started client backed by a throwaway COPILOT_HOME so plugin/marketplace state is
    /// isolated from every other test and from the shared fixture client.
    /// </summary>
    private async Task<(CopilotClient Client, string Home)> CreateIsolatedClientAsync()
    {
        var home = Path.Combine(Path.GetTempPath(), "copilot-e2e-home-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(home);

        var env = Ctx.GetEnvironment();
        env["COPILOT_HOME"] = home;
        env["GH_CONFIG_DIR"] = home;
        env["XDG_CONFIG_HOME"] = home;
        env["XDG_STATE_HOME"] = home;

        var client = Ctx.CreateClient(options: new CopilotClientOptions { Environment = env });
        await client.StartAsync();
        return (client, home);
    }

    private static async Task DisposeIsolatedAsync(CopilotClient client, string home, string? fixtureDir)
    {
        try { await client.DisposeAsync(); }
        catch { /* best-effort */ }

        TryDeleteDirectory(home);
        if (fixtureDir is not null)
        {
            TryDeleteDirectory(fixtureDir);
        }
    }

    private static void TryDeleteDirectory(string path)
    {
        try
        {
            if (Directory.Exists(path))
            {
                Directory.Delete(path, recursive: true);
            }
        }
        catch
        {
            // Temp directories are reclaimed by the OS; ignore transient locks on cleanup.
        }
    }
}
