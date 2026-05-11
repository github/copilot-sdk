use github_copilot_sdk::SessionId;

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_delete_session_by_id() {
    with_e2e_context("client_api", "should_delete_session_by_id", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let session_id = session.id().clone();

            session.send_and_wait("Say OK.").await.expect("send");
            session.disconnect().await.expect("disconnect session");
            client
                .delete_session(&session_id)
                .await
                .expect("delete session");

            let metadata = client
                .get_session_metadata(&session_id)
                .await
                .expect("get metadata");
            assert!(metadata.is_none());

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_report_error_when_deleting_unknown_session_id() {
    with_e2e_context(
        "client_api",
        "should_report_error_when_deleting_unknown_session_id",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                let unknown = SessionId::new("00000000-0000-0000-0000-000000000000");

                client
                    .delete_session(&unknown)
                    .await
                    .expect("delete unknown session is idempotent");
                let metadata = client
                    .get_session_metadata(&unknown)
                    .await
                    .expect("get unknown metadata");
                assert!(metadata.is_none());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_null_last_session_id_before_any_sessions_exist() {
    with_e2e_context(
        "client_api",
        "should_get_null_last_session_id_before_any_sessions_exist",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let last_id = client.get_last_session_id().await.expect("get last id");

                assert!(last_id.is_none());
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_track_last_session_id_after_session_created() {
    with_e2e_context(
        "client_api",
        "should_track_last_session_id_after_session_created",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                session.send_and_wait("Say OK.").await.expect("send");
                session.disconnect().await.expect("disconnect session");

                wait_for_condition("last session id to update", || {
                    let client = client.clone();
                    let session_id = session_id.clone();
                    async move {
                        client
                            .get_last_session_id()
                            .await
                            .is_ok_and(|id| id.as_ref() == Some(&session_id))
                    }
                })
                .await;
                assert_eq!(
                    client.get_last_session_id().await.expect("get last id"),
                    Some(session_id)
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_null_foreground_session_id_in_headless_mode() {
    with_e2e_context(
        "client_api",
        "should_get_null_foreground_session_id_in_headless_mode",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let foreground = client
                    .get_foreground_session_id()
                    .await
                    .expect("get foreground");

                assert!(foreground.is_none());
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_setting_foreground_session_in_headless_mode() {
    with_e2e_context(
        "client_api",
        "should_report_error_when_setting_foreground_session_in_headless_mode",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                client
                    .set_foreground_session_id(session.id())
                    .await
                    .expect("set foreground is ignored in headless mode");
                assert!(
                    client
                        .get_foreground_session_id()
                        .await
                        .expect("get foreground")
                        .is_none()
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
