use super::support::{E2eContext, with_e2e_context};

/// Starts an in-process client, performs a round-trip, and stops cleanly.
/// Fails hard if the in-process runtime library cannot be loaded.
#[tokio::test]
async fn should_start_ping_and_stop_inprocess_client() {
    with_e2e_context("client", "should_start_ping_and_stop_stdio_client", |ctx| {
        Box::pin(async move {
            let mut sibling_context =
                E2eContext::new_with_cli("client", "should_start_ping_and_stop_stdio_client", None)
                    .await
                    .expect("create sibling context");
            let (client, sibling) = tokio::join!(
                ctx.start_inprocess_client(),
                sibling_context.start_inprocess_client()
            );
            assert_ne!(
                client.in_process_connection_id(),
                sibling.in_process_connection_id()
            );
            let timings = client.startup_timings().expect("startup timings");
            assert!(timings.program_resolve_ms.is_some());
            assert!(timings.process_spawn_ms.is_none());
            assert!(timings.port_wait_ms.is_none());
            assert!(timings.total_ms >= timings.transport_setup_ms);
            assert!(timings.total_ms >= timings.handshake_ms);

            let response = client
                .ping(Some("hello from rust in-process"))
                .await
                .expect("ping over in-process FFI transport");
            assert_eq!(response.message, "pong: hello from rust in-process");
            assert!(!response.timestamp.is_empty());

            let status = client.get_status().await.expect("get status");
            assert!(status.protocol_version > 0);

            let (first, second) =
                tokio::join!(client.ping(Some("first")), sibling.ping(Some("second")));
            assert_eq!(first.expect("first reply").message, "pong: first");
            assert_eq!(second.expect("second reply").message, "pong: second");
            client.stop().await.expect("stop in-process client");
            assert_eq!(
                sibling
                    .ping(Some("survivor"))
                    .await
                    .expect("sibling remains live")
                    .message,
                "pong: survivor"
            );
            sibling.stop().await.expect("stop sibling");
            sibling_context
                .cleanup(true)
                .await
                .expect("clean up sibling");
        })
    })
    .await;
}
