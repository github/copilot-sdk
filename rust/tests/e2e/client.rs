use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, ConnectionState, Error, ListModelsHandler, Model,
    ModelCapabilities, Transport,
};

use super::support::with_e2e_context;

#[tokio::test]
async fn should_start_ping_and_stop_stdio_client() {
    with_e2e_context("client", "should_start_ping_and_stop_stdio_client", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;
            assert_eq!(client.state(), ConnectionState::Connected);

            let response = client.ping(Some("hello from rust")).await.expect("ping");
            assert_eq!(response.message, "pong: hello from rust");
            assert!(response.timestamp > 0);

            client.stop().await.expect("stop client");
            assert_eq!(client.state(), ConnectionState::Disconnected);
        })
    })
    .await;
}

#[tokio::test]
async fn should_start_ping_and_stop_tcp_client() {
    with_e2e_context("client", "should_start_ping_and_stop_tcp_client", |ctx| {
        Box::pin(async move {
            let client = Client::start(
                ctx.client_options_with_transport(Transport::Tcp { port: 0 })
                    .with_tcp_connection_token("tcp-e2e-token"),
            )
            .await
            .expect("start TCP client");
            assert_eq!(client.state(), ConnectionState::Connected);

            let response = client.ping(Some("tcp hello")).await.expect("ping");
            assert_eq!(response.message, "pong: tcp hello");

            client.stop().await.expect("stop client");
            assert_eq!(client.state(), ConnectionState::Disconnected);
        })
    })
    .await;
}

#[tokio::test]
async fn should_get_status() {
    with_e2e_context("client", "should_get_status", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;
            let status = client.get_status().await.expect("status");

            assert!(!status.version.is_empty());
            assert!(status.protocol_version > 0);

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_get_authenticated_status() {
    with_e2e_context("client", "should_get_authenticated_status", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = Client::start(
                ctx.client_options()
                    .with_github_token(super::support::DEFAULT_TEST_TOKEN),
            )
            .await
            .expect("start client");
            let status = client.get_auth_status().await.expect("auth status");

            assert!(status.is_authenticated);

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_list_models_when_authenticated() {
    with_e2e_context("client", "should_list_models_when_authenticated", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = Client::start(
                ctx.client_options()
                    .with_github_token(super::support::DEFAULT_TEST_TOKEN),
            )
            .await
            .expect("start client");
            let models = client.list_models().await.expect("list models");

            assert!(
                models.iter().any(|model| model.id == "claude-sonnet-4.5"),
                "expected default replay model in {models:?}"
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_stop_client_with_active_session() {
    with_e2e_context("client", "should_stop_client_with_active_session", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let _session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            client.stop().await.expect("stop client");
            assert_eq!(client.state(), ConnectionState::Disconnected);
        })
    })
    .await;
}

#[tokio::test]
async fn should_force_stop_client() {
    with_e2e_context("client", "should_force_stop_client", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;
            assert_eq!(client.state(), ConnectionState::Connected);

            client.force_stop();
            assert_eq!(client.state(), ConnectionState::Disconnected);
        })
    })
    .await;
}

#[tokio::test]
async fn should_report_error_with_stderr_when_cli_fails_to_start() {
    let err = Client::start(
        ClientOptions::new()
            .with_program(CliProgram::Path(std::path::PathBuf::from(
                "definitely-not-copilot-cli-for-rust-e2e",
            )))
            .with_use_logged_in_user(false),
    )
    .await
    .expect_err("start should fail for missing CLI");

    let message = err.to_string();
    assert!(
        !message.trim().is_empty(),
        "missing CLI start failure should include an error message"
    );
}

#[tokio::test]
async fn listmodels_withcustomhandler_callshandler() {
    with_e2e_context(
        "client",
        "listmodels_withcustomhandler_callshandler",
        |ctx| {
            Box::pin(async move {
                let handler = CountingModelsHandler::default();
                let calls = Arc::clone(&handler.calls);
                let client = Client::start(
                    ctx.client_options()
                        .with_list_models_handler(handler)
                        .with_use_logged_in_user(false),
                )
                .await
                .expect("start client");

                let models = client.list_models().await.expect("list models");

                assert_eq!(calls.load(Ordering::SeqCst), 1);
                assert_eq!(models.len(), 1);
                assert_eq!(models[0].id, "custom-handler-model");

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_not_throw_when_disposing_session_after_stopping_client() {
    with_e2e_context(
        "client",
        "should_not_throw_when_disposing_session_after_stopping_client",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                client.stop().await.expect("stop client");
                drop(session);
            })
        },
    )
    .await;
}

#[tokio::test]
async fn listmodels_withcustomhandler_cachesresults() {
    with_e2e_context(
        "client",
        "listmodels_withcustomhandler_cachesresults",
        |ctx| {
            Box::pin(async move {
                let handler = CountingModelsHandler::default();
                let calls = Arc::clone(&handler.calls);
                let client = Client::start(
                    ctx.client_options()
                        .with_list_models_handler(handler)
                        .with_use_logged_in_user(false),
                )
                .await
                .expect("start client");

                let first = client.list_models().await.expect("list models first");
                let second = client.list_models().await.expect("list models second");

                assert_eq!(calls.load(Ordering::SeqCst), 1);
                assert_eq!(first[0].id, second[0].id);

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn listmodels_withcustomhandler_workswithoutstart() {
    let handler = CountingModelsHandler::default();
    let models = handler.list_models().await.expect("list models");

    assert_eq!(handler.calls.load(Ordering::SeqCst), 1);
    assert_eq!(models[0].id, "custom-handler-model");
}

#[derive(Default)]
struct CountingModelsHandler {
    calls: Arc<AtomicUsize>,
}

#[async_trait]
impl ListModelsHandler for CountingModelsHandler {
    async fn list_models(&self) -> Result<Vec<Model>, Error> {
        self.calls.fetch_add(1, Ordering::SeqCst);
        Ok(vec![Model {
            billing: None,
            capabilities: ModelCapabilities {
                limits: None,
                supports: None,
            },
            default_reasoning_effort: None,
            id: "custom-handler-model".to_string(),
            model_picker_category: None,
            model_picker_price_category: None,
            name: "Custom Handler Model".to_string(),
            policy: None,
            supported_reasoning_efforts: Vec::new(),
        }])
    }
}
