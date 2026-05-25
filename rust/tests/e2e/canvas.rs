use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::canvas::{
    CanvasDeclaration, CanvasHandler, CanvasOpenContext, CanvasOpenResponse, CanvasResult,
};

use super::support::with_e2e_context;

struct TestCanvasHandler;

#[async_trait]
impl CanvasHandler for TestCanvasHandler {
    async fn on_open(&self, _ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
        Ok(CanvasOpenResponse::default())
    }
}

#[tokio::test]
async fn canvas_list_discovers_declared_canvases() {
    with_e2e_context("canvas", "canvas_list_discovers_declared_canvases", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_canvases([CanvasDeclaration::new(
                            "counter",
                            "Counter",
                            "Count things",
                        )])
                        .with_canvas_handler(Arc::new(TestCanvasHandler)),
                )
                .await
                .expect("create session");

            let result = session.rpc().canvas().list().await.expect("list canvases");

            assert_eq!(result.canvases.len(), 1);
            assert_eq!(result.canvases[0].canvas_id, "counter");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}
