use github_copilot_sdk::generated::api_types::{
    CommandsRespondToQueuedCommandRequest, EnqueueCommandParams, RegisterEventInterestParams,
    ReleaseEventInterestParams,
};
use github_copilot_sdk::generated::session_events::{CommandQueuedData, SessionEventType};
use serde_json::json;

use super::support::{wait_for_event, with_e2e_context};

#[tokio::test]
async fn fresh_queue_is_empty_and_empty_mutations_are_noops() {
    with_e2e_context(
        "rpc_queue",
        "fresh_queue_is_empty_and_empty_mutations_are_noops",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let pending = session
                    .rpc()
                    .queue()
                    .pending_items()
                    .await
                    .expect("pending items");
                assert!(pending.items.is_empty());
                assert!(pending.steering_messages.is_empty());
                assert!(
                    !session
                        .rpc()
                        .queue()
                        .remove_most_recent()
                        .await
                        .expect("remove most recent")
                        .removed
                );
                session.rpc().queue().clear().await.expect("clear queue");
                let after = session
                    .rpc()
                    .queue()
                    .pending_items()
                    .await
                    .expect("pending after clear");
                assert!(after.items.is_empty());
                assert!(after.steering_messages.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn pendingitems_reports_queued_command_and_remove_and_clear_update_queue() {
    with_e2e_context(
        "rpc_queue",
        "pendingitems_reports_queued_command_and_remove_and_clear_update_queue",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let interest = session
                    .rpc()
                    .event_log()
                    .register_interest(RegisterEventInterestParams {
                        event_type: "command.queued".to_string(),
                    })
                    .await
                    .expect("register command interest")
                    .handle;
                let queued_event = wait_for_event(session.subscribe(), "command queued", |event| {
                    event.parsed_type() == SessionEventType::CommandQueued
                });

                let enqueue = session
                    .rpc()
                    .commands()
                    .enqueue(EnqueueCommandParams {
                        command: "/help".to_string(),
                    })
                    .await
                    .expect("enqueue command");
                assert!(enqueue.queued);
                let queued = queued_event
                    .await
                    .typed_data::<CommandQueuedData>()
                    .expect("command queued data");

                let pending = session
                    .rpc()
                    .queue()
                    .pending_items()
                    .await
                    .expect("pending queued command");
                assert!(pending.items.is_empty());
                assert!(pending.steering_messages.is_empty());
                let removed = session
                    .rpc()
                    .queue()
                    .remove_most_recent()
                    .await
                    .expect("remove after command event");
                assert!(!removed.removed);
                assert!(
                    session
                        .rpc()
                        .queue()
                        .pending_items()
                        .await
                        .expect("pending after remove")
                        .items
                        .is_empty()
                );
                session
                    .rpc()
                    .commands()
                    .respond_to_queued_command(CommandsRespondToQueuedCommandRequest {
                        request_id: queued.request_id,
                        result: json!({
                            "handled": true,
                            "stopProcessingQueue": true
                        }),
                    })
                    .await
                    .expect("respond to removed command");

                session.rpc().queue().clear().await.expect("clear queue");
                assert!(
                    session
                        .rpc()
                        .queue()
                        .pending_items()
                        .await
                        .expect("pending after clear")
                        .items
                        .is_empty()
                );
                session
                    .rpc()
                    .event_log()
                    .release_interest(ReleaseEventInterestParams { handle: interest })
                    .await
                    .expect("release command interest");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
