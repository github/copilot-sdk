//! Subscription handles for observing session and lifecycle events.
//!
//! Returned by [`Session::subscribe`](crate::session::Session::subscribe) and
//! [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
//!
//! Each subscription is an opt-in **observer** of events that are also
//! delivered to the per-event handlers installed on the session config
//! (see [`crate::handler`]). Subscribers receive a clone of every event but
//! cannot influence permission decisions, tool results, or any other event
//! whose handler return value affects the runtime.
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
//! # Resume bootstrap and lag policy
//!
//! The first subscription on a session returned by
//! [`Client::resume_session`](crate::Client::resume_session) may begin with
//! a lossless, ordered bootstrap prefix retained during resume startup. Once
//! that subscriber catches up, delivery switches atomically to the normal
//! live broadcast stream.
//!
//! Each live subscriber maintains its own finite queue. If a consumer cannot
//! keep up, the oldest live events are dropped and the next call yields
//! [`Lagged`](crate::subscription::Lagged) reporting how many events were skipped.
//! Slow subscribers do not block the producer.

use std::collections::VecDeque;
use std::fmt;
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use parking_lot::Mutex;
use tokio::sync::broadcast::{Receiver, Sender};
use tokio_stream::wrappers::BroadcastStream;
use tokio_stream::wrappers::errors::BroadcastStreamRecvError;
use tokio_stream::{Stream, StreamExt as _};

use crate::types::{SessionEvent, SessionLifecycleEvent};
use crate::{Custom, Repr};

/// The subscription fell behind the producer.
///
/// Reports the number of events that were dropped from this subscriber's
/// queue because the consumer didn't keep up. The subscription continues
/// after this error, starting from the next live event — callers who care
/// about lag should match on it and decide whether to resync, re-fetch, or
/// log and continue.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Lagged(pub(crate) u64);

impl Lagged {
    /// Number of events skipped before this consumer could read them.
    pub fn skipped(&self) -> u64 {
        self.0
    }
}

impl fmt::Display for Lagged {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "subscription lagged behind by {} events", self.0)
    }
}

impl std::error::Error for Lagged {}

/// Error kind for subscription receive operations.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[non_exhaustive]
pub enum RecvErrorKind {
    /// The producer is gone — the session has shut down or the client has
    /// stopped. No further events will be delivered.
    Closed,

    /// The subscriber fell behind. See [`Lagged`].
    Lagged(Lagged),
}

impl fmt::Display for RecvErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            RecvErrorKind::Closed => write!(f, "subscription closed"),
            RecvErrorKind::Lagged(l) => write!(f, "{l}"),
        }
    }
}

/// Error returned by [`crate::subscription::EventSubscription::recv`] and
/// [`crate::subscription::LifecycleSubscription::recv`].
#[derive(Debug)]
pub struct RecvError {
    repr: Repr<RecvErrorKind>,
}

impl RecvError {
    /// The [`RecvErrorKind`] of this error.
    pub fn kind(&self) -> &RecvErrorKind {
        match &self.repr {
            Repr::Simple(k) | Repr::SimpleMessage(k, ..) | Repr::Custom(Custom { kind: k, .. }) => {
                k
            }
        }
    }
}

impl fmt::Display for RecvError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match &self.repr {
            Repr::Simple(k) => write!(f, "{k}"),
            Repr::SimpleMessage(_, m) => write!(f, "{m}"),
            Repr::Custom(Custom { error, .. }) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for RecvError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            Repr::Custom(Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}

impl From<RecvErrorKind> for RecvError {
    fn from(kind: RecvErrorKind) -> Self {
        Self {
            repr: Repr::Simple(kind),
        }
    }
}

impl From<Lagged> for RecvError {
    fn from(lagged: Lagged) -> Self {
        Self::from(RecvErrorKind::Lagged(lagged))
    }
}

enum ResumeBootstrapState {
    Unclaimed(VecDeque<SessionEvent>),
    Claimed(VecDeque<SessionEvent>),
    Disabled,
}

/// Lossless, one-shot queue for routed events emitted before the first
/// post-resume session subscription catches up.
pub(crate) struct ResumeBootstrap {
    state: Mutex<ResumeBootstrapState>,
}

impl ResumeBootstrap {
    pub(crate) fn new() -> Arc<Self> {
        Arc::new(Self {
            state: Mutex::new(ResumeBootstrapState::Unclaimed(VecDeque::new())),
        })
    }

