/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Mutex};
use std::time::Duration;

use github_copilot_sdk::github_token::{
    GitHubToken, GitHubTokenProviderArgs, GitHubTokenProviderResult, GitHubTokenRequestReason,
};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::rpc::{
    ConnectorAccountRequest, ConnectorAuthorizationScope, ConnectorAvailability,
    ConnectorConnectRequest, ConnectorConnectResult, ConnectorMcpStatus, ConnectorReconcileOptions,
    ConnectorReconcileRequest, ConnectorSessionAccount, ConnectorStatus, McpDisableRequest,
    McpListToolsRequest,
};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::{
    Client, ClientOptions, LogLevel, McpHttpServerConfig, McpServerConfig, SessionConfig, Transport,
};
use serde_json::{Value, json};

type Result<T> = std::result::Result<T, Box<dyn std::error::Error + Send + Sync>>;
const FIRST: &str = "ghu_connector_session_first";
const ROTATED: &str = "ghu_connector_session_rotated";
const OTHER: &str = "ghu_connector_session_other_user";
const REPOSITORY: &str = "ghu_connector_repository_identity";

fn setting(name: &str) -> String {
    std::env::var(name).unwrap_or_else(|_| panic!("Missing test setting {name}"))
}

fn persistent_config(label: &str) -> Result<Option<Vec<u8>>> {
    let path = PathBuf::from(setting("CONNECTOR_LOCAL_DIRECTORY"))
        .join(label)
        .join("config.json");
    match std::fs::read(path) {
        Ok(contents) => Ok(Some(contents)),
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => Ok(None),
        Err(error) => Err(error.into()),
    }
}

async fn control(update: Option<Value>) -> Result<Value> {
    let url = format!("{}/local-test-control", setting("CONNECTOR_LOCAL_API"));
    let client = reqwest::Client::new();
    let response = match update {
        Some(update) => {
            client
                .post(url)
                .header("content-type", "application/json")
                .body(serde_json::to_vec(&update)?)
                .send()
                .await?
        }
        None => client.get(url).send().await?,
    };
    Ok(serde_json::from_str(
        &response.error_for_status()?.text().await?,
    )?)
}

async fn client(label: &str, stdio: bool) -> Result<Client> {
    let transport = if stdio || setting("CONNECTOR_LOCAL_TRANSPORT") == "stdio" {
        Transport::Stdio
    } else {
        Transport::InProcess
    };
    Ok(Client::start(
        ClientOptions::new()
            .with_transport(transport)
            .with_log_level(LogLevel::Error)
            .with_use_logged_in_user(false)
            .with_base_directory(PathBuf::from(setting("CONNECTOR_LOCAL_DIRECTORY")).join(label)),
    )
    .await?)
}

fn config(enabled: bool) -> SessionConfig {
    SessionConfig::default()
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_feature_flags(HashMap::from([
            ("MANAGED_MCP_SERVERS".to_owned(), enabled),
            ("CONNECTORS".to_owned(), true),
        ]))
        .with_request_extensions(false)
        .with_disabled_mcp_servers(["github-mcp-server"])
        .with_working_directory(setting("CONNECTOR_LOCAL_DIRECTORY"))
        .with_mcp_servers(
            [(
                "ordinary".to_owned(),
                McpServerConfig::Http(McpHttpServerConfig {
                    url: setting("CONNECTOR_LOCAL_ORDINARY"),
                    tools: Some(vec!["*".to_owned()]),
                    ..Default::default()
                }),
            )]
            .into_iter()
            .collect(),
        )
}

async fn account(session: &Session) -> Result<ConnectorSessionAccount> {
    let capabilities = session.rpc().connectors().get_capabilities().await?;
    assert_eq!(capabilities.availability, ConnectorAvailability::Enabled);
    assert_eq!(capabilities.session_account_selection, Some(true));
    assert_eq!(capabilities.targeted_reconcile, Some(true));
    let before = control(None).await?;
    let selected = session
        .rpc()
        .connectors()
        .get_account()
        .await?
        .ok_or("Expected an eligible session account")?;
    let after = control(None).await?;
    assert_eq!(before["reads"], after["reads"]);
    assert_eq!(selected.auth_info.login, "octocat");
    assert_eq!(selected.auth_info.host, "https://github.com");
    let public = serde_json::to_value(&selected)?;
    assert_eq!(
        public
            .as_object()
            .expect("serialized account is a JSON object")
            .len(),
        2
    );
    assert_eq!(
        public["authInfo"]
            .as_object()
            .expect("authInfo is a JSON object")
            .len(),
        3
    );
    assert_no_secrets(&public);
    Ok(selected)
}

