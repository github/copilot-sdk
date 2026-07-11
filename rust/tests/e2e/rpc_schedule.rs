use github_copilot_sdk::rpc::ScheduleStopRequest;

use super::support::with_e2e_context;

#[tokio::test]
async fn should_list_no_schedules_for_fresh_session() {
    with_e2e_context(
        "rpc_schedule",
        "should_list_no_schedules_for_fresh_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let schedules = session
                    .rpc()
                    .schedule()
                    .list()
                    .await
                    .expect("list schedules");
                assert!(schedules.entries.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_null_entry_when_stopping_unknown_schedule() {
    with_e2e_context(
        "rpc_schedule",
        "should_return_null_entry_when_stopping_unknown_schedule",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let stopped = session
                    .rpc()
                    .schedule()
                    .stop(ScheduleStopRequest { id: i64::MAX })
                    .await
                    .expect("stop missing schedule");
                assert!(stopped.entry.is_none());
                assert!(
                    session
                        .rpc()
                        .schedule()
                        .list()
                        .await
                        .expect("list schedules")
                        .entries
                        .is_empty()
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
