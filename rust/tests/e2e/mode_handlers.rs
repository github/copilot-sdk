use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::generated::session_events::{
    AutoModeSwitchCompletedData, AutoModeSwitchRequestedData, ExitPlanModeCompletedData,
    ExitPlanModeRequestedData, SessionEventType, SessionModelChangeData,
};
use github_copilot_sdk::handler::{AutoModeSwitchResponse, ExitPlanModeResult, SessionHandler};
use github_copilot_sdk::{ExitPlanModeData, SessionConfig, SessionId};
use serde_json::json;
use tokio::sync::mpsc;

use super::support::{
    recv_with_timeout, wait_for_event, wait_for_event_allowing_rate_limit, with_e2e_context,
};

const MODE_HANDLER_TOKEN: &str = "mode-handler-token";
const PLAN_SUMMARY: &str = "Greeting file implementation plan";
const PLAN_PROMPT: &str = "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode.";
const AUTO_MODE_PROMPT: &str =
    "Explain that auto mode recovered from a rate limit in one short sentence.";

#[derive(Debug)]
struct ModeHandler {
    requests: mpsc::UnboundedSender<(SessionId, ExitPlanModeData)>,
}

#[derive(Debug)]
struct AutoModeHandler {
    requests: mpsc::UnboundedSender<(SessionId, Option<String>, Option<f64>)>,
}

#[async_trait]
impl SessionHandler for ModeHandler {
    async fn on_exit_plan_mode(
        &self,
        session_id: SessionId,
        data: ExitPlanModeData,
    ) -> ExitPlanModeResult {
        let _ = self.requests.send((session_id, data));
        ExitPlanModeResult {
            approved: true,
            selected_action: Some("interactive".to_string()),
            feedback: Some("Approved by the Rust E2E test".to_string()),
        }
    }
}

