use github_copilot_sdk::{ConnectionState, SessionLifecycleEventType};
use serde_json::json;

use super::support::{wait_for_lifecycle_event, with_e2e_context};

#[tokio::test]
async fn should_receive_session_created_lifecycle_event() {
    with_e2e_context(
        "client_lifecycle",
        "should_receive_session_created_lifecycle_event",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let created = client.subscribe_lifecycle();
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let event =
                    wait_for_lifecycle_event(created, "session.created lifecycle event", |event| {
                        event.event_type == SessionLifecycleEventType::Created
                    })
                    .await;
                assert_eq!(event.event_type, SessionLifecycleEventType::Created);
                assert_eq!(&event.session_id, session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_filter_session_lifecycle_events_by_type() {
    with_e2e_context(
        "client_lifecycle",
        "should_filter_session_lifecycle_events_by_type",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let created = client.subscribe_lifecycle();
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let event = wait_for_lifecycle_event(
                    created,
                    "filtered session.created lifecycle event",
                    |event| event.event_type == SessionLifecycleEventType::Created,
                )
                .await;
                assert_eq!(&event.session_id, session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn disposing_lifecycle_subscription_stops_receiving_events() {
    with_e2e_context(
        "client_lifecycle",
        "disposing_lifecycle_subscription_stops_receiving_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                drop(client.subscribe_lifecycle());
                let created = client.subscribe_lifecycle();
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let event = wait_for_lifecycle_event(
                    created,
                    "active session.created lifecycle event",
                    |event| event.event_type == SessionLifecycleEventType::Created,
                )
                .await;
                assert_eq!(event.session_id, *session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn dispose_disconnects_client_and_disposes_rpc_surface_async() {
    with_e2e_context(
        "client_lifecycle",
        "dispose_disconnects_client_and_disposes_rpc_surface_async_true",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                assert_eq!(client.state(), ConnectionState::Connected);

                client.stop().await.expect("stop client");

                assert_eq!(client.state(), ConnectionState::Disconnected);
                assert!(
                    client.call("rpc.ping", Some(json!({}))).await.is_err(),
                    "stopped client should reject RPC calls"
                );
            })
        },
    )
    .await;
}

#[tokio::test]
async fn dispose_disconnects_client_and_disposes_rpc_surface_drop() {
    with_e2e_context(
        "client_lifecycle",
        "dispose_disconnects_client_and_disposes_rpc_surface_async_false",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                assert_eq!(client.state(), ConnectionState::Connected);

                client.force_stop();

                assert_eq!(client.state(), ConnectionState::Disconnected);
                assert!(
                    client.call("rpc.ping", Some(json!({}))).await.is_err(),
                    "force-stopped client should reject RPC calls"
                );
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_receive_session_updated_lifecycle_event_for_non_ephemeral_activity() {
    with_e2e_context(
        "client_lifecycle",
        "should_receive_session_updated_lifecycle_event_for_non_ephemeral_activity",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let updated = client.subscribe_lifecycle();

                session
                    .client()
                    .call(
                        "session.mode.set",
                        Some(json!({
                            "sessionId": session.id().as_str(),
                            "mode": "plan",
                        })),
                    )
                    .await
                    .expect("set session mode");

                let event =
                    wait_for_lifecycle_event(updated, "session.updated lifecycle event", |event| {
                        event.event_type == SessionLifecycleEventType::Updated
                            && event.session_id == *session.id()
                    })
                    .await;
                assert_eq!(event.event_type, SessionLifecycleEventType::Updated);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_receive_session_deleted_lifecycle_event_when_deleted() {
    with_e2e_context(
        "client_lifecycle",
        "should_receive_session_deleted_lifecycle_event_when_deleted",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                session
                    .send_and_wait("Say SESSION_DELETED_OK exactly.")
                    .await
                    .expect("send");
                let deleted = client.subscribe_lifecycle();

                client
                    .delete_session(&session_id)
                    .await
                    .expect("delete session");

                let event =
                    wait_for_lifecycle_event(deleted, "session.deleted lifecycle event", |event| {
                        event.event_type == SessionLifecycleEventType::Deleted
                            && event.session_id == session_id
                    })
                    .await;
                assert_eq!(event.event_type, SessionLifecycleEventType::Deleted);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
