use std::sync::{Arc, Mutex};
use std::time::Duration;

use github_copilot_sdk::github_telemetry::GitHubTelemetryNotification;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::{Client, SessionConfig};

use super::support::{DEFAULT_TEST_TOKEN, with_e2e_context_no_snapshot};

#[tokio::test]
async fn should_forward_github_telemetry_on_session_create() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();

            let notifications = Arc::new(Mutex::new(Vec::<GitHubTelemetryNotification>::new()));
            let collected = notifications.clone();
            let client = Client::start(ctx.client_options().with_on_github_telemetry(move |n| {
                collected.lock().unwrap().push(n);
            }))
            .await
            .expect("start client");
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_github_token(DEFAULT_TEST_TOKEN)
                        .with_permission_handler(Arc::new(ApproveAllHandler)),
                )
                .await
                .expect("create session");

            let deadline = tokio::time::Instant::now() + Duration::from_secs(30);
            loop {
                if !notifications.lock().unwrap().is_empty() {
                    break;
                }
                assert!(
                    tokio::time::Instant::now() < deadline,
                    "timed out waiting for github telemetry notification"
                );
                tokio::time::sleep(Duration::from_millis(100)).await;
            }

            {
                let notifications = notifications.lock().unwrap();
                assert!(!notifications.is_empty());
                let first = notifications
                    .first()
                    .expect("github telemetry notification");
                assert!(!first.session_id.is_empty());
                let _: bool = first.restricted;
                assert!(!first.event.kind.is_empty());
            }

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}
