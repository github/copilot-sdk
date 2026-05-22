use std::net::TcpListener;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use github_copilot_sdk::generated::session_events::{
    PermissionCompletedData, PermissionResult as EventPermissionResult, SessionEventType,
};
use github_copilot_sdk::handler::{ApproveAllHandler, PermissionHandler, PermissionResult};
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{
    Client, PermissionRequestData, RequestId, ResumeSessionConfig, SessionConfig, SessionEvent,
    SessionId, Tool, ToolInvocation, ToolResult, Transport,
};
use serde_json::json;

use super::support::{
    DEFAULT_TEST_TOKEN, E2eContext, assistant_message_content, wait_for_event, with_e2e_context,
};

const SHARED_TOKEN: &str = "rust-multi-client-shared-token";

#[tokio::test]
async fn both_clients_see_tool_request_and_completion_events() {
    with_e2e_context(
        "rust_multi_client",
        "both_clients_see_tool_request_and_completion_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_tools(selective_tools(vec![EchoTool::new(
                                "magic_number",
                                "seed",
                                "MAGIC_",
                                "_42",
                            )]))
                            .with_available_tools(["magic_number"]),
                    )
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;
                let session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone())
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_tools(selective_tools(Vec::new())),
                    )
                    .await
                    .expect("resume session");

                let client1_requested =
                    wait_for_event(session1.subscribe(), "client1 tool request", |event| {
                        event.parsed_type() == SessionEventType::ExternalToolRequested
                    });
                let client2_requested =
                    wait_for_event(session2.subscribe(), "client2 tool request", |event| {
                        event.parsed_type() == SessionEventType::ExternalToolRequested
                    });
                let client1_completed =
                    wait_for_event(session1.subscribe(), "client1 tool completion", |event| {
                        event.parsed_type() == SessionEventType::ExternalToolCompleted
                    });
                let client2_completed =
                    wait_for_event(session2.subscribe(), "client2 tool completion", |event| {
                        event.parsed_type() == SessionEventType::ExternalToolCompleted
                    });

                let answer = session1
                    .send_and_wait(
                        "Use the magic_number tool with seed 'hello' and tell me the result",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("MAGIC_hello_42"));
                let _ = tokio::join!(
                    client1_requested,
                    client2_requested,
                    client1_completed,
                    client2_completed
                );

                session2
                    .disconnect()
                    .await
                    .expect("disconnect second session");
                client2.force_stop();
                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn one_client_approves_permission_and_both_see_the_result() {
    with_e2e_context(
        "multi_client",
        "one_client_approves_permission_and_both_see_the_result",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let permission_requests = Arc::new(AtomicUsize::new(0));
                let session1 = server
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(permission_handler_with_counter(
                                PermissionResult::approve_once(),
                                Arc::clone(&permission_requests),
                            )),
                    )
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;
                let session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone()).with_permission_handler(
                            permission_handler(PermissionResult::NoResult),
                        ),
                    )
                    .await
                    .expect("resume session");

                let client1_requested = wait_for_event(
                    session1.subscribe(),
                    "client1 permission request",
                    |event| event.parsed_type() == SessionEventType::PermissionRequested,
                );
                let client2_requested = wait_for_event(
                    session2.subscribe(),
                    "client2 permission request",
                    |event| event.parsed_type() == SessionEventType::PermissionRequested,
                );
                let client1_completed = wait_for_event(
                    session1.subscribe(),
                    "client1 permission approved",
                    is_permission_approved,
                );
                let client2_completed = wait_for_event(
                    session2.subscribe(),
                    "client2 permission approved",
                    is_permission_approved,
                );

                let answer = session1
                    .send_and_wait(
                        "Create a file called hello.txt containing the text 'hello world'",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(!assistant_message_content(&answer).is_empty());
                assert!(
                    permission_requests.load(Ordering::SeqCst) > 0,
                    "expected client 1 to handle at least one permission request"
                );
                let _ = tokio::join!(
                    client1_requested,
                    client2_requested,
                    client1_completed,
                    client2_completed
                );

                session2
                    .disconnect()
                    .await
                    .expect("disconnect second session");
                client2.force_stop();
                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn one_client_rejects_permission_and_both_see_the_result() {
    with_e2e_context(
        "multi_client",
        "one_client_rejects_permission_and_both_see_the_result",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let protected_file = ctx.work_dir().join("protected.txt");
                std::fs::write(&protected_file, "protected content").expect("write protected file");
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(permission_handler(PermissionResult::reject(
                                None,
                            ))),
                    )
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;
                let session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone()).with_permission_handler(
                            permission_handler(PermissionResult::NoResult),
                        ),
                    )
                    .await
                    .expect("resume session");

                let client1_requested = wait_for_event(
                    session1.subscribe(),
                    "client1 permission request",
                    |event| event.parsed_type() == SessionEventType::PermissionRequested,
                );
                let client2_requested = wait_for_event(
                    session2.subscribe(),
                    "client2 permission request",
                    |event| event.parsed_type() == SessionEventType::PermissionRequested,
                );
                let client1_completed = wait_for_event(
                    session1.subscribe(),
                    "client1 permission denied",
                    is_permission_denied,
                );
                let client2_completed = wait_for_event(
                    session2.subscribe(),
                    "client2 permission denied",
                    is_permission_denied,
                );

                session1
                    .send_and_wait("Edit protected.txt and replace 'protected' with 'hacked'.")
                    .await
                    .expect("send");
                let content =
                    std::fs::read_to_string(&protected_file).expect("read protected file");
                assert_eq!(content, "protected content");
                let _ = tokio::join!(
                    client1_requested,
                    client2_requested,
                    client1_completed,
                    client2_completed
                );

                session2
                    .disconnect()
                    .await
                    .expect("disconnect second session");
                client2.force_stop();
                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn two_clients_register_different_tools_and_agent_uses_both() {
    with_e2e_context(
        "rust_multi_client",
        "two_clients_register_different_tools_and_agent_uses_both",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(Arc::new(ApproveAllHandler)).with_tools(selective_tools(vec![EchoTool::new(
                                "city_lookup",
                                "countryCode",
                                "CITY_FOR_",
                                "",
                            )]))
                            .with_available_tools(["city_lookup", "currency_lookup"]),
                    )
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;
                let session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone())
                            .with_permission_handler(Arc::new(ApproveAllHandler)).with_tools(selective_tools(vec![EchoTool::new(
                                "currency_lookup",
                                "countryCode",
                                "CURRENCY_FOR_",
                                "",
                            )]))
                            .with_available_tools(["city_lookup", "currency_lookup"]),
                    )
                    .await
                    .expect("resume session");

                let city = session1
                    .send_and_wait(
                        "Use the city_lookup tool with countryCode 'US' and tell me the result.",
                    )
                    .await
                    .expect("send city")
                    .expect("city answer");
                assert!(assistant_message_content(&city).contains("CITY_FOR_US"));
                let currency = session1
                    .send_and_wait(
                        "Now use the currency_lookup tool with countryCode 'US' and tell me the result.",
                    )
                    .await
                    .expect("send currency")
                    .expect("currency answer");
                assert!(assistant_message_content(&currency).contains("CURRENCY_FOR_US"));

                session2.disconnect().await.expect("disconnect second session");
                client2.force_stop();
                session1.disconnect().await.expect("disconnect first session");
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn disconnecting_client_removes_its_tools() {
    with_e2e_context(
        "rust_multi_client",
        "disconnecting_client_removes_its_tools",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(Arc::new(ApproveAllHandler)).with_tools(selective_tools(vec![EchoTool::new(
                                "stable_tool",
                                "input",
                                "STABLE_",
                                "",
                            )]))
                            .with_available_tools(["stable_tool", "ephemeral_tool"]),
                    )
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;
                let _session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone())
                            .with_permission_handler(Arc::new(ApproveAllHandler)).with_tools(selective_tools(vec![EchoTool::new(
                                "ephemeral_tool",
                                "input",
                                "EPHEMERAL_",
                                "",
                            )]))
                            .with_available_tools(["stable_tool", "ephemeral_tool"]),
                    )
                    .await
                    .expect("resume session");

                let stable = session1
                    .send_and_wait("Use the stable_tool with input 'test1' and tell me the result.")
                    .await
                    .expect("send stable")
                    .expect("stable answer");
                assert!(assistant_message_content(&stable).contains("STABLE_test1"));
                let ephemeral = session1
                    .send_and_wait(
                        "Use the ephemeral_tool with input 'test2' and tell me the result.",
                    )
                    .await
                    .expect("send ephemeral")
                    .expect("ephemeral answer");
                assert!(assistant_message_content(&ephemeral).contains("EPHEMERAL_test2"));

                let tools_removed = wait_for_event(
                    session1.subscribe(),
                    "ephemeral tool removal",
                    |event| event.parsed_type() == SessionEventType::SessionToolsUpdated,
                );
                client2.force_stop();
                tools_removed.await;
                let after = session1
                    .send_and_wait(
                        "Use the stable_tool with input 'still_here'. Also try using ephemeral_tool if it is available.",
                    )
                    .await
                    .expect("send after disconnect")
                    .expect("after answer");
                let content = assistant_message_content(&after);
                assert!(content.contains("STABLE_still_here"));
                assert!(!content.contains("EPHEMERAL_"));

                session1.disconnect().await.expect("disconnect first session");
                server.stop().await.expect("stop server client");
            })
        },
    )
    .await;
}

