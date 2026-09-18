use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::ResumeSessionConfig;
use github_copilot_sdk::canvas::{CanvasDeclaration, CanvasError, CanvasHandler, CanvasResult};
use github_copilot_sdk::rpc::{
    CanvasAction, CanvasProviderCloseRequest, CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest, CanvasProviderOpenResult,
};
use github_copilot_sdk::types::{CanvasProviderIdentity, ExtensionInfo};
use parking_lot::Mutex;
use serde_json::{Value, json};
use tokio::sync::Notify;

struct TestCanvasHandler {
    open_calls: Mutex<Vec<CanvasProviderOpenRequest>>,
    close_calls: Mutex<Vec<CanvasProviderCloseRequest>>,
    action_calls: Mutex<Vec<CanvasProviderInvokeActionRequest>>,
    callback_order: Mutex<Vec<String>>,
    open_calls_changed: Notify,
    error_operation: Option<&'static str>,
}

impl TestCanvasHandler {
    fn new() -> Self {
        Self::with_error(None)
    }

    fn failing(operation: &'static str) -> Self {
        Self::with_error(Some(operation))
    }

    fn with_error(error_operation: Option<&'static str>) -> Self {
        Self {
            open_calls: Mutex::new(Vec::new()),
            close_calls: Mutex::new(Vec::new()),
            action_calls: Mutex::new(Vec::new()),
            callback_order: Mutex::new(Vec::new()),
            open_calls_changed: Notify::new(),
            error_operation,
        }
    }

    async fn wait_for_open_calls(&self, count: usize) {
        loop {
            let changed = self.open_calls_changed.notified();
            if self.open_calls.lock().len() >= count {
                return;
            }
            changed.await;
        }
    }

    fn fail_if_configured(&self, operation: &'static str) -> CanvasResult<()> {
        if self.error_operation == Some(operation) {
            return Err(CanvasError::new(
                format!("scenario_canvas_{operation}_failed"),
                format!("The scenario canvas {operation} operation failed."),
            ));
        }
        Ok(())
    }
}

#[async_trait]
impl CanvasHandler for TestCanvasHandler {
    async fn on_open(
        &self,
        ctx: CanvasProviderOpenRequest,
    ) -> CanvasResult<CanvasProviderOpenResult> {
        self.open_calls.lock().push(ctx.clone());
        self.open_calls_changed.notify_one();
        self.callback_order
            .lock()
            .push(format!("open:{}", ctx.instance_id));
        self.fail_if_configured("open")?;
        let value = ctx
            .input
            .as_ref()
            .and_then(|input| input.get("value"))
            .and_then(Value::as_str)
            .unwrap_or_default();
        Ok(CanvasProviderOpenResult {
            url: Some(format!("https://example.com/counter/{}", ctx.instance_id)),
            title: Some(format!("Counter: {value}")),
            status: Some("ready".to_string()),
        })
    }

    async fn on_action(&self, ctx: CanvasProviderInvokeActionRequest) -> CanvasResult<Value> {
        self.action_calls.lock().push(ctx.clone());
        self.callback_order
            .lock()
            .push(format!("action:{}:{}", ctx.instance_id, ctx.action_name));
        self.fail_if_configured("action")?;
        Ok(ctx.input.unwrap_or(Value::Null))
    }

    async fn on_close(&self, ctx: CanvasProviderCloseRequest) -> CanvasResult<()> {
        self.close_calls.lock().push(ctx.clone());
        self.callback_order
            .lock()
            .push(format!("close:{}", ctx.instance_id));
        self.fail_if_configured("close")?;
        Ok(())
    }
}

fn canvas_session_config(
    ctx: &super::support::E2eContext,
    handler: Arc<TestCanvasHandler>,
) -> github_copilot_sdk::types::SessionConfig {
    let mut decl = CanvasDeclaration::new("counter", "Counter", "Tracks a counter value.");
    decl.actions = Some(vec![CanvasAction {
        name: "increment".to_string(),
        description: Some("Increments the counter.".to_string()),
        input_schema: None,
    }]);

    ctx.approve_all_session_config()
        .with_request_canvas_renderer(true)
        .with_extension_info(ExtensionInfo::new("rust-sdk-tests", "canvas-provider"))
        .with_canvas_provider(
            CanvasProviderIdentity::new("scenario:builtin:rust-canvas")
                .with_name("Rust canvas E2E"),
        )
        .with_canvases([decl])
        .with_canvas_handler(handler)
}

