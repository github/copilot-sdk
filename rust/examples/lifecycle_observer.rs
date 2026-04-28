//! Observe lifecycle and event traffic without owning permission decisions.
//!
//! Demonstrates the channel-based observer APIs:
//!
//! - [`Client::subscribe_lifecycle`] — `tokio::sync::broadcast::Receiver` of
//!   every `session.lifecycle` notification (created / destroyed / errored /
//!   foreground / background). Filter by matching on `event.event_type` in
//!   the consumer.
//! - [`Session::subscribe`] — receiver for the per-session `session.event`
//!   stream (assistant messages, tool calls, permission prompts, etc.).
//!   Observe-only — the constructor handler still owns permission decisions.
//! - [`Client::state`] — current connection state without polling.
//! - [`Client::get_session_metadata`] — inspect a session without resuming
//!   it.
//! - [`Client::force_stop`] — synchronous shutdown for cleanup paths.
//!
//! Drop the receiver to unsubscribe — there is no separate cancel handle.
//! Slow consumers receive `RecvError::Lagged(n)` and resync on the next
//! event; they do not block the producer.
//!
//! ```sh
//! cargo run -p copilot-sdk --example lifecycle_observer
//! ```
//!
//! [`Client::subscribe_lifecycle`]: copilot::Client::subscribe_lifecycle
//! [`Session::subscribe`]: copilot::session::Session::subscribe
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

    // Wildcard lifecycle subscriber: see every session.lifecycle event,
    // counting deletions inline by filtering on event_type.
    let mut lifecycle_rx = client.subscribe_lifecycle();
    let deleted = Arc::new(AtomicUsize::new(0));
    let deleted_clone = Arc::clone(&deleted);
    let lifecycle_task = tokio::spawn(async move {
        while let Ok(event) = lifecycle_rx.recv().await {
            let summary = event
                .metadata
                .as_ref()
                .and_then(|m| m.summary.as_deref())
                .unwrap_or("<no summary>");
            println!(
                "[lifecycle:*] {:?} session={} summary={}",
                event.event_type, event.session_id, summary,
            );
            if event.event_type == SessionLifecycleEventType::Deleted {
                deleted_clone.fetch_add(1, Ordering::Relaxed);
            }
        }
    });

    let config = SessionConfig::default().with_handler(Arc::new(ApproveAllHandler));
    let session = client.create_session(config).await?;
    println!("[client] state after create: {:?}", client.state());

    // Per-session observer: see every assistant message, tool call, etc.
    // Subscribers fire alongside the constructor handler; they're great for
    // logging or metrics that should run regardless of how the handler
    // decides to respond.
    let mut session_rx = session.subscribe();
    let session_events = Arc::new(AtomicUsize::new(0));
    let session_events_clone = Arc::clone(&session_events);
    let session_task = tokio::spawn(async move {
        while let Ok(event) = session_rx.recv().await {
            session_events_clone.fetch_add(1, Ordering::Relaxed);
            println!("[session-event] {}", event.event_type);
        }
    });

    if let Some(metadata) = client.get_session_metadata(session.id()).await? {
        println!(
            "[metadata] id={} modified={} summary={}",
            metadata.session_id,
            metadata.modified_time,
            metadata.summary.as_deref().unwrap_or("<no summary>"),
        );
    }

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

    // Stopping the client closes the broadcast senders, so the consumer
    // tasks observe `RecvError::Closed` and exit cleanly.
    let _ = lifecycle_task.await;
    let _ = session_task.await;

    println!(
        "\n[summary] session_events={} sessions_deleted={}",
        session_events.load(Ordering::Relaxed),
        deleted.load(Ordering::Relaxed),
    );

    Ok(())
}
