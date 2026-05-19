use std::collections::VecDeque;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::{PermissionResult, SessionHandler};
use github_copilot_sdk::{
    ElicitationMode, ElicitationRequest, ElicitationResult, InputFormat, InputOptions, RequestId,
    ResumeSessionConfig, SessionConfig, SessionId, UiCapabilities,
};
use serde_json::json;
use tokio::sync::Mutex;

use super::support::{DEFAULT_TEST_TOKEN, assert_uuid_like, with_e2e_context};

#[tokio::test]
async fn defaults_capabilities_when_not_provided() {
    with_e2e_context(
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
    with_e2e_context(
        "elicitation",
        "elicitation_throws_when_capability_is_missing",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_request_elicitation(false),
                    )
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
    with_e2e_context(
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
                            .with_handler(Arc::new(QueuedElicitationHandler::new([accept(
                                json!({}),
                            )]))),
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
    with_e2e_context(
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
                            .with_handler(Arc::new(QueuedElicitationHandler::new([accept(
                                json!({}),
                            )]))),
                    )
                    .await
                    .expect("create elicitation-capable session");
                assert_eq!(
                    with_handler.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
                );
                with_handler.disconnect().await.expect("disconnect first");

                let without_handler = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_request_elicitation(false),
                    )
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
    with_e2e_context(
        "elicitation",
        "session_without_elicitationhandler_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_request_elicitation(false),
                    )
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
    with_e2e_context(
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
                            .with_handler(Arc::new(QueuedElicitationHandler::new([accept(
                                json!({ "confirmed": true }),
                            )]))),
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
    with_e2e_context(
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
                            .with_handler(Arc::new(QueuedElicitationHandler::new([decline()]))),
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
    with_e2e_context("elicitation", "select_returns_selected_option", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(QueuedElicitationHandler::new([accept(
                            json!({ "selection": "beta" }),
                        )]))),
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
    })
    .await;
}

#[tokio::test]
async fn input_returns_freeform_value() {
    with_e2e_context("elicitation", "input_returns_freeform_value", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(QueuedElicitationHandler::new([accept(
                            json!({ "value": "typed value" }),
                        )]))),
                )
                .await
                .expect("create session");
            let options = InputOptions {
                title: Some("Value"),
                description: Some("A value to test"),
                min_length: Some(1),
                max_length: Some(20),
                default: Some("default"),
                ..InputOptions::default()
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
    })
    .await;
}

#[tokio::test]
async fn elicitation_returns_all_action_shapes() {
    with_e2e_context(
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
                            .with_handler(Arc::new(QueuedElicitationHandler::new([
                                accept(json!({ "name": "Mona" })),
                                decline(),
                                cancel(),
                            ]))),
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

#[tokio::test]
async fn session_capabilities_types_are_properly_structured() {
    let capabilities = github_copilot_sdk::SessionCapabilities {
        ui: Some(UiCapabilities {
            elicitation: Some(true),
        }),
    };

    assert_eq!(
        capabilities.ui.as_ref().and_then(|ui| ui.elicitation),
        Some(true)
    );

    let empty = github_copilot_sdk::SessionCapabilities::default();
    assert!(empty.ui.is_none());
}

#[tokio::test]
async fn elicitation_schema_types_are_properly_structured() {
    let schema = json!({
        "type": "object",
        "properties": {
            "name": { "type": "string", "minLength": 1 },
            "confirmed": { "type": "boolean", "default": true },
        },
        "required": ["name"],
    });

    assert_eq!(schema["type"], "object");
    assert_eq!(
        schema["properties"].as_object().expect("properties").len(),
        2
    );
    assert_eq!(schema["required"].as_array().expect("required").len(), 1);
}

#[tokio::test]
async fn elicitation_params_types_are_properly_structured() {
    let request = ElicitationRequest {
        message: "Enter your name".to_string(),
        requested_schema: Some(json!({
            "type": "object",
            "properties": { "name": { "type": "string" } },
        })),
        mode: Some(ElicitationMode::Form),
        elicitation_source: None,
        url: None,
    };

    assert_eq!(request.message, "Enter your name");
    assert!(request.requested_schema.is_some());
    assert_eq!(request.mode, Some(ElicitationMode::Form));
}

#[tokio::test]
async fn elicitation_result_types_are_properly_structured() {
    let result = accept(json!({ "name": "Alice" }));

    assert_eq!(result.action, "accept");
    assert_eq!(
        result
            .content
            .as_ref()
            .and_then(|content| content.get("name")),
        Some(&json!("Alice"))
    );

    let declined = decline();
    assert_eq!(declined.action, "decline");
    assert!(declined.content.is_none());
}

#[tokio::test]
async fn input_options_has_all_properties() {
    let options = InputOptions {
        title: Some("Email Address"),
        description: Some("Enter your email"),
        min_length: Some(5),
        max_length: Some(100),
        format: Some(InputFormat::Email),
        default: Some("user@example.com"),
    };

    assert_eq!(options.title, Some("Email Address"));
    assert_eq!(options.description, Some("Enter your email"));
    assert_eq!(options.min_length, Some(5));
    assert_eq!(options.max_length, Some(100));
    assert_eq!(options.format.map(|format| format.as_str()), Some("email"));
    assert_eq!(options.default, Some("user@example.com"));
}

#[tokio::test]
async fn elicitation_context_has_all_properties() {
    let context = ElicitationRequest {
        message: "Pick a color".to_string(),
        requested_schema: Some(json!({
            "type": "object",
            "properties": {
                "color": { "type": "string", "enum": ["red", "blue"] },
            },
        })),
        mode: Some(ElicitationMode::Form),
        elicitation_source: Some("mcp-server".to_string()),
        url: None,
    };

    assert_eq!(context.message, "Pick a color");
    assert!(context.requested_schema.is_some());
    assert_eq!(context.mode, Some(ElicitationMode::Form));
    assert_eq!(context.elicitation_source.as_deref(), Some("mcp-server"));
    assert!(context.url.is_none());
}

#[tokio::test]
async fn session_config_onelicitationrequest_is_cloned() {
    let handler: Arc<dyn SessionHandler> = Arc::new(QueuedElicitationHandler::new([cancel()]));
    let config = SessionConfig::default().with_handler(handler);

    let clone = config.clone();

    assert!(Arc::ptr_eq(
        config.handler.as_ref().expect("original handler"),
        clone.handler.as_ref().expect("cloned handler")
    ));
}

#[tokio::test]
async fn resume_config_onelicitationrequest_is_cloned() {
    let handler: Arc<dyn SessionHandler> = Arc::new(QueuedElicitationHandler::new([cancel()]));
    let config = ResumeSessionConfig::new(SessionId::from("session-1")).with_handler(handler);

    let clone = config.clone();

    assert!(Arc::ptr_eq(
        config.handler.as_ref().expect("original handler"),
        clone.handler.as_ref().expect("cloned handler")
    ));
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
impl SessionHandler for QueuedElicitationHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: github_copilot_sdk::PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::Approved
    }

    async fn on_elicitation(
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
