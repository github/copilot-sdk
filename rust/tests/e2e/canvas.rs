use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::canvas::{CanvasDeclaration, CanvasHandler, CanvasResult};
use github_copilot_sdk::generated::api_types::{
    CanvasAction, CanvasProviderCloseRequest, CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest, CanvasProviderOpenResult,
};
use github_copilot_sdk::types::ExtensionInfo;
use parking_lot::Mutex;
use serde_json::{Value, json};

use super::support::with_e2e_context;

struct TestCanvasHandler {
    open_calls: Mutex<Vec<CanvasProviderOpenRequest>>,
    close_calls: Mutex<Vec<CanvasProviderCloseRequest>>,
    action_calls: Mutex<Vec<CanvasProviderInvokeActionRequest>>,
}

impl TestCanvasHandler {
    fn new() -> Self {
        Self {
            open_calls: Mutex::new(Vec::new()),
            close_calls: Mutex::new(Vec::new()),
            action_calls: Mutex::new(Vec::new()),
        }
    }
}

#[async_trait]
impl CanvasHandler for TestCanvasHandler {
    async fn on_open(&self, ctx: CanvasProviderOpenRequest) -> CanvasResult<CanvasProviderOpenResult> {
        self.open_calls.lock().push(ctx.clone());
        Ok(CanvasProviderOpenResult {
            url: Some(format!(
                "https://example.com/counter/{}",
                ctx.instance_id
            )),
            title: Some(format!("Counter {}", ctx.instance_id)),
            status: Some("ready".to_string()),
        })
    }

    async fn on_action(&self, ctx: CanvasProviderInvokeActionRequest) -> CanvasResult<Value> {
        self.action_calls.lock().push(ctx.clone());
        Ok(json!({ "newValue": 42 }))
    }

    async fn on_close(&self, ctx: CanvasProviderCloseRequest) -> CanvasResult<()> {
        self.close_calls.lock().push(ctx.clone());
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
        .with_request_extensions(true)
        .with_extension_info(ExtensionInfo::new("rust-sdk-tests", "canvas-provider"))
        .with_canvases([decl])
        .with_canvas_handler(handler)
}

#[tokio::test]
async fn canvas_list_discovers_declared_canvases() {
    with_e2e_context("canvas", "canvas_list_discovers_declared_canvases", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let handler = Arc::new(TestCanvasHandler::new());
            let session = client
                .create_session(canvas_session_config(&ctx, handler))
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
    })
    .await;
}

#[tokio::test]
async fn canvas_open_round_trip() {
    with_e2e_context("canvas", "canvas_open_round_trip", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let handler = Arc::new(TestCanvasHandler::new());
            let session = client
                .create_session(canvas_session_config(&ctx, handler.clone()))
                .await
                .expect("create session");

            let canvas_list = session.rpc().canvas().list().await.expect("list canvases");
            let canvas = &canvas_list.canvases[0];

            let open_result = session
                .rpc()
                .canvas()
                .open(github_copilot_sdk::generated::api_types::CanvasOpenRequest {
                    canvas_id: "counter".to_string(),
                    instance_id: "counter-1".to_string(),
                    extension_id: Some(canvas.extension_id.clone()),
                    input: Some(json!({ "start": 41 })),
                })
                .await
                .expect("open canvas");

            assert_eq!(open_result.instance_id, "counter-1");
            assert_eq!(
                open_result.title.as_deref(),
                Some("Counter counter-1")
            );
            assert_eq!(open_result.status.as_deref(), Some("ready"));
            assert_eq!(
                open_result.url.as_deref(),
                Some("https://example.com/counter/counter-1")
            );

            let opens = handler.open_calls.lock();
            assert_eq!(opens.len(), 1);
            assert_eq!(opens[0].canvas_id, "counter");
            assert_eq!(opens[0].instance_id, "counter-1");
            drop(opens);

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
    with_e2e_context("canvas", "canvas_invoke_action_round_trip", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let handler = Arc::new(TestCanvasHandler::new());
            let session = client
                .create_session(canvas_session_config(&ctx, handler.clone()))
                .await
                .expect("create session");

            let canvas_list = session.rpc().canvas().list().await.expect("list canvases");
            let canvas = &canvas_list.canvases[0];

            session
                .rpc()
                .canvas()
                .open(github_copilot_sdk::generated::api_types::CanvasOpenRequest {
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
                .invoke_action(
                    github_copilot_sdk::generated::api_types::CanvasInvokeActionRequest {
                        instance_id: "counter-2".to_string(),
                        action_name: "increment".to_string(),
                        input: Some(json!({ "delta": 1 })),
                    },
                )
                .await
                .expect("invoke action");

            assert_eq!(result.result, Some(json!({ "newValue": 42 })));

            let actions = handler.action_calls.lock();
            assert_eq!(actions.len(), 1);
            assert_eq!(actions[0].canvas_id, "counter");
            assert_eq!(actions[0].instance_id, "counter-2");
            assert_eq!(actions[0].action_name, "increment");
            assert_eq!(actions[0].input, Some(json!({ "delta": 1 })));
            drop(actions);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn canvas_close_round_trip() {
    with_e2e_context("canvas", "canvas_close_round_trip", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let handler = Arc::new(TestCanvasHandler::new());
            let session = client
                .create_session(canvas_session_config(&ctx, handler.clone()))
                .await
                .expect("create session");

            let canvas_list = session.rpc().canvas().list().await.expect("list canvases");
            let canvas = &canvas_list.canvases[0];

            session
                .rpc()
                .canvas()
                .open(github_copilot_sdk::generated::api_types::CanvasOpenRequest {
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
                .close(github_copilot_sdk::generated::api_types::CanvasCloseRequest {
                    instance_id: "counter-3".to_string(),
                })
                .await
                .expect("close canvas");

            let closes = handler.close_calls.lock();
            assert_eq!(closes.len(), 1);
            assert_eq!(closes[0].canvas_id, "counter");
            assert_eq!(closes[0].instance_id, "counter-3");
            drop(closes);

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
