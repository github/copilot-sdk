use github_copilot_sdk::generated::session_events::SessionEventType;

use super::support::{
    assistant_message_content, collect_until_idle, event_types, wait_for_condition,
    with_e2e_context,
};

#[tokio::test]
async fn should_list_created_sessions_after_sending_a_message() {
    with_e2e_context(
        "session_lifecycle",
        "should_list_created_sessions_after_sending_a_message",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create second session");

                session1.send_and_wait("Say hello").await.expect("send one");
                session2.send_and_wait("Say world").await.expect("send two");

                wait_for_condition("both sessions to appear in list", || {
                    let client = client.clone();
                    let id1 = session1.id().clone();
                    let id2 = session2.id().clone();
                    async move {
                        client.list_sessions(None).await.is_ok_and(|sessions| {
                            let ids: std::collections::HashSet<_> = sessions
                                .into_iter()
                                .map(|session| session.session_id)
                                .collect();
                            ids.contains(&id1) && ids.contains(&id2)
                        })
                    }
                })
                .await;

                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                session2
                    .disconnect()
                    .await
                    .expect("disconnect second session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_delete_session_permanently() {
    with_e2e_context(
        "session_lifecycle",
        "should_delete_session_permanently",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                session.send_and_wait("Say hi").await.expect("send");
                wait_for_condition("session to appear in list before delete", || {
                    let client = client.clone();
                    let session_id = session_id.clone();
                    async move {
                        client.list_sessions(None).await.is_ok_and(|sessions| {
                            sessions
                                .iter()
                                .any(|session| session.session_id == session_id)
                        })
                    }
                })
                .await;

                session.disconnect().await.expect("disconnect session");
                client
                    .delete_session(&session_id)
                    .await
                    .expect("delete session");

                let after = client.list_sessions(None).await.expect("list sessions");
                assert!(!after.iter().any(|session| session.session_id == session_id));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_events_via_getmessages_after_conversation() {
    with_e2e_context(
        "session_lifecycle",
        "should_return_events_via_getmessages_after_conversation",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait("What is 2+2? Reply with just the number.")
                    .await
                    .expect("send");

                let messages = session.get_messages().await.expect("get messages");
                let types = event_types(&messages);
                assert!(types.contains(&"session.start"));
                assert!(types.contains(&"user.message"));
                assert!(types.contains(&"assistant.message"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_support_multiple_concurrent_sessions() {
    with_e2e_context(
        "session_lifecycle",
        "should_support_multiple_concurrent_sessions",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create second session");

                let (first, second) = tokio::join!(
                    session1.send_and_wait("What is 1+1? Reply with just the number."),
                    session2.send_and_wait("What is 3+3? Reply with just the number.")
                );
                let first = first.expect("first send").expect("first assistant message");
                let second = second
                    .expect("second send")
                    .expect("second assistant message");
                assert!(assistant_message_content(&first).contains('2'));
                assert!(assistant_message_content(&second).contains('6'));

                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                session2
                    .disconnect()
                    .await
                    .expect("disconnect second session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_isolate_events_between_concurrent_sessions() {
    with_e2e_context(
        "session_lifecycle",
        "should_isolate_events_between_concurrent_sessions",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create first session");
                let session2 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create second session");
                let events1 = session1.subscribe();
                let events2 = session2.subscribe();

                session1
                    .send_and_wait("Say 'session_one_response'.")
                    .await
                    .expect("send one");
                session2
                    .send_and_wait("Say 'session_two_response'.")
                    .await
                    .expect("send two");

                let observed1 = collect_until_idle(events1).await;
                let observed2 = collect_until_idle(events2).await;
                let messages1: Vec<_> = observed1
                    .iter()
                    .filter(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .map(assistant_message_content)
                    .collect();
                let messages2: Vec<_> = observed2
                    .iter()
                    .filter(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .map(assistant_message_content)
                    .collect();

                assert!(
                    messages1
                        .iter()
                        .any(|message| message.contains("session_one_response"))
                );
                assert!(
                    !messages1
                        .iter()
                        .any(|message| message.contains("session_two_response"))
                );
                assert!(
                    messages2
                        .iter()
                        .any(|message| message.contains("session_two_response"))
                );
                assert!(
                    !messages2
                        .iter()
                        .any(|message| message.contains("session_one_response"))
                );

                session1
                    .disconnect()
                    .await
                    .expect("disconnect first session");
                session2
                    .disconnect()
                    .await
                    .expect("disconnect second session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
