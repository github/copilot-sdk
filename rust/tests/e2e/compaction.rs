use github_copilot_sdk::generated::api_types::{LogRequest, SessionLogLevel};

use super::support::with_e2e_context;

#[tokio::test]
async fn should_return_empty_handoff_summary_for_fresh_session() {
    with_e2e_context(
        "compaction",
        "should_return_empty_handoff_summary_for_fresh_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let summary = session
                    .rpc()
                    .history()
                    .summarize_for_handoff()
                    .await
                    .expect("summarize fresh session");
                assert!(summary.summary.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_noop_when_cancelling_compaction_without_inflight_work() {
    with_e2e_context(
        "compaction",
        "should_report_noop_when_cancelling_compaction_without_inflight_work",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let cancelled = session
                    .rpc()
                    .history()
                    .cancel_background_compaction()
                    .await
                    .expect("cancel background compaction");
                assert!(!cancelled.cancelled);
                let aborted = session
                    .rpc()
                    .history()
                    .abort_manual_compaction()
                    .await
                    .expect("abort manual compaction");
                assert!(!aborted.aborted);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_summarize_for_handoff_after_non_ephemeral_log_event() {
    with_e2e_context(
        "compaction",
        "should_summarize_for_handoff_after_non_ephemeral_log_event",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let log = session
                    .rpc()
                    .log(LogRequest {
                        ephemeral: Some(false),
                        level: Some(SessionLogLevel::Info),
                        message: "Rust handoff summary source".to_string(),
                        tip: None,
                        r#type: Some("notification".to_string()),
                        url: None,
                    })
                    .await
                    .expect("log handoff source");
                assert!(!log.event_id.trim().is_empty());
                let summary = session
                    .rpc()
                    .history()
                    .summarize_for_handoff()
                    .await
                    .expect("summarize after log");
                assert!(summary.summary.is_empty() || summary.summary.contains("Rust"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
