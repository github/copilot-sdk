use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::{SessionHandler, UserInputResponse};
use github_copilot_sdk::{RequestId, SessionConfig, SessionId};
use tokio::sync::mpsc;

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
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingUserInputHandler {
                                request_tx,
                                answer: UserInputAnswer::FirstChoiceOrFreeform("freeform answer"),
                            })),
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
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingUserInputHandler {
                                request_tx,
                                answer: UserInputAnswer::FirstChoiceOrFreeform("default"),
                            })),
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
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(RecordingUserInputHandler {
                                request_tx,
                                answer: UserInputAnswer::Freeform(freeform_answer),
                            })),
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
impl SessionHandler for RecordingUserInputHandler {
    async fn on_user_input(
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

    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: github_copilot_sdk::PermissionRequestData,
    ) -> github_copilot_sdk::handler::PermissionResult {
        github_copilot_sdk::handler::PermissionResult::Approved
    }
}
