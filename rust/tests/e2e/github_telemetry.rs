use std::sync::Arc;
use std::time::Duration;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::{Client, SessionConfig};

use super::support::{DEFAULT_TEST_TOKEN, with_e2e_context_no_snapshot};

#[tokio::test]
async fn should_forward_github_telemetry_on_session_create() {
    // TODO(cli-1.0.81-2): CLI 1.0.81-2 does not forward GitHub telemetry notifications over
    // the in-process (FFI) host, mirroring the existing telemetry-configuration limitation.
    if super::support::skip_inprocess("GitHub telemetry forwarding is not honored in-process") {
        return;
    }
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();

            let (collected, mut notifications) = tokio::sync::mpsc::unbounded_channel();
            let client = Client::start(ctx.client_options().with_on_github_telemetry(move |n| {
                let _ = collected.send(n);
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

            let first = tokio::time::timeout(Duration::from_secs(30), notifications.recv())
                .await
                .expect("timed out waiting for github telemetry notification")
                .expect("github telemetry notification");
            assert!(
                first
                    .session_id
                    .as_deref()
                    .is_some_and(|session_id| !session_id.is_empty())
            );
            let _: bool = first.restricted;
            assert!(!first.event.kind.is_empty());
            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}