fn assert_no_secrets(value: &Value) {
    let text = serde_json::to_string(value).expect("value serializes to JSON");
    for forbidden in [
        FIRST,
        ROTATED,
        OTHER,
        REPOSITORY,
        "registrationId",
        "accessToken",
        "127.0.0.1",
    ] {
        assert!(
            !text.contains(forbidden),
            "Public Connector state leaked a private value"
        );
    }
}

async fn reconcile(
    session: &Session,
    account_id: &str,
    force: Option<&str>,
) -> Result<ConnectorStatus> {
    let connectors = session.rpc().connectors();
    Ok(match force {
        Some(force) => {
            connectors
                .reconcile_with_options(
                    ConnectorReconcileOptions::new(account_id)
                        .refresh_catalog(true)
                        .force_connector_name(force),
                )
                .await?
        }
        None => {
            connectors
                .reconcile(ConnectorReconcileRequest {
                    account_id: account_id.to_owned(),
                    refresh_catalog: Some(true),
                })
                .await?
        }
    })
}

fn server_id(status: &ConnectorStatus, name: &str) -> String {
    status
        .runtime_servers
        .iter()
        .find(|server| server.connector_name == name)
        .unwrap_or_else(|| panic!("Missing projected Connector {name}"))
        .runtime_server_id
        .clone()
}

fn check_connected(status: &ConnectorStatus) {
    assert!(
        status
            .runtime_servers
            .iter()
            .all(|server| server.status == ConnectorMcpStatus::Connected)
    );
    assert!(
        status
            .runtime_servers
            .iter()
            .any(|server| server.connector_name == "mail")
    );
    let catalog = status.catalog.as_ref().expect("catalog");
    assert_eq!(catalog.connectors[0].tier.as_deref(), Some("standard"));
    assert_eq!(
        catalog.connectors[0].release_tag.as_deref(),
        Some("preview")
    );
    assert!(catalog.connectors[0].logo.is_some());
    assert_no_secrets(&serde_json::to_value(status).expect("status serializes to JSON"));
}

async fn static_identity() -> Result<()> {
    let client = client("static", false).await?;
    let config_before = persistent_config("static")?;
    let accounts_before = serde_json::to_value(client.rpc().account().get_all_users().await?)?;
    let session = client
        .create_session(config(true).with_github_token(FIRST))
        .await?;
    let selected = account(&session).await?;
    assert!(
        session
            .rpc()
            .connectors()
            .get_status()
            .await?
            .account_id
            .is_none()
    );
    let second = client
        .create_session(config(true).with_github_token(FIRST))
        .await?;
    let other = account(&second).await?;
    assert_ne!(selected.account_id, other.account_id);
    let before = control(None).await?;
    assert!(
        second
            .rpc()
            .connectors()
            .refresh(ConnectorAccountRequest {
                account_id: selected.account_id.clone()
            })
            .await
            .is_err()
    );
    assert_eq!(before["reads"], control(None).await?["reads"]);
    let status = reconcile(&session, &selected.account_id, None).await?;
    check_connected(&status);
    assert_eq!(control(None).await?["generations"], json!(["first"]));
    assert_eq!(control(None).await?["writes"], json!(0));
    assert_eq!(
        serde_json::to_value(client.rpc().account().get_all_users().await?)?,
        accounts_before
    );
    second.disconnect().await?;
    session.disconnect().await?;
    client.stop().await?;
    assert!(
        persistent_config("static")? == config_before,
        "Session credentials changed persistent account configuration"
    );
    Ok(())
}

