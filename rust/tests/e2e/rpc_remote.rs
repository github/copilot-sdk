use github_copilot_sdk::rpc::{RemoteEnableRequest, RemoteSessionMode};
use github_copilot_sdk::session_events::{SessionEventType, SessionRemoteSteerableChangedData};

use super::support::{wait_for_event, with_e2e_context};

#[tokio::test]
async fn should_treat_remote_off_as_noop_or_implemented_error() {
    with_e2e_context(
        "rpc_remote",
        "should_treat_remote_off_as_noop_or_implemented_error",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                match session
                    .rpc()
                    .remote()
                    .enable(RemoteEnableRequest {
                        mode: Some(RemoteSessionMode::Off),
                    })
                    .await
                {
                    Ok(result) => {
                        assert!(!result.remote_steerable);
                        assert!(result.url.as_deref().unwrap_or_default().is_empty());
                    }
                    Err(err) => assert!(
                        !err.to_string()
                            .contains("Unhandled method session.remote.enable")
                    ),
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_treat_remote_disable_as_noop_or_implemented_error() {
    with_e2e_context(
        "rpc_remote",
        "should_treat_remote_disable_as_noop_or_implemented_error",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                if let Err(err) = session.rpc().remote().disable().await {
                    assert!(
                        !err.to_string()
                            .contains("Unhandled method session.remote.disable")
                    );
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_notify_steerable_changed_event_and_persist_flag() {
    with_e2e_context(
        "rpc_remote",
        "should_notify_steerable_changed_event_and_persist_flag",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let changed =
                    wait_for_event(session.subscribe(), "remote steerable changed", |event| {
                        event.parsed_type() == SessionEventType::SessionRemoteSteerableChanged
                            && event
                                .typed_data::<SessionRemoteSteerableChangedData>()
                                .is_some_and(|data| data.remote_steerable)
                    });

                session
                    .rpc()
                    .remote()
                    .notify_steerable_changed(
                        github_copilot_sdk::rpc::RemoteNotifySteerableChangedRequest {
                            remote_steerable: true,
                        },
                    )
                    .await
                    .expect("notify remote steerable");
                changed.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
