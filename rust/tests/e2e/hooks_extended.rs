use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::hooks::{
    ErrorOccurredInput, ErrorOccurredOutput, HookContext, PostToolUseInput, PostToolUseOutput,
    PreToolUseInput, PreToolUseOutput, SessionEndInput, SessionEndOutput, SessionHooks,
    SessionStartInput, SessionStartOutput, UserPromptSubmittedInput, UserPromptSubmittedOutput,
};
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::{Error, SessionConfig, Tool, ToolInvocation, ToolResult};
use serde_json::json;
use tokio::sync::mpsc;

use super::support::{assistant_message_content, recv_with_timeout, with_e2e_context};

#[tokio::test]
async fn should_invoke_onsessionstart_hook_on_new_session() {
    with_e2e_context(
        "hooks_extended",
        "should_invoke_onsessionstart_hook_on_new_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(RecordingHooks::session_start(tx, None))),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("Say hi").await.expect("send");
                let input = recv_with_timeout(&mut rx, "sessionStart hook").await;
                assert_eq!(input.source, "new");
                assert!(input.timestamp > 0);
                assert!(!input.cwd.as_os_str().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_onuserpromptsubmitted_hook_when_sending_a_message() {
    with_e2e_context(
        "hooks_extended",
        "should_invoke_onuserpromptsubmitted_hook_when_sending_a_message",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(RecordingHooks::user_prompt(tx, None))),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("Say hello").await.expect("send");
                let input = recv_with_timeout(&mut rx, "userPromptSubmitted hook").await;
                assert!(input.prompt.contains("Say hello"));
                assert!(input.timestamp > 0);
                assert!(!input.cwd.as_os_str().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_onsessionend_hook_when_session_is_disconnected() {
    with_e2e_context(
        "hooks_extended",
        "should_invoke_onsessionend_hook_when_session_is_disconnected",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(RecordingHooks::session_end(tx, None))),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("Say hi").await.expect("send");
                session.disconnect().await.expect("disconnect session");
                let input = recv_with_timeout(&mut rx, "sessionEnd hook").await;
                assert!(input.timestamp > 0);
                assert!(!input.cwd.as_os_str().is_empty());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_onerroroccurred_hook_when_error_occurs() {
    with_e2e_context(
        "hooks_extended",
        "should_invoke_onerroroccurred_hook_when_error_occurs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(RecordingHooks::error(tx, None))),
                    )
                    .await
                    .expect("create session");

                session.send_and_wait("Say hi").await.expect("send");
                assert!(rx.try_recv().is_err());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_userpromptsubmitted_hook_and_modify_prompt() {
    with_e2e_context(
        "hooks_extended",
        "should_invoke_userpromptsubmitted_hook_and_modify_prompt",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks::user_prompt(
                            tx,
                            Some(UserPromptSubmittedOutput {
                                modified_prompt: Some(
                                    "Reply with exactly: HOOKED_PROMPT".to_string(),
                                ),
                                ..UserPromptSubmittedOutput::default()
                            }),
                        ),
                    )))
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Say something else")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let input = recv_with_timeout(&mut rx, "userPromptSubmitted hook").await;
                assert!(input.prompt.contains("Say something else"));
                assert!(assistant_message_content(&answer).contains("HOOKED_PROMPT"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_sessionstart_hook() {
    with_e2e_context("hooks_extended", "should_invoke_sessionstart_hook", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let (tx, mut rx) = mpsc::unbounded_channel();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                    RecordingHooks::session_start(
                        tx,
                        Some(SessionStartOutput {
                            additional_context: Some("Session start hook context.".to_string()),
                            ..SessionStartOutput::default()
                        }),
                    ),
                )))
                .await
                .expect("create session");

            session.send_and_wait("Say hi").await.expect("send");
            let input = recv_with_timeout(&mut rx, "sessionStart hook").await;
            assert_eq!(input.source, "new");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_invoke_sessionend_hook() {
    with_e2e_context("hooks_extended", "should_invoke_sessionend_hook", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let (tx, mut rx) = mpsc::unbounded_channel();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                    RecordingHooks::session_end(
                        tx,
                        Some(SessionEndOutput {
                            session_summary: Some("session ended".to_string()),
                            ..SessionEndOutput::default()
                        }),
                    ),
                )))
                .await
                .expect("create session");

            session.send_and_wait("Say bye").await.expect("send");
            session.disconnect().await.expect("disconnect session");
            let input = recv_with_timeout(&mut rx, "sessionEnd hook").await;
            assert!(input.timestamp > 0);

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_register_erroroccurred_hook() {
    with_e2e_context(
        "hooks_extended",
        "should_register_erroroccurred_hook",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks::error(
                            tx,
                            Some(ErrorOccurredOutput {
                                error_handling: Some("skip".to_string()),
                                ..ErrorOccurredOutput::default()
                            }),
                        ),
                    )))
                    .await
                    .expect("create session");

                session.send_and_wait("Say hi").await.expect("send");
                assert!(rx.try_recv().is_err());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_allow_pretooluse_to_return_modifiedargs_and_suppressoutput() {
    with_e2e_context(
        "hooks_extended",
        "should_allow_pretooluse_to_return_modifiedargs_and_suppressoutput",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let router = ToolHandlerRouter::new(
                    vec![Box::new(EchoValueTool)],
                    Arc::new(ApproveAllHandler),
                );
                let tools = router.tools();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools)
                            .with_hooks(Arc::new(RecordingHooks::pre_tool(tx))),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Call echo_value with value 'original', then reply with the result.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let mut saw_echo = false;
                while let Ok(input) = rx.try_recv() {
                    saw_echo |= input.tool_name == "echo_value";
                }
                assert!(saw_echo, "expected preToolUse hook for echo_value");
                assert!(assistant_message_content(&answer).contains("modified by hook"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_allow_posttooluse_to_return_modifiedresult() {
    with_e2e_context(
        "hooks_extended",
        "should_allow_posttooluse_to_return_modifiedresult",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_available_tools(["report_intent"])
                            .with_hooks(Arc::new(RecordingHooks::post_tool(tx))),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Call the report_intent tool with intent 'Testing post hook', then reply done.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let mut saw_report_intent = false;
                while let Ok(input) = rx.try_recv() {
                    saw_report_intent |= input.tool_name == "report_intent";
                }
                assert!(saw_report_intent, "expected postToolUse hook for report_intent");
                assert_eq!(assistant_message_content(&answer), "Done.");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[derive(Default)]
struct RecordingHooks {
    session_start: Option<mpsc::UnboundedSender<SessionStartInput>>,
    session_start_output: Option<SessionStartOutput>,
    session_end: Option<mpsc::UnboundedSender<SessionEndInput>>,
    session_end_output: Option<SessionEndOutput>,
    user_prompt: Option<mpsc::UnboundedSender<UserPromptSubmittedInput>>,
    user_prompt_output: Option<UserPromptSubmittedOutput>,
    error: Option<mpsc::UnboundedSender<ErrorOccurredInput>>,
    error_output: Option<ErrorOccurredOutput>,
    pre_tool: Option<mpsc::UnboundedSender<PreToolUseInput>>,
    post_tool: Option<mpsc::UnboundedSender<PostToolUseInput>>,
}

impl RecordingHooks {
    fn session_start(
        tx: mpsc::UnboundedSender<SessionStartInput>,
        output: Option<SessionStartOutput>,
    ) -> Self {
        Self {
            session_start: Some(tx),
            session_start_output: output,
            ..Self::default()
        }
    }

    fn session_end(
        tx: mpsc::UnboundedSender<SessionEndInput>,
        output: Option<SessionEndOutput>,
    ) -> Self {
        Self {
            session_end: Some(tx),
            session_end_output: output,
            ..Self::default()
        }
    }

    fn user_prompt(
        tx: mpsc::UnboundedSender<UserPromptSubmittedInput>,
        output: Option<UserPromptSubmittedOutput>,
    ) -> Self {
        Self {
            user_prompt: Some(tx),
            user_prompt_output: output,
            ..Self::default()
        }
    }

    fn error(
        tx: mpsc::UnboundedSender<ErrorOccurredInput>,
        output: Option<ErrorOccurredOutput>,
    ) -> Self {
        Self {
            error: Some(tx),
            error_output: output,
            ..Self::default()
        }
    }

    fn pre_tool(tx: mpsc::UnboundedSender<PreToolUseInput>) -> Self {
        Self {
            pre_tool: Some(tx),
            ..Self::default()
        }
    }

    fn post_tool(tx: mpsc::UnboundedSender<PostToolUseInput>) -> Self {
        Self {
            post_tool: Some(tx),
            ..Self::default()
        }
    }
}

#[async_trait]
impl SessionHooks for RecordingHooks {
    async fn on_session_start(
        &self,
        input: SessionStartInput,
        ctx: HookContext,
    ) -> Option<SessionStartOutput> {
        assert!(!ctx.session_id.as_str().is_empty());
        if let Some(tx) = &self.session_start {
            let _ = tx.send(input);
        }
        self.session_start_output.clone()
    }

    async fn on_session_end(
        &self,
        input: SessionEndInput,
        ctx: HookContext,
    ) -> Option<SessionEndOutput> {
        assert!(!ctx.session_id.as_str().is_empty());
        if let Some(tx) = &self.session_end {
            let _ = tx.send(input);
        }
        self.session_end_output.clone()
    }

    async fn on_user_prompt_submitted(
        &self,
        input: UserPromptSubmittedInput,
        ctx: HookContext,
    ) -> Option<UserPromptSubmittedOutput> {
        assert!(!ctx.session_id.as_str().is_empty());
        if let Some(tx) = &self.user_prompt {
            let _ = tx.send(input);
        }
        self.user_prompt_output.clone()
    }

    async fn on_error_occurred(
        &self,
        input: ErrorOccurredInput,
        ctx: HookContext,
    ) -> Option<ErrorOccurredOutput> {
        assert!(!ctx.session_id.as_str().is_empty());
        assert!(
            ["model_call", "tool_execution", "system", "user_input"]
                .contains(&input.error_context.as_str())
        );
        if let Some(tx) = &self.error {
            let _ = tx.send(input);
        }
        self.error_output.clone()
    }

    async fn on_pre_tool_use(
        &self,
        input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        let output = if input.tool_name == "echo_value" {
            PreToolUseOutput {
                permission_decision: Some("allow".to_string()),
                modified_args: Some(json!({ "value": "modified by hook" })),
                suppress_output: Some(false),
                ..PreToolUseOutput::default()
            }
        } else {
            PreToolUseOutput {
                permission_decision: Some("allow".to_string()),
                ..PreToolUseOutput::default()
            }
        };
        if let Some(tx) = &self.pre_tool {
            let _ = tx.send(input);
        }
        Some(output)
    }

    async fn on_post_tool_use(
        &self,
        input: PostToolUseInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseOutput> {
        let output = (input.tool_name == "report_intent").then(|| PostToolUseOutput {
            modified_result: Some(json!("modified by post hook")),
            suppress_output: Some(false),
            ..PostToolUseOutput::default()
        });
        if let Some(tx) = &self.post_tool {
            let _ = tx.send(input);
        }
        output
    }
}

struct EchoValueTool;

#[async_trait]
impl ToolHandler for EchoValueTool {
    fn tool(&self) -> Tool {
        Tool::new("echo_value")
            .with_description("Echoes the supplied value")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "value": { "type": "string" }
                },
                "required": ["value"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        Ok(ToolResult::Text(
            invocation
                .arguments
                .get("value")
                .and_then(serde_json::Value::as_str)
                .unwrap_or_default()
                .to_string(),
        ))
    }
}
