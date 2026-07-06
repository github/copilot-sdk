use github_copilot_sdk::Client;
use github_copilot_sdk::rpc::{
    AccountLoginRequest, AccountLogoutRequest, AgentRegistrySpawnRequest,
    SendAttachmentsToMessageParams, SessionsOpenStatus, UserSettingsSetRequest,
};
use serde_json::{Map, Value, json};

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_reload_user_settings() {
    with_e2e_context("rpc_server_misc", "should_reload_user_settings", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;

            client
                .rpc()
                .user()
                .settings()
                .reload()
                .await
                .expect("reload user settings");

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_get_set_and_clear_user_settings() {
    with_e2e_context(
        "rpc_server_misc",
        "should_get_set_and_clear_user_settings",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let initial = client
                    .rpc()
                    .user()
                    .settings()
                    .get()
                    .await
                    .expect("get initial user settings");
                let (key, value) = initial
                    .settings
                    .iter()
                    .find_map(|(key, setting)| {
                        setting.value.as_bool().map(|value| (key.clone(), value))
                    })
                    .expect("at least one boolean user setting");
                let toggled = !value;

                let set = client
                    .rpc()
                    .user()
                    .settings()
                    .set(UserSettingsSetRequest {
                        settings: setting_patch(&key, json!(toggled)),
                    })
                    .await
                    .expect("set user setting");
                assert!(set.shadowed_keys.is_empty());
                client
                    .rpc()
                    .user()
                    .settings()
                    .reload()
                    .await
                    .expect("reload after set");
                let after_set = client
                    .rpc()
                    .user()
                    .settings()
                    .get()
                    .await
                    .expect("get after set");
                let metadata = after_set.settings.get(&key).expect("updated setting");
                assert_eq!(metadata.value, json!(toggled));
                assert!(!metadata.is_default);

                let clear = client
                    .rpc()
                    .user()
                    .settings()
                    .set(UserSettingsSetRequest {
                        settings: setting_patch(&key, Value::Null),
                    })
                    .await
                    .expect("clear user setting");
                assert!(clear.shadowed_keys.is_empty());
                client
                    .rpc()
                    .user()
                    .settings()
                    .reload()
                    .await
                    .expect("reload after clear");
                let after_clear = client
                    .rpc()
                    .user()
                    .settings()
                    .get()
                    .await
                    .expect("get after clear");
                assert!(
                    after_clear
                        .settings
                        .get(&key)
                        .expect("cleared setting")
                        .is_default
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_login_list_getcurrentauth_and_logout_account() {
    with_e2e_context(
        "rpc_server_misc",
        "should_login_list_getcurrentauth_and_logout_account",
        |ctx| {
            Box::pin(async move {
                ctx.set_copilot_user_by_token_with_login("rust-account-token", "rust-account-user");
                let client = Client::start(ctx.client_options().with_use_logged_in_user(false))
                    .await
                    .expect("start no-token client");

                let initial = client
                    .rpc()
                    .account()
                    .get_current_auth()
                    .await
                    .expect("get initial auth");
                assert!(initial.auth_info.is_none());

                let login = client
                    .rpc()
                    .account()
                    .login(AccountLoginRequest {
                        host: "https://github.com".to_string(),
                        login: "rust-account-user".to_string(),
                        token: "rust-account-token".to_string(),
                    })
                    .await
                    .expect("account login");
                let _stored_in_vault = login.stored_in_vault;

                let current = client
                    .rpc()
                    .account()
                    .get_current_auth()
                    .await
                    .expect("get current auth after login");
                let auth_info = current.auth_info.expect("auth info after login");
                assert_eq!(auth_info["login"], json!("rust-account-user"));
                assert_eq!(auth_info["host"], json!("https://github.com"));

                let users = client
                    .rpc()
                    .account()
                    .get_all_users()
                    .await
                    .expect("get all users");
                if let Some(user) = users
                    .iter()
                    .find(|user| user.auth_info["login"] == json!("rust-account-user"))
                {
                    user.token
                        .as_deref()
                        .filter(|token| *token == "rust-account-token")
                        .unwrap_or_else(|| {
                            panic!("expected stored account token, got {:?}", user.token)
                        });
                }

                let logout = client
                    .rpc()
                    .account()
                    .logout(AccountLogoutRequest { auth_info })
                    .await
                    .expect("account logout");
                assert!(!logout.has_more_users);
                assert!(
                    client
                        .rpc()
                        .account()
                        .get_current_auth()
                        .await
                        .expect("get auth after logout")
                        .auth_info
                        .is_none()
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_agent_registry_spawn_gate_closed() {
    with_e2e_context(
        "rpc_server_misc",
        "should_report_agent_registry_spawn_gate_closed",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let err = client
                    .rpc()
                    .agent_registry()
                    .spawn(AgentRegistrySpawnRequest {
                        agent_name: None,
                        cwd: ctx.work_dir().to_string_lossy().to_string(),
                        initial_prompt: None,
                        model: None,
                        name: None,
                        permission_mode: None,
                    })
                    .await
                    .expect_err("agent registry spawn should be gated");

                let message = err.to_string();
                assert_not_unhandled(&message);
                let lower = message.to_ascii_lowercase();
                assert!(lower.contains("agentregistry.spawn"), "{message}");
                assert!(
                    lower.contains("not enabled") || lower.contains("no delegate"),
                    "{message}"
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_shut_down_owned_runtime() {
    with_e2e_context("rpc_server_misc", "should_shut_down_owned_runtime", |ctx| {
        Box::pin(async move {
            let client = Client::start(ctx.client_options())
                .await
                .expect("start dedicated client");

            client
                .rpc()
                .user()
                .settings()
                .reload()
                .await
                .expect("runtime should start live");

            client
                .rpc()
                .runtime()
                .shutdown()
                .await
                .expect("shut down runtime");

            wait_for_condition("runtime to stop serving RPCs", || async {
                client.rpc().user().settings().reload().await.is_err()
            })
            .await;

            let _ = client.stop().await;
        })
    })
    .await;
}

#[tokio::test]
async fn should_report_not_found_when_opening_session_without_context() {
    with_e2e_context(
        "rpc_server_misc",
        "should_report_not_found_when_opening_session_without_context",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .sessions()
                    .open()
                    .await
                    .expect("open session without context");

                assert_eq!(result.status, SessionsOpenStatus::NotFound);
                assert!(result.session_id.is_none());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reject_send_attachments_from_non_extension_connection() {
    with_e2e_context(
        "rpc_server_misc",
        "should_reject_send_attachments_from_non_extension_connection",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let err = session
                    .rpc()
                    .extensions()
                    .send_attachments_to_message(SendAttachmentsToMessageParams {
                        attachments: Vec::new(),
                        instance_id: None,
                    })
                    .await
                    .expect_err("normal session connection should be rejected");
                let message = err.to_string();
                assert_not_unhandled(&message);
                assert!(
                    message.to_ascii_lowercase().contains("extension"),
                    "{message}"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn assert_not_unhandled(message: &str) {
    assert!(
        !message.to_ascii_lowercase().contains("unhandled method"),
        "{message}"
    );
}

fn setting_patch(key: &str, value: Value) -> Value {
    let mut settings = Map::new();
    settings.insert(key.to_string(), value);
    Value::Object(settings)
}
