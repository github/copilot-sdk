use github_copilot_sdk::rpc::{
    EventLogReadRequest, EventsCursorStatus, RegisterEventInterestParams,
    ReleaseEventInterestParams,
};
use github_copilot_sdk::session_events::{
    PlanChangedOperation, SessionEventType, SessionPlanChangedData, SessionTitleChangedData,
};
use serde_json::json;

#[tokio::test]
async fn should_read_persisted_events_from_beginning() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_event_log",
        "should_read_persisted_events_from_beginning",
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
                    .update(github_copilot_sdk::rpc::PlanUpdateRequest {
                        content: "# event log plan".to_string(),
                    })
                    .await
                    .expect("write plan");
                client
                    .rpc()
                    .sessions()
                    .save(github_copilot_sdk::rpc::SessionsSaveRequest {
                        session_id: session.id().clone(),
                    })
                    .await
                    .expect("save session");

                let read = session
                    .rpc()
                    .event_log()
                    .read(EventLogReadRequest {
                        agent_ids: None,
                        agent_scope: None,
                        cursor: None,
                        direction: None,
                        include_ephemeral: None,
                        max: Some(100),
                        types: Some(json!("*")),
                        wait_ms: Some(0),
                    })
                    .await
                    .expect("read event log");
                assert_eq!(read.cursor_status, EventsCursorStatus::Ok);
                assert!(!read.cursor.trim().is_empty());
                assert!(read.events.iter().any(|event| {
                    event.parsed_type() == SessionEventType::SessionPlanChanged
                        && event
                            .typed_data::<SessionPlanChangedData>()
                            .is_some_and(|data| data.operation == PlanChangedOperation::Create)
                }));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_tail_cursor_and_read_empty_when_no_new_events() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_event_log",
        "should_return_tail_cursor_and_read_empty_when_no_new_events",
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
                    .update(github_copilot_sdk::rpc::PlanUpdateRequest {
                        content: "# tail cursor plan".to_string(),
                    })
                    .await
                    .expect("write plan");
                client
                    .rpc()
                    .sessions()
                    .save(github_copilot_sdk::rpc::SessionsSaveRequest {
                        session_id: session.id().clone(),
                    })
                    .await
                    .expect("save session");
                let before_tail = session
                    .rpc()
                    .event_log()
                    .read(EventLogReadRequest {
                        agent_ids: None,
                        agent_scope: None,
                        cursor: None,
                        direction: None,
                        include_ephemeral: None,
                        max: Some(100),
                        types: Some(json!(["session.plan_changed"])),
                        wait_ms: Some(0),
                    })
                    .await
                    .expect("read persisted plan event");
                assert!(
                    before_tail
                        .events
                        .iter()
                        .any(|event| event.parsed_type() == SessionEventType::SessionPlanChanged)
                );

                let tail = session.rpc().event_log().tail().await.expect("tail");
                assert!(!tail.cursor.trim().is_empty());
                let request = EventLogReadRequest {
                    agent_ids: None,
                    agent_scope: None,
                    cursor: Some(tail.cursor),
                    direction: None,
                    include_ephemeral: Some(false),
                    max: Some(10),
                    // Startup events can arrive after the tail; only the plan update is controlled here.
                    types: Some(json!(["session.plan_changed"])),
                    wait_ms: Some(0),
                };
                let read = session
                    .rpc()
                    .event_log()
                    .read(request.clone())
                    .await
                    .expect("read from tail");
                assert_eq!(read.cursor_status, EventsCursorStatus::Ok);
                assert!(
                    read.events.is_empty(),
                    "unexpected events after tail: {:?}",
                    read.events
                );
                assert!(!read.has_more);

                session
                    .rpc()
                    .plan()
                    .update(github_copilot_sdk::rpc::PlanUpdateRequest {
                        content: "# durable event after tail".to_string(),
                    })
                    .await
                    .expect("write plan");
                client
                    .rpc()
                    .sessions()
                    .save(github_copilot_sdk::rpc::SessionsSaveRequest {
                        session_id: session.id().clone(),
                    })
                    .await
                    .expect("save session");
                let read = session
                    .rpc()
                    .event_log()
                    .read(request)
                    .await
                    .expect("read new durable event from tail");
                assert_eq!(read.cursor_status, EventsCursorStatus::Ok);
                assert!(
                    read.events
                        .iter()
                        .any(|event| event.parsed_type() == SessionEventType::SessionPlanChanged),
                    "new durable event missing after tail: {:?}",
                    read.events
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_register_and_release_event_interest_idempotently() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_event_log",
        "should_register_and_release_event_interest_idempotently",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let handle = session
                    .rpc()
                    .event_log()
                    .register_interest(RegisterEventInterestParams {
                        event_type: "session.title_changed".to_string(),
                    })
                    .await
                    .expect("register interest")
                    .handle;
                assert!(!handle.trim().is_empty());
                for _ in 0..2 {
                    assert!(
                        session
                            .rpc()
                            .event_log()
                            .release_interest(ReleaseEventInterestParams {
                                handle: handle.clone(),
                            })
                            .await
                            .expect("release interest")
                            .success
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
async fn should_longpoll_with_types_filter_for_titlechanged_event() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_event_log",
        "should_longpoll_with_types_filter_for_titlechanged_event",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let tail = session.rpc().event_log().tail().await.expect("tail");
                let event_log = session.rpc().event_log();
                let mut request = EventLogReadRequest {
                    agent_ids: None,
                    agent_scope: None,
                    cursor: Some(tail.cursor),
                    direction: None,
                    include_ephemeral: None,
                    max: Some(10),
                    types: Some(json!(["session.title_changed"])),
                    wait_ms: Some(5_000),
                };
                let read_future = event_log.read(request.clone());
                let write_future = async {
                    session
                        .rpc()
                        .plan()
                        .update(github_copilot_sdk::rpc::PlanUpdateRequest {
                            content: "# Unrelated event during a filtered read".to_string(),
                        })
                        .await
                        .expect("write unrelated plan");
                };
                let (read, _) = tokio::join!(read_future, write_future);
                let mut read = read.expect("long-poll event log");
                assert!(
                    read.events.is_empty(),
                    "unexpected events after unrelated plan update: {:?}",
                    read.events
                );
                session
                    .rpc()
                    .name()
                    .set(github_copilot_sdk::rpc::NameSetRequest {
                        name: "Rust event log title".to_string(),
                    })
                    .await
                    .expect("set title");
                tokio::time::timeout(std::time::Duration::from_secs(10), async {
                    loop {
                        assert_eq!(read.cursor_status, EventsCursorStatus::Ok);
                        assert!(read.events.iter().all(|event| {
                            event.parsed_type() == SessionEventType::SessionTitleChanged
                        }));
                        if read.events.iter().any(|event| {
                            event
                                .typed_data::<SessionTitleChangedData>()
                                .is_some_and(|data| data.title == "Rust event log title")
                        }) {
                            break;
                        }
                        request.cursor = Some(read.cursor.clone());
                        read = event_log
                            .read(request.clone())
                            .await
                            .expect("continue filtered event log read");
                    }
                })
                .await
                .unwrap_or_else(|_| {
                    panic!(
                        "timed out waiting for filtered title event; last batch: {:?}",
                        read.events
                    )
                });

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("rpc_event_log", 4);
