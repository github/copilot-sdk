use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::{
    CommandContext, CommandDefinition, CommandHandler, ResumeSessionConfig, SessionConfig,
    SessionId,
};

use super::support::{DEFAULT_TEST_TOKEN, assert_uuid_like, with_e2e_context};

#[tokio::test]
async fn session_with_commands_creates_successfully() {
    with_e2e_context(
        "commands",
        "session_with_commands_creates_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_commands(vec![
                            CommandDefinition::new("deploy", Arc::new(NoopCommandHandler))
                                .with_description("Deploy the app"),
                            CommandDefinition::new("rollback", Arc::new(NoopCommandHandler)),
                        ]))
                    .await
                    .expect("create session");

                assert_uuid_like(session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_with_commands_resumes_successfully() {
    with_e2e_context(
        "commands",
        "session_with_commands_resumes_successfully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                session.send_and_wait("Say OK.").await.expect("send");
                session
                    .disconnect()
                    .await
                    .expect("disconnect first session");

                let resumed = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id.clone())
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_commands(vec![
                                CommandDefinition::new("deploy", Arc::new(NoopCommandHandler))
                                    .with_description("Deploy"),
                            ]),
                    )
                    .await
                    .expect("resume session");

                assert_eq!(*resumed.id(), session_id);

                resumed.disconnect().await.expect("disconnect resumed");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn session_with_no_commands_creates_successfully() {
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

                assert_uuid_like(session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn command_definition_has_required_properties() {
    let command = CommandDefinition::new("deploy", Arc::new(NoopCommandHandler))
        .with_description("Deploy the app");
    assert_eq!(command.name, "deploy");
    assert_eq!(command.description.as_deref(), Some("Deploy the app"));
}

#[tokio::test]
async fn command_definition_without_description_uses_none() {
    let command = CommandDefinition::new("deploy", Arc::new(NoopCommandHandler));

    assert_eq!(command.name, "deploy");
    assert_eq!(command.description, None);
}

#[tokio::test]
async fn session_config_commands_are_cloned() {
    let config = SessionConfig::default().with_commands(vec![CommandDefinition::new(
        "deploy",
        Arc::new(NoopCommandHandler),
    )]);

    let mut clone = config.clone();

    let clone_commands = clone.commands.as_mut().expect("cloned commands");
    assert_eq!(clone_commands.len(), 1);
    assert_eq!(clone_commands[0].name, "deploy");

    clone_commands.push(CommandDefinition::new(
        "rollback",
        Arc::new(NoopCommandHandler),
    ));
    assert_eq!(
        config.commands.as_ref().expect("original commands").len(),
        1
    );
}

#[tokio::test]
async fn resume_config_commands_are_cloned() {
    let config = ResumeSessionConfig::new(SessionId::from("session-1")).with_commands(vec![
        CommandDefinition::new("deploy", Arc::new(NoopCommandHandler)),
    ]);

    let clone = config.clone();

    let clone_commands = clone.commands.as_ref().expect("cloned commands");
    assert_eq!(clone_commands.len(), 1);
    assert_eq!(clone_commands[0].name, "deploy");
}

struct NoopCommandHandler;

#[async_trait]
impl CommandHandler for NoopCommandHandler {
    async fn on_command(&self, _ctx: CommandContext) -> Result<(), github_copilot_sdk::Error> {
        Ok(())
    }
}
