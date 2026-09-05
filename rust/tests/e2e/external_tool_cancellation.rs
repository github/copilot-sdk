use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{Error, SessionConfig, Tool, ToolInvocation, ToolResult};
use serde_json::json;
use tokio::sync::{Mutex, mpsc, oneshot};
use tokio::time::{Duration, timeout};

use super::support::DEFAULT_TEST_TOKEN;

#[tokio::test]
async fn should_cancel_tool_handler_when_session_disconnects() {
    super::support::with_dedicated_e2e_context(
        "external_tool_cancellation",
        "should_cancel_tool_handler_when_session_disconnects",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (started_tx, mut started_rx) = mpsc::unbounded_channel();
                let (release_tx, release_rx) = oneshot::channel();
                let (cancelled_tx, cancelled_rx) = oneshot::channel();
                let tool = Arc::new(CancelAwareSlowTool {
                    started_tx,
                    release_rx: Mutex::new(Some(release_rx)),
                    cancelled_tx: Mutex::new(Some(cancelled_tx)),
                });

                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_tools(vec![
                                Tool::new("slow_analysis")
                                    .with_description(
                                        "A slow analysis tool that blocks until released",
                                    )
                                    .with_parameters(json!({
                                        "type": "object",
                                        "properties": {
                                            "value": {
                                                "type": "string",
                                                "description": "Value to analyze"
                                            }
                                        },
                                        "required": ["value"]
                                    }))
                                    .with_handler(tool),
                            ]),
                    )
                    .await
                    .expect("create session");

                session
                    .send("Use slow_analysis with value 'test_abort'. Wait for the result.")
                    .await
                    .expect("send tool turn");

                let started_value = timeout(Duration::from_secs(60), started_rx.recv())
                    .await
                    .expect("tool start wait timed out")
                    .expect("tool start channel closed");
                assert_eq!(started_value, "test_abort");

                session.disconnect().await.expect("disconnect session");
                timeout(Duration::from_secs(60), cancelled_rx)
                    .await
                    .expect("tool cancellation wait timed out")
                    .expect("tool cancellation sender dropped");

                let _ = release_tx.send("RELEASED".to_string());
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct CancelAwareSlowTool {
    started_tx: mpsc::UnboundedSender<String>,
    release_rx: Mutex<Option<oneshot::Receiver<String>>>,
    cancelled_tx: Mutex<Option<oneshot::Sender<()>>>,
}

struct CancelSignalGuard {
    cancelled_tx: Option<oneshot::Sender<()>>,
}

impl Drop for CancelSignalGuard {
    fn drop(&mut self) {
        if let Some(sender) = self.cancelled_tx.take() {
            let _ = sender.send(());
        }
    }
}

#[async_trait]
impl ToolHandler for CancelAwareSlowTool {
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let value = invocation
            .arguments
            .get("value")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default()
            .to_string();
        let _ = self.started_tx.send(value);

        let cancelled_tx = self.cancelled_tx.lock().await.take();
        let _guard = CancelSignalGuard { cancelled_tx };

        let release_rx = self
            .release_rx
            .lock()
            .await
            .take()
            .expect("slow tool called once");
        let released = release_rx.await.unwrap_or_else(|_| "released".to_string());
        Ok(ToolResult::Text(released))
    }
}
