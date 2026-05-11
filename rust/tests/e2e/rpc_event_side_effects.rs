use github_copilot_sdk::generated::api_types::{
    HistoryTruncateRequest, ModeSetRequest, NameSetRequest, PlanUpdateRequest, SessionMode,
    WorkspacesCreateFileRequest,
};
use github_copilot_sdk::generated::session_events::{
    PlanChangedOperation, SessionEventType, SessionModeChangedData, SessionPlanChangedData,
    SessionSnapshotRewindData, SessionTitleChangedData, SessionWorkspaceFileChangedData,
};

use super::support::{assistant_message_content, wait_for_event, with_e2e_context};

#[tokio::test]
async fn should_emit_mode_changed_event_when_mode_set() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_emit_mode_changed_event_when_mode_set",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let changed = wait_for_event(session.subscribe(), "mode changed", |event| {
                    if event.parsed_type() != SessionEventType::SessionModeChanged {
                        return false;
                    }
                    let data = event
                        .typed_data::<SessionModeChangedData>()
                        .expect("mode changed data");
                    data.previous_mode == "interactive" && data.new_mode == "plan"
                });
                session
                    .rpc()
                    .mode()
                    .set(ModeSetRequest {
                        mode: SessionMode::Plan,
                    })
                    .await
                    .expect("set mode");
                changed.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_plan_changed_event_for_update_and_delete() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_emit_plan_changed_event_for_update_and_delete",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let create = wait_for_plan_event(&session, PlanChangedOperation::Create);
                session
                    .rpc()
                    .plan()
                    .update(PlanUpdateRequest {
                        content: "# Test plan\n- item".to_string(),
                    })
                    .await
                    .expect("create plan");
                create.await;

                let delete = wait_for_plan_event(&session, PlanChangedOperation::Delete);
                session.rpc().plan().delete().await.expect("delete plan");
                delete.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_plan_changed_update_operation_on_second_update() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_emit_plan_changed_update_operation_on_second_update",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .rpc()
                    .plan()
                    .update(PlanUpdateRequest {
                        content: "# initial".to_string(),
                    })
                    .await
                    .expect("create plan");
                let update = wait_for_plan_event(&session, PlanChangedOperation::Update);
                session
                    .rpc()
                    .plan()
                    .update(PlanUpdateRequest {
                        content: "# updated".to_string(),
                    })
                    .await
                    .expect("update plan");
                update.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_workspace_file_changed_event_when_file_created() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_emit_workspace_file_changed_event_when_file_created",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let path = "side-effect-rust.txt";

                let changed =
                    wait_for_event(session.subscribe(), "workspace file changed", |event| {
                        if event.parsed_type() != SessionEventType::SessionWorkspaceFileChanged {
                            return false;
                        }
                        event
                            .typed_data::<SessionWorkspaceFileChangedData>()
                            .expect("workspace file changed data")
                            .path
                            == path
                    });
                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "hello".to_string(),
                    })
                    .await
                    .expect("create workspace file");
                changed.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_title_changed_event_when_name_set() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_emit_title_changed_event_when_name_set",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let title = "Renamed-Rust";

                let changed = wait_for_event(session.subscribe(), "title changed", |event| {
                    if event.parsed_type() != SessionEventType::SessionTitleChanged {
                        return false;
                    }
                    event
                        .typed_data::<SessionTitleChangedData>()
                        .expect("title changed data")
                        .title
                        == title
                });
                session
                    .rpc()
                    .name()
                    .set(NameSetRequest {
                        name: title.to_string(),
                    })
                    .await
                    .expect("set name");
                changed.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_snapshot_rewind_event_and_remove_events_on_truncate() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_emit_snapshot_rewind_event_and_remove_events_on_truncate",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Say SNAPSHOT_REWIND_TARGET exactly.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("SNAPSHOT_REWIND_TARGET"));
                let user_event = session
                    .get_messages()
                    .await
                    .expect("messages")
                    .into_iter()
                    .find(|event| event.parsed_type() == SessionEventType::UserMessage)
                    .expect("user.message event");
                let target_event_id = user_event.id.clone();

                let rewind = wait_for_event(session.subscribe(), "snapshot rewind", |event| {
                    if event.parsed_type() != SessionEventType::SessionSnapshotRewind {
                        return false;
                    }
                    event
                        .typed_data::<SessionSnapshotRewindData>()
                        .expect("snapshot rewind data")
                        .up_to_event_id
                        == target_event_id
                });
                let result = session
                    .rpc()
                    .history()
                    .truncate(HistoryTruncateRequest {
                        event_id: target_event_id.clone(),
                    })
                    .await
                    .expect("truncate history");
                assert!(result.events_removed >= 1);
                rewind.await;

                let remaining = session
                    .get_messages()
                    .await
                    .expect("messages after truncate");
                assert!(!remaining.iter().any(|event| event.id == target_event_id));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_allow_session_use_after_truncate() {
    with_e2e_context(
        "rpc_event_side_effects",
        "should_allow_session_use_after_truncate",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Say SNAPSHOT_REWIND_TARGET exactly.")
                    .await
                    .expect("send");
                let user_event = session
                    .get_messages()
                    .await
                    .expect("messages")
                    .into_iter()
                    .find(|event| event.parsed_type() == SessionEventType::UserMessage)
                    .expect("user.message event");

                let result = session
                    .rpc()
                    .history()
                    .truncate(HistoryTruncateRequest {
                        event_id: user_event.id,
                    })
                    .await
                    .expect("truncate history");
                assert!(result.events_removed >= 1);
                session
                    .rpc()
                    .mode()
                    .get()
                    .await
                    .expect("mode after truncate");
                session
                    .rpc()
                    .workspaces()
                    .get_workspace()
                    .await
                    .expect("workspace after truncate");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn wait_for_plan_event(
    session: &github_copilot_sdk::session::Session,
    operation: PlanChangedOperation,
) -> impl std::future::Future<Output = github_copilot_sdk::SessionEvent> {
    let events = session.subscribe();
    wait_for_event(events, "plan changed", move |event| {
        if event.parsed_type() != SessionEventType::SessionPlanChanged {
            return false;
        }
        event
            .typed_data::<SessionPlanChangedData>()
            .expect("plan changed data")
            .operation
            == operation
    })
}