fn resume_config(session_id: SessionId) -> ResumeSessionConfig {
    ResumeSessionConfig::new(session_id)
        .with_github_token(DEFAULT_TEST_TOKEN)
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_tools(selective_tools(Vec::new()))
        .with_suppress_resume_event(true)
}

async fn start_tcp_server(ctx: &E2eContext, port: u16) -> Client {
    Client::start(ctx.client_options_with_transport(Transport::Tcp {
        port,
        connection_token: Some(SHARED_TOKEN.to_string()),
    }))
    .await
    .expect("start TCP server client")
}

async fn start_external_client(ctx: &E2eContext, port: u16) -> Client {
    Client::start(ctx.client_options_with_transport(Transport::External {
        host: "127.0.0.1".to_string(),
        port,
        connection_token: Some(SHARED_TOKEN.to_string()),
    }))
    .await
    .expect("start external client")
}

fn free_tcp_port() -> u16 {
    let listener = TcpListener::bind(("127.0.0.1", 0)).expect("bind free TCP port");
    listener.local_addr().expect("local addr").port()
}

fn selective_tools(tools: Vec<EchoTool>) -> Vec<Tool> {
    tools
        .into_iter()
        .map(|t| {
            let name = t.name;
            let argument_name = t.argument_name;
            EchoTool::tool_definition(name, argument_name).with_handler(Arc::new(t))
        })
        .collect()
}

