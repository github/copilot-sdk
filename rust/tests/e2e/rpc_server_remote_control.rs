use github_copilot_sdk::SessionId;
use github_copilot_sdk::rpc::{
    RemoteControlConfig, SessionsSetRemoteControlSteeringRequest,
    SessionsStartRemoteControlRequest, SessionsStopRemoteControlRequest,
    SessionsTransferRemoteControlRequest,
};
use serde_json::Value;

use super::support::with_e2e_context;

#[tokio::test]
async fn should_report_remote_control_status_as_off() {
    with_e2e_context(
        "rpc_server_remote_control",
        "should_report_remote_control_status_as_off",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .sessions()
                    .get_remote_control_status()
                    .await
                    .expect("get remote control status");

                assert_status_off(&result.status);
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_treat_set_steering_as_no_op_when_off() {
    with_e2e_context(
        "rpc_server_remote_control",
        "should_treat_set_steering_as_no_op_when_off",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .sessions()
                    .set_remote_control_steering(SessionsSetRemoteControlSteeringRequest {
                        enabled: false,
                    })
                    .await
                    .expect("set remote control steering");

                assert_status_off(&result.status);
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_not_stopped_when_remote_control_is_off() {
    with_e2e_context(
        "rpc_server_remote_control",
        "should_report_not_stopped_when_remote_control_is_off",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .sessions()
                    .stop_remote_control()
                    .await
                    .expect("stop remote control");

                assert!(!result.stopped);
                assert_status_off(&result.status);
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reject_transfer_when_off_with_compare_and_swap() {
    with_e2e_context(
        "rpc_server_remote_control",
        "should_reject_transfer_when_off_with_compare_and_swap",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .sessions()
                    .transfer_remote_control(SessionsTransferRemoteControlRequest {
                        expected_from_session_id: Some(format!(
                            "rc-from-{}",
                            uuid::Uuid::new_v4().simple()
                        )),
                        to_session_id: format!("rc-to-{}", uuid::Uuid::new_v4().simple()),
                    })
                    .await
                    .expect("transfer remote control");

                assert!(!result.transferred);
                assert_status_off(&result.status);
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reach_runtime_when_starting_remote_control_for_unknown_session() {
    with_e2e_context(
        "rpc_server_remote_control",
        "should_reach_runtime_when_starting_remote_control_for_unknown_session",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .sessions()
                    .start_remote_control(SessionsStartRemoteControlRequest {
                        session_id: SessionId::from(format!(
                            "missing-session-{}",
                            uuid::Uuid::new_v4().simple()
                        )),
                        config: RemoteControlConfig {
                            existing_mc_session: None,
                            explicit: false,
                            remote: false,
                            silent: true,
                            steerable: false,
                            task_id: None,
                        },
                    })
                    .await;

                let _ = client
                    .rpc()
                    .sessions()
                    .stop_remote_control_with_params(SessionsStopRemoteControlRequest {
                        expected_session_id: None,
                        force: Some(true),
                    })
                    .await;

                let err = result.expect_err("unknown session should fail");
                let message = err.to_string();
                assert_not_unhandled(&message);
                let lower = message.to_ascii_lowercase();
                assert!(
                    lower.contains("session") || lower.contains("remote"),
                    "{message}"
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn assert_status_off(status: &Value) {
    assert_eq!(status.get("state").and_then(Value::as_str), Some("off"));
}

fn assert_not_unhandled(message: &str) {
    assert!(
        !message.to_ascii_lowercase().contains("unhandled method"),
        "{message}"
    );
}
