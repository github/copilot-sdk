use std::fs;
use std::path::Path;

use github_copilot_sdk::rpc::{
    InstalledPluginInfo, PluginListResult, PluginsDisableRequest, PluginsEnableRequest,
    PluginsInstallRequest, PluginsMarketplacesAddRequest, PluginsMarketplacesBrowseRequest,
    PluginsMarketplacesRefreshRequest, PluginsMarketplacesRemoveRequest, PluginsUninstallRequest,
    PluginsUpdateRequest,
};

use super::support::with_e2e_context;

const MARKETPLACE_NAME: &str = "csharp-e2e-marketplace";
const PLUGIN_NAME: &str = "csharp-e2e-plugin";
const DIRECT_PLUGIN_NAME: &str = "csharp-e2e-direct";

#[tokio::test]
async fn should_install_list_and_uninstall_plugin_from_local_marketplace() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_install_list_and_uninstall_plugin_from_local_marketplace",
        |ctx| {
            Box::pin(async move {
                let marketplace = create_local_marketplace_fixture();
                let client = ctx.start_client().await;
                let spec = format!("{PLUGIN_NAME}@{MARKETPLACE_NAME}");

                client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .add(PluginsMarketplacesAddRequest {
                        source: marketplace.source(),
                    })
                    .await
                    .expect("add marketplace");

                let install = client
                    .rpc()
                    .plugins()
                    .install(PluginsInstallRequest {
                        source: spec.clone(),
                        working_directory: None,
                    })
                    .await
                    .expect("install marketplace plugin");

                assert_eq!(install.plugin.name, PLUGIN_NAME);
                assert_eq!(install.plugin.marketplace, MARKETPLACE_NAME);
                assert!(install.plugin.enabled);
                assert!(install.skills_installed >= 1);
                assert!(install.deprecation_warning.is_none());

                let after_install = client.rpc().plugins().list().await.expect("list plugins");
                let listed = single_plugin(&after_install, PLUGIN_NAME, MARKETPLACE_NAME);
                assert!(listed.enabled);

                client
                    .rpc()
                    .plugins()
                    .uninstall(PluginsUninstallRequest { name: spec })
                    .await
                    .expect("uninstall marketplace plugin");

                let after_uninstall = client
                    .rpc()
                    .plugins()
                    .list()
                    .await
                    .expect("list after uninstall");
                assert!(!contains_plugin(
                    &after_uninstall,
                    PLUGIN_NAME,
                    MARKETPLACE_NAME
                ));

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_enable_and_disable_marketplace_plugin() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_enable_and_disable_marketplace_plugin",
        |ctx| {
            Box::pin(async move {
                let marketplace = create_local_marketplace_fixture();
                let client = ctx.start_client().await;
                let spec = format!("{PLUGIN_NAME}@{MARKETPLACE_NAME}");

                client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .add(PluginsMarketplacesAddRequest {
                        source: marketplace.source(),
                    })
                    .await
                    .expect("add marketplace");
                client
                    .rpc()
                    .plugins()
                    .install(PluginsInstallRequest {
                        source: spec.clone(),
                        working_directory: None,
                    })
                    .await
                    .expect("install marketplace plugin");

                client
                    .rpc()
                    .plugins()
                    .disable(PluginsDisableRequest {
                        names: vec![spec.clone()],
                    })
                    .await
                    .expect("disable plugin");
                assert!(
                    !single_plugin(
                        &client.rpc().plugins().list().await.expect("list disabled"),
                        PLUGIN_NAME,
                        MARKETPLACE_NAME
                    )
                    .enabled
                );

                client
                    .rpc()
                    .plugins()
                    .enable(PluginsEnableRequest { names: vec![spec] })
                    .await
                    .expect("enable plugin");
                assert!(
                    single_plugin(
                        &client.rpc().plugins().list().await.expect("list enabled"),
                        PLUGIN_NAME,
                        MARKETPLACE_NAME
                    )
                    .enabled
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_update_single_marketplace_plugin() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_update_single_marketplace_plugin",
        |ctx| {
            Box::pin(async move {
                let marketplace = create_local_marketplace_fixture();
                let client = ctx.start_client().await;
                let spec = format!("{PLUGIN_NAME}@{MARKETPLACE_NAME}");

                client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .add(PluginsMarketplacesAddRequest {
                        source: marketplace.source(),
                    })
                    .await
                    .expect("add marketplace");
                client
                    .rpc()
                    .plugins()
                    .install(PluginsInstallRequest {
                        source: spec.clone(),
                        working_directory: None,
                    })
                    .await
                    .expect("install marketplace plugin");

                let update = client
                    .rpc()
                    .plugins()
                    .update(PluginsUpdateRequest { name: spec })
                    .await
                    .expect("update plugin");

                assert!(update.skills_installed >= 1);
                assert_eq!(update.previous_version.as_deref(), Some("1.0.0"));
                assert_eq!(update.new_version.as_deref(), Some("1.0.0"));

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_update_all_installed_plugins() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_update_all_installed_plugins",
        |ctx| {
            Box::pin(async move {
                let marketplace = create_local_marketplace_fixture();
                let client = ctx.start_client().await;
                let spec = format!("{PLUGIN_NAME}@{MARKETPLACE_NAME}");

                client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .add(PluginsMarketplacesAddRequest {
                        source: marketplace.source(),
                    })
                    .await
                    .expect("add marketplace");
                client
                    .rpc()
                    .plugins()
                    .install(PluginsInstallRequest {
                        source: spec,
                        working_directory: None,
                    })
                    .await
                    .expect("install marketplace plugin");

                let result = client
                    .rpc()
                    .plugins()
                    .update_all()
                    .await
                    .expect("update all plugins");

                let matches: Vec<_> = result
                    .results
                    .iter()
                    .filter(|entry| {
                        entry.name == PLUGIN_NAME && entry.marketplace == MARKETPLACE_NAME
                    })
                    .collect();
                assert_eq!(matches.len(), 1, "expected one update entry: {result:?}");
                let entry = matches[0];
                assert!(entry.success, "{:?}", entry.error);
                assert!(entry.skills_installed.unwrap_or_default() >= 1);

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_install_direct_local_plugin_with_deprecation_warning() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_install_direct_local_plugin_with_deprecation_warning",
        |ctx| {
            Box::pin(async move {
                let plugin = create_direct_plugin_fixture();
                let client = ctx.start_client().await;

                let install = client
                    .rpc()
                    .plugins()
                    .install(PluginsInstallRequest {
                        source: plugin.source(),
                        working_directory: None,
                    })
                    .await
                    .expect("install direct plugin");

                assert_eq!(install.plugin.name, DIRECT_PLUGIN_NAME);
                assert_eq!(install.plugin.marketplace, "");
                let warning = install
                    .deprecation_warning
                    .as_deref()
                    .expect("direct installs should warn");
                assert!(warning.to_ascii_lowercase().contains("deprecated"));
                assert!(install.skills_installed >= 1);

                let after_install = client.rpc().plugins().list().await.expect("list plugins");
                let direct_matches = after_install
                    .plugins
                    .iter()
                    .filter(|plugin| plugin.name == DIRECT_PLUGIN_NAME)
                    .count();
                assert_eq!(
                    direct_matches, 1,
                    "expected direct plugin in {after_install:?}"
                );

                client
                    .rpc()
                    .plugins()
                    .uninstall(PluginsUninstallRequest {
                        name: DIRECT_PLUGIN_NAME.to_string(),
                    })
                    .await
                    .expect("uninstall direct plugin");

                let after_uninstall = client
                    .rpc()
                    .plugins()
                    .list()
                    .await
                    .expect("list after uninstall");
                assert!(
                    !after_uninstall
                        .plugins
                        .iter()
                        .any(|plugin| plugin.name == DIRECT_PLUGIN_NAME)
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_list_browse_refresh_and_remove_local_marketplace() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_list_browse_refresh_and_remove_local_marketplace",
        |ctx| {
            Box::pin(async move {
                let marketplace = create_local_marketplace_fixture();
                let client = ctx.start_client().await;

                let add = client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .add(PluginsMarketplacesAddRequest {
                        source: marketplace.source(),
                    })
                    .await
                    .expect("add marketplace");
                assert_eq!(add.name, MARKETPLACE_NAME);

                let list = client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .list()
                    .await
                    .expect("list marketplaces");
                let mine: Vec<_> = list
                    .marketplaces
                    .iter()
                    .filter(|marketplace| marketplace.name == MARKETPLACE_NAME)
                    .collect();
                assert_eq!(mine.len(), 1, "expected local marketplace in {list:?}");
                assert_ne!(mine[0].is_default, Some(true));
                assert!(
                    list.marketplaces
                        .iter()
                        .any(|marketplace| marketplace.is_default == Some(true))
                );

                let browse = client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .browse(PluginsMarketplacesBrowseRequest {
                        name: MARKETPLACE_NAME.to_string(),
                    })
                    .await
                    .expect("browse marketplace");
                let advertised: Vec<_> = browse
                    .plugins
                    .iter()
                    .filter(|plugin| plugin.name == PLUGIN_NAME)
                    .collect();
                assert_eq!(
                    advertised.len(),
                    1,
                    "expected advertised plugin in {browse:?}"
                );
                assert!(
                    advertised[0]
                        .description
                        .as_deref()
                        .is_some_and(|description| !description.is_empty())
                );

                let refresh = client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .refresh_with_params(PluginsMarketplacesRefreshRequest {
                        name: Some(MARKETPLACE_NAME.to_string()),
                    })
                    .await
                    .expect("refresh marketplace");
                let refreshed: Vec<_> = refresh
                    .results
                    .iter()
                    .filter(|entry| entry.name == MARKETPLACE_NAME)
                    .collect();
                assert_eq!(refreshed.len(), 1, "expected refresh result in {refresh:?}");
                assert!(refreshed[0].success, "{:?}", refreshed[0].error);

                let remove = client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .remove(PluginsMarketplacesRemoveRequest {
                        force: None,
                        name: MARKETPLACE_NAME.to_string(),
                    })
                    .await
                    .expect("remove marketplace");
                assert!(remove.removed);

                let after_remove = client
                    .rpc()
                    .plugins()
                    .marketplaces()
                    .list()
                    .await
                    .expect("list after remove");
                assert!(
                    !after_remove
                        .marketplaces
                        .iter()
                        .any(|marketplace| marketplace.name == MARKETPLACE_NAME)
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reload_mcp_config_cache() {
    with_e2e_context(
        "rpc_server_plugins",
        "should_reload_mcp_config_cache",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                client
                    .rpc()
                    .mcp()
                    .config()
                    .reload()
                    .await
                    .expect("reload MCP config cache");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct LocalFixture {
    dir: tempfile::TempDir,
}

impl LocalFixture {
    fn source(&self) -> String {
        self.dir.path().to_string_lossy().to_string()
    }
}

fn create_local_marketplace_fixture() -> LocalFixture {
    let dir = tempfile::Builder::new()
        .prefix("copilot-e2e-mp-")
        .tempdir()
        .expect("create local marketplace fixture");
    let manifest = format!(
        r#"{{
  "name": "{MARKETPLACE_NAME}",
  "owner": {{ "name": "Copilot SDK E2E" }},
  "metadata": {{ "description": "Local marketplace fixture for SDK E2E tests." }},
  "plugins": [
    {{
      "name": "{PLUGIN_NAME}",
      "source": "./{PLUGIN_NAME}",
      "description": "E2E demo plugin advertised by the local marketplace.",
      "version": "1.0.0"
    }}
  ]
}}
"#
    );
    fs::write(dir.path().join("marketplace.json"), manifest).expect("write marketplace manifest");

    let plugin_dir = dir.path().join(PLUGIN_NAME);
    fs::create_dir_all(&plugin_dir).expect("create marketplace plugin directory");
    write_skill_file(&plugin_dir);

    LocalFixture { dir }
}

fn create_direct_plugin_fixture() -> LocalFixture {
    let dir = tempfile::Builder::new()
        .prefix("copilot-e2e-plugin-")
        .tempdir()
        .expect("create direct plugin fixture");
    let manifest = format!(
        r#"{{
  "name": "{DIRECT_PLUGIN_NAME}",
  "description": "E2E demo plugin installed directly from a local path.",
  "version": "1.0.0"
}}
"#
    );
    fs::write(dir.path().join("plugin.json"), manifest).expect("write plugin manifest");
    write_skill_file(dir.path());

    LocalFixture { dir }
}

fn write_skill_file(plugin_dir: &Path) {
    let skill = r#"---
name: csharp-e2e-skill
description: A demo skill contributed by the E2E test plugin.
---
# Demo Skill

This skill exists so the plugin reports at least one installed skill.
"#;
    fs::write(plugin_dir.join("SKILL.md"), skill).expect("write skill file");
}

fn single_plugin<'a>(
    list: &'a PluginListResult,
    name: &str,
    marketplace: &str,
) -> &'a InstalledPluginInfo {
    let matches: Vec<_> = list
        .plugins
        .iter()
        .filter(|plugin| plugin.name == name && plugin.marketplace == marketplace)
        .collect();
    assert_eq!(matches.len(), 1, "expected one plugin in {list:?}");
    matches[0]
}

fn contains_plugin(list: &PluginListResult, name: &str, marketplace: &str) -> bool {
    list.plugins
        .iter()
        .any(|plugin| plugin.name == name && plugin.marketplace == marketplace)
}
