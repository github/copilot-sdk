//! Subscription handles for observing session and lifecycle events.
//!
//! Returned by [`Session::subscribe`](crate::session::Session::subscribe) and
//! [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
//!
//! Each subscription is an opt-in **observer** of events that are also
//! delivered to the [`SessionHandler`](crate::handler::SessionHandler).
//! Subscribers receive a clone of every event but cannot influence
//! permission decisions, tool results, or anything else that requires
//! returning a [`HandlerResponse`](crate::handler::HandlerResponse).
//!
//! # Async iteration
//!
//! The subscription types implement [`tokio_stream::Stream`], so consumers
//! can use adapter combinators from [`tokio_stream::StreamExt`] or
//! `futures::StreamExt` (filtering, mapping, batching, racing with
//! `tokio::select!`, etc.) without learning the SDK's internal channel
//! choice. A simple `while let Ok(event) = sub.recv().await { ... }` loop
//! also works for callers who don't need the [`Stream`](tokio_stream::Stream)
//! surface.
//!
//! # Lag policy
//!
//! Each subscriber maintains its own internal queue. If a consumer cannot
//! keep up, the oldest events are dropped and the next call yields
//! [`Lagged`] reporting how many events were skipped. Slow subscribers do
//! not block the producer.

use std::pin::Pin;
use std::task::{Context, Poll};

use tokio::sync::broadcast::Receiver;
use tokio_stream::Stream;
use tokio_stream::StreamExt as _;
use tokio_stream::wrappers::BroadcastStream;
use tokio_stream::wrappers::errors::BroadcastStreamRecvError;

use crate::types::{SessionEvent, SessionLifecycleEvent};

/// The subscription fell behind the producer.
///
/// Reports the number of events that were dropped from this subscriber's
/// queue because the consumer didn't keep up. The subscription continues
/// after this error, starting from the next live event — callers who care
/// about lag should match on it and decide whether to resync, re-fetch, or
/// log and continue.
#[derive(Debug, Clone, Copy, PartialEq, Eq, thiserror::Error)]
#[error("subscription lagged behind by {0} events")]
pub struct Lagged(u64);

impl Lagged {
    /// Number of events skipped before this consumer could read them.
    pub fn skipped(&self) -> u64 {
        self.0
    }
}

/// Error returned by [`EventSubscription::recv`] and
/// [`LifecycleSubscription::recv`].
#[derive(Debug, thiserror::Error)]
#[non_exhaustive]
pub enum RecvError {
    /// The producer is gone — the session has shut down or the client has
    /// stopped. No further events will be delivered.
    #[error("subscription closed")]
    Closed,

    /// The subscriber fell behind. See [`Lagged`].
    #[error(transparent)]
    Lagged(#[from] Lagged),
}

macro_rules! define_subscription {
    (
        $(#[$meta:meta])*
        $name:ident, $item:ty $(,)?
    ) => {
        $(#[$meta])*
        #[must_use = "subscriptions are inert until polled"]
        pub struct $name {
            inner: BroadcastStream<$item>,
        }

        impl $name {
            pub(crate) fn new(rx: Receiver<$item>) -> Self {
                Self {
                    inner: BroadcastStream::new(rx),
                }
            }

            /// Receive the next event.
            ///
            /// Returns:
            ///
            /// - `Ok(event)` for the next delivered event.
            /// - `Err(`[`RecvError::Lagged`]`)` if the subscriber fell behind;
            ///   call `recv` again to continue from the next live event.
            /// - `Err(`[`RecvError::Closed`]`)` once the producer is gone.
            pub async fn recv(&mut self) -> Result<$item, RecvError> {
                match self.inner.next().await {
                    Some(Ok(event)) => Ok(event),
                    Some(Err(BroadcastStreamRecvError::Lagged(n))) => {
                        Err(RecvError::Lagged(Lagged(n)))
                    }
                    None => Err(RecvError::Closed),
                }
            }
        }

        impl Stream for $name {
            type Item = Result<$item, Lagged>;

            fn poll_next(
                mut self: Pin<&mut Self>,
                cx: &mut Context<'_>,
            ) -> Poll<Option<Self::Item>> {
                match Pin::new(&mut self.inner).poll_next(cx) {
                    Poll::Ready(Some(Ok(event))) => Poll::Ready(Some(Ok(event))),
                    Poll::Ready(Some(Err(BroadcastStreamRecvError::Lagged(n)))) => {
                        Poll::Ready(Some(Err(Lagged(n))))
                    }
                    Poll::Ready(None) => Poll::Ready(None),
                    Poll::Pending => Poll::Pending,
                }
            }
        }
    };
}

define_subscription! {
    /// Subscription to runtime events for a single
    /// [`Session`](crate::session::Session).
    ///
    /// Created by [`Session::subscribe`](crate::session::Session::subscribe).
    /// Implements [`Stream`] yielding `Result<SessionEvent, Lagged>`.
    /// Drop the value to unsubscribe; there is no separate cancel handle.
    EventSubscription, SessionEvent
}

define_subscription! {
    /// Subscription to lifecycle events on a [`Client`](crate::Client).
    ///
    /// Created by
    /// [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
    /// Implements [`Stream`] yielding `Result<SessionLifecycleEvent, Lagged>`.
    /// Drop the value to unsubscribe; there is no separate cancel handle.
    LifecycleSubscription, SessionLifecycleEvent
}

#[cfg(test)]
mod tests {
    use super::*;
    use tokio::sync::broadcast;

    fn make_event(id: &str) -> SessionEvent {
        SessionEvent {
            id: id.into(),
            timestamp: "2025-01-01T00:00:00Z".into(),
            parent_id: None,
            ephemeral: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: "noop".into(),
            data: serde_json::json!({}),
        }
    }

    #[tokio::test]
    async fn recv_yields_then_closes_on_drop_sender() {
        let (tx, rx) = broadcast::channel(8);
        let mut sub = EventSubscription::new(rx);
        tx.send(make_event("a")).unwrap();
        tx.send(make_event("b")).unwrap();
        drop(tx);

        assert_eq!(sub.recv().await.unwrap().id, "a");
        assert_eq!(sub.recv().await.unwrap().id, "b");
        assert!(matches!(sub.recv().await, Err(RecvError::Closed)));
    }

    #[tokio::test]
    async fn recv_surfaces_lag() {
        let (tx, rx) = broadcast::channel(2);
        let mut sub = EventSubscription::new(rx);
        for id in ["a", "b", "c", "d"] {
            tx.send(make_event(id)).unwrap();
        }
        match sub.recv().await {
            Err(RecvError::Lagged(l)) => assert_eq!(l.skipped(), 2),
            other => panic!("expected Lagged, got {other:?}"),
        }
        // Subscription continues with the live tail.
        assert_eq!(sub.recv().await.unwrap().id, "c");
        assert_eq!(sub.recv().await.unwrap().id, "d");
    }

    #[tokio::test]
    async fn stream_impl_matches_recv_semantics() {
        let (tx, rx) = broadcast::channel(8);
        let mut sub = EventSubscription::new(rx);
        tx.send(make_event("a")).unwrap();
        drop(tx);

        // poll_next path
        let next = sub.next().await;
        assert_eq!(next.unwrap().unwrap().id, "a");
        assert!(sub.next().await.is_none());
    }
}
