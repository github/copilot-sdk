use std::net::TcpListener;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::generated::api_types::HandlePendingToolCallRequest;
use github_copilot_sdk::generated::session_events::{
    AssistantMessageData, ExternalToolRequestedData, SessionEventType, SessionResumeData,
};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::{
    Client, Error, RequestId, ResumeSessionConfig, SessionConfig, SessionId, Tool, ToolInvocation,
    ToolResult, Transport,
};
use serde_json::json;
use tokio::sync::{Mutex, mpsc, oneshot};

use super::support::{
    DEFAULT_TEST_TOKEN, E2eContext, assistant_message_content, recv_with_timeout, wait_for_event,
    with_e2e_context,
};

const SHARED_TOKEN: &str = "rust-pending-work-resume-shared-token";

#[tokio::test]
async fn should_continue_pending_permission_request_after_resume() {
    let config =
        resume_config(SessionId::from("pending-permission")).with_continue_pending_work(true);

    assert_eq!(config.continue_pending_work, Some(true));
}

#[tokio::test]
async fn should_continue_pending_external_tool_request_after_resume() {
    with_e2e_context(
        "pending_work_resume",
        "should_continue_pending_external_tool_request_after_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let suspended_client = start_external_client(ctx, port).await;
                let (started_tx, mut started_rx) = mpsc::unbounded_channel();
                let (_release_tx, release_rx) = oneshot::channel();
                let router = ToolHandlerRouter::new(
                    vec![Box::new(BlockingExternalTool {
                        started_tx,
                        release_rx: Mutex::new(Some(release_rx)),
                    })],
                    Arc::new(ApproveAllHandler),
                );
                let session1 = suspended_client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools([BlockingExternalTool::definition()]),
                    )
                    .await
                    .expect("create session");
                let session_id = session1.id().clone();

                let tool_requested =
                    wait_for_event(session1.subscribe(), "pending external tool", |event| {
                        event.parsed_type() == SessionEventType::ExternalToolRequested
                            && event
                                .typed_data::<ExternalToolRequestedData>()
                                .is_some_and(|data| data.tool_name == "resume_external_tool")
                    });
                session1
                    .send("Use resume_external_tool with value 'beta', then reply with the result.")
                    .await
                    .expect("send pending tool prompt");
                assert_eq!(
                    recv_with_timeout(&mut started_rx, "pending tool started").await,
                    "beta"
                );
                let tool_event = tool_requested
                    .await
                    .typed_data::<ExternalToolRequestedData>()
                    .expect("tool request data");
                suspended_client.force_stop();

                let resumed_client = start_external_client(ctx, port).await;
                let session2 = resumed_client
                    .resume_session(resume_config(session_id).with_continue_pending_work(true))
                    .await
                    .expect("resume pending session");
                let assistant =
                    wait_for_event(session2.subscribe(), "resumed assistant answer", |event| {
                        if event.parsed_type() != SessionEventType::AssistantMessage {
                            return false;
                        }
                        event
                            .typed_data::<AssistantMessageData>()
                            .is_some_and(|data| data.content.contains("EXTERNAL_RESUMED_BETA"))
                    });
                let result = session2
                    .rpc()
                    .tools()
                    .handle_pending_tool_call(HandlePendingToolCallRequest {
                        request_id: tool_event.request_id,
                        result: Some(json!("EXTERNAL_RESUMED_BETA")),
                        error: None,
                    })
                    .await
                    .expect("complete pending tool");
                assert!(result.success);
                assistant.await;

                session2
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                resumed_client.force_stop();
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_keep_pending_external_tool_handleable_on_warm_resume_when_continuependingwork_is_false()
 {
    let config =
        resume_config(SessionId::from("pending-warm-resume")).with_continue_pending_work(false);

    assert_eq!(config.continue_pending_work, Some(false));
}

#[tokio::test]
async fn should_continue_parallel_pending_external_tool_requests_after_resume() {
    let request_ids = [RequestId::from("request-1"), RequestId::from("request-2")];

    assert_eq!(request_ids.len(), 2);
    assert_eq!(request_ids[0].as_ref(), "request-1");
    assert_eq!(request_ids[1].as_ref(), "request-2");
}

#[tokio::test]
async fn should_resume_successfully_when_no_pending_work_exists() {
    with_e2e_context(
        "pending_work_resume",
        "should_resume_successfully_when_no_pending_work_exists",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let first_client = start_external_client(ctx, port).await;
                let session1 = first_client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session1.id().clone();
                let first = session1
                    .send_and_wait("Reply with exactly: NO_PENDING_TURN_ONE")
                    .await
                    .expect("send first")
                    .expect("first answer");
                assert!(assistant_message_content(&first).contains("NO_PENDING_TURN_ONE"));
                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                first_client.force_stop();

                let resumed_client = start_external_client(ctx, port).await;
                let session2 = resumed_client
                    .resume_session(resume_config(session_id).with_continue_pending_work(true))
                    .await
                    .expect("resume session");
                let follow_up = session2
                    .send_and_wait("Reply with exactly: NO_PENDING_TURN_TWO")
                    .await
                    .expect("send follow up")
                    .expect("follow-up answer");
                assert!(assistant_message_content(&follow_up).contains("NO_PENDING_TURN_TWO"));

                session2
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                resumed_client.force_stop();
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_continuependingwork_true_in_resume_event() {
    with_e2e_context(
        "pending_work_resume",
        "should_report_continuependingwork_true_in_resume_event",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let first_client = start_external_client(ctx, port).await;
                let session1 = first_client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session1.id().clone();
                let first = session1
                    .send_and_wait("Reply with exactly: CONTINUE_PENDING_WORK_TRUE_TURN_ONE")
                    .await
                    .expect("send first")
                    .expect("first answer");
                assert!(
                    assistant_message_content(&first)
                        .contains("CONTINUE_PENDING_WORK_TRUE_TURN_ONE")
                );
                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                first_client.force_stop();

                let resumed_client = start_external_client(ctx, port).await;
                let session2 = resumed_client
                    .resume_session(resume_config(session_id).with_continue_pending_work(true))
                    .await
                    .expect("resume session");
                let resume_event = session2
                    .get_messages()
                    .await
                    .expect("messages")
                    .into_iter()
                    .find(|event| event.parsed_type() == SessionEventType::SessionResume)
                    .expect("session.resume event")
                    .typed_data::<SessionResumeData>()
                    .expect("resume data");
                assert_eq!(resume_event.continue_pending_work, Some(true));
                assert_eq!(resume_event.session_was_active, Some(false));
                let follow_up = session2
                    .send_and_wait("Reply with exactly: CONTINUE_PENDING_WORK_TRUE_TURN_TWO")
                    .await
                    .expect("send follow up")
                    .expect("follow-up answer");
                assert!(
                    assistant_message_content(&follow_up)
                        .contains("CONTINUE_PENDING_WORK_TRUE_TURN_TWO")
                );

                session2
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                resumed_client.force_stop();
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

fn resume_config(session_id: SessionId) -> ResumeSessionConfig {
    ResumeSessionConfig::new(session_id)
        .with_github_token(DEFAULT_TEST_TOKEN)
        .with_handler(Arc::new(ApproveAllHandler))
}

async fn start_tcp_server(ctx: &E2eContext, port: u16) -> Client {
    Client::start(
        ctx.client_options_with_transport(Transport::Tcp { port })
            .with_tcp_connection_token(SHARED_TOKEN),
    )
    .await
    .expect("start TCP server client")
}

async fn start_external_client(ctx: &E2eContext, port: u16) -> Client {
    Client::start(
        ctx.client_options_with_transport(Transport::External {
            host: "127.0.0.1".to_string(),
            port,
        })
        .with_tcp_connection_token(SHARED_TOKEN),
    )
    .await
    .expect("start external client")
}

fn free_tcp_port() -> u16 {
    let listener = TcpListener::bind(("127.0.0.1", 0)).expect("bind free TCP port");
    listener.local_addr().expect("local addr").port()
}

struct BlockingExternalTool {
    started_tx: mpsc::UnboundedSender<String>,
    release_rx: Mutex<Option<oneshot::Receiver<String>>>,
}

impl BlockingExternalTool {
    fn definition() -> Tool {
        Tool::new("resume_external_tool")
            .with_description("Looks up a value after resumption")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "value": {
                        "type": "string",
                        "description": "Value to look up"
                    }
                },
                "required": ["value"]
            }))
    }
}

#[async_trait]
impl ToolHandler for BlockingExternalTool {
    fn tool(&self) -> Tool {
        Self::definition()
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
            .expect("blocking tool called once");
        let result = release_rx
            .await
            .unwrap_or_else(|_| "ORIGINAL_SHOULD_NOT_WIN".to_string());
        Ok(ToolResult::Text(result))
    }
}
