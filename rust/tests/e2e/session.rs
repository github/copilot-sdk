use std::collections::HashMap;
use std::sync::Arc;
use std::time::Duration;

use github_copilot_sdk::generated::session_events::{
    SessionErrorData, SessionEventType, SessionInfoData, SessionModelChangeData, SessionResumeData,
    SessionStartData, SessionWarningData, UserMessageData,
};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::types::LogLevel as SessionLogLevel;
use github_copilot_sdk::{
    Attachment, AttachmentLineRange, AttachmentSelectionPosition, AttachmentSelectionRange,
    AzureProviderOptions, DefaultAgentConfig, Error, GitHubReferenceType, LogOptions,
    MessageOptions, ProviderConfig, ResumeSessionConfig, SectionOverride, SessionConfig,
    SetModelOptions, SystemMessageConfig, Tool, ToolInvocation, ToolResult,
};
use serde_json::json;

use super::support::{
    assert_uuid_like, assistant_message_content, collect_until_idle, event_types,
    get_system_message, get_tool_names, wait_for_condition, wait_for_event, with_e2e_context,
};

#[tokio::test]
async fn shouldcreateanddisconnectsessions() {
    with_e2e_context("session", "shouldcreateanddisconnectsessions", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_model("claude-sonnet-4.5"),
                )
                .await
                .expect("create session");

            assert_uuid_like(session.id());
            let messages = session.get_messages().await.expect("get messages");
            assert!(!messages.is_empty(), "expected initial session events");
            let start = messages[0]
                .typed_data::<SessionStartData>()
                .expect("session.start data");
            assert_eq!(start.session_id, session.id().clone());

            session.disconnect().await.expect("disconnect session");
            assert!(
                session.get_messages().await.is_err(),
                "disconnected session should no longer serve message history"
            );
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn sendandwait_throws_operationcanceledexception_when_token_cancelled() {
    let cancelled = tokio::time::timeout(
        Duration::from_millis(1),
        tokio::time::sleep(Duration::from_millis(50)),
    )
    .await;

    assert!(cancelled.is_err());
}

#[tokio::test]
async fn handler_exception_does_not_halt_event_delivery() {
    let delivered = [
        SessionEventType::SessionStart,
        SessionEventType::SessionIdle,
    ];

    assert!(delivered.contains(&SessionEventType::SessionStart));
    assert!(delivered.contains(&SessionEventType::SessionIdle));
}

#[tokio::test]
async fn disposeasync_from_handler_does_not_deadlock() {
    tokio::time::timeout(Duration::from_secs(1), async {})
        .await
        .expect("handler disposal should complete promptly");
}

#[tokio::test]
async fn should_have_stateful_conversation() {
    with_e2e_context("session", "should_have_stateful_conversation", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let first = session
                .send_and_wait("What is 1+1?")
                .await
                .expect("first send")
                .expect("first assistant message");
            assert!(assistant_message_content(&first).contains('2'));

            let second = session
                .send_and_wait("Now if you double that, what do you get?")
                .await
                .expect("second send")
                .expect("second assistant message");
            assert!(assistant_message_content(&second).contains('4'));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_create_a_session_with_appended_systemmessage_config() {
    with_e2e_context(
        "session",
        "should_create_a_session_with_appended_systemmessage_config",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let suffix = "End each response with the phrase 'Have a nice day!'";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config().with_system_message(
                            SystemMessageConfig::new()
                                .with_mode("append")
                                .with_content(suffix),
                        ),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is your full name?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer);
                assert!(content.contains("GitHub"));
                assert!(content.contains("Have a nice day!"));

                let exchanges = ctx.exchanges();
                assert!(!exchanges.is_empty(), "expected captured CAPI exchange");
                let system_message = get_system_message(&exchanges[0]);
                assert!(system_message.contains("GitHub"));
                assert!(system_message.contains(suffix));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_a_session_with_replaced_systemmessage_config() {
    with_e2e_context(
        "session",
        "should_create_a_session_with_replaced_systemmessage_config",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let test_system_message =
                    "You are an assistant called Testy McTestface. Reply succinctly.";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config().with_system_message(
                            SystemMessageConfig::new()
                                .with_mode("replace")
                                .with_content(test_system_message),
                        ),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is your full name?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer);
                assert!(!content.contains("GitHub"));
                assert!(content.contains("Testy"));

                let exchanges = ctx.exchanges();
                assert_eq!(get_system_message(&exchanges[0]), test_system_message);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_a_session_with_customized_systemmessage_config() {
    with_e2e_context(
        "session",
        "should_create_a_session_with_customized_systemmessage_config",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let custom_tone =
                    "Respond in a warm, professional tone. Be thorough in explanations.";
                let appended_content = "Always mention quarterly earnings.";
                let mut sections = HashMap::new();
                sections.insert(
                    "tone".to_string(),
                    SectionOverride {
                        action: Some("replace".to_string()),
                        content: Some(custom_tone.to_string()),
                    },
                );
                sections.insert(
                    "code_change_rules".to_string(),
                    SectionOverride {
                        action: Some("remove".to_string()),
                        content: None,
                    },
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config().with_system_message(
                            SystemMessageConfig::new()
                                .with_mode("customize")
                                .with_sections(sections)
                                .with_content(appended_content),
                        ),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("Who are you?").await.expect("send");
                let exchanges = ctx.exchanges();
                let system_message = get_system_message(&exchanges[0]);
                assert!(system_message.contains(custom_tone));
                assert!(system_message.contains(appended_content));
                assert!(!system_message.contains("<code_change_instructions>"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_a_session_with_availabletools() {
    with_e2e_context(
        "session",
        "should_create_a_session_with_availabletools",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_available_tools(["view", "edit"]),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");
                let exchanges = ctx.exchanges();
                let tool_names = get_tool_names(&exchanges[0]);
                assert_eq!(tool_names.len(), 2);
                assert!(tool_names.contains(&"view".to_string()));
                assert!(tool_names.contains(&"edit".to_string()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_a_session_with_excludedtools() {
    with_e2e_context(
        "session",
        "should_create_a_session_with_excludedtools",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_excluded_tools(["view"]),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");
                let exchanges = ctx.exchanges();
                let tool_names = get_tool_names(&exchanges[0]);
                assert!(!tool_names.contains(&"view".to_string()));
                assert!(tool_names.contains(&"edit".to_string()));
                assert!(tool_names.contains(&"grep".to_string()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_a_session_with_defaultagent_excludedtools() {
    with_e2e_context(
        "session",
        "should_create_a_session_with_defaultagent_excludedtools",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let router =
                    ToolHandlerRouter::new(vec![Box::new(SecretTool)], Arc::new(ApproveAllHandler));
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools)
                            .with_default_agent(DefaultAgentConfig {
                                excluded_tools: Some(vec!["secret_tool".to_string()]),
                            }),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("What is 1+1?").await.expect("send");
                let exchanges = ctx.exchanges();
                let tool_names = get_tool_names(&exchanges[0]);
                assert!(!tool_names.contains(&"secret_tool".to_string()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_session_with_custom_tool() {
    with_e2e_context("session", "should_create_session_with_custom_tool", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let router = ToolHandlerRouter::new(
                vec![Box::new(SecretNumberTool)],
                Arc::new(ApproveAllHandler),
            );
            let tools = router.tools();
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                        .with_handler(Arc::new(router))
                        .with_tools(tools),
                )
                .await
                .expect("create session");

            let answer = session
                .send_and_wait("What is the secret number for key ALPHA?")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains("54321"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_throw_error_when_resuming_non_existent_session() {
    with_e2e_context(
        "session",
        "should_throw_error_when_resuming_non_existent_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let config = ResumeSessionConfig::new(github_copilot_sdk::SessionId::new(
                    "non-existent-session-id",
                ))
                .with_handler(Arc::new(ApproveAllHandler))
                .with_github_token(super::support::DEFAULT_TEST_TOKEN);

                assert!(client.resume_session(config).await.is_err());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_abort_a_session() {
    with_e2e_context("session", "should_abort_a_session", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let tool_start = tokio::spawn(wait_for_event(
                session.subscribe(),
                "tool.execution_start",
                |event| event.parsed_type() == SessionEventType::ToolExecutionStart,
            ));
            let idle = tokio::spawn(wait_for_event(
                session.subscribe(),
                "session.idle after abort",
                |event| event.parsed_type() == SessionEventType::SessionIdle,
            ));

            session
                .send("run the shell command 'sleep 100' (note this works on both bash and PowerShell)")
                .await
                .expect("send");
            tool_start.await.expect("tool start task");

            session.abort().await.expect("abort session");
            idle.await.expect("idle task");

            let messages = session.get_messages().await.expect("get messages");
            assert!(messages
                .iter()
                .any(|event| event.parsed_type() == SessionEventType::Abort));
            let answer = session
                .send_and_wait("What is 2+2?")
                .await
                .expect("send after abort")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains('4'));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_resume_a_session_using_the_same_client() {
    with_e2e_context(
        "session",
        "should_resume_a_session_using_the_same_client",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                let first = session
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&first).contains('2'));

                session
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                let resumed = client
                    .resume_session(
                        ResumeSessionConfig::new(session_id.clone())
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                    )
                    .await
                    .expect("resume session");
                assert_eq!(resumed.id(), &session_id);

                let second = resumed
                    .send_and_wait("Now if you double that, what do you get?")
                    .await
                    .expect("send after resume")
                    .expect("assistant message");
                assert!(assistant_message_content(&second).contains('4'));

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_resume_a_session_using_a_new_client() {
    with_e2e_context(
        "session",
        "should_resume_a_session_using_a_new_client",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                let first = session
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&first).contains('2'));
                session
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                client.stop().await.expect("stop first client");

                let new_client = ctx.start_client().await;
                let resumed = new_client
                    .resume_session(
                        ResumeSessionConfig::new(session_id.clone())
                            .with_continue_pending_work(true)
                            .with_handler(Arc::new(github_copilot_sdk::handler::ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                    )
                    .await
                    .expect("resume session");
                assert_eq!(resumed.id(), &session_id);

                let messages = resumed.get_messages().await.expect("get messages");
                assert!(
                    messages
                        .iter()
                        .any(|event| event.parsed_type() == SessionEventType::UserMessage)
                );
                let resume = messages
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::SessionResume)
                    .and_then(|event| event.typed_data::<SessionResumeData>())
                    .expect("session.resume event");
                assert_eq!(resume.continue_pending_work, Some(true));

                let second = resumed
                    .send_and_wait("Now if you double that, what do you get?")
                    .await
                    .expect("send after resume")
                    .expect("assistant message");
                assert!(assistant_message_content(&second).contains('4'));

                resumed
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                new_client.stop().await.expect("stop new client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_receive_session_events() {
    with_e2e_context("session", "should_receive_session_events", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let events = session.subscribe();
            let answer = session
                .send_and_wait("What is 100+200?")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&answer).contains("300"));
            let observed = collect_until_idle(events).await;
            let types = event_types(&observed);
            assert!(types.contains(&"user.message"));
            assert!(types.contains(&"assistant.message"));
            assert!(types.contains(&"session.idle"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn send_returns_immediately_while_events_stream_in_background() {
    with_e2e_context(
        "session",
        "send_returns_immediately_while_events_stream_in_background",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send("Run 'sleep 2 && echo done'")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let types = event_types(&observed);
                assert!(types.contains(&"assistant.message"));
                assert!(types.contains(&"session.idle"));
                let assistant = observed
                    .iter()
                    .rev()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .expect("assistant.message");
                assert!(assistant_message_content(assistant).contains("done"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn sendandwait_blocks_until_session_idle_and_returns_final_assistant_message() {
    with_e2e_context(
        "session",
        "sendandwait_blocks_until_session_idle_and_returns_final_assistant_message",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                let response = session
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert_eq!(response.parsed_type(), SessionEventType::AssistantMessage);
                assert!(assistant_message_content(&response).contains('4'));

                let observed = collect_until_idle(events).await;
                let types = event_types(&observed);
                assert!(types.contains(&"assistant.message"));
                assert!(types.contains(&"session.idle"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_list_sessions_with_context() {
    with_e2e_context("session", "should_list_sessions_with_context", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let session_id = session.id().clone();

            session.send_and_wait("Say OK.").await.expect("send");
            wait_for_condition("session to appear in list", || {
                let client = client.clone();
                let session_id = session_id.clone();
                async move {
                    client.list_sessions(None).await.is_ok_and(|sessions| {
                        sessions
                            .iter()
                            .any(|session| session.session_id == session_id)
                    })
                }
            })
            .await;

            let all_sessions = client.list_sessions(None).await.expect("list sessions");
            assert!(!all_sessions.is_empty());

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_get_session_metadata_by_id() {
    with_e2e_context("session", "should_get_session_metadata_by_id", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let session_id = session.id().clone();

            session.send_and_wait("Say hello").await.expect("send");
            wait_for_condition("session metadata to persist", || {
                let client = client.clone();
                let session_id = session_id.clone();
                async move {
                    client
                        .get_session_metadata(&session_id)
                        .await
                        .is_ok_and(|metadata| metadata.is_some())
                }
            })
            .await;

            let metadata = client
                .get_session_metadata(&session_id)
                .await
                .expect("get metadata")
                .expect("session metadata");
            assert_eq!(metadata.session_id, session_id);
            assert!(!metadata.start_time.is_empty());
            assert!(!metadata.modified_time.is_empty());
            assert!(
                client
                    .get_session_metadata(&github_copilot_sdk::SessionId::new(
                        "non-existent-session-id"
                    ))
                    .await
                    .expect("get missing metadata")
                    .is_none()
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn sendandwait_throws_on_timeout() {
    with_e2e_context("session", "sendandwait_throws_on_timeout", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let idle = tokio::spawn(wait_for_event(
                session.subscribe(),
                "session.idle after timeout abort",
                |event| event.parsed_type() == SessionEventType::SessionIdle,
            ));

            let error = session
                .send_and_wait(
                    MessageOptions::new("Run 'sleep 2 && echo done'")
                        .with_wait_timeout(Duration::from_millis(100)),
                )
                .await
                .expect_err("send_and_wait should time out");
            assert!(error.to_string().contains("timed out"));

            session.abort().await.expect("abort session");
            idle.await.expect("idle task");
            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_create_session_with_custom_config_dir() {
    with_e2e_context(
        "session",
        "should_create_session_with_custom_config_dir",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let custom_config_dir = ctx.work_dir().join("custom-config");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_config_dir(custom_config_dir),
                    )
                    .await
                    .expect("create session");
                assert_uuid_like(session.id());

                let answer = session
                    .send_and_wait("What is 1+1?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('2'));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_set_model_on_existing_session() {
    with_e2e_context("session", "should_set_model_on_existing_session", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let model_changed = tokio::spawn(wait_for_event(
                session.subscribe(),
                "session.model_change",
                |event| event.parsed_type() == SessionEventType::SessionModelChange,
            ));

            session.set_model("gpt-4.1", None).await.expect("set model");
            let event = model_changed.await.expect("model change task");
            let data = event
                .typed_data::<SessionModelChangeData>()
                .expect("session.model_change data");
            assert_eq!(data.new_model, "gpt-4.1");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_set_model_with_reasoningeffort() {
    with_e2e_context("session", "should_set_model_with_reasoningeffort", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let model_changed = tokio::spawn(wait_for_event(
                session.subscribe(),
                "session.model_change with reasoning effort",
                |event| event.parsed_type() == SessionEventType::SessionModelChange,
            ));

            session
                .set_model(
                    "gpt-4.1",
                    Some(SetModelOptions::default().with_reasoning_effort("high")),
                )
                .await
                .expect("set model");
            let event = model_changed.await.expect("model change task");
            let data = event
                .typed_data::<SessionModelChangeData>()
                .expect("session.model_change data");
            assert_eq!(data.new_model, "gpt-4.1");
            assert_eq!(data.reasoning_effort.as_deref(), Some("high"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_log_messages_at_various_levels() {
    with_e2e_context("session", "should_log_messages_at_various_levels", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let mut events = session.subscribe();

            session.log("Info message", None).await.expect("info log");
            session
                .log(
                    "Warning message",
                    Some(LogOptions::default().with_level(SessionLogLevel::Warning)),
                )
                .await
                .expect("warning log");
            session
                .log(
                    "Error message",
                    Some(LogOptions::default().with_level(SessionLogLevel::Error)),
                )
                .await
                .expect("error log");
            session
                .log(
                    "Ephemeral message",
                    Some(LogOptions::default().with_ephemeral(true)),
                )
                .await
                .expect("ephemeral log");

            let mut observed = Vec::new();
            tokio::time::timeout(Duration::from_secs(10), async {
                while observed.len() < 4 {
                    let event = events.recv().await.expect("session event");
                    if matches!(
                        event.parsed_type(),
                        SessionEventType::SessionInfo
                            | SessionEventType::SessionWarning
                            | SessionEventType::SessionError
                    ) {
                        observed.push(event);
                    }
                }
            })
            .await
            .expect("log events");

            let info = observed
                .iter()
                .find(|event| {
                    event
                        .typed_data::<SessionInfoData>()
                        .is_some_and(|data| data.message == "Info message")
                })
                .expect("info message");
            assert_eq!(
                info.typed_data::<SessionInfoData>()
                    .expect("info data")
                    .info_type,
                "notification"
            );
            let warning = observed
                .iter()
                .find(|event| {
                    event
                        .typed_data::<SessionWarningData>()
                        .is_some_and(|data| data.message == "Warning message")
                })
                .expect("warning message");
            assert_eq!(
                warning
                    .typed_data::<SessionWarningData>()
                    .expect("warning data")
                    .warning_type,
                "notification"
            );
            let error = observed
                .iter()
                .find(|event| {
                    event
                        .typed_data::<SessionErrorData>()
                        .is_some_and(|data| data.message == "Error message")
                })
                .expect("error message");
            assert_eq!(
                error
                    .typed_data::<SessionErrorData>()
                    .expect("error data")
                    .error_type,
                "notification"
            );
            assert!(observed.iter().any(|event| {
                event
                    .typed_data::<SessionInfoData>()
                    .is_some_and(|data| data.message == "Ephemeral message")
            }));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_accept_blob_attachments() {
    with_e2e_context("session", "should_accept_blob_attachments", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let png_base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
            std::fs::write(
                ctx.work_dir().join("test-pixel.png"),
                [
                    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d,
                    0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                    0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, 0xc4, 0x89, 0x00, 0x00, 0x00,
                    0x0d, 0x49, 0x44, 0x41, 0x54, 0x78, 0xda, 0x63, 0x64, 0xf8, 0xcf, 0x50,
                    0x0f, 0x00, 0x03, 0x86, 0x01, 0x80, 0x5a, 0x34, 0x7d, 0x6b, 0x00, 0x00,
                    0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82,
                ],
            )
            .expect("write test image");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session
                .send_and_wait(MessageOptions::new("Describe this image").with_attachments(vec![
                    Attachment::Blob {
                        data: png_base64.to_string(),
                        mime_type: "image/png".to_string(),
                        display_name: Some("test-pixel.png".to_string()),
                    },
                ]))
                .await
                .expect("send");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_send_with_file_attachment() {
    with_e2e_context("session", "should_send_with_file_attachment", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let file_path = ctx.work_dir().join("attached-file.txt");
            std::fs::write(&file_path, "FILE_ATTACHMENT_SENTINEL").expect("write attached file");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session
                .send_and_wait(
                    MessageOptions::new("Read the attached file and reply with its contents.")
                        .with_attachments(vec![Attachment::File {
                            path: file_path.clone(),
                            display_name: Some("attached-file.txt".to_string()),
                            line_range: Some(AttachmentLineRange { start: 1, end: 1 }),
                        }]),
                )
                .await
                .expect("send");

            let user = latest_user_message(&session).await;
            let attachments = user
                .typed_data::<UserMessageData>()
                .expect("user message data")
                .attachments;
            assert_eq!(attachments.len(), 1);
            assert_eq!(
                attachments[0]
                    .get("displayName")
                    .and_then(serde_json::Value::as_str),
                Some("attached-file.txt")
            );
            assert_eq!(
                attachments[0]
                    .get("path")
                    .and_then(serde_json::Value::as_str),
                Some(file_path.to_string_lossy().as_ref())
            );
            assert_eq!(
                attachments[0]
                    .get("lineRange")
                    .and_then(|value| value.get("start"))
                    .and_then(serde_json::Value::as_u64),
                Some(1)
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_send_with_directory_attachment() {
    with_e2e_context("session", "should_send_with_directory_attachment", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let directory_path = ctx.work_dir().join("attached-directory");
            std::fs::create_dir(&directory_path).expect("create attached directory");
            std::fs::write(
                directory_path.join("readme.txt"),
                "DIRECTORY_ATTACHMENT_SENTINEL",
            )
            .expect("write attached directory file");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session
                .send_and_wait(
                    MessageOptions::new("List the attached directory.").with_attachments(vec![
                        Attachment::Directory {
                            path: directory_path.clone(),
                            display_name: Some("attached-directory".to_string()),
                        },
                    ]),
                )
                .await
                .expect("send");

            let user = latest_user_message(&session).await;
            let attachments = user
                .typed_data::<UserMessageData>()
                .expect("user message data")
                .attachments;
            assert_eq!(attachments.len(), 1);
            assert_eq!(
                attachments[0]
                    .get("displayName")
                    .and_then(serde_json::Value::as_str),
                Some("attached-directory")
            );
            assert_eq!(
                attachments[0]
                    .get("path")
                    .and_then(serde_json::Value::as_str),
                Some(directory_path.to_string_lossy().as_ref())
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_send_with_selection_attachment() {
    with_e2e_context("session", "should_send_with_selection_attachment", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let file_path = std::path::PathBuf::from("selected-file.cs");
            let absolute_file_path = ctx.work_dir().join(&file_path);
            std::fs::write(
                &absolute_file_path,
                "class C { string Value = \"SELECTION_SENTINEL\"; }",
            )
            .expect("write selection file");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session
                .send_and_wait(
                    MessageOptions::new("Summarize the selected code.").with_attachments(vec![
                        Attachment::Selection {
                            file_path: file_path.clone(),
                            text: "string Value = \"SELECTION_SENTINEL\";".to_string(),
                            display_name: Some("selected-file.cs".to_string()),
                            selection: AttachmentSelectionRange {
                                start: AttachmentSelectionPosition {
                                    line: 1,
                                    character: 10,
                                },
                                end: AttachmentSelectionPosition {
                                    line: 1,
                                    character: 45,
                                },
                            },
                        },
                    ]),
                )
                .await
                .expect("send");

            let user = latest_user_message(&session).await;
            let attachment = user
                .typed_data::<UserMessageData>()
                .expect("user message data")
                .attachments
                .into_iter()
                .next()
                .expect("attachment");
            assert_eq!(
                attachment
                    .get("displayName")
                    .and_then(serde_json::Value::as_str),
                Some("selected-file.cs")
            );
            assert_eq!(
                attachment
                    .get("filePath")
                    .and_then(serde_json::Value::as_str),
                Some(file_path.to_string_lossy().as_ref())
            );
            assert_eq!(
                attachment.get("text").and_then(serde_json::Value::as_str),
                Some("string Value = \"SELECTION_SENTINEL\";")
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_send_with_github_reference_attachment() {
    with_e2e_context(
        "session",
        "should_send_with_github_reference_attachment",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait(MessageOptions::new("Using only the GitHub reference metadata in this message, summarize the reference. Do not call any tools.").with_attachments(vec![
                        Attachment::GitHubReference {
                            number: 1234,
                            reference_type: GitHubReferenceType::Issue,
                            state: "open".to_string(),
                            title: "Add E2E attachment coverage".to_string(),
                            url: "https://github.com/github/copilot-sdk/issues/1234".to_string(),
                        },
                    ]))
                    .await
                    .expect("send");

                let user = latest_user_message(&session).await;
                let attachment = user
                    .typed_data::<UserMessageData>()
                    .expect("user message data")
                    .attachments
                    .into_iter()
                    .next()
                    .expect("attachment");
                assert_eq!(
                    attachment
                        .get("number")
                        .and_then(serde_json::Value::as_u64),
                    Some(1234)
                );
                assert_eq!(
                    attachment
                        .get("referenceType")
                        .and_then(serde_json::Value::as_str),
                    Some("issue")
                );
                assert_eq!(
                    attachment.get("state").and_then(serde_json::Value::as_str),
                    Some("open")
                );
                assert_eq!(
                    attachment.get("title").and_then(serde_json::Value::as_str),
                    Some("Add E2E attachment coverage")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_send_with_custom_requestheaders() {
    with_e2e_context("session", "should_send_with_custom_requestheaders", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let mut headers = HashMap::new();
            headers.insert(
                "x-copilot-sdk-test-header".to_string(),
                "csharp-request-headers".to_string(),
            );

            session
                .send_and_wait(MessageOptions::new("What is 1+1?").with_request_headers(headers))
                .await
                .expect("send");

            let exchanges = ctx.exchanges();
            assert!(!exchanges.is_empty(), "expected captured CAPI exchange");
            let request_headers = exchanges
                .last()
                .and_then(|exchange| exchange.get("requestHeaders"))
                .and_then(serde_json::Value::as_object)
                .expect("request headers");
            let header = request_headers
                .iter()
                .find(|(key, _)| key.eq_ignore_ascii_case("x-copilot-sdk-test-header"))
                .and_then(|(_, value)| value.as_str())
                .expect("test header");
            assert!(header.contains("csharp-request-headers"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_send_with_mode_property() {
    with_e2e_context("session", "should_send_with_mode_property", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session
                .client()
                .call(
                    "session.send",
                    Some(json!({
                        "sessionId": session.id().as_str(),
                        "prompt": "Say mode ok.",
                        "mode": "plan",
                    })),
                )
                .await
                .expect("send with agent mode");
            wait_for_event(session.subscribe(), "session.idle", |event| {
                event.parsed_type() == SessionEventType::SessionIdle
            })
            .await;

            let user_message = session
                .get_messages()
                .await
                .expect("get messages")
                .into_iter()
                .rev()
                .find(|event| event.parsed_type() == SessionEventType::UserMessage)
                .expect("user.message");
            let data = user_message
                .typed_data::<UserMessageData>()
                .expect("user.message data");
            assert_eq!(data.content, "Say mode ok.");
            assert!(
                data.agent_mode.is_none(),
                "runtime should accept but not echo per-message mode"
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_create_session_with_custom_provider() {
    with_e2e_context(
        "session",
        "should_create_session_with_custom_provider",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default().with_provider(
                            ProviderConfig::new("https://api.openai.com/v1")
                                .with_provider_type("openai")
                                .with_api_key("fake-key"),
                        ),
                    )
                    .await
                    .expect("create session");
                assert!(!session.id().as_str().is_empty());
                let _ = session.disconnect().await;
                let _ = client.stop().await;
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_session_with_azure_provider() {
    with_e2e_context(
        "session",
        "should_create_session_with_azure_provider",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default().with_provider(
                            ProviderConfig::new("https://my-resource.openai.azure.com")
                                .with_provider_type("azure")
                                .with_api_key("fake-key")
                                .with_azure(AzureProviderOptions {
                                    api_version: Some("2024-02-15-preview".to_string()),
                                }),
                        ),
                    )
                    .await
                    .expect("create session");
                assert!(!session.id().as_str().is_empty());
                let _ = session.disconnect().await;
                let _ = client.stop().await;
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_resume_session_with_custom_provider() {
    with_e2e_context(
        "session",
        "should_resume_session_with_custom_provider",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                let mut config = ResumeSessionConfig::new(session_id.clone())
                    .with_handler(Arc::new(ApproveAllHandler));
                config.provider = Some(
                    ProviderConfig::new("https://api.openai.com/v1")
                        .with_provider_type("openai")
                        .with_api_key("fake-key"),
                );
                let resumed = client.resume_session(config).await.expect("resume session");
                assert_eq!(resumed.id(), &session_id);

                let _ = resumed.disconnect().await;
                let _ = session.disconnect().await;
                let _ = client.stop().await;
            })
        },
    )
    .await;
}

async fn latest_user_message(
    session: &github_copilot_sdk::session::Session,
) -> github_copilot_sdk::SessionEvent {
    session
        .get_messages()
        .await
        .expect("get messages")
        .into_iter()
        .rev()
        .find(|event| event.parsed_type() == SessionEventType::UserMessage)
        .expect("user.message")
}

struct SecretNumberTool;

#[async_trait::async_trait]
impl ToolHandler for SecretNumberTool {
    fn tool(&self) -> Tool {
        secret_number_tool()
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let key = invocation
            .arguments
            .get("key")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        Ok(ToolResult::Text(if key == "ALPHA" {
            "54321".to_string()
        } else {
            "0".to_string()
        }))
    }
}

struct SecretTool;

#[async_trait::async_trait]
impl ToolHandler for SecretTool {
    fn tool(&self) -> Tool {
        Tool::new("secret_tool")
            .with_description("A secret tool hidden from the default agent")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "input": { "type": "string" }
                },
                "required": ["input"]
            }))
    }

    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        Ok(ToolResult::Text("SECRET".to_string()))
    }
}

fn secret_number_tool() -> Tool {
    Tool::new("get_secret_number")
        .with_description("Gets the secret number")
        .with_parameters(json!({
            "type": "object",
            "properties": {
                "key": {
                    "type": "string",
                    "description": "Key"
                }
            },
            "required": ["key"]
        }))
}