#[async_trait]
impl SessionHandler for AutoModeHandler {
    async fn on_auto_mode_switch(
        &self,
        session_id: SessionId,
        error_code: Option<String>,
        retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse {
        let _ = self
            .requests
            .send((session_id, error_code, retry_after_seconds));
        AutoModeSwitchResponse::Yes
    }
}

#[tokio::test]
async fn should_invoke_exit_plan_mode_handler_when_model_uses_tool() {
    with_e2e_context(
        "mode_handlers",
        "should_invoke_exit_plan_mode_handler_when_model_uses_tool",
        |ctx| {
            Box::pin(async move {
                ctx.set_copilot_user_by_token(MODE_HANDLER_TOKEN);
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(MODE_HANDLER_TOKEN)
                            .with_handler(Arc::new(ModeHandler {
                                requests: request_tx,
                            }))
                            .approve_all_permissions(),
                    )
                    .await
                    .expect("create session");

                let requested_event = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "exit_plan_mode.requested event",
                    |event| {
                        event.parsed_type() == SessionEventType::ExitPlanModeRequested
                            && event
                                .typed_data::<ExitPlanModeRequestedData>()
                                .is_some_and(|data| data.summary == PLAN_SUMMARY)
                    },
                ));
                let completed_event = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "exit_plan_mode.completed event",
                    |event| {
                        event.parsed_type() == SessionEventType::ExitPlanModeCompleted
                            && event
                                .typed_data::<ExitPlanModeCompletedData>()
                                .is_some_and(|data| {
                                    data.approved == Some(true)
                                        && data.selected_action.as_deref() == Some("interactive")
                                })
                    },
                ));
                let idle_event = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "session.idle event",
                    |event| event.parsed_type() == SessionEventType::SessionIdle,
                ));

                let send_result = session
                    .client()
                    .call(
                        "session.send",
                        Some(json!({
                            "sessionId": session.id().as_str(),
                            "prompt": PLAN_PROMPT,
                            "mode": "plan",
                        })),
                    )
                    .await
                    .expect("send plan-mode prompt");
                assert!(
                    send_result.get("messageId").is_some(),
                    "expected messageId in send result"
                );

                let (session_id, request) =
                    recv_with_timeout(&mut request_rx, "exit-plan-mode request").await;
                assert_eq!(session_id, session.id().clone());
                assert_eq!(request.summary, PLAN_SUMMARY);
                assert_eq!(
                    request.actions,
                    ["interactive", "autopilot", "exit_only"].map(str::to_string)
                );
                assert_eq!(request.recommended_action, "interactive");

                let requested = requested_event.await.expect("requested task");
                let requested_data = requested
                    .typed_data::<ExitPlanModeRequestedData>()
                    .expect("typed requested event");
                assert_eq!(requested_data.summary, request.summary);
                assert_eq!(requested_data.actions, request.actions);
                assert_eq!(
                    requested_data.recommended_action,
                    request.recommended_action
                );

                let completed = completed_event.await.expect("completed task");
                let completed_data = completed
                    .typed_data::<ExitPlanModeCompletedData>()
                    .expect("typed completed event");
                assert_eq!(completed_data.approved, Some(true));
                assert_eq!(
                    completed_data.selected_action.as_deref(),
                    Some("interactive")
                );
                assert_eq!(
                    completed_data.feedback.as_deref(),
                    Some("Approved by the Rust E2E test")
                );
                idle_event.await.expect("idle task");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_auto_mode_switch_handler_when_rate_limited() {
    with_e2e_context(
        "mode_handlers",
        "should_invoke_auto_mode_switch_handler_when_rate_limited",
        |ctx| {
            Box::pin(async move {
                ctx.set_copilot_user_by_token(MODE_HANDLER_TOKEN);
                let client = ctx.start_client().await;
                let (request_tx, mut request_rx) = mpsc::unbounded_channel();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(MODE_HANDLER_TOKEN)
                            .with_handler(Arc::new(AutoModeHandler {
                                requests: request_tx,
                            }))
                            .approve_all_permissions(),
                    )
                    .await
                    .expect("create session");

                let requested_event = tokio::spawn(wait_for_event_allowing_rate_limit(
                    session.subscribe(),
                    "auto_mode_switch.requested event",
                    |event| {
                        event.parsed_type() == SessionEventType::AutoModeSwitchRequested
                            && event
                                .typed_data::<AutoModeSwitchRequestedData>()
                                .is_some_and(|data| {
                                    data.error_code.as_deref() == Some("user_weekly_rate_limited")
                                        && data.retry_after_seconds == Some(1.0)
                                })
                    },
                ));
                let completed_event = tokio::spawn(wait_for_event_allowing_rate_limit(
                    session.subscribe(),
                    "auto_mode_switch.completed event",
                    |event| {
                        event.parsed_type() == SessionEventType::AutoModeSwitchCompleted
                            && event
                                .typed_data::<AutoModeSwitchCompletedData>()
                                .is_some_and(|data| data.response == "yes")
                    },
                ));
                let model_change_event =
                    tokio::spawn(wait_for_event_allowing_rate_limit(
                        session.subscribe(),
                        "rate-limit auto-mode model change",
                        |event| {
                            event.parsed_type() == SessionEventType::SessionModelChange
                                && event.typed_data::<SessionModelChangeData>().is_some_and(
                                    |data| data.cause.as_deref() == Some("rate_limit_auto_switch"),
                                )
                        },
                    ));
                let idle_event = tokio::spawn(wait_for_event_allowing_rate_limit(
                    session.subscribe(),
                    "session.idle after auto-mode switch",
                    |event| event.parsed_type() == SessionEventType::SessionIdle,
                ));

                let message_id = session
                    .send(AUTO_MODE_PROMPT)
                    .await
                    .expect("send auto-mode-switch prompt");
                assert!(!message_id.is_empty(), "expected message ID");

                let (session_id, error_code, retry_after_seconds) =
                    recv_with_timeout(&mut request_rx, "auto-mode-switch request").await;
                assert_eq!(session_id, session.id().clone());
                assert_eq!(error_code.as_deref(), Some("user_weekly_rate_limited"));
                assert_eq!(retry_after_seconds, Some(1.0));

                let requested = requested_event.await.expect("requested task");
                let requested_data = requested
                    .typed_data::<AutoModeSwitchRequestedData>()
                    .expect("typed requested event");
                assert_eq!(requested_data.error_code, error_code);
                assert_eq!(requested_data.retry_after_seconds, retry_after_seconds);

                let completed = completed_event.await.expect("completed task");
                let completed_data = completed
                    .typed_data::<AutoModeSwitchCompletedData>()
                    .expect("typed completed event");
                assert_eq!(completed_data.response, "yes");

                let model_change = model_change_event.await.expect("model change task");
                let model_change_data = model_change
                    .typed_data::<SessionModelChangeData>()
                    .expect("typed model change event");
                assert_eq!(
                    model_change_data.cause.as_deref(),
                    Some("rate_limit_auto_switch")
                );
                idle_event.await.expect("idle task");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
