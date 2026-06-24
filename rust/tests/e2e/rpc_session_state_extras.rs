use github_copilot_sdk::Client;
use github_copilot_sdk::rpc::PermissionsSetAllowAllRequest;

use super::support::{assistant_message_content, with_e2e_context};

const MODEL_ID: &str = "claude-sonnet-4.5";

#[tokio::test]
async fn should_list_models_for_session() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_list_models_for_session",
        |ctx| {
            Box::pin(async move {
                let token = "rpc-session-model-list-token";
                ctx.set_copilot_user_by_token_with_login(token, "rpc-session-extras-user");
                let client = Client::start(ctx.client_options().with_github_token(token))
                    .await
                    .expect("start authenticated client");
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_github_token(token)
                            .with_model(MODEL_ID),
                    )
                    .await
                    .expect("create session");

                let result = session.rpc().model().list().await.expect("list models");

                assert!(!result.list.is_empty());
                assert!(
                    result
                        .list
                        .iter()
                        .any(|model| model.to_string().contains(MODEL_ID))
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_session_activity_when_idle() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_report_session_activity_when_idle",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let activity = session
                    .rpc()
                    .metadata()
                    .activity()
                    .await
                    .expect("get activity");

                assert!(!activity.has_active_work);
                assert!(!activity.abortable);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_and_set_allowall_permissions() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_and_set_allowall_permissions",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let initial = session
                    .rpc()
                    .permissions()
                    .get_allow_all()
                    .await
                    .expect("get initial allow-all");
                assert!(!initial.enabled);

                let enable = session
                    .rpc()
                    .permissions()
                    .set_allow_all(PermissionsSetAllowAllRequest {
                        enabled: true,
                        source: None,
                    })
                    .await
                    .expect("enable allow-all");
                assert!(enable.success);
                assert!(enable.enabled);
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .get_allow_all()
                        .await
                        .expect("get enabled allow-all")
                        .enabled
                );

                let disable = session
                    .rpc()
                    .permissions()
                    .set_allow_all(PermissionsSetAllowAllRequest {
                        enabled: false,
                        source: None,
                    })
                    .await
                    .expect("disable allow-all");
                assert!(disable.success);
                assert!(!disable.enabled);
                assert!(
                    !session
                        .rpc()
                        .permissions()
                        .get_allow_all()
                        .await
                        .expect("get disabled allow-all")
                        .enabled
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_read_empty_sql_todos_for_fresh_session() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_read_empty_sql_todos_for_fresh_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .plan()
                    .read_sql_todos()
                    .await
                    .expect("read SQL todos");

                assert!(result.rows.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_telemetry_engagement_id() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_telemetry_engagement_id",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let _result = session
                    .rpc()
                    .telemetry()
                    .get_engagement_id()
                    .await
                    .expect("get telemetry engagement id");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_current_tool_metadata_after_initialization() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_get_current_tool_metadata_after_initialization",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send prompt")
                    .expect("assistant message");
                assert!(!assistant_message_content(&answer).trim().is_empty());

                let result = session
                    .rpc()
                    .tools()
                    .get_current_metadata()
                    .await
                    .expect("get current tool metadata");

                let tools = result.tools.expect("current tool metadata");
                assert!(!tools.is_empty());
                assert!(tools.iter().all(|tool| !tool.name.trim().is_empty()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reload_session_plugins() {
    with_e2e_context(
        "rpc_session_state_extras",
        "should_reload_session_plugins",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .rpc()
                    .plugins()
                    .reload()
                    .await
                    .expect("reload session plugins");

                let plugins = session
                    .rpc()
                    .plugins()
                    .list()
                    .await
                    .expect("list session plugins");
                assert!(
                    plugins
                        .plugins
                        .iter()
                        .all(|plugin| !plugin.name.trim().is_empty())
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