async fn provider_case(scope: bool) -> Result<()> {
    if scope {
        control(Some(json!({ "expandedScope": true }))).await?;
    }
    let mode = Arc::new(AtomicUsize::new(0));
    let reasons = Arc::new(Mutex::new(Vec::new()));
    let provider = {
        let mode = mode.clone();
        let reasons = reasons.clone();
        Arc::new(move |args: GitHubTokenProviderArgs| {
            let mode = mode.clone();
            let reasons = reasons.clone();
            async move {
                reasons
                    .lock()
                    .expect("token reason lock is not poisoned")
                    .push(args.reason);
                let token = match mode.load(Ordering::SeqCst) {
                    0 => FIRST,
                    1 => ROTATED,
                    _ => OTHER,
                };
                Ok(GitHubTokenProviderResult::Token(GitHubToken::new(
                    token, 28_800,
                )))
            }
        })
    };
    let client = client("provider", false).await?;
    let config_before = persistent_config("provider")?;
    let accounts_before = serde_json::to_value(client.rpc().account().get_all_users().await?)?;
    let session = client
        .create_session(config(true).with_github_token_provider(provider))
        .await?;
    let selected = account(&session).await?;
    if scope {
        assert!(
            session
                .rpc()
                .connectors()
                .refresh(ConnectorAccountRequest {
                    account_id: selected.account_id.clone(),
                })
                .await
                .is_err()
        );
        let blocked = session.rpc().connectors().get_status().await?;
        let requirement = blocked
            .authorization_requirement
            .ok_or("Missing scope requirement")?;
        assert_eq!(requirement.account_id, selected.account_id);
        assert_eq!(
            requirement.scope,
            ConnectorAuthorizationScope::WritePluginGatewayConnections
        );
        assert_eq!(
            *reasons.lock().expect("token reason lock is not poisoned"),
            vec![GitHubTokenRequestReason::Initial]
        );
        mode.store(1, Ordering::SeqCst);
        control(Some(json!({ "credential": "rotated" }))).await?;
        let recovered = reconcile(&session, &selected.account_id, Some("mail")).await?;
        check_connected(&recovered);
        assert!(recovered.authorization_requirement.is_none());
        assert_eq!(
            recovered.account_id.as_deref(),
            Some(selected.account_id.as_str())
        );
        assert_eq!(
            control(None).await?["generations"],
            json!(["first", "rotated"])
        );
        assert_eq!(
            *reasons.lock().expect("token reason lock is not poisoned"),
            vec![
                GitHubTokenRequestReason::Initial,
                GitHubTokenRequestReason::Refresh
            ]
        );
    } else {
        let status = reconcile(&session, &selected.account_id, None).await?;
        check_connected(&status);
        let mail = server_id(&status, "mail");
        let before = control(None).await?;
        mode.store(1, Ordering::SeqCst);
        control(Some(json!({ "credential": "rotated" }))).await?;
        let tools = session
            .rpc()
            .mcp()
            .list_tools(McpListToolsRequest {
                server_name: mail.clone(),
            })
            .await?;
        assert!(tools.tools.iter().any(|tool| tool.name == "remote_ping"));
        assert_eq!(account(&session).await?.account_id, selected.account_id);
        let after = control(None).await?;
        assert_eq!(
            after["mail"]["initializations"],
            before["mail"]["initializations"]
        );
        assert!(
            after["mail"]["unauthorized"]
                .as_u64()
                .expect("mail unauthorized count is a number")
                > 0
        );
        assert_eq!(
            *reasons.lock().expect("token reason lock is not poisoned"),
            vec![
                GitHubTokenRequestReason::Initial,
                GitHubTokenRequestReason::Refresh
            ]
        );
        mode.store(2, Ordering::SeqCst);
        control(Some(json!({ "credential": "other" }))).await?;
        let successful_lists = after["mail"]["lists"].clone();
        assert!(
            session
                .rpc()
                .mcp()
                .list_tools(McpListToolsRequest { server_name: mail })
                .await
                .is_err()
        );
        assert_eq!(control(None).await?["mail"]["lists"], successful_lists);
        assert_eq!(account(&session).await?.auth_info.login, "octocat");
    }
    assert_eq!(control(None).await?["writes"], json!(0));
    assert_eq!(
        serde_json::to_value(client.rpc().account().get_all_users().await?)?,
        accounts_before
    );
    session.disconnect().await?;
    client.stop().await?;
    assert!(
        persistent_config("provider")? == config_before,
        "Session callback changed persistent account configuration"
    );
    Ok(())
}

