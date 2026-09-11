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
use tokio::sync::broadcast::{Receiver, Sender, WeakSender};
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
    live: WeakSender<SessionEvent>,
}

impl ResumeBootstrap {
    pub(crate) fn new(event_tx: &Sender<SessionEvent>) -> Arc<Self> {
        Arc::new(Self {
            state: Mutex::new(ResumeBootstrapState::Unclaimed(VecDeque::new())),
            live: event_tx.downgrade(),
        })
    }

    pub(crate) fn publish(&self, event_tx: &Sender<SessionEvent>, event: SessionEvent) {
        let mut state = self.state.lock();
        match &mut *state {
            ResumeBootstrapState::Unclaimed(events) | ResumeBootstrapState::Claimed(events) => {
                events.push_back(event.clone());
            }
            ResumeBootstrapState::Disabled => {}
        }
        // Other observers remain live even while the bootstrap owner catches up.
        let _ = event_tx.send(event);
    }

    pub(crate) fn subscribe(
        self: &Arc<Self>,
        event_tx: &Sender<SessionEvent>,
    ) -> EventSubscription {
        let mut state = self.state.lock();
        match &mut *state {
            ResumeBootstrapState::Unclaimed(events) => {
                let events = std::mem::take(events);
                *state = ResumeBootstrapState::Claimed(events);
                EventSubscription {
                    inner: None,
                    bootstrap: Some(self.clone()),
                }
            }
            ResumeBootstrapState::Claimed(_) | ResumeBootstrapState::Disabled => {
                EventSubscription::new(event_tx.subscribe())
            }
        }
    }

    fn pop(&self, live: &mut Option<BroadcastStream<SessionEvent>>) -> Option<SessionEvent> {
        let mut state = self.state.lock();
        let ResumeBootstrapState::Claimed(events) = &mut *state else {
            return None;
        };
        if let Some(event) = events.pop_front() {
            return Some(event);
        }
        // Installing the live receiver under the publication lock makes the
        // empty-queue boundary gap-free without replaying broadcast duplicates.
        *live = self
            .live
            .upgrade()
            .map(|sender| BroadcastStream::new(sender.subscribe()));
        *state = ResumeBootstrapState::Disabled;
        None
    }

