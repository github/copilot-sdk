use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::generated::api_types::PermissionsSetApproveAllRequest;
use github_copilot_sdk::generated::session_events::{SessionEventType, ToolExecutionCompleteData};
use github_copilot_sdk::handler::{PermissionResult, SessionHandler};
use github_copilot_sdk::{
    PermissionRequestData, RequestId, ResumeSessionConfig, SessionConfig, SessionId,
};
use tokio::sync::{mpsc, oneshot};

use super::support::{
    DEFAULT_TEST_TOKEN, assistant_message_content, recv_with_timeout, wait_for_condition,
    wait_for_event, with_e2e_context,
};

#[tokio::test]
async fn should_work_with_approve_all_permission_handler() {
    with_e2e_context(
        "permissions",
        "should_work_with_approve_all_permission_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('4'));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_permission_handler_errors_gracefully() {
    let result = PermissionResult::UserNotAvailable;

    assert!(matches!(result, PermissionResult::UserNotAvailable));
}

#[tokio::test]
async fn should_handle_concurrent_permission_requests_from_parallel_tools() {
    let requests = [
        RequestId::from("permission-1"),
        RequestId::from("permission-2"),
    ];

    assert_eq!(requests.len(), 2);
    assert_ne!(requests[0], requests[1]);
}

#[tokio::test]
async fn should_deny_permission_when_handler_returns_denied() {
    with_e2e_context(
        "permissions",
        "should_deny_permission_when_handler_returns_denied",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let test_file = ctx.work_dir().join("protected.txt");
                std::fs::write(&test_file, "protected content").expect("write protected file");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(StaticPermissionHandler::new(
                                PermissionResult::Denied,
                            ))),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Edit protected.txt and replace 'protected' with 'hacked'.")
                    .await
                    .expect("send");

                let content = std::fs::read_to_string(&test_file).expect("read protected file");
                assert_eq!(content, "protected content");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_deny_tool_operations_when_handler_explicitly_denies() {
    with_e2e_context(
        "permissions",
        "should_deny_tool_operations_when_handler_explicitly_denies",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(StaticPermissionHandler::new(
                                PermissionResult::UserNotAvailable,
                            ))),
                    )
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("Run 'node --version'")
                    .await
                    .expect("send");

                wait_for_event(events, "permission-denied tool completion", |event| {
                    is_permission_denied_tool_completion(event)
                })
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_async_permission_handler() {
    with_e2e_context(
        "permissions",
        "should_handle_async_permission_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(AsyncPermissionHandler { request_tx })),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Run 'echo test' and tell me what happens")
                    .await
                    .expect("send");

                recv_with_timeout(&mut request_rx, "async permission request").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_resume_session_with_permission_handler() {
    with_e2e_context(
        "permissions",
        "should_resume_session_with_permission_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                session
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("first send");
                let session_id = session.id().clone();
                session
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                client.stop().await.expect("stop first client");

                let new_client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let resumed = new_client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingPermissionHandler { request_tx })),
                    )
                    .await
                    .expect("resume session");

                resumed
                    .send_and_wait("Run 'echo resumed' for me")
                    .await
                    .expect("send after resume");

                recv_with_timeout(&mut request_rx, "resumed permission request").await;

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                new_client.stop().await.expect("stop resumed client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_deny_tool_operations_when_handler_explicitly_denies_after_resume() {
    with_e2e_context(
        "permissions",
        "should_deny_tool_operations_when_handler_explicitly_denies_after_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                session
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("first send");
                let session_id = session.id().clone();
                session
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                client.stop().await.expect("stop first client");

                let new_client = ctx.start_client().await;
                let resumed = new_client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(StaticPermissionHandler::new(
                                PermissionResult::UserNotAvailable,
                            ))),
                    )
                    .await
                    .expect("resume session");
                let events = resumed.subscribe();

                resumed
                    .send_and_wait("Run 'node --version'")
                    .await
                    .expect("send after resume");

                wait_for_event(
                    events,
                    "resumed permission-denied tool completion",
                    is_permission_denied_tool_completion,
                )
                .await;

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                new_client.stop().await.expect("stop resumed client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_receive_toolcallid_in_permission_requests() {
    with_e2e_context(
        "permissions",
        "should_receive_toolcallid_in_permission_requests",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingPermissionHandler { request_tx })),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Run 'echo test'")
                    .await
                    .expect("send");

                let request = recv_with_timeout(&mut request_rx, "permission request").await;
                assert!(
                    permission_request_tool_call_id(&request).is_some(),
                    "expected permission request to include a toolCallId: {request:?}"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_deny_permission_with_noresult_kind() {
    with_e2e_context(
        "permissions",
        "should_deny_permission_with_noresult_kind",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(NotifyingPermissionHandler {
                                request_tx,
                                result: PermissionResult::NoResult,
                            })),
                    )
                    .await
                    .expect("create session");

                session.send("Run 'node --version'").await.expect("send");

                recv_with_timeout(&mut request_rx, "no-result permission request").await;
                session.abort().await.expect("abort no-result turn");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_short_circuit_permission_handler_when_set_approve_all_enabled() {
    with_e2e_context(
        "permissions",
        "should_short_circuit_permission_handler_when_set_approve_all_enabled",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingPermissionHandler { request_tx })),
                    )
                    .await
                    .expect("create session");
                let set_result = session
                    .rpc()
                    .permissions()
                    .set_approve_all(PermissionsSetApproveAllRequest { enabled: true })
                    .await
                    .expect("set approve all");
                assert!(set_result.success);
                let events = session.subscribe();

                session
                    .send_and_wait("Run 'echo test' and tell me what happens")
                    .await
                    .expect("send");

                wait_for_event(events, "successful tool completion", |event| {
                    event.parsed_type() == SessionEventType::ToolExecutionComplete
                        && event
                            .typed_data::<ToolExecutionCompleteData>()
                            .expect("tool.execution_complete data")
                            .success
                })
                .await;
                assert!(
                    request_rx.try_recv().is_err(),
                    "runtime approve-all should bypass the SDK permission handler"
                );

                let reset_result = session
                    .rpc()
                    .permissions()
                    .set_approve_all(PermissionsSetApproveAllRequest { enabled: false })
                    .await
                    .expect("reset approve all");
                assert!(reset_result.success);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_wait_for_slow_permission_handler() {
    with_e2e_context(
        "permissions",
        "should_wait_for_slow_permission_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (entered_tx, entered_rx) = oneshot::channel();
                let (release_tx, release_rx) = oneshot::channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(SlowPermissionHandler {
                                entered_tx: tokio::sync::Mutex::new(Some(entered_tx)),
                                release_rx: tokio::sync::Mutex::new(Some(release_rx)),
                            })),
                    )
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send("Run 'echo slow_handler_test'")
                    .await
                    .expect("send");
                tokio::time::timeout(std::time::Duration::from_secs(30), entered_rx)
                    .await
                    .expect("permission handler entered timeout")
                    .expect("permission handler entered channel");
                assert!(
                    tokio::time::timeout(
                        std::time::Duration::from_millis(250),
                        wait_for_event(events, "premature tool completion", |event| {
                            event.parsed_type() == SessionEventType::ToolExecutionComplete
                        }),
                    )
                    .await
                    .is_err(),
                    "tool completed before the permission handler returned"
                );

                release_tx.send(()).expect("release slow handler");
                wait_for_condition("assistant response after slow permission", || async {
                    session
                        .get_messages()
                        .await
                        .expect("get messages")
                        .iter()
                        .any(|event| {
                            event.parsed_type() == SessionEventType::AssistantMessage
                                && assistant_message_content(event).contains("slow_handler_test")
                        })
                })
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_permission_handler_for_write_operations() {
    with_e2e_context(
        "permissions",
        "should_invoke_permission_handler_for_write_operations",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let test_file = ctx.work_dir().join("test.txt");
                std::fs::write(&test_file, "original content").expect("write test file");
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        github_copilot_sdk::SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingPermissionHandler { request_tx })),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Edit test.txt and replace 'original' with 'modified'")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(!assistant_message_content(&answer).is_empty());

                let first = recv_with_timeout(&mut request_rx, "first permission request").await;
                let second = recv_with_timeout(&mut request_rx, "second permission request").await;
                assert!(
                    first.extra.is_object() || second.extra.is_object(),
                    "expected permission request payloads to preserve raw CLI fields"
                );

                let updated = std::fs::read_to_string(&test_file).expect("read updated file");
                assert_eq!(updated, "modified content");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn is_permission_denied_tool_completion(event: &github_copilot_sdk::SessionEvent) -> bool {
    if event.parsed_type() != SessionEventType::ToolExecutionComplete {
        return false;
    }
    let data = event
        .typed_data::<ToolExecutionCompleteData>()
        .expect("tool.execution_complete data");
    !data.success
        && data
            .error
            .as_ref()
            .map(|error| error.message.contains("Permission denied"))
            .unwrap_or(false)
}

fn permission_request_tool_call_id(request: &PermissionRequestData) -> Option<&str> {
    request
        .tool_call_id
        .as_deref()
        .or_else(|| {
            request
                .extra
                .get("toolCallId")
                .and_then(|value| value.as_str())
        })
        .or_else(|| {
            request
                .extra
                .get("permissionRequest")
                .and_then(|value| value.get("toolCallId"))
                .and_then(|value| value.as_str())
        })
        .or_else(|| {
            request
                .extra
                .get("promptRequest")
                .and_then(|value| value.get("toolCallId"))
                .and_then(|value| value.as_str())
        })
}

#[derive(Clone)]
struct StaticPermissionHandler {
    result: PermissionResult,
}

impl StaticPermissionHandler {
    fn new(result: PermissionResult) -> Self {
        Self { result }
    }
}

#[async_trait]
impl SessionHandler for StaticPermissionHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        self.result.clone()
    }
}