    pub(crate) fn publish(&self, event_tx: &Sender<SessionEvent>, event: SessionEvent) {
        let mut state = self.state.lock();
        match &mut *state {
            ResumeBootstrapState::Unclaimed(events) | ResumeBootstrapState::Claimed(events) => {
                events.push_back(event)
            }
            ResumeBootstrapState::Disabled => {
                let _ = event_tx.send(event);
            }
        }
    }

    pub(crate) fn subscribe(
        self: &Arc<Self>,
        event_tx: &Sender<SessionEvent>,
    ) -> EventSubscription {
        let mut state = self.state.lock();
        let receiver = event_tx.subscribe();
        let bootstrap = match &mut *state {
            ResumeBootstrapState::Unclaimed(events) => {
                let events = std::mem::take(events);
                *state = ResumeBootstrapState::Claimed(events);
                Some(self.clone())
            }
            ResumeBootstrapState::Claimed(_) | ResumeBootstrapState::Disabled => None,
        };
        EventSubscription::with_bootstrap(receiver, bootstrap)
    }

    fn pop(&self) -> Option<SessionEvent> {
        let mut state = self.state.lock();
        let ResumeBootstrapState::Claimed(events) = &mut *state else {
            return None;
        };
        if let Some(event) = events.pop_front() {
            return Some(event);
        }
        *state = ResumeBootstrapState::Disabled;
        None
    }

    fn abandon(&self) {
        let mut state = self.state.lock();
        if matches!(*state, ResumeBootstrapState::Claimed(_)) {
            *state = ResumeBootstrapState::Disabled;
        }
    }
}

/// Subscription to runtime events for a single
/// [`Session`](crate::session::Session).
///
/// Created by [`Session::subscribe`](crate::session::Session::subscribe).
/// Implements [`Stream`] yielding `Result<SessionEvent, Lagged>`.
/// Drop the value to unsubscribe; there is no separate cancel handle.
#[must_use = "subscriptions are inert until polled"]
pub struct EventSubscription {
    inner: BroadcastStream<SessionEvent>,
    bootstrap: Option<Arc<ResumeBootstrap>>,
}

impl EventSubscription {
    pub(crate) fn new(rx: Receiver<SessionEvent>) -> Self {
        Self::with_bootstrap(rx, None)
    }

    fn with_bootstrap(rx: Receiver<SessionEvent>, bootstrap: Option<Arc<ResumeBootstrap>>) -> Self {
        Self {
            inner: BroadcastStream::new(rx),
            bootstrap,
        }
    }

    fn next_bootstrap_event(&mut self) -> Option<SessionEvent> {
        let event = self
            .bootstrap
            .as_ref()
            .and_then(|bootstrap| bootstrap.pop());
        if event.is_none() {
            self.bootstrap = None;
        }
        event
    }

