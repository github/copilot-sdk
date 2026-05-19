#![allow(clippy::unwrap_used)]

use std::time::Instant;

use github_copilot_sdk::resolve::copilot_binary_with_source;
use github_copilot_sdk::{Client, ClientOptions, SDK_PROTOCOL_VERSION};

fn default_options() -> ClientOptions {
    let mut opts = ClientOptions::default();
    opts.cwd = std::env::current_dir().expect("cwd");
    opts
}

#[tokio::test]
#[ignore] // requires `copilot` CLI on PATH — run with `cargo test -- --ignored`
async fn start_ping_stop() {
    let client = Client::start(default_options())
        .await
        .expect("failed to start copilot CLI");

    // start() calls verify_protocol_version(), so this should be set
    let version = client
        .protocol_version()
        .expect("protocol version not negotiated");
    assert!((2..=SDK_PROTOCOL_VERSION).contains(&version));

    client.ping(None).await.expect("ping failed");
    client.stop().await.expect("stop failed");
}

#[tokio::test]
#[ignore] // requires `copilot` CLI on PATH — run with `cargo test -- --ignored`
async fn force_stop_kills_real_child() {
    let client = Client::start(default_options())
        .await
        .expect("failed to start copilot CLI");

    let pid = client.pid().expect("expected a CLI child pid");
    assert!(pid > 0);

    // force_stop is synchronous and must not panic. After it returns,
    // pid() should report None because we've taken the child out of the
    // mutex.
    client.force_stop();
    assert!(client.pid().is_none());

    // Calling it again should be a no-op rather than panicking.
    client.force_stop();
}

/// Measures the latency of individual CLI operations that contribute to
/// session creation time. Run with:
///
///   cargo test -p github-copilot-sdk --test integration_test -- --ignored --nocapture
#[tokio::test]
#[ignore]
async fn cli_operation_latency() {
    // Cold start: spawn CLI process + verify protocol version
    let t0 = Instant::now();
    let client = Client::start(default_options())
        .await
        .expect("cold start failed");
    let cold_start = t0.elapsed();

    // Warm ping: RPC round-trip on an already-running process
    let t1 = Instant::now();
    client.ping(None).await.expect("warm ping failed");
    let warm_ping = t1.elapsed();

    // list_models: RPC that fetches available models from the CLI
    let t2 = Instant::now();
    let models = client.list_models().await.expect("list_models failed");
    let list_models = t2.elapsed();

    // Second list_models: does the CLI cache internally?
    let t2b = Instant::now();
    let _ = client.list_models().await.expect("list_models 2 failed");
    let list_models_2 = t2b.elapsed();

    client.stop().await.expect("stop first client failed");

    // Second cold start: measures process spawn cost when the binary is
    // already resolved and cached (no extraction overhead)
    let t3 = Instant::now();
    let client2 = Client::start(default_options())
        .await
        .expect("second cold start failed");
    let second_start = t3.elapsed();

    client2.stop().await.expect("stop second client failed");

    let (cli_path, source) = copilot_binary_with_source().expect("copilot binary not found");

    eprintln!();
    eprintln!("=== CLI operation latency ===");
    eprintln!("  binary: {} ({:?})", cli_path.display(), source);
    eprintln!("  cold Client::start:   {:>8.1?}", cold_start);
    eprintln!("  warm ping():          {:>8.1?}", warm_ping);
    eprintln!(
        "  list_models() ({:>2}):   {:>8.1?}",
        models.len(),
        list_models
    );
    eprintln!("  list_models() again:  {:>8.1?}", list_models_2);
    eprintln!("  second Client::start: {:>8.1?}", second_start);
    eprintln!();
}