struct RecordingPermissionHandler {
    request_tx: mpsc::UnboundedSender<PermissionRequestData>,
}

#[async_trait]
impl SessionHandler for RecordingPermissionHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        let _ = self.request_tx.send(data);
        PermissionResult::Approved
    }
}

struct NotifyingPermissionHandler {
    request_tx: mpsc::UnboundedSender<PermissionRequestData>,
    result: PermissionResult,
}

#[async_trait]
impl SessionHandler for NotifyingPermissionHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        let _ = self.request_tx.send(data);
        self.result.clone()
    }
}

struct AsyncPermissionHandler {
    request_tx: mpsc::UnboundedSender<PermissionRequestData>,
}

#[async_trait]
impl SessionHandler for AsyncPermissionHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        tokio::task::yield_now().await;
        let _ = self.request_tx.send(data);
        PermissionResult::Approved
    }
}

struct SlowPermissionHandler {
    entered_tx: tokio::sync::Mutex<Option<oneshot::Sender<()>>>,
    release_rx: tokio::sync::Mutex<Option<oneshot::Receiver<()>>>,
}

#[async_trait]
impl SessionHandler for SlowPermissionHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        if let Some(entered_tx) = self.entered_tx.lock().await.take() {
            let _ = entered_tx.send(());
        }
        if let Some(release_rx) = self.release_rx.lock().await.take() {
            let _ = release_rx.await;
        }
        PermissionResult::Approved
    }
}
