use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use github_copilot_sdk::{Client, Error, ListModelsHandler, Model, Transport};

use super::support::{is_inprocess_default, with_e2e_context};

#[tokio::test]
async fn should_start_ping_and_stop_stdio_client() {
    with_e2e_context("client", "should_start_ping_and_stop_stdio_client", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;
            let timings = client.startup_timings().expect("startup timings");
            if is_inprocess_default() {
                assert!(timings.program_resolve_ms.is_some());
                assert!(timings.process_spawn_ms.is_none());
            } else {
                assert!(timings.program_resolve_ms.is_none());
                assert!(timings.process_spawn_ms.is_some());
            }
            assert!(timings.port_wait_ms.is_none());
            assert!(timings.total_ms >= timings.transport_setup_ms);
            assert!(timings.total_ms >= timings.handshake_ms);

            let response = client.ping(Some("hello from rust")).await.expect("ping");
            assert_eq!(response.message, "pong: hello from rust");
            assert!(!response.timestamp.is_empty());

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_start_ping_and_stop_tcp_client() {
    with_e2e_context("client", "should_start_ping_and_stop_tcp_client", |ctx| {
        Box::pin(async move {
            let client = Client::start(ctx.client_options_with_transport(Transport::Tcp {
                port: 0,
                connection_token: Some("tcp-e2e-token".to_string()),
            }))
            .await
            .expect("start TCP client");
            let timings = client.startup_timings().expect("startup timings");
            assert!(timings.program_resolve_ms.is_none());
            assert!(timings.process_spawn_ms.is_some());
            assert!(timings.port_wait_ms.is_some());
            assert!(timings.total_ms >= timings.transport_setup_ms);
            assert!(timings.total_ms >= timings.handshake_ms);

            let response = client.ping(Some("tcp hello")).await.expect("ping");
            assert_eq!(response.message, "pong: tcp hello");

            client.stop().await.expect("stop client");
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
                ctx.client_options_with_github_token(super::support::DEFAULT_TEST_TOKEN),
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
async fn should_use_configured_github_host_for_authentication() {
    with_e2e_context("client", "should_get_authenticated_status", |ctx| {
        Box::pin(async move {
            for (copilot_host, gh_host, expected_host) in [
                (
                    "tenant.ghe.example",
                    "fallback.ghe.example",
                    "https://tenant.ghe.example",
                ),
                ("", "fallback.ghe.example", "https://fallback.ghe.example"),
                ("", "", "https://github.com"),
            ] {
                // This tests process-environment precedence, not embedded configuration.
                let mut options = ctx
                    .client_options_with_transport(Transport::Stdio)
                    .with_env(ctx.environment())
                    .with_github_token(super::support::DEFAULT_TEST_TOKEN);
                options.env.extend([
                    ("COPILOT_GH_HOST".into(), copilot_host.into()),
                    ("GH_HOST".into(), gh_host.into()),
                ]);
                let client = Client::start(options).await.expect("start client");
                let status = client.get_auth_status().await.expect("auth status");

                assert!(status.is_authenticated, "{status:?}");
                assert_eq!(status.host.as_deref(), Some(expected_host));

                client.stop().await.expect("stop client");
            }
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
                ctx.client_options_with_github_token(super::support::DEFAULT_TEST_TOKEN),
            )
            .await
            .expect("start client");
            let models = client.list_models().await.expect("list models");

            assert!(
                models.iter().any(|model| model.id == "claude-sonnet-5"),
                "expected default replay model in {models:?}"
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
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

#[derive(Default)]
struct CountingModelsHandler {
    calls: Arc<AtomicUsize>,
}

#[async_trait]
impl ListModelsHandler for CountingModelsHandler {
    async fn list_models(&self) -> Result<Vec<Model>, Error> {
        self.calls.fetch_add(1, Ordering::SeqCst);
        Ok(vec![Model {
            id: "custom-handler-model".to_string(),
            name: "Custom Handler Model".to_string(),
            ..Default::default()
        }])
    }
}
