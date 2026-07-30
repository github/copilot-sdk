use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::handler::{
    ApproveAllHandler, PermissionHandler, PermissionResult, UserInputHandler, UserInputResponse,
};
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{
    Error, RequestId, SessionConfig, SessionId, Tool, ToolInvocation, ToolResult,
};
use serde_json::json;
use tokio::sync::{Notify, mpsc};

use super::support::{
    DEFAULT_TEST_TOKEN, assistant_message_content, recv_with_timeout, with_e2e_context,
};

#[tokio::test]
async fn should_invoke_user_input_handler_when_model_uses_ask_user_tool() {
    with_e2e_context(
        "ask_user",
        "should_invoke_user_input_handler_when_model_uses_ask_user_tool",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let handler = Arc::new(RecordingUserInputHandler {
                    request_tx,
                    answer: UserInputAnswer::FirstChoiceOrFreeform("freeform answer"),
                });
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_user_input_handler(handler.clone() as Arc<dyn UserInputHandler>)
                            .with_permission_handler(handler as Arc<dyn PermissionHandler>),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        "Ask me to choose between 'Option A' and 'Option B' using the ask_user tool. \
                         Wait for my response before continuing.",
                    )
                    .await
                    .expect("send");

                let request = recv_with_timeout(&mut request_rx, "user input request").await;
                assert_eq!(request.session_id, *session.id());
                assert!(!request.question.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_receive_choices_in_user_input_request() {
    with_e2e_context(
        "ask_user",
        "should_receive_choices_in_user_input_request",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let handler = Arc::new(RecordingUserInputHandler {
                    request_tx,
                    answer: UserInputAnswer::FirstChoiceOrFreeform("default"),
                });
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_user_input_handler(handler.clone() as Arc<dyn UserInputHandler>)
                            .with_permission_handler(handler as Arc<dyn PermissionHandler>),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        "Use the ask_user tool to ask me to pick between exactly two options: \
                         'Red' and 'Blue'. These should be provided as choices. Wait for my answer.",
                    )
                    .await
                    .expect("send");

                let request = recv_with_timeout(&mut request_rx, "user input request").await;
                let choices = request.choices.expect("choices");
                assert!(choices.iter().any(|choice| choice == "Red"));
                assert!(choices.iter().any(|choice| choice == "Blue"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_freeform_user_input_response() {
    with_e2e_context(
        "ask_user",
        "should_handle_freeform_user_input_response",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let freeform_answer =
                    "This is my custom freeform answer that was not in the choices";
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let handler = Arc::new(RecordingUserInputHandler {
                    request_tx,
                    answer: UserInputAnswer::Freeform(freeform_answer),
                });
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_user_input_handler(handler.clone() as Arc<dyn UserInputHandler>)
                            .with_permission_handler(handler as Arc<dyn PermissionHandler>),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Ask me a question using ask_user and then include my answer in your response. \
                         The question should be 'What is your favorite color?'",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");

                let request = recv_with_timeout(&mut request_rx, "user input request").await;
                assert!(!request.question.is_empty());
                assert!(assistant_message_content(&answer).contains(freeform_answer));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

/// Regression test for the per-session event-loop starvation bug where a pending
/// `ask_user` (`userInput.request`) blocked the `tokio::select!` loop and starved
/// a sibling tool call co-emitted in the same turn (github/copilot-experiences#12540).
///
/// The model emits both `set_marker` and `ask_user` in one assistant turn. The
/// `set_marker` tool fires a `Notify`; the user-input handler waits on that
/// `Notify` before answering. If `ask_user` were awaited inline, the loop could
/// never dispatch the `set_marker` notification, so the handler would never
/// observe the tool firing. With the handler spawned, both run concurrently and
/// the handler observes the sibling tool while its own request is still pending.
#[tokio::test]
async fn ask_user_does_not_block_sibling_tool_call_in_same_turn() {
    with_e2e_context(
        "ask_user",
        "ask_user_does_not_block_sibling_tool_call_in_same_turn",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;

                // Fired by `set_marker` when the sibling tool executes.
                let tool_fired = Arc::new(Notify::new());
                // Reports whether the user-input handler observed the sibling tool
                // firing while its own `ask_user` request was still pending.
                let (observed_tx, mut observed_rx) = mpsc::unbounded_channel();

                let user_input_handler = Arc::new(SiblingAwareUserInputHandler {
                    tool_fired: tool_fired.clone(),
                    observed_tx,
                });
                let tools = vec![set_marker_tool(tool_fired.clone())];

                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_user_input_handler(
                                user_input_handler as Arc<dyn UserInputHandler>,
                            )
                            .with_tools(tools),
                    )
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        "Call set_marker with value 'go' and, at the same time, use the ask_user \
                         tool to ask me to choose between 'Option A' and 'Option B'. Wait for my \
                         answer before continuing.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");

                let observed =
                    recv_with_timeout(&mut observed_rx, "user input handler observation").await;
                assert!(
                    observed,
                    "ask_user handler must observe the sibling set_marker tool executing while \
                     its own userInput.request is still pending (event loop must not be starved)"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[derive(Debug)]
struct RecordedUserInputRequest {
    session_id: SessionId,
    question: String,
    choices: Option<Vec<String>>,
}

struct RecordingUserInputHandler {
    request_tx: mpsc::UnboundedSender<RecordedUserInputRequest>,
    answer: UserInputAnswer,
}

enum UserInputAnswer {
    FirstChoiceOrFreeform(&'static str),
    Freeform(&'static str),
}

#[async_trait]
impl UserInputHandler for RecordingUserInputHandler {
    async fn handle(
        &self,
        session_id: SessionId,
        question: String,
        choices: Option<Vec<String>>,
        allow_freeform: Option<bool>,
    ) -> Option<UserInputResponse> {
        let _ = self.request_tx.send(RecordedUserInputRequest {
            session_id,
            question,
            choices: choices.clone(),
        });
        let (answer, was_freeform) = match (&self.answer, choices.as_ref().and_then(|c| c.first()))
        {
            (UserInputAnswer::FirstChoiceOrFreeform(_), Some(choice)) => (choice.clone(), false),
            (UserInputAnswer::FirstChoiceOrFreeform(fallback), None) => {
                ((*fallback).to_string(), allow_freeform.unwrap_or(true))
            }
            (UserInputAnswer::Freeform(answer), _) => ((*answer).to_string(), true),
        };
        Some(UserInputResponse {
            answer,
            was_freeform,
        })
    }
}

#[async_trait]
impl PermissionHandler for RecordingUserInputHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: github_copilot_sdk::PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::approve_once()
    }
}

/// A user-input handler that waits for a sibling tool to fire before answering,
/// then reports whether it observed that tool while its own request was pending.
struct SiblingAwareUserInputHandler {
    tool_fired: Arc<Notify>,
    observed_tx: mpsc::UnboundedSender<bool>,
}

#[async_trait]
impl UserInputHandler for SiblingAwareUserInputHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _question: String,
        choices: Option<Vec<String>>,
        _allow_freeform: Option<bool>,
    ) -> Option<UserInputResponse> {
        // Wait (bounded) for the sibling `set_marker` tool to execute. On the
        // buggy inline-await path the event loop is parked here, the tool
        // notification is never dispatched, and this times out.
        let observed = tokio::time::timeout(Duration::from_secs(30), self.tool_fired.notified())
            .await
            .is_ok();
        let _ = self.observed_tx.send(observed);

        let answer = choices
            .as_ref()
            .and_then(|c| c.first())
            .cloned()
            .unwrap_or_else(|| "Option A".to_string());
        Some(UserInputResponse {
            answer,
            was_freeform: false,
        })
    }
}

struct SetMarkerTool {
    tool_fired: Arc<Notify>,
}

fn set_marker_tool(tool_fired: Arc<Notify>) -> Tool {
    Tool::new("set_marker")
        .with_description("Records a marker value")
        .with_parameters(json!({
            "type": "object",
            "properties": {
                "value": { "type": "string", "description": "Marker value" }
            },
            "required": ["value"]
        }))
        .with_handler(Arc::new(SetMarkerTool { tool_fired }))
}

#[async_trait]
impl ToolHandler for SetMarkerTool {
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let value = invocation
            .arguments
            .get("value")
            .and_then(serde_json::Value::as_str)
            .unwrap_or_default();
        self.tool_fired.notify_one();
        Ok(ToolResult::Text(format!("MARKER_{}", value.to_uppercase())))
    }
}
