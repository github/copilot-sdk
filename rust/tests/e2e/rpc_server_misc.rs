use github_copilot_sdk::Client;
use github_copilot_sdk::rpc::{
    AgentRegistrySpawnRequest, SendAttachmentsToMessageParams, SessionsOpenStatus,
};

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
