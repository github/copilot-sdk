//! Observe lifecycle and event traffic without owning permission decisions.
//!
//! Demonstrates the observer-shaped APIs added in 0.1.0:
//!
//! - [`Client::on`] — fire-and-forget subscriber for *all* lifecycle events
//!   (`session.lifecycle` notifications: created / destroyed / errored / etc.).
//! - [`Client::on_event_type`] — same idea, but filtered to a single event
//!   type.
//! - [`Session::on`] — observe-only subscriber for the per-session
//!   `session.event` stream (assistant messages, tool calls, permission
//!   prompts, etc.). Cannot return a `HandlerResponse` — that's still the
//!   constructor handler's job.
//! - [`Client::state`] — current connection state without polling.
//! - [`Client::get_session_metadata`] — inspect a session without resuming
//!   it.
//! - [`Client::force_stop`] — synchronous shutdown for cleanup paths.
//!
//! Each subscriber returns an `Unsubscribe` handle. Drop it (or call
//! `.cancel()`) to stop receiving events. Subscribers cannot poison each
//! other: panics are isolated via `catch_unwind`.
//!
//! ```sh
//! cargo run -p copilot-sdk --example lifecycle_observer
//! ```
//!
//! [`Client::on`]: copilot::Client::on
//! [`Client::on_event_type`]: copilot::Client::on_event_type
//! [`Session::on`]: copilot::session::Session::on
//! [`Client::state`]: copilot::Client::state
//! [`Client::get_session_metadata`]: copilot::Client::get_session_metadata
//! [`Client::force_stop`]: copilot::Client::force_stop

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SendOptions, SessionConfig, SessionLifecycleEventType};
use copilot::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions::default()).await?;
    println!("[client] state: {:?}", client.state());

    // Wildcard lifecycle subscriber: see every session.lifecycle event.
    let _all = client.on(|event| {
        let summary = event
            .metadata
            .as_ref()
            .and_then(|m| m.summary.as_deref())
            .unwrap_or("<no summary>");
        println!(
            "[lifecycle:*] {:?} session={} summary={}",
            event.event_type, event.session_id, summary,
        );
    });

    // Typed subscriber: count how many sessions get deleted in this run.
    // Useful for metrics or debugging session leaks.
    let deleted = Arc::new(AtomicUsize::new(0));
    let deleted_clone = Arc::clone(&deleted);
    let _deleted_handle = client.on_event_type(SessionLifecycleEventType::Deleted, move |_event| {
        deleted_clone.fetch_add(1, Ordering::Relaxed);
    });

    let config = SessionConfig::default().with_handler(Arc::new(ApproveAllHandler));
    let session = client.create_session(config).await?;
    println!("[client] state after create: {:?}", client.state());

    // Per-session observer: see every assistant message, tool call, etc.
    // Observers fire *before* the constructor handler, so they're great for
    // logging or metrics that should run regardless of how the handler
    // decides to respond.
    let session_events = Arc::new(AtomicUsize::new(0));
    let session_events_clone = Arc::clone(&session_events);
    let _events_handle = session.on(move |event| {
        session_events_clone.fetch_add(1, Ordering::Relaxed);
        println!("[session-event] {}", event.event_type);
    });

    // Inspect the session without resuming it.
    if let Some(metadata) = client.get_session_metadata(session.id()).await? {
        println!(
            "[metadata] id={} modified={} summary={}",
            metadata.session_id,
            metadata.modified_time,
            metadata.summary.as_deref().unwrap_or("<no summary>"),
        );
    }

    // Drive a short interaction so subscribers have something to observe.
    session
        .send_and_wait(
            SendOptions::new("Say hello in five words or fewer.")
                .with_wait_timeout(Duration::from_secs(60)),
        )
        .await?;

    session.destroy().await?;

    // Synchronous shutdown — useful in panicking-cleanup paths or tests
    // where you don't have an async runtime available to await `stop()`.
    // For graceful shutdown in normal flow, prefer `client.stop().await`.
    client.force_stop();
    println!("[client] state after force_stop: {:?}", client.state());

    println!(
        "\n[summary] session_events={} sessions_deleted={}",
        session_events.load(Ordering::Relaxed),
        deleted.load(Ordering::Relaxed),
    );

    Ok(())
}