    /// Receive the next event.
    ///
    /// Returns:
    ///
    /// - `Ok(event)` for the next delivered event.
    /// - `Err(`[`RecvError`]`)` with [`RecvError::kind()`] [`RecvErrorKind::Lagged`] if the subscriber fell behind;
    ///   call `recv` again to continue from the next live event.
    /// - `Err(`[`RecvError`]`)` with [`RecvError::kind()`] [`RecvErrorKind::Closed`] once the producer is gone.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** Bootstrap events are removed before the future's
    /// first suspension point. Once live delivery begins, this wraps a
    /// `tokio::sync::broadcast::Receiver` via `BroadcastStream`, which is
    /// cancel-safe by design.
    pub async fn recv(&mut self) -> Result<SessionEvent, RecvError> {
        if let Some(event) = self.next_bootstrap_event() {
            return Ok(event);
        }
        match self.inner.next().await {
            Some(Ok(event)) => Ok(event),
            Some(Err(BroadcastStreamRecvError::Lagged(n))) => Err(Lagged(n).into()),
            None => Err(RecvErrorKind::Closed.into()),
        }
    }
}

impl Stream for EventSubscription {
    type Item = Result<SessionEvent, Lagged>;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        if let Some(event) = self.next_bootstrap_event() {
            return Poll::Ready(Some(Ok(event)));
        }
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

impl Drop for EventSubscription {
    fn drop(&mut self) {
        if let Some(bootstrap) = self.bootstrap.take() {
            bootstrap.abandon();
        }
    }
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
            /// - `Err(`[`RecvError`]`)` with [`RecvError::kind()`] [`RecvErrorKind::Lagged`] if the subscriber fell behind;
            ///   call `recv` again to continue from the next live event.
            /// - `Err(`[`RecvError`]`)` with [`RecvError::kind()`] [`RecvErrorKind::Closed`] once the producer is gone.
            ///
            /// # Cancel safety
            ///
            /// **Cancel-safe.** Wraps a `tokio::sync::broadcast::Receiver`
            /// via `BroadcastStream`; both are cancel-safe by design.
            /// Dropping the future before completion is harmless — events
            /// already buffered for this subscriber remain available on
            /// the next `recv` call.
            pub async fn recv(&mut self) -> Result<$item, RecvError> {
                match self.inner.next().await {
                    Some(Ok(event)) => Ok(event),
                    Some(Err(BroadcastStreamRecvError::Lagged(n))) => {
                        Err(Lagged(n).into())
                    }
                    None => Err(RecvErrorKind::Closed.into()),
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
    use tokio::sync::broadcast;

    use super::*;

    fn make_event(id: &str) -> SessionEvent {
        SessionEvent {
            id: id.into(),
            timestamp: "2025-01-01T00:00:00Z".into(),
            parent_id: None,
            ephemeral: None,
            agent_id: None,
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
        assert!(matches!(
            sub.recv().await.unwrap_err().kind(),
            RecvErrorKind::Closed
        ));
    }

    #[tokio::test]
    async fn recv_surfaces_lag() {
        let (tx, rx) = broadcast::channel(2);
        let mut sub = EventSubscription::new(rx);
        for id in ["a", "b", "c", "d"] {
            tx.send(make_event(id)).unwrap();
        }
        let err = sub.recv().await.expect_err("expected a Lagged error");
        let RecvErrorKind::Lagged(l) = err.kind() else {
            panic!("expected Lagged, got {:?}", err.kind());
        };
        assert_eq!(l.skipped(), 2);
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

    #[tokio::test]
    async fn resume_bootstrap_is_lossless_and_ordered_before_live_events() {
        let (tx, _) = broadcast::channel(1);
        let bootstrap = ResumeBootstrap::new();
        for index in 0..600 {
            bootstrap.publish(&tx, make_event(&format!("bootstrap-{index}")));
        }

        let mut sub = bootstrap.subscribe(&tx);
        bootstrap.publish(&tx, make_event("bootstrap-after-subscribe"));

        for index in 0..600 {
            assert_eq!(sub.recv().await.unwrap().id, format!("bootstrap-{index}"));
        }
        assert_eq!(sub.recv().await.unwrap().id, "bootstrap-after-subscribe");

        assert!(sub.next_bootstrap_event().is_none());
        bootstrap.publish(&tx, make_event("live"));
        assert_eq!(sub.recv().await.unwrap().id, "live");
    }

    #[tokio::test]
    async fn only_first_subscriber_claims_resume_bootstrap() {
        let (tx, _) = broadcast::channel(8);
        let bootstrap = ResumeBootstrap::new();
        bootstrap.publish(&tx, make_event("bootstrap"));

        let mut first = bootstrap.subscribe(&tx);
        let mut second = bootstrap.subscribe(&tx);

        assert_eq!(first.recv().await.unwrap().id, "bootstrap");
        assert!(first.next_bootstrap_event().is_none());
        bootstrap.publish(&tx, make_event("live"));

        assert_eq!(first.recv().await.unwrap().id, "live");
        assert_eq!(second.recv().await.unwrap().id, "live");
    }

    #[tokio::test]
    async fn dropping_bootstrap_owner_activates_live_delivery() {
        let (tx, _) = broadcast::channel(8);
        let bootstrap = ResumeBootstrap::new();
        bootstrap.publish(&tx, make_event("discarded-bootstrap"));

        let first = bootstrap.subscribe(&tx);
        let mut second = bootstrap.subscribe(&tx);
        drop(first);

        bootstrap.publish(&tx, make_event("live"));
        assert_eq!(second.recv().await.unwrap().id, "live");
    }

    #[tokio::test]
    async fn ordinary_subscription_does_not_replay_events_without_a_receiver() {
        let (tx, _) = broadcast::channel(8);
        assert!(tx.send(make_event("before-subscribe")).is_err());

        let mut sub = EventSubscription::new(tx.subscribe());
        tx.send(make_event("live")).unwrap();
        assert_eq!(sub.recv().await.unwrap().id, "live");
    }
}