async fn targeted() -> Result<()> {
    let first_client = client("first-process", true).await?;
    let second_client = client("second-process", false).await?;
    let first = first_client
        .create_session(config(true).with_github_token(FIRST))
        .await?;
    let second = second_client
        .create_session(config(true).with_github_token(FIRST))
        .await?;
    let first_account = account(&first).await?;
    let second_account = account(&second).await?;
    reconcile(&first, &first_account.account_id, None).await?;
    reconcile(&second, &second_account.account_id, None).await?;
    let outcome = first
        .rpc()
        .connectors()
        .reconnect(ConnectorConnectRequest {
            account_id: first_account.account_id,
            connector_name: "mail".to_owned(),
        })
        .await?;
    assert!(matches!(outcome, ConnectorConnectResult::Connected(_)));
    assert_eq!(control(None).await?["writes"], json!(1));
    let before = control(None).await?;
    let status = reconcile(&second, &second_account.account_id, Some("mail")).await?;
    check_connected(&status);
    let after = control(None).await?;
    assert_eq!(
        after["mail"]["initializations"]
            .as_u64()
            .expect("mail initializations count is a number"),
        before["mail"]["initializations"]
            .as_u64()
            .expect("mail initializations count is a number")
            + 1
    );
    assert_eq!(
        after["calendar"]["initializations"],
        before["calendar"]["initializations"]
    );
    assert_eq!(
        after["ordinary"]["initializations"],
        before["ordinary"]["initializations"]
    );
    assert_eq!(after["writes"], json!(1));
    second
        .rpc()
        .mcp()
        .disable(McpDisableRequest {
            server_name: server_id(&status, "calendar"),
        })
        .await?;
    let status = reconcile(&second, &second_account.account_id, Some("mail")).await?;
    assert!(
        status
            .runtime_servers
            .iter()
            .any(|server| server.connector_name == "calendar"
                && server.status == ConnectorMcpStatus::Disabled)
    );
    let final_state = control(None).await?;
    assert_eq!(
        final_state["calendar"]["initializations"],
        before["calendar"]["initializations"]
    );
    assert_eq!(
        final_state["ordinary"]["initializations"],
        before["ordinary"]["initializations"]
    );
    assert_eq!(final_state["writes"], json!(1));
    first.disconnect().await?;
    second.disconnect().await?;
    first_client.stop().await?;
    second_client.stop().await?;
    Ok(())
}

async fn disabled() -> Result<()> {
    let client = client("disabled", false).await?;
    let session = client
        .create_session(config(false).with_github_token(FIRST))
        .await?;
    let capabilities = session.rpc().connectors().get_capabilities().await?;
    assert_eq!(capabilities.availability, ConnectorAvailability::Disabled);
    assert_eq!(capabilities.session_account_selection, Some(true));
    assert_eq!(capabilities.targeted_reconcile, Some(true));
    let before = control(None).await?;
    assert!(session.rpc().connectors().get_account().await?.is_none());
    reconcile(&session, "not-resolved", Some("mail")).await?;
    let after = control(None).await?;
    for key in ["reads", "writes", "mail", "calendar"] {
        assert_eq!(after[key], before[key]);
    }
    session.disconnect().await?;
    client.stop().await?;
    Ok(())
}

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<()> {
    let case = setting("CONNECTOR_LOCAL_CASE");
    let work = async {
        match case.as_str() {
            "static" => static_identity().await,
            "rotation" => provider_case(false).await,
            "scope" => provider_case(true).await,
            "targeted" => targeted().await,
            "disabled" => disabled().await,
            _ => Err("Unknown local integration case".into()),
        }
    };
    tokio::time::timeout(Duration::from_secs(180), work).await??;
    println!(
        "PASS Rust SDK {} {}",
        setting("CONNECTOR_LOCAL_TRANSPORT"),
        case
    );
    Ok(())
}