    pub(crate) fn release_unclaimed(&self) {
        let mut state = self.state.lock();
        if matches!(*state, ResumeBootstrapState::Unclaimed(_)) {
            *state = ResumeBootstrapState::Disabled;
        }
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
/// A resume bootstrap is claimed when this subscription is created, not when
/// it is first polled. Dropping its owner discards any unread bootstrap events.
#[must_use = "dropping the subscription unsubscribes and discards any owned resume bootstrap backlog"]
pub struct EventSubscription {
    inner: Option<BroadcastStream<SessionEvent>>,
    bootstrap: Option<Arc<ResumeBootstrap>>,
}

impl EventSubscription {
    pub(crate) fn new(rx: Receiver<SessionEvent>) -> Self {
        Self {
            inner: Some(BroadcastStream::new(rx)),
            bootstrap: None,
        }
    }

    fn next_bootstrap_event(&mut self) -> Option<SessionEvent> {
        let event = self
            .bootstrap
            .as_ref()
            .and_then(|bootstrap| bootstrap.pop(&mut self.inner));
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
        match self.next().await {
            Some(Ok(event)) => Ok(event),
            Some(Err(lagged)) => Err(lagged.into()),
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
        let Some(inner) = self.inner.as_mut() else {
            return Poll::Ready(None);
        };
        match Pin::new(inner).poll_next(cx) {
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
        let bootstrap = ResumeBootstrap::new(&tx);
        for index in 0..600 {
            bootstrap.publish(&tx, make_event(&format!("bootstrap-{index}")));
        }

        let mut sub = bootstrap.subscribe(&tx);
        for index in 600..1200 {
            bootstrap.publish(&tx, make_event(&format!("bootstrap-{index}")));
        }

        for index in 0..1200 {
            assert_eq!(sub.recv().await.unwrap().id, format!("bootstrap-{index}"));
        }

        assert!(sub.next_bootstrap_event().is_none());
        bootstrap.publish(&tx, make_event("live"));
        assert_eq!(sub.recv().await.unwrap().id, "live");
    }

    #[tokio::test]
    async fn only_first_subscriber_claims_resume_bootstrap() {
        let (tx, _) = broadcast::channel(8);
        let bootstrap = ResumeBootstrap::new(&tx);
        bootstrap.publish(&tx, make_event("bootstrap"));

        let mut first = bootstrap.subscribe(&tx);
        let mut second = bootstrap.subscribe(&tx);

        bootstrap.publish(&tx, make_event("during-catchup"));
        assert_eq!(second.recv().await.unwrap().id, "during-catchup");
        assert_eq!(first.recv().await.unwrap().id, "bootstrap");
        assert_eq!(first.recv().await.unwrap().id, "during-catchup");
        assert!(first.next_bootstrap_event().is_none());
        bootstrap.publish(&tx, make_event("live"));

        assert_eq!(first.recv().await.unwrap().id, "live");
        assert_eq!(second.recv().await.unwrap().id, "live");
    }

    #[tokio::test]
    async fn concurrent_subscribers_claim_bootstrap_exactly_once() {
        use futures_util::FutureExt;

        let (tx, _) = broadcast::channel(8);
        let bootstrap = ResumeBootstrap::new(&tx);
        bootstrap.publish(&tx, make_event("bootstrap"));
        let barrier = std::sync::Barrier::new(2);
        let mut subscriptions = std::thread::scope(|scope| {
            let subscribe = || {
                barrier.wait();
                bootstrap.subscribe(&tx)
            };
            let first = scope.spawn(subscribe);
            let second = scope.spawn(subscribe);
            [first.join().unwrap(), second.join().unwrap()]
        });

        let mut owners = 0;
        for sub in &mut subscriptions {
            if let Some(event) = sub.recv().now_or_never() {
                assert_eq!(event.unwrap().id, "bootstrap");
                owners += 1;
            }
        }
        assert_eq!(owners, 1);

        bootstrap.publish(&tx, make_event("during-catchup"));
        for sub in &mut subscriptions {
            assert_eq!(sub.recv().await.unwrap().id, "during-catchup");
            assert!(sub.recv().now_or_never().is_none());
        }
        bootstrap.publish(&tx, make_event("live"));
        for sub in &mut subscriptions {
            assert_eq!(sub.recv().await.unwrap().id, "live");
            assert!(sub.recv().now_or_never().is_none());
        }
    }

    #[tokio::test]
    async fn bootstrap_stream_drains_before_closing_without_retaining_the_sender() {
        let (tx, _) = broadcast::channel(1);
        let bootstrap = ResumeBootstrap::new(&tx);
        bootstrap.publish(&tx, make_event("first"));
        let mut events = bootstrap.subscribe(&tx);
        bootstrap.publish(&tx, make_event("second"));
        drop(tx);

        assert_eq!(events.next().await.unwrap().unwrap().id, "first");
        assert_eq!(events.next().await.unwrap().unwrap().id, "second");
        assert!(events.next().await.is_none());
        assert!(matches!(
            events.recv().await.unwrap_err().kind(),
            RecvErrorKind::Closed
        ));
    }

    #[tokio::test]
    async fn bootstrap_handoff_is_cancel_safe_and_preserves_live_lag() {
        use futures_util::FutureExt;

        let (tx, _) = broadcast::channel(1);
        let bootstrap = ResumeBootstrap::new(&tx);
        let mut events = bootstrap.subscribe(&tx);
        // Poll through the empty bootstrap and cancel the pending live receive.
        assert!(events.recv().now_or_never().is_none());
        bootstrap.publish(&tx, make_event("overwritten"));
        bootstrap.publish(&tx, make_event("live"));
        assert!(matches!(
            events.recv().await.unwrap_err().kind(),
            RecvErrorKind::Lagged(_)
        ));
        assert_eq!(events.recv().await.unwrap().id, "live");
    }

    #[tokio::test]
    async fn publication_racing_catchup_has_no_gap_or_duplicate() {
        for _ in 0..32 {
            let (tx, _) = broadcast::channel(1024);
            let bootstrap = ResumeBootstrap::new(&tx);
            bootstrap.publish(&tx, make_event("prefix"));
            let mut events = bootstrap.subscribe(&tx);
            assert_eq!(events.recv().await.unwrap().id, "prefix");

            let barrier = Arc::new(std::sync::Barrier::new(2));
            let producer = std::thread::spawn({
                let barrier = barrier.clone();
                let bootstrap = bootstrap.clone();
                move || {
                    barrier.wait();
                    for index in 0..600 {
                        bootstrap.publish(&tx, make_event(&format!("event-{index}")));
                    }
                }
            });
            barrier.wait();
            for index in 0..600 {
                let event = tokio::time::timeout(std::time::Duration::from_secs(5), events.recv())
                    .await
                    .unwrap()
                    .unwrap();
                assert_eq!(event.id, format!("event-{index}"));
            }
            producer.join().unwrap();
            assert!(events.next().await.is_none());
        }
    }

    #[tokio::test]
    async fn shutdown_releases_only_unclaimed_bootstrap() {
        let (tx, _) = broadcast::channel(1);
        let bootstrap = ResumeBootstrap::new(&tx);
        bootstrap.publish(&tx, make_event("unclaimed"));
        bootstrap.release_unclaimed();
        assert!(matches!(
            *bootstrap.state.lock(),
            ResumeBootstrapState::Disabled
        ));

        let claimed = ResumeBootstrap::new(&tx);
        claimed.publish(&tx, make_event("claimed"));
        let mut events = claimed.subscribe(&tx);
        claimed.release_unclaimed();
        drop(tx);
        assert_eq!(events.recv().await.unwrap().id, "claimed");
        assert!(events.next().await.is_none());
    }

    #[tokio::test]
    async fn dropping_bootstrap_owner_activates_live_delivery() {
        let (tx, _) = broadcast::channel(8);
        let bootstrap = ResumeBootstrap::new(&tx);
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
