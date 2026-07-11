use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::rpc::{
    CommandsInvokeRequest, CommandsListRequest, CommandsRespondToQueuedCommandRequest,
    EnqueueCommandParams, ExecuteCommandParams, RegisterEventInterestParams,
    ReleaseEventInterestParams, SlashCommandInvocationResult, SlashCommandKind,
};
use github_copilot_sdk::session_events::{CommandQueuedData, SessionEventType};
use github_copilot_sdk::{CommandContext, CommandDefinition, CommandHandler, RequestId};
use serde_json::json;
use tokio::sync::mpsc;

use super::support::{recv_with_timeout, wait_for_event, with_e2e_context};

#[tokio::test]
async fn session_commands_list_returns_builtins_and_respects_client_command_filter() {
    with_e2e_context(
        "commands",
        "session_with_commands_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_commands(vec![
                        CommandDefinition::new("rust-e2e-command", Arc::new(NoopCommandHandler))
                            .with_description("Rust E2E command"),
                    ]))
                    .await
                    .expect("create session");

                let all = session
                    .rpc()
                    .commands()
                    .list()
                    .await
                    .expect("list commands");
                assert_command(&all.commands, "model", SlashCommandKind::Builtin);
                assert_command(&all.commands, "compact", SlashCommandKind::Builtin);
                assert_command(&all.commands, "context", SlashCommandKind::Builtin);
                assert_command(&all.commands, "rust-e2e-command", SlashCommandKind::Client);

                let no_builtins = session
                    .rpc()
                    .commands()
                    .list_with_params(CommandsListRequest {
                        include_builtins: Some(false),
                        include_client_commands: Some(true),
                        include_skills: Some(false),
                    })
                    .await
                    .expect("list without builtins");
                assert!(
                    !no_builtins
                        .commands
                        .iter()
                        .any(|command| command.kind == SlashCommandKind::Builtin)
                );
                assert_command(
                    &no_builtins.commands,
                    "rust-e2e-command",
                    SlashCommandKind::Client,
                );

                let client_only_disabled = session
                    .rpc()
                    .commands()
                    .list_with_params(CommandsListRequest {
                        include_builtins: Some(false),
                        include_client_commands: Some(false),
                        include_skills: Some(false),
                    })
                    .await
                    .expect("list with all dynamic sources disabled");
                assert!(client_only_disabled.commands.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_commands_invoke_known_builtin_returns_expected_result() {
    with_e2e_context(
        "commands",
        "session_with_no_commands_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .commands()
                    .invoke(CommandsInvokeRequest {
                        name: "context".to_string(),
                        input: None,
                    })
                    .await
                    .expect("invoke context");
                match result {
                    SlashCommandInvocationResult::Text(text) => {
                        assert!(!text.text.trim().is_empty());
                    }
                    SlashCommandInvocationResult::SelectSubcommand(select) => {
                        assert!(!select.options.is_empty());
                    }
                    SlashCommandInvocationResult::AgentPrompt(prompt) => {
                        assert!(!prompt.prompt.trim().is_empty());
                    }
                    SlashCommandInvocationResult::Completed(_) => {}
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_commands_execute_runs_registered_command_handler() {
    with_e2e_context(
        "commands",
        "session_with_commands_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_commands(vec![
                        CommandDefinition::new(
                            "rust-execute",
                            Arc::new(RecordingCommandHandler { tx }),
                        )
                        .with_description("Records command invocations"),
                    ]))
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .commands()
                    .execute(ExecuteCommandParams {
                        command_name: "rust-execute".to_string(),
                        args: "alpha beta".to_string(),
                    })
                    .await
                    .expect("execute command");
                assert!(result.error.is_none());

                let context = recv_with_timeout(&mut rx, "command context").await;
                assert_eq!(context.session_id, session.id().clone());
                assert_eq!(context.command_name, "rust-execute");
                assert_eq!(context.command, "/rust-execute alpha beta");
                assert_eq!(context.args, "alpha beta");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_commands_enqueue_and_respond_to_queued_command() {
    with_e2e_context(
        "commands",
        "session_with_no_commands_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let interest = session
                    .rpc()
                    .event_log()
                    .register_interest(RegisterEventInterestParams {
                        event_type: "command.queued".to_string(),
                    })
                    .await
                    .expect("register command interest")
                    .handle;
                let queued_event = wait_for_event(session.subscribe(), "command queued", |event| {
                    event.parsed_type() == SessionEventType::CommandQueued
                });

                let result = session
                    .rpc()
                    .commands()
                    .enqueue(EnqueueCommandParams {
                        command: "/help".to_string(),
                    })
                    .await
                    .expect("enqueue command");
                assert!(result.queued);

                let queued = queued_event
                    .await
                    .typed_data::<CommandQueuedData>()
                    .expect("command queued data");
                assert_eq!(queued.command, "/help");
                let response = session
                    .rpc()
                    .commands()
                    .respond_to_queued_command(CommandsRespondToQueuedCommandRequest {
                        request_id: queued.request_id,
                        result: json!({
                            "handled": true,
                            "stopProcessingQueue": true
                        }),
                    })
                    .await
                    .expect("respond to queued command");
                assert!(response.success);

                let missing = session
                    .rpc()
                    .commands()
                    .respond_to_queued_command(CommandsRespondToQueuedCommandRequest {
                        request_id: RequestId::from("missing-command-request"),
                        result: json!({
                            "handled": false,
                            "stopProcessingQueue": false
                        }),
                    })
                    .await
                    .expect("respond to missing queued command");
                assert!(!missing.success);
                session
                    .rpc()
                    .event_log()
                    .release_interest(ReleaseEventInterestParams { handle: interest })
                    .await
                    .expect("release command interest");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct NoopCommandHandler;

#[async_trait]
impl CommandHandler for NoopCommandHandler {
    async fn on_command(&self, _ctx: CommandContext) -> Result<(), github_copilot_sdk::Error> {
        Ok(())
    }
}

struct RecordingCommandHandler {
    tx: mpsc::UnboundedSender<CommandContext>,
}

#[async_trait]
impl CommandHandler for RecordingCommandHandler {
    async fn on_command(&self, ctx: CommandContext) -> Result<(), github_copilot_sdk::Error> {
        self.tx.send(ctx).expect("record command context");
        Ok(())
    }
}

fn assert_command(
    commands: &[github_copilot_sdk::rpc::SlashCommandInfo],
    name: &str,
    kind: SlashCommandKind,
) {
    let command = commands
        .iter()
        .find(|command| command.name == name)
        .unwrap_or_else(|| panic!("missing command {name}; actual commands: {commands:?}"));
    assert_eq!(command.kind, kind);
    assert!(!command.description.trim().is_empty());
}
