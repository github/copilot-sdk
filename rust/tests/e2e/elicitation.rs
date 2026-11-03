use std::collections::VecDeque;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::{ElicitationHandler, PermissionHandler, PermissionResult};
use github_copilot_sdk::{
    ElicitationRequest, ElicitationResult, RequestId, SessionConfig, SessionId, UiInputOptions,
};
use serde_json::json;
use tokio::sync::Mutex;

use super::support::{DEFAULT_TEST_TOKEN, assert_uuid_like};

#[tokio::test]
async fn defaults_capabilities_when_not_provided() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "defaults_capabilities_when_not_provided",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let _capabilities = session.capabilities();
                assert_uuid_like(session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn elicitation_throws_when_capability_is_missing() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "elicitation_throws_when_capability_is_missing",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert_ne!(
                    session.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
                );
                assert!(session.ui().confirm("test").await.is_err());
                assert!(session.ui().select("test", &["a", "b"]).await.is_err());
                assert!(session.ui().input("test", None).await.is_err());
                assert!(
                    session
                        .ui()
                        .elicitation(
                            "Enter name",
                            json!({
                                "type": "object",
                                "properties": { "name": { "type": "string" } },
                                "required": ["name"]
                            }),
                        )
                        .await
                        .is_err()
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn sends_requestelicitation_when_handler_provided() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "sends_requestelicitation_when_handler_provided",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([accept(json!({}))])),
                    )
                    .await
                    .expect("create session");

                assert_uuid_like(session.id());
                assert_eq!(
                    session.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_elicitation_capability_based_on_handler_presence() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "should_report_elicitation_capability_based_on_handler_presence",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let with_handler = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([accept(json!({}))])),
                    )
                    .await
                    .expect("create elicitation-capable session");
                assert_eq!(
                    with_handler.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
                );
                with_handler.disconnect().await.expect("disconnect first");

                let without_handler = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create non-elicitation session");
                assert_ne!(
                    without_handler
                        .capabilities()
                        .ui
                        .and_then(|ui| ui.elicitation),
                    Some(true)
                );

                without_handler
                    .disconnect()
                    .await
                    .expect("disconnect second");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_without_elicitationhandler_creates_successfully() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "session_without_elicitationhandler_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert_uuid_like(session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn confirm_returns_true_when_handler_accepts() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "confirm_returns_true_when_handler_accepts",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([accept(
                                json!({ "confirmed": true }),
                            )])),
                    )
                    .await
                    .expect("create session");

                assert!(session.ui().confirm("Confirm?").await.expect("confirm"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn confirm_returns_false_when_handler_declines() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "confirm_returns_false_when_handler_declines",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([decline()])),
                    )
                    .await
                    .expect("create session");

                assert!(!session.ui().confirm("Confirm?").await.expect("confirm"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn select_returns_selected_option() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "select_returns_selected_option",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([accept(
                                json!({ "selection": "beta" }),
                            )])),
                    )
                    .await
                    .expect("create session");

                assert_eq!(
                    session
                        .ui()
                        .select("Choose", &["alpha", "beta"])
                        .await
                        .expect("select")
                        .as_deref(),
                    Some("beta")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn input_returns_freeform_value() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "input_returns_freeform_value",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([accept(
                                json!({ "value": "typed value" }),
                            )])),
                    )
                    .await
                    .expect("create session");
                let options = UiInputOptions {
                    title: Some("Value"),
                    description: Some("A value to test"),
                    min_length: Some(1),
                    max_length: Some(20),
                    default: Some("default"),
                    ..UiInputOptions::default()
                };

                assert_eq!(
                    session
                        .ui()
                        .input("Enter value", Some(&options))
                        .await
                        .expect("input")
                        .as_deref(),
                    Some("typed value")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn elicitation_returns_all_action_shapes() {
    super::support::with_shared_e2e_context(
        &E2E,
        "elicitation",
        "elicitation_returns_all_action_shapes",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .pipe_handler(QueuedElicitationHandler::new([
                                accept(json!({ "name": "Mona" })),
                                decline(),
                                cancel(),
                            ])),
                    )
                    .await
                    .expect("create session");
                let schema = json!({
                    "type": "object",
                    "properties": { "name": { "type": "string" } },
                    "required": ["name"]
                });

                let accepted = session
                    .ui()
                    .elicitation("Name?", schema.clone())
                    .await
                    .expect("accepted elicitation");
                let declined = session
                    .ui()
                    .elicitation("Name?", schema.clone())
                    .await
                    .expect("declined elicitation");
                let cancelled = session
                    .ui()
                    .elicitation("Name?", schema)
                    .await
                    .expect("cancelled elicitation");

                assert_eq!(accepted.action, "accept");
                assert_eq!(
                    accepted
                        .content
                        .and_then(|content| content.get("name").cloned()),
                    Some(json!("Mona"))
                );
                assert_eq!(declined.action, "decline");
                assert_eq!(cancelled.action, "cancel");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct QueuedElicitationHandler {
    responses: Mutex<VecDeque<ElicitationResult>>,
}

impl QueuedElicitationHandler {
    fn new(responses: impl IntoIterator<Item = ElicitationResult>) -> Self {
        Self {
            responses: Mutex::new(responses.into_iter().collect()),
        }
    }
}

#[async_trait]
impl PermissionHandler for QueuedElicitationHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: github_copilot_sdk::PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::approve_once()
    }
}

#[async_trait]
impl ElicitationHandler for QueuedElicitationHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _request: ElicitationRequest,
    ) -> ElicitationResult {
        self.responses
            .lock()
            .await
            .pop_front()
            .expect("queued elicitation response")
    }
}

/// Test helper: install a single struct that implements both
/// [`PermissionHandler`] and [`ElicitationHandler`] on a [`SessionConfig`].
trait PipeHandler {
    fn pipe_handler<H>(self, handler: H) -> Self
    where
        H: PermissionHandler + ElicitationHandler + 'static;
}

impl PipeHandler for SessionConfig {
    fn pipe_handler<H>(self, handler: H) -> Self
    where
        H: PermissionHandler + ElicitationHandler + 'static,
    {
        let handler = Arc::new(handler);
        self.with_permission_handler(handler.clone() as Arc<dyn PermissionHandler>)
            .with_elicitation_handler(handler as Arc<dyn ElicitationHandler>)
    }
}

fn accept(content: serde_json::Value) -> ElicitationResult {
    ElicitationResult {
        action: "accept".to_string(),
        content: Some(content),
    }
}

fn decline() -> ElicitationResult {
    ElicitationResult {
        action: "decline".to_string(),
        content: None,
    }
}

fn cancel() -> ElicitationResult {
    ElicitationResult {
        action: "cancel".to_string(),
        content: None,
    }
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("elicitation", 10);
