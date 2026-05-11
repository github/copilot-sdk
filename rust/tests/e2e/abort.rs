use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::generated::session_events::{AssistantMessageDeltaData, SessionEventType};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::{Error, SessionConfig, Tool, ToolInvocation, ToolResult};
use serde_json::json;
use tokio::sync::{Mutex, mpsc, oneshot};

use super::support::{
    DEFAULT_TEST_TOKEN, assistant_message_content, recv_with_timeout, wait_for_event,
    with_e2e_context,
};

#[tokio::test]
async fn should_abort_during_active_streaming() {
    with_e2e_context("abort", "should_abort_during_active_streaming", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config().with_streaming(true))
                .await
                .expect("create session");
            let events = session.subscribe();

            session
                .send(
                    "Write a very long essay about the history of computing, covering every decade \
                         from the 1940s to the 2020s in great detail.",
                )
                .await
                .expect("send long streaming turn");

            let delta = wait_for_event(events, "assistant.message_delta", |event| {
                event.parsed_type() == SessionEventType::AssistantMessageDelta
            })
            .await;
            assert!(
                !delta
                    .typed_data::<AssistantMessageDeltaData>()
                    .expect("assistant.message_delta data")
                    .delta_content
                    .is_empty()
            );

            session.abort().await.expect("abort session");

            let recovery = session
                .send_and_wait("Say 'abort_recovery_ok'.")
                .await
                .expect("send recovery")
                .expect("assistant message");
            assert!(
                assistant_message_content(&recovery)
                    .to_lowercase()
                    .contains("abort_recovery_ok")
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_abort_during_active_tool_execution() {
    with_e2e_context(
        "abort",
        "should_abort_during_active_tool_execution",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (started_tx, mut started_rx) = mpsc::unbounded_channel();
                let (release_tx, release_rx) = oneshot::channel();
                let router = ToolHandlerRouter::new(
                    vec![Box::new(SlowAnalysisTool {
                        started_tx,
                        release_rx: Mutex::new(Some(release_rx)),
                    })],
                    Arc::new(ApproveAllHandler),
                );
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools),
                    )
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send("Use slow_analysis with value 'test_abort'. Wait for the result.")
                    .await
                    .expect("send tool turn");

                let tool_value = recv_with_timeout(&mut started_rx, "slow tool start").await;
                assert_eq!(tool_value, "test_abort");

                session.abort().await.expect("abort session");
                release_tx
                    .send("RELEASED_AFTER_ABORT".to_string())
                    .expect("release slow tool");
                wait_for_event(events, "session.idle after abort", |event| {
                    event.parsed_type() == SessionEventType::SessionIdle
                })
                .await;

                let recovery = session
                    .send_and_wait("Say 'tool_abort_recovery_ok'.")
                    .await
                    .expect("send recovery")
                    .expect("assistant message");
                assert!(
                    assistant_message_content(&recovery)
                        .to_lowercase()
                        .contains("tool_abort_recovery_ok")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct SlowAnalysisTool {
    started_tx: mpsc::UnboundedSender<String>,
    release_rx: Mutex<Option<oneshot::Receiver<String>>>,
}

#[async_trait]
impl ToolHandler for SlowAnalysisTool {
    fn tool(&self) -> Tool {
        Tool::new("slow_analysis")
            .with_description("A slow analysis tool that blocks until released")
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
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let value = invocation
            .arguments
            .get("value")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default()
            .to_string();
        let _ = self.started_tx.send(value);
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