#[tokio::test]
async fn canvas_list_discovers_declared_canvases() {
    super::support::with_shared_e2e_context(
        &E2E,
        "canvas",
        "canvas_list_discovers_declared_canvases",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let handler = Arc::new(TestCanvasHandler::new());
                let session = client
                    .create_session(canvas_session_config(ctx, handler))
                    .await
                    .expect("create session");

                let result = session.rpc().canvas().list().await.expect("list canvases");

                assert_eq!(result.canvases.len(), 1);
                assert_eq!(result.canvases[0].canvas_id, "counter");
                assert_eq!(result.canvases[0].display_name, "Counter");
                assert_eq!(result.canvases[0].description, "Tracks a counter value.");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn canvas_open_round_trip() {
    super::support::with_shared_e2e_context(&E2E, "canvas", "canvas_open_round_trip", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let handler = Arc::new(TestCanvasHandler::new());
            let session = client
                .create_session(canvas_session_config(ctx, handler.clone()))
                .await
                .expect("create session");

            let canvas_list = session.rpc().canvas().list().await.expect("list canvases");
            let canvas = &canvas_list.canvases[0];

            let open_result = session
                .rpc()
                .canvas()
                .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                    canvas_id: "counter".to_string(),
                    instance_id: "counter-1".to_string(),
                    extension_id: Some(canvas.extension_id.clone()),
                    input: Some(json!({ "start": 41 })),
                })
                .await
                .expect("open canvas");

            assert_eq!(open_result.instance_id, "counter-1");
            assert_eq!(open_result.title.as_deref(), Some("Counter: "));
            assert_eq!(open_result.status.as_deref(), Some("ready"));
            assert_eq!(
                open_result.url.as_deref(),
                Some("https://example.com/counter/counter-1")
            );

            {
                let opens = handler.open_calls.lock();
                assert_eq!(opens.len(), 1);
                assert_eq!(opens[0].canvas_id, "counter");
                assert_eq!(opens[0].instance_id, "counter-1");
            }

            let open_list = session
                .rpc()
                .canvas()
                .list_open()
                .await
                .expect("list open canvases");
            assert_eq!(open_list.open_canvases.len(), 1);
            assert_eq!(open_list.open_canvases[0].instance_id, "counter-1");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn canvas_invoke_action_round_trip() {
    super::support::with_shared_e2e_context(
        &E2E,
        "canvas",
        "canvas_invoke_action_round_trip",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let handler = Arc::new(TestCanvasHandler::new());
                let session = client
                    .create_session(canvas_session_config(ctx, handler.clone()))
                    .await
                    .expect("create session");

                let canvas_list = session.rpc().canvas().list().await.expect("list canvases");
                let canvas = &canvas_list.canvases[0];

                session
                    .rpc()
                    .canvas()
                    .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                        canvas_id: "counter".to_string(),
                        instance_id: "counter-2".to_string(),
                        extension_id: Some(canvas.extension_id.clone()),
                        input: Some(json!({})),
                    })
                    .await
                    .expect("open canvas");

                let result = session
                    .rpc()
                    .canvas()
                    .action()
                    .invoke(github_copilot_sdk::rpc::CanvasActionInvokeRequest {
                        instance_id: "counter-2".to_string(),
                        action_name: "increment".to_string(),
                        input: Some(json!({ "delta": 1 })),
                    })
                    .await
                    .expect("invoke action");

                assert_eq!(result.result, Some(json!({ "delta": 1 })));

                {
                    let actions = handler.action_calls.lock();
                    assert_eq!(actions.len(), 1);
                    assert_eq!(actions[0].canvas_id, "counter");
                    assert_eq!(actions[0].instance_id, "counter-2");
                    assert_eq!(actions[0].action_name, "increment");
                    assert_eq!(actions[0].input, Some(json!({ "delta": 1 })));
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn canvas_close_round_trip() {
    super::support::with_shared_e2e_context(&E2E, "canvas", "canvas_close_round_trip", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let handler = Arc::new(TestCanvasHandler::new());
            let session = client
                .create_session(canvas_session_config(ctx, handler.clone()))
                .await
                .expect("create session");

            let canvas_list = session.rpc().canvas().list().await.expect("list canvases");
            let canvas = &canvas_list.canvases[0];

            session
                .rpc()
                .canvas()
                .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                    canvas_id: "counter".to_string(),
                    instance_id: "counter-3".to_string(),
                    extension_id: Some(canvas.extension_id.clone()),
                    input: Some(json!({})),
                })
                .await
                .expect("open canvas");

            assert!(handler.close_calls.lock().is_empty());

            session
                .rpc()
                .canvas()
                .close(github_copilot_sdk::rpc::CanvasCloseRequest {
                    instance_id: "counter-3".to_string(),
                })
                .await
                .expect("close canvas");

            {
                let closes = handler.close_calls.lock();
                assert_eq!(closes.len(), 1);
                assert_eq!(closes[0].canvas_id, "counter");
                assert_eq!(closes[0].instance_id, "counter-3");
            }

            let open_list = session
                .rpc()
                .canvas()
                .list_open()
                .await
                .expect("list open canvases");
            assert!(open_list.open_canvases.is_empty());

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn structured_canvas_open_error_surfaces_to_caller() {
    super::support::with_dedicated_e2e_context(
        "scenario_testing_canvas",
        "should_handle_structured_scenario_canvas_error",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let handler = Arc::new(TestCanvasHandler::failing("open"));
                let session = client
                    .create_session(canvas_session_config(ctx, handler.clone()))
                    .await
                    .expect("create session");
                let canvas = session
                    .rpc()
                    .canvas()
                    .list()
                    .await
                    .expect("list canvases")
                    .canvases
                    .into_iter()
                    .next()
                    .expect("declared canvas");

                let error = session
                    .rpc()
                    .canvas()
                    .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                        canvas_id: "counter".to_string(),
                        instance_id: "counter-error".to_string(),
                        extension_id: Some(canvas.extension_id),
                        input: Some(json!({ "value": "before" })),
                    })
                    .await
                    .expect_err("open error should surface");

                assert_eq!(error.rpc_code(), Some(-32603));
                assert!(
                    error
                        .to_string()
                        .contains("The scenario canvas open operation failed.")
                );
                assert_eq!(
                    handler.callback_order.lock().as_slice(),
                    ["open:counter-error"]
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn structured_canvas_action_error_surfaces_to_caller() {
    super::support::with_dedicated_e2e_context(
        "scenario_testing_canvas",
        "should_handle_structured_scenario_canvas_error",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let handler = Arc::new(TestCanvasHandler::failing("action"));
                let session = client
                    .create_session(canvas_session_config(ctx, handler.clone()))
                    .await
                    .expect("create session");
                let canvas = session
                    .rpc()
                    .canvas()
                    .list()
                    .await
                    .expect("list canvases")
                    .canvases
                    .into_iter()
                    .next()
                    .expect("declared canvas");
                session
                    .rpc()
                    .canvas()
                    .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                        canvas_id: "counter".to_string(),
                        instance_id: "counter-error".to_string(),
                        extension_id: Some(canvas.extension_id),
                        input: Some(json!({ "value": "before" })),
                    })
                    .await
                    .expect("open canvas");

                let error = session
                    .rpc()
                    .canvas()
                    .action()
                    .invoke(github_copilot_sdk::rpc::CanvasActionInvokeRequest {
                        instance_id: "counter-error".to_string(),
                        action_name: "increment".to_string(),
                        input: Some(json!({ "value": "after" })),
                    })
                    .await
                    .expect_err("action error should surface");

                assert_eq!(error.rpc_code(), Some(-32603));
                assert!(
                    error
                        .to_string()
                        .contains("The scenario canvas action operation failed.")
                );
                assert_eq!(
                    handler.callback_order.lock().as_slice(),
                    ["open:counter-error", "action:counter-error:increment"]
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn structured_canvas_close_error_is_best_effort() {
    super::support::with_dedicated_e2e_context(
        "scenario_testing_canvas",
        "should_handle_structured_scenario_canvas_error",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let handler = Arc::new(TestCanvasHandler::failing("close"));
                let session = client
                    .create_session(canvas_session_config(ctx, handler.clone()))
                    .await
                    .expect("create session");
                let canvas = session
                    .rpc()
                    .canvas()
                    .list()
                    .await
                    .expect("list canvases")
                    .canvases
                    .into_iter()
                    .next()
                    .expect("declared canvas");
                session
                    .rpc()
                    .canvas()
                    .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                        canvas_id: "counter".to_string(),
                        instance_id: "counter-error".to_string(),
                        extension_id: Some(canvas.extension_id),
                        input: Some(json!({ "value": "before" })),
                    })
                    .await
                    .expect("open canvas");

                session
                    .rpc()
                    .canvas()
                    .close(github_copilot_sdk::rpc::CanvasCloseRequest {
                        instance_id: "counter-error".to_string(),
                    })
                    .await
                    .expect("close remains best effort");
                assert_eq!(
                    handler.callback_order.lock().as_slice(),
                    ["open:counter-error", "close:counter-error"]
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn resumed_canvas_reattaches_and_routes_all_callbacks() {
    super::support::with_dedicated_e2e_context(
        "scenario_testing_canvas",
        "should_reattach_scenario_canvas_and_route_all_callbacks_after_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let original_handler = Arc::new(TestCanvasHandler::new());
                let session = client
                    .create_session(canvas_session_config(ctx, original_handler))
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                let canvas = session
                    .rpc()
                    .canvas()
                    .list()
                    .await
                    .expect("list canvases")
                    .canvases
                    .into_iter()
                    .next()
                    .expect("declared canvas");
                session
                    .rpc()
                    .canvas()
                    .open(github_copilot_sdk::rpc::CanvasOpenRequest {
                        canvas_id: "counter".to_string(),
                        instance_id: "counter-resume".to_string(),
                        extension_id: Some(canvas.extension_id),
                        input: Some(json!({ "value": "persisted" })),
                    })
                    .await
                    .expect("open canvas");
                let snapshots = session.open_canvases();
                assert_eq!(snapshots.len(), 1);

                session.rpc().suspend().await.expect("suspend session");
                session.stop_event_loop().await;
                drop(session);

                let resumed_handler = Arc::new(TestCanvasHandler::new());
                let mut declaration =
                    CanvasDeclaration::new("counter", "Counter", "Tracks a counter value.");
                declaration.actions = Some(vec![CanvasAction {
                    name: "increment".to_string(),
                    description: Some("Increments the counter.".to_string()),
                    input_schema: None,
                }]);
                let resumed = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id.clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .approve_all_permissions()
                            .with_request_canvas_renderer(true)
                            .with_canvas_provider(
                                CanvasProviderIdentity::new("scenario:builtin:rust-canvas")
                                    .with_name("Rust canvas E2E"),
                            )
                            .with_canvases([declaration])
                            .with_canvas_handler(resumed_handler.clone())
                            .with_open_canvases(snapshots),
                    )
                    .await
                    .expect("resume session");

                tokio::time::timeout(
                    Duration::from_secs(10),
                    resumed_handler.wait_for_open_calls(1),
                )
                .await
                .expect("reattached canvas open callback");
                {
                    let opens = resumed_handler.open_calls.lock();
                    assert_eq!(opens.len(), 1);
                    assert_eq!(opens[0].session_id, session_id);
                    assert_eq!(opens[0].instance_id, "counter-resume");
                    assert_eq!(opens[0].input, Some(json!({ "value": "persisted" })));
                }
                // The renderer callback precedes the runtime's authoritative opened event.
                let mut events = resumed.subscribe();
                tokio::time::timeout(Duration::from_secs(10), async {
                    while resumed.open_canvases().is_empty() {
                        events.recv().await.expect("resumed canvas event");
                    }
                })
                .await
                .expect("reattached canvas snapshot");
                let resumed_snapshots = resumed.open_canvases();
                assert_eq!(resumed_snapshots.len(), 1);
                assert_eq!(resumed_snapshots[0].instance_id, "counter-resume");

                let action = resumed
                    .rpc()
                    .canvas()
                    .action()
                    .invoke(github_copilot_sdk::rpc::CanvasActionInvokeRequest {
                        instance_id: "counter-resume".to_string(),
                        action_name: "increment".to_string(),
                        input: Some(json!({ "value": "resumed" })),
                    })
                    .await
                    .expect("invoke resumed action");
                assert_eq!(action.result, Some(json!({ "value": "resumed" })));
                resumed
                    .rpc()
                    .canvas()
                    .close(github_copilot_sdk::rpc::CanvasCloseRequest {
                        instance_id: "counter-resume".to_string(),
                    })
                    .await
                    .expect("close resumed canvas");
                assert_eq!(
                    resumed_handler.callback_order.lock().as_slice(),
                    [
                        "open:counter-resume",
                        "action:counter-resume:increment",
                        "close:counter-resume"
                    ]
                );
                assert!(resumed.open_canvases().is_empty());

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
static E2E: super::support::SharedE2eGroup = super::support::SharedE2eGroup::standard("canvas", 4);
