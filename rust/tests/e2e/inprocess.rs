use super::support::with_e2e_context;

/// Starts an in-process client, performs a round-trip, and stops cleanly.
/// Fails hard if the in-process runtime library cannot be loaded.
#[tokio::test]
async fn should_start_ping_and_stop_inprocess_client() {
    with_e2e_context("client", "should_start_ping_and_stop_stdio_client", |ctx| {
        Box::pin(async move {
            let client = ctx.start_inprocess_client().await;

            let response = client
                .ping(Some("hello from rust in-process"))
                .await
                .expect("ping over in-process FFI transport");
            assert_eq!(response.message, "pong: hello from rust in-process");
            assert!(!response.timestamp.is_empty());

            let status = client.get_status().await.expect("get status");
            assert!(status.protocol_version > 0);

            client.stop().await.expect("stop in-process client");
        })
    })
    .await;
}
