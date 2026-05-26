use github_copilot_sdk::generated::api_types::{
    CommandsRespondToQueuedCommandRequest, EnqueueCommandParams, QueuePendingItems,
    QueuePendingItemsKind, RegisterEventInterestParams, ReleaseEventInterestParams,
};
use github_copilot_sdk::generated::session_events::{CommandQueuedData, SessionEventType};
use github_copilot_sdk::session::Session;
use serde_json::json;
use uuid::Uuid;

use super::support::{wait_for_condition, wait_for_event, with_e2e_context};

fn is_pending_command(item: &QueuePendingItems, command: &str) -> bool {
    item.kind == QueuePendingItemsKind::Command
        && (item.display_text == command
            || item.display_text.contains(command.trim_start_matches('/')))
}

async fn wait_for_command_in_pending_items(session: &Session, command: &str) {
    wait_for_condition(
        "queued command to appear in pending items",
        move || async move {
            session
                .rpc()
                .queue()
                .pending_items()
                .await
                .expect("pending queued command")
                .items
                .iter()
                .any(|item| is_pending_command(item, command))
        },
    )
    .await;
}

async fn wait_for_command_not_in_pending_items(session: &Session, command: &str) {
    wait_for_condition(
        "queued command to leave pending items",
        move || async move {
            !session
                .rpc()
                .queue()
                .pending_items()
                .await
                .expect("pending queued command")
                .items
                .iter()
                .any(|item| is_pending_command(item, command))
        },
    )
    .await;
}

async fn wait_for_queue_empty(session: &Session) {
    wait_for_condition("queue to empty", move || async move {
        let pending = session
            .rpc()
            .queue()
            .pending_items()
            .await
            .expect("pending after clear");
        pending.items.is_empty() && pending.steering_messages.is_empty()
    })
    .await;
}

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
                let first_command = format!("/sdk-queue-first-{}", Uuid::new_v4());
                let second_command = format!("/sdk-queue-second-{}", Uuid::new_v4());
                let third_command = format!("/sdk-queue-third-{}", Uuid::new_v4());
                let interest = session
                    .rpc()
                    .event_log()
                    .register_interest(RegisterEventInterestParams {
                        event_type: "command.queued".to_string(),
                    })
                    .await
                    .expect("register command interest")
                    .handle;
                let first_command_for_event = first_command.clone();
                let queued_event =
                    wait_for_event(session.subscribe(), "command queued", move |event| {
                        event.parsed_type() == SessionEventType::CommandQueued
                            && event.data.get("command").and_then(|value| value.as_str())
                                == Some(first_command_for_event.as_str())
                    });

                let enqueue = session
                    .rpc()
                    .commands()
                    .enqueue(EnqueueCommandParams {
                        command: first_command,
                    })
                    .await
                    .expect("enqueue command");
                assert!(enqueue.queued);
                let queued = queued_event
                    .await
                    .typed_data::<CommandQueuedData>()
                    .expect("command queued data");

                let second = session
                    .rpc()
                    .commands()
                    .enqueue(EnqueueCommandParams {
                        command: second_command.clone(),
                    })
                    .await
                    .expect("enqueue second command");
                assert!(second.queued);
                wait_for_command_in_pending_items(&session, &second_command).await;

                let removed = session
                    .rpc()
                    .queue()
                    .remove_most_recent()
                    .await
                    .expect("remove second command");
                assert!(removed.removed);
                wait_for_command_not_in_pending_items(&session, &second_command).await;

                let third = session
                    .rpc()
                    .commands()
                    .enqueue(EnqueueCommandParams {
                        command: third_command.clone(),
                    })
                    .await
                    .expect("enqueue third command");
                assert!(third.queued);
                wait_for_command_in_pending_items(&session, &third_command).await;

                session.rpc().queue().clear().await.expect("clear queue");
                wait_for_command_not_in_pending_items(&session, &third_command).await;

                let completed = session
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
                    .expect("respond to first command");
                assert!(completed.success);

                wait_for_queue_empty(&session).await;
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