fn permission_handler(result: PermissionResult) -> Arc<PermissionDecisionHandler> {
    Arc::new(PermissionDecisionHandler {
        result,
        request_count: None,
    })
}

fn permission_handler_with_counter(
    result: PermissionResult,
    request_count: Arc<AtomicUsize>,
) -> Arc<PermissionDecisionHandler> {
    Arc::new(PermissionDecisionHandler {
        result,
        request_count: Some(request_count),
    })
}

fn is_permission_approved(event: &SessionEvent) -> bool {
    event.parsed_type() == SessionEventType::PermissionCompleted
        && event
            .typed_data::<PermissionCompletedData>()
            .is_some_and(|data| matches!(data.result, EventPermissionResult::Approved(_)))
}

fn is_permission_denied(event: &SessionEvent) -> bool {
    event.parsed_type() == SessionEventType::PermissionCompleted
        && event
            .typed_data::<PermissionCompletedData>()
            .is_some_and(|data| {
                matches!(
                    data.result,
                    EventPermissionResult::DeniedInteractivelyByUser(_)
                )
            })
}

struct PermissionDecisionHandler {
    result: PermissionResult,
    request_count: Option<Arc<AtomicUsize>>,
}

#[async_trait]
impl PermissionHandler for PermissionDecisionHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        if let Some(request_count) = &self.request_count {
            request_count.fetch_add(1, Ordering::SeqCst);
        }
        self.result.clone()
    }
}

#[async_trait]
impl ToolHandler for EchoTool {
    async fn call(
        &self,
        invocation: ToolInvocation,
    ) -> Result<ToolResult, github_copilot_sdk::Error> {
        Ok(EchoTool::call(self, invocation))
    }
}

struct EchoTool {
    name: &'static str,
    argument_name: &'static str,
    prefix: &'static str,
    suffix: &'static str,
}

impl EchoTool {
    fn new(
        name: &'static str,
        argument_name: &'static str,
        prefix: &'static str,
        suffix: &'static str,
    ) -> Self {
        Self {
            name,
            argument_name,
            prefix,
            suffix,
        }
    }

    fn tool_definition(name: &'static str, argument_name: &'static str) -> Tool {
        Tool::new(name)
            .with_description(format!("Returns a deterministic value for {argument_name}"))
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    argument_name: {
                        "type": "string",
                        "description": "Input value"
                    }
                },
                "required": [argument_name]
            }))
    }
}

impl EchoTool {
    fn call(&self, invocation: ToolInvocation) -> ToolResult {
        let input = invocation
            .arguments
            .get(self.argument_name)
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        ToolResult::Text(format!("{}{}{}", self.prefix, input, self.suffix))
    }
}
