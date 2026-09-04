use super::support::with_e2e_context;

/// Starts an in-process client, performs a round-trip, and stops cleanly.
/// Fails hard if the in-process runtime library cannot be loaded.
#[tokio::test]
async fn should_start_ping_and_stop_inprocess_client() {
    with_e2e_context("client", "should_start_ping_and_stop_stdio_client", |ctx| {
        Box::pin(async move {
            let client = ctx.start_inprocess_client().await;
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

            client.stop().await.expect("stop in-process client");
        })
    })
    .await;
}

/// Regression test for github/copilot-sdk#2525: `force_stop` is documented as
/// a synchronous, infallible recovery path, but it used to call the native
/// `host_shutdown` export in-line with no bound. A slow or stuck native
/// shutdown (observed on Windows in-process, closing the runtime's SQLite
/// session store) would hang `force_stop` itself, defeating its purpose as
/// the fallback for exactly that kind of hang. Asserting that `force_stop`
/// returns quickly, on a dedicated thread bounded by a generous timeout,
/// catches any regression back to an unbounded, in-line wait.
#[tokio::test]
async fn should_force_stop_inprocess_client_within_bounded_time() {
    with_e2e_context(
        "client",
        "should_force_stop_inprocess_client_within_bounded_time",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_inprocess_client().await;
                client
                    .ping(Some("hello before force_stop"))
                    .await
                    .expect("ping over in-process FFI transport");

                let (done_tx, done_rx) = std::sync::mpsc::channel::<()>();
                std::thread::spawn(move || {
                    client.force_stop();
                    let _ = done_tx.send(());
                });

                tokio::time::timeout(
                    std::time::Duration::from_secs(30),
                    tokio::task::spawn_blocking(move || done_rx.recv()),
                )
                .await
                .expect("force_stop should complete within a bounded time")
                .expect("blocking task should not panic")
                .expect("force_stop thread should signal completion");
            })
        },
    )
    .await;
}
