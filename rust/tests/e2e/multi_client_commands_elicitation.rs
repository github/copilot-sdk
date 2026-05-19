use std::net::TcpListener;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::generated::session_events::{
    CapabilitiesChangedData, CommandsChangedData, SessionEventType,
};
use github_copilot_sdk::handler::{PermissionResult, SessionHandler};
use github_copilot_sdk::{
    Client, CommandContext, CommandDefinition, CommandHandler, ElicitationRequest,
    ElicitationResult, RequestId, ResumeSessionConfig, SessionId, Transport,
};

use super::support::{DEFAULT_TEST_TOKEN, E2eContext, wait_for_event, with_e2e_context};

const SHARED_TOKEN: &str = "rust-multi-client-cmd-shared-token";

#[tokio::test]
async fn client_receives_commands_changed_when_another_client_joins_with_commands() {
    with_e2e_context(
        "multi_client_commands_elicitation",
        "client_receives_commands_changed_when_another_client_joins_with_commands",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;

                let commands_changed =
                    wait_for_event(session1.subscribe(), "commands changed", |event| {
                        if event.parsed_type() != SessionEventType::CommandsChanged {
                            return false;
                        }
                        let data = event
                            .typed_data::<CommandsChangedData>()
                            .expect("commands changed data");
                        data.commands.iter().any(|command| {
                            command.name == "deploy"
                                && command.description.as_deref() == Some("Deploy the app")
                        })
                    });
                let session2 = client2
                    .resume_session(resume_config(session1.id().clone()).with_commands(vec![
                            CommandDefinition::new("deploy", Arc::new(NoopCommandHandler))
                                .with_description("Deploy the app"),
                        ]))
                    .await
                    .expect("resume session from second client");
                commands_changed.await;

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
async fn capabilities_changed_fires_when_second_client_joins_with_elicitation_handler() {
    with_e2e_context(
        "multi_client_commands_elicitation",
        "capabilities_changed_fires_when_second_client_joins_with_elicitation_handler",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_request_elicitation(false),
                    )
                    .await
                    .expect("create session");
                assert_ne!(
                    session1.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
                );
                let client2 = start_external_client(ctx, port).await;

                let capabilities_changed =
                    wait_for_event(session1.subscribe(), "elicitation enabled", |event| {
                        if event.parsed_type() != SessionEventType::CapabilitiesChanged {
                            return false;
                        }
                        event
                            .typed_data::<CapabilitiesChangedData>()
                            .and_then(|data| data.ui.and_then(|ui| ui.elicitation))
                            == Some(true)
                    });
                let session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone())
                            .with_handler(Arc::new(ElicitationApproveHandler)),
                    )
                    .await
                    .expect("resume session with elicitation handler");
                capabilities_changed.await;
                assert_eq!(
                    session1.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
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
async fn capabilities_changed_fires_when_elicitation_provider_disconnects() {
    with_e2e_context(
        "multi_client_commands_elicitation",
        "capabilities_changed_fires_when_elicitation_provider_disconnects",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let port = free_tcp_port();
                let server = start_tcp_server(ctx, port).await;
                let session1 = server
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_request_elicitation(false),
                    )
                    .await
                    .expect("create session");
                let client2 = start_external_client(ctx, port).await;
                let enabled =
                    wait_for_event(session1.subscribe(), "elicitation enabled", |event| {
                        if event.parsed_type() != SessionEventType::CapabilitiesChanged {
                            return false;
                        }
                        event
                            .typed_data::<CapabilitiesChangedData>()
                            .and_then(|data| data.ui.and_then(|ui| ui.elicitation))
                            == Some(true)
                    });
                let _session2 = client2
                    .resume_session(
                        resume_config(session1.id().clone())
                            .with_handler(Arc::new(ElicitationApproveHandler)),
                    )
                    .await
                    .expect("resume session with elicitation handler");
                enabled.await;

                let disabled =
                    wait_for_event(session1.subscribe(), "elicitation disabled", |event| {
                        if event.parsed_type() != SessionEventType::CapabilitiesChanged {
                            return false;
                        }
                        event
                            .typed_data::<CapabilitiesChangedData>()
                            .and_then(|data| data.ui.and_then(|ui| ui.elicitation))
                            == Some(false)
                    });
                client2.force_stop();
                disabled.await;
                assert_ne!(
                    session1.capabilities().ui.and_then(|ui| ui.elicitation),
                    Some(true)
                );

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

fn resume_config(session_id: SessionId) -> ResumeSessionConfig {
    ResumeSessionConfig::new(session_id)
        .with_github_token(DEFAULT_TEST_TOKEN)
        .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
        .with_disable_resume(true)
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

struct NoopCommandHandler;

#[async_trait]
impl CommandHandler for NoopCommandHandler {
    async fn on_command(&self, _ctx: CommandContext) -> Result<(), github_copilot_sdk::Error> {
        Ok(())
    }
}

struct ElicitationApproveHandler;

#[async_trait]
impl SessionHandler for ElicitationApproveHandler {
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
        ElicitationResult {
            action: "accept".to_string(),
            content: Some(serde_json::json!({})),
        }
    }
}
